package cn.chahuyun.authorize.listener

import cn.chahuyun.authorize.HuYanAuthorize.log
import cn.chahuyun.authorize.exception.ExceptionHandleApi
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.events.MessageEvent
import java.util.*

/**
 * 监听管理器
 * 负责选择使用反射还是 KSP 生成的注册器
 */
object ListenerManager {

    fun register(
        classList: Set<Class<*>>,
        channel: EventChannel<MessageEvent>,
        handleApi: ExceptionHandleApi,
        prefix: String,
        plugin: JvmPlugin,
        forceKsp: Boolean = false
    ) {
        val filter = ListenerFilter(prefix)

        // forceKsp=true：强制 KSP；false：走反射
        val useKsp = forceKsp

        var kspSuccess = false
        if (useKsp) {
            // 使用当前插件 classLoader 加载 providers，避免跨插件误加载
            val loader = ServiceLoader.load(GeneratedListenerRegistrar::class.java, plugin.javaClass.classLoader)
            var foundAny = false
            val seen = HashSet<String>()
            for (registrar in loader) {
                foundAny = true
                val registrarName = registrar.javaClass.name
                if (!seen.add(registrarName)) {
                    // 避免重复订阅
                    continue
                }
                // 只要能发现任何编译期注册器，就优先使用 KSP 通道；避免部分注册成功后再回退反射导致重复订阅
                kspSuccess = true
                try {
                    log.debug("检测到编译期生成的监听注册器: ${registrar.javaClass.simpleName}")
                    registrar.register(channel, filter, handleApi, plugin)
                } catch (e: Throwable) {
                    // 不回退到反射（否则可能造成重复注册）；保留日志以便定位生成器/注册器错误
                    log.error("KSP 注册器 ${registrar.javaClass.name} 执行失败：${e.message}", e)
                }
            }

            // 强制 KSP 且未找到任何生成注册器，直接报错阻止继续（便于定位“明明开了 KSP 却没生效/打包不包含生成物”）
            if (forceKsp && !foundAny) {
                error("强制使用 KSP 注册事件，但未找到任何 GeneratedListenerRegistrar（请确认已启用 KSP 并重新构建插件产物）")
            }
        }

        // 如果 KSP 未成功（没有找到生成类），或者配置强制使用反射
        if (!kspSuccess) {
            if (forceKsp) {
                error("强制使用 KSP 注册事件，但 KSP 注册流程未成功（请查看启动日志中的 KSP 注册器报错）")
            }
            log.info("使用反射方式注册消息监听器")
            ReflectionListenerRegistrar(classList).register(channel, filter, handleApi, plugin)
        }
    }
}

