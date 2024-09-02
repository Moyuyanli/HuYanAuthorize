package cn.chahuyun.authorize

import cn.chahuyun.authorize.constant.PermConstant
import cn.chahuyun.authorize.entity.Perm
import cn.chahuyun.authorize.exception.ExceptionHandle
import cn.chahuyun.authorize.exception.ExceptionHandleApi
import cn.chahuyun.authorize.utils.Log.debug
import cn.chahuyun.authorize.utils.Log.warning
import cn.chahuyun.hibernateplus.HibernateFactory
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin
import net.mamoe.mirai.console.plugin.name
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder


object PermissionServer {

    fun init(
        plugin: JvmPlugin,
        packageName: String,
        exceptionHandle: ExceptionHandleApi = ExceptionHandle(),
    ) {
        val eventChannel = GlobalEventChannel
        val classes = scanPackage(plugin.javaClass.classLoader, packageName)

        if (classes.isEmpty()) throw RuntimeException("注册类扫描为空!")

        MessageFilter.register(classes, eventChannel.filterIsInstance(MessageEvent::class), exceptionHandle)
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
     *
     *
     * @param plugin 注册插件
     * @param perms 权限
     */
    fun registerPermCode(plugin: JvmPlugin, vararg perms: Perm) {
        val list = mutableListOf(PermConstant.NULL, PermConstant.ADMIN, PermConstant.OWNER)

        val filter = perms.filter {
            if (list.contains(it.code)) {
                warning("权限 ${it.code} 是内置关键词，无法注册!")
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
                debug("权限 ${it.code} 已注册! 注册插件: ${it.createPlugin}")
            } ?: if (HibernateFactory.merge(perm.setCreatePlugin(plugin.name)).id != 0) {
                debug("权限 ${perm.code} 注册成功!")
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
            HuYanAuthorize.INSTANCE,
            mutableListOf(
                Perm(
                    code = PermConstant.OWNER,
                    description = "主人权限"
                ),
                Perm(
                    code = PermConstant.ADMIN,
                    description = "管理员权限"
                ),
            )
        )
    }
}


