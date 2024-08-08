package cn.chahuyun.authorize

import cn.chahuyun.authorize.command.AuthorizeCommand
import cn.chahuyun.authorize.config.AuthorizeConfig
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.plugin.jvm.reloadPluginConfig
import net.mamoe.mirai.utils.MiraiLogger


/**
 * @author Moyuyanli
 */
class HuYanAuthorize : KotlinPlugin(
    JvmPluginDescription(
    id = "cn.chahuyun.HuYanAuthorize",
    version = VERSION,
    name = "HuYanAuthorize"
    ){
        author("Moyuyanli")
        info("壶言权限管理")
        dependsOn("xyz.cssxsh.mirai.plugin.mirai-hibernate-plugin",false)
    }
) {
    companion object {
        val INSTANCE: HuYanAuthorize = HuYanAuthorize()
        const val VERSION = "1.0.7"
        val LOGGER: MiraiLogger = INSTANCE.logger
    }

    override fun onEnable() {
        // 加载配置
        reloadPluginConfig(AuthorizeConfig)
        // 加载指令
        CommandManager.registerCommand(AuthorizeCommand, true)
        // 加载配置

        // 初始化插件数据库

        // 添加本插件的注册消息包信息
        PermissionServer.getInstance().init(this, "cn.chahuyun.authorize.manager")
        LOGGER.info("HuYanAuthorize plugin loaded!")
    }

    override fun onDisable() {
        LOGGER.info("HuYanAuthorize plugin uninstalled!")
    }
}