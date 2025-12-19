package cn.chahuyun.authorize

import cn.chahuyun.authorize.entity.Perm
import cn.chahuyun.authorize.exception.ExceptionHandle
import cn.chahuyun.authorize.exception.ExceptionHandleApi
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin

/**
 * 权限服务入口 (已弃用，请使用 [AuthorizeServer])
 */
@Deprecated("请迁移至 AuthorizeServer", ReplaceWith("AuthorizeServer"))
object PermissionServer {

    @JvmStatic
    fun registerMessageEvent(plugin: JvmPlugin, packageName: String) {
        AuthorizeServer.registerEvents(plugin, packageName)
    }

    @JvmStatic
    fun registerMessageEvent(plugin: JvmPlugin, packageName: String, prefix: String) {
        AuthorizeServer.registerEvents(plugin, packageName, prefix = prefix)
    }

    @JvmStatic
    fun registerMessageEvent(plugin: JvmPlugin, packageName: String, exceptionHandle: ExceptionHandleApi) {
        AuthorizeServer.registerEvents(plugin, packageName, exceptionHandle = exceptionHandle)
    }

    @JvmStatic
    fun registerMessageEvent(
        plugin: JvmPlugin,
        packageName: String,
        exceptionHandle: ExceptionHandleApi = ExceptionHandle(),
        prefix: String = "",
    ) {
        AuthorizeServer.registerEvents(plugin, packageName, exceptionHandle, prefix)
    }

    @JvmStatic
    fun registerPermCode(plugin: JvmPlugin, vararg perms: Perm) {
        AuthorizeServer.registerPermissions(plugin, *perms)
    }

    @JvmStatic
    fun checkPermExist(code: String): Boolean {
        return AuthorizeServer.isPermissionRegistered(code)
    }

}
