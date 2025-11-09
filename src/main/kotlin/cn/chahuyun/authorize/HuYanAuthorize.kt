package cn.chahuyun.authorize

import cn.chahuyun.authorize.command.AuthorizeCommand
import cn.chahuyun.authorize.config.AuthorizeConfig
import cn.chahuyun.authorize.entity.Perm
import cn.chahuyun.authorize.entity.PermGroup
import cn.chahuyun.authorize.entity.User
import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import cn.chahuyun.hibernateplus.HibernateFactory
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
        PermissionServer.authorizePermRegister()
        // 添加本插件的注册消息
        PermissionServer.registerMessageEvent(this, "cn.chahuyun.authorize.manager")
        // 尝试同步主人
        syncOwner()
        logger.info("HuYanAuthorize plugin loaded!")
    }

    override fun onDisable() {
        logger.info("HuYanAuthorize plugin uninstalled!")
    }

    private fun syncOwner() {
        val owner = AuthorizeConfig.owner

        // 获取 "owner" 权限
        val permGroup = PermUtil.talkPermGroupByName("主人")
        if (owner == 123456L) {

            if (permGroup.users.isEmpty()) {
                log.warning("没有设置主人,请设置主人!")
                return
            }

            permGroup.users.forEach {
                AuthorizeConfig.owner = it.userId!!
            }
        } else {
            if (permGroup.users.none { it.userId == owner }) {
                permGroup.addUser(UserUtil.globalUser(owner))
            }
        }

    }

    private fun addOwnerPermGroup(perm: Perm, owner: User) {
        // 创建权限组
        val ownerPermGroup = PermGroup(
            name = "主人",
            perms = mutableSetOf(perm),
            users = mutableSetOf(owner)
        )

        if (HibernateFactory.merge(ownerPermGroup).id != 0) {
            logger.info("主人 $owner 已同步!")
        }
    }

    /**
     * 获取主人
     */
    fun getOwner(): Long {
        return AuthorizeConfig.owner
    }

}