package cn.chahuyun.authorize

import cn.chahuyun.authorize.config.AuthorizeConfig
import cn.chahuyun.authorize.entity.Perm
import cn.chahuyun.authorize.entity.PermGroup
import cn.chahuyun.authorize.entity.User
import cn.chahuyun.hibernateplus.HibernateFactory
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin


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
        const val VERSION = "1.1.2"
    }

    override fun onEnable() {
        // 加载配置
        AuthorizeConfig.reload()
        // 加载指令
//        CommandManager.registerCommand(AuthorizeCommand, false)
        // 初始化插件数据库
        DataManager.init(this)
        // 注册本插件的权限
        PermissionServer.authorizePermRegister()
        // 添加本插件的注册消息
        PermissionServer.init(this, "cn.chahuyun.authorize.manager")
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
        val selectOne = HibernateFactory.selectOne(Perm::class.java, "code", "owner")
        val permGroup = selectOne?.permGroup

        if (owner != 123456L) {
            // 创建 User 对象
            val ownerUser = User.globalUser(userId = owner)

            // 如果权限组为空
            if (permGroup.isNullOrEmpty()) {
                addOwnerPermGroup(selectOne, ownerUser)
            } else {
                // 查找名称为 "主人" 的权限组
                val group = permGroup.find { it.name == "主人" }
                if (group == null) {
                    addOwnerPermGroup(selectOne, ownerUser)
                } else {
                    // 检查权限组中是否已经有该用户
                    val find = group.users.find { it.userId == owner }
                    if (find == null) {
                        // 如果没有该用户，则添加
                        addOwnerPermGroup(selectOne, ownerUser, group)
                    }
                }
            }
        } else {
            // 如果 owner 的值为默认值 123456
            // 查找权限组中除 123456 外的第一个用户
            val otherUsers = permGroup?.flatMap { it.users }?.filter { it.userId != 123456L }
            if (otherUsers?.isNotEmpty() == true) {
                // 更新配置文件中的 owner
                otherUsers.first().userId?.let { AuthorizeConfig.owner = it }
            } else {
                logger.warning("没有设置主人,请设置主人!")
            }
        }
    }

    private fun addOwnerPermGroup(perm: Perm, owner: User, permGroup: PermGroup? = null) {
        // 创建权限组
        val ownerPermGroup = permGroup ?: PermGroup(
            name = "主人",
            perms = mutableSetOf(perm),
            users = mutableSetOf(owner)
        )

        if (HibernateFactory.merge(ownerPermGroup).id != 0) {
            logger.info("主人 $owner 已同步!")
        }
    }

}