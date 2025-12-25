package cn.chahuyun.authorize.listener

import cn.chahuyun.authorize.HuYanAuthorize.log
import cn.chahuyun.authorize.config.AuthorizeConfig
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
        plugin: JvmPlugin
    ) {
        val filter = ListenerFilter(prefix)

        // 获取配置，决定是否使用 KSP
        val useKsp = AuthorizeConfig.useKsp

        var kspSuccess = false
        if (useKsp) {
            try {
                val loader = ServiceLoader.load(GeneratedListenerRegistrar::class.java)
                for (registrar in loader) {
                    log.debug("检测到编译期生成的监听注册器: ${registrar.javaClass.simpleName}")
                    registrar.register(channel, filter, handleApi, plugin)
                    kspSuccess = true
                }
            } catch (e: Throwable) {
                log.debug("尝试使用 KSP 注册器失败: ${e.message}")
            }
        }

        // 如果 KSP 未成功（没有找到生成类），或者配置强制使用反射
        if (!kspSuccess) {
            log.info("使用反射方式注册消息监听器")
            ReflectionListenerRegistrar(classList).register(channel, filter, handleApi, plugin)
        }
    }
}

