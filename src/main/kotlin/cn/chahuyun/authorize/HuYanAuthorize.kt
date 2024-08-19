package cn.chahuyun.authorize

import cn.chahuyun.authorize.command.AuthorizeCommand
import cn.chahuyun.authorize.config.AuthorizeConfig
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.plugin.jvm.reloadPluginConfig


/**
 * @author Moyuyanli
 */
class HuYanAuthorize : KotlinPlugin(
    JvmPluginDescription(
        id = "cn.chahuyun.HuYanAuthorize",
        version = VERSION,
        name = "HuYanAuthorize"
    ) {
        author("Moyuyanli")
        info("壶言权限管理")
    }
) {
    companion object {
        /**
         * 插件实例
         */
        val INSTANCE = HuYanAuthorize()

        /**
         * 插件版本
         */
        const val VERSION = "1.0.8"
    }

    override fun onEnable() {
        // 加载配置
        AuthorizeConfig.reload()
        // 加载指令
//        CommandManager.registerCommand(AuthorizeCommand, false)
        // 初始化插件数据库
        DataManager.init(this)

        // 添加本插件的注册消息包信息
//        PermissionServer.instance.init(this, "cn.chahuyun.authorize.manager")
        logger.info("HuYanAuthorize plugin loaded!")
    }

    override fun onDisable() {
        logger.info("HuYanAuthorize plugin uninstalled!")
    }
}