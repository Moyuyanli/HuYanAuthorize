package cn.chahuyun.authorize

import cn.chahuyun.authorize.constant.AuthPerm
import cn.chahuyun.authorize.entity.Perm
import cn.chahuyun.authorize.exception.ExceptionHandle
import cn.chahuyun.authorize.exception.ExceptionHandleApi
import cn.chahuyun.authorize.listener.ListenerManager
import cn.chahuyun.hibernateplus.HibernateFactory
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin
import net.mamoe.mirai.console.plugin.name
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder

/**
 * 壶言授权核心服务
 * 提供事件注册、权限管理等核心功能
 */
object AuthorizeServer {

    /**
     * 注册消息事件监听器
     *
     * @param plugin 插件实例
     * @param packageName 需要扫描的包名
     * @param exceptionHandle 异常处理器（可选）
     * @param prefix 指令前缀（可选）
     */
    @JvmOverloads
    fun registerEvents(
        plugin: JvmPlugin,
        packageName: String,
        exceptionHandle: ExceptionHandleApi = ExceptionHandle(),
        prefix: String = "",
        useKsp: Boolean = false
    ) {
        val eventChannel = GlobalEventChannel
        val classes = scanPackage(plugin.javaClass.classLoader, packageName)

        if (classes.isEmpty()) {
            HuYanAuthorize.log.warning("在包 $packageName 中未找到带有 @EventComponent 的类")
            return
        }

        ListenerManager.register(
            classes,
            eventChannel.filterIsInstance(MessageEvent::class),
            exceptionHandle,
            prefix,
            plugin,
            forceKsp = useKsp
        )
    }

    /**
     * 注册权限码
     *
     * @param plugin 插件实例
     * @param perms 权限信息
     */
    fun registerPermissions(plugin: JvmPlugin, vararg perms: Perm) {
        val validPerms = perms.filter {
            if (listOf(AuthPerm.NULL, AuthPerm.ADMIN, AuthPerm.OWNER).contains(it.code)) {
                HuYanAuthorize.log.warning("权限码 ${it.code} 是内置关键词，无法手动注册")
                false
            } else true
        }

        validPerms.forEach { registerSinglePermission(plugin, it) }
    }

    /**
     * 内部注册权限码通道
     * 仅供核心插件使用
     */
    internal fun registerPermissionsInternal(plugin: JvmPlugin, vararg perms: Perm) {
        perms.forEach { registerSinglePermission(plugin, it) }
    }

    /**
     * 注册单个权限
     */
    private fun registerSinglePermission(plugin: JvmPlugin, perm: Perm) {
        val code = perm.code ?: error("权限code为空!")
        val existing = HibernateFactory.selectOne(Perm::class.java, "code", code)
        if (existing != null) {
            HuYanAuthorize.log.debug("权限码 $code 已由插件 ${existing.createPlugin} 注册")
        } else {
            perm.setCreatePlugin(plugin.name)
            if (HibernateFactory.merge(perm).id != 0) {
                HuYanAuthorize.log.debug("权限码 $code 注册成功")
            } else {
                HuYanAuthorize.log.error("权限码 $code 注册失败")
            }
        }
    }

    /**
     * 检查权限是否存在
     */
    fun isPermissionRegistered(code: String): Boolean {
        return HibernateFactory.selectOne(Perm::class.java, "code", code) != null
    }

    private fun scanPackage(loader: ClassLoader, packageName: String): Set<Class<*>> {
        val reflections = Reflections(
            ConfigurationBuilder()
                .forPackage(packageName, loader)
                .addClassLoaders(loader)
        )
        return reflections.get(
            Scanners.TypesAnnotated.with(EventComponent::class.java).asClass<Any>(loader)
        )
    }
}

