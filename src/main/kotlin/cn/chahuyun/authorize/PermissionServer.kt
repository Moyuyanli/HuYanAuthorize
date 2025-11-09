package cn.chahuyun.authorize

import cn.chahuyun.authorize.HuYanAuthorize.log
import cn.chahuyun.authorize.constant.AuthPerm
import cn.chahuyun.authorize.entity.Perm
import cn.chahuyun.authorize.exception.ExceptionHandle
import cn.chahuyun.authorize.exception.ExceptionHandleApi
import cn.chahuyun.hibernateplus.HibernateFactory
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin
import net.mamoe.mirai.console.plugin.name
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder


object PermissionServer {

    /**
     * 注册消息事件
     *
     * 简单方法
     *
     * @author moyuyanli
     */
    fun registerMessageEvent(plugin: JvmPlugin, packageName: String) {
        registerMessageEvent(plugin, packageName, ExceptionHandle(), "")
    }

    /**
     * 注册消息事件
     *
     * 带指令前缀的方法
     *
     * @author moyuyanli
     */
    fun registerMessageEvent(plugin: JvmPlugin, packageName: String, prefix: String) {
        registerMessageEvent(plugin, packageName, ExceptionHandle(), prefix)
    }

    /**
     * 注册消息事件
     *
     * 带错误收集的方法
     *
     * @author moyuyanli
     */
    fun registerMessageEvent(plugin: JvmPlugin, packageName: String, exceptionHandle: ExceptionHandleApi) {
        registerMessageEvent(plugin, packageName, exceptionHandle, "")
    }

    /**
     * 注册消息事件
     *
     * 全参数方法
     *
     * 消息匹配前缀可为空，为空则没有前缀匹配
     *
     * @author moyuyanli
     * @param plugin 插件
     * @param packageName 事件包
     * @param exceptionHandle 自定义异常处理
     * @param prefix 消息匹配前缀
     */
    fun registerMessageEvent(
        plugin: JvmPlugin,
        packageName: String,
        exceptionHandle: ExceptionHandleApi = ExceptionHandle(),
        prefix: String = "",
    ) {
        val eventChannel = GlobalEventChannel
        val classes = scanPackage(plugin.javaClass.classLoader, packageName)

        if (classes.isEmpty()) throw RuntimeException("注册类扫描为空!")

        MessageFilter.register(classes, eventChannel.filterIsInstance(MessageEvent::class), exceptionHandle, prefix)
    }

    private fun scanPackage(loader: ClassLoader, packageName: String): MutableSet<Class<*>> {
        // 创建ConfigurationBuilder并设置自定义ClassLoader
        val reflections = Reflections(
            ConfigurationBuilder()
                .forPackage(packageName, loader)
                .addClassLoaders(loader)
        )

        val queryFunction = Scanners.TypesAnnotated.of(
            EventComponent::class.java
        ).asClass<Any>(loader)

        return queryFunction.apply(reflections.store)
    }

    /**
     * 注册一个权限
     *
     * 注:
     * null;admin;owner;
     *
     * 无法注册，请选择别的code。
     *
     * 注册方法
     * kotlin调用
     * ```kotlin
     * PermissionServer.registerPermCode(
     *      this,
     *      Perm("admin","管理员权限")
     * )
     * ```
     * java调用
     * ```java
     * PermissionServer.INSTANCE.registerPermCode(
     *                 this,
     *                 new Perm("admin","管理员权限")
     *                 );
     * ```
     *
     *
     * @param plugin 注册插件
     * @param perms 权限
     */
    fun registerPermCode(plugin: JvmPlugin, vararg perms: Perm) {
        val list = mutableListOf(AuthPerm.NULL, AuthPerm.ADMIN, AuthPerm.OWNER)

        val filter = perms.filter {
            if (list.contains(it.code)) {
                log.warning("权限 ${it.code} 是内置关键词，无法注册!")
                return@filter false
            } else {
                return@filter true
            }
        }

        registerPerm(plugin, filter)
    }

    private fun registerPerm(plugin: JvmPlugin, perms: List<Perm>) {
        perms.forEach { perm ->
            HibernateFactory.selectOne(Perm::class.java, "code", perm.code)?.let {
                log.debug("权限 ${it.code} 已注册! 注册插件: ${it.createPlugin}")
            } ?: if (HibernateFactory.merge(perm.setCreatePlugin(plugin.name)).id != 0) {
                log.debug("权限 ${perm.code} 注册成功!")
            } else {
                error("权限 ${perm.code} 注册失败!")
            }
        }
    }

    /**
     * 检查这个权限code是否已经注册
     *
     * @param code 权限code
     * @return true 存在
     * @author Moyuyanli
     * @date 2024/8/29 11:32
     */
    fun checkPermExist(code: String): Boolean {
        return HibernateFactory.selectOne(Perm::class.java, "code", code) != null
    }




    /**
     * 请勿调用!
     */
    internal fun authorizePermRegister() {
        registerPerm(
            HuYanAuthorize,
            mutableListOf(
                Perm(
                    code = AuthPerm.OWNER,
                    description = "主人权限"
                ),
                Perm(
                    code = AuthPerm.ADMIN,
                    description = "管理员权限"
                ),
            )
        )
    }
}


