package cn.chahuyun.authorize

import cn.chahuyun.authorize.command.AuthorizeCommand
import cn.chahuyun.authorize.config.AuthorizeConfig
import cn.chahuyun.authorize.constant.AuthPerm
import cn.chahuyun.authorize.entity.Perm
import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin

/**
 * @author Moyuyanli
 */
object HuYanAuthorize : KotlinPlugin(
    JvmPluginDescription(
        id = "cn.chahuyun.HuYanAuthorize",
        version = BuildConstants.VERSION,
        name = "HuYanAuthorize"
    ) {
        author("Moyuyanli")
        info("壶言权限管理")
    }
) {

    val log by lazy { logger }

    override fun onEnable() {
        // 加载配置
        AuthorizeConfig.reload()
        // 加载指令
        AuthorizeCommand.register()
        // 初始化插件数据库
        DataManager.init(this)
        // 注册本插件的权限
        AuthorizeServer.registerPermissionsInternal(
            this,
            Perm(AuthPerm.OWNER, "主人权限"),
            Perm(AuthPerm.ADMIN, "管理员权限")
        )
        // 添加本插件的注册消息
        AuthorizeServer.registerEvents(this, "cn.chahuyun.authorize.manager")
        // 尝试同步主人
        syncOwner()
        logger.info("HuYanAuthorize plugin loaded!")
    }

    override fun onDisable() {
        logger.info("HuYanAuthorize plugin uninstalled!")
    }

    /**
     * 同步主人信息
     * 确保配置文件和数据库中的“主人”权限组一致
     */
    private fun syncOwner() {
        val configOwner = AuthorizeConfig.owner
        val permGroup = PermUtil.takePermGroupByName("主人")

        if (configOwner == 123456L) {
            // 如果配置是默认值，尝试从数据库权限组中找回主人
            val firstOwner = permGroup.users.firstOrNull()?.userId
            if (firstOwner != null) {
                AuthorizeConfig.owner = firstOwner
                log.info("从数据库同步主人信息: $firstOwner")
            } else {
                log.warning("当前未设置主人（默认 123456），请尽快设置主人权限！")
            }
        } else {
            // 如果配置已经指定了主人，确保他在权限组里
            val globalUser = UserUtil.globalUser(configOwner)
            if (permGroup.users.none { it.userId == configOwner }) {
                permGroup.addUser(globalUser)
                log.info("已将配置文件中的主人 $configOwner 同步至权限组")
            }
        }
    }

    /**
     * 获取主人
     */
    fun getOwner(): Long {
        return AuthorizeConfig.owner
    }

}