@file:Suppress("unused")

package cn.chahuyun.authorize.manager

import cn.chahuyun.authorize.AuthorizeServer
import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.AuthPerm
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.authorize.entity.Perm
import cn.chahuyun.authorize.entity.PermGroup
import cn.chahuyun.authorize.entity.PermGroupTree
import cn.chahuyun.authorize.entity.User
import cn.chahuyun.authorize.utils.*
import cn.chahuyun.authorize.utils.AuthMessageUtil.sendMessageQuote
import cn.chahuyun.hibernateplus.HibernateFactory
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.QuoteReply

/**
 * 权限命令管理器，用于处理权限组管理和用户权限分配相关的命令
 */
@EventComponent
class PermCommandManager {

    /**
     * 测试命令，用于验证权限系统是否正常工作
     * @param event 消息事件对象
     */
    @MessageAuthorize(
        text = ["进行测试"],
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN]
    )
    suspend fun test(event: MessageEvent) {
        event.sendMessageQuote(getSystemInfo())
    }

    // ================= 权限组管理 =================

    /**
     * 添加或更新权限组命令，支持添加权限和设置父权限组
     * @param event 消息事件对象
     */
    @MessageAuthorize(
        text = ["\\+perm \\S+( %?\\S+)*"],
        messageMatching = MessageMatchingEnum.REGULAR,
        userPermissions = [AuthPerm.OWNER]
    )
    suspend fun addPermGroup(event: MessageEvent) {
        val split = event.message.contentToString().split(" ")
        val groupName = split[1]
        val builder = MessageChainBuilder()
        builder.append(QuoteReply(event.message))

        val permGroup = HibernateFactory.selectOne(PermGroup::class.java, "name", groupName)
            ?: PermGroup(name = groupName)

        builder.append(if (permGroup.id == null) "创建权限组 $groupName, 并执行:\n" else "更新权限组 $groupName, 并执行:\n")

        for (i in 2 until split.size) {
            val s = split[i]
            if (s.startsWith("%")) {
                val parentName = s.substring(1)
                val parent = HibernateFactory.selectOne(PermGroup::class.java, "name", parentName)
                if (parent == null) {
                    sendMessageQuote(event, "父权限组 $parentName 不存在!\n")
                    return
                }
                val currentParent = HibernateFactory.selectOne(PermGroup::class.java, permGroup.parentId)
                builder.append(currentParent?.let { "已有父权限组 ${it.name}, 添加父权限组失败!\n" })
            } else {
                if (!AuthorizeServer.isPermissionRegistered(s)) {
                    builder.append("权限 $s 未注册, 添加失败!\n")
                    continue
                }
                val perm = HibernateFactory.selectOne(Perm::class.java, "code", s)!!
                if (permGroup.perms.contains(perm)) {
                    builder.append("权限 $s 已存在, 添加失败!\n")
                } else {
                    permGroup.perms.add(perm)
                    builder.append("权限 $s 添加成功!\n")
                }
            }
        }

        saveAndRefresh(permGroup)
        updateChildren(permGroup)

        builder.append("执行完成!")
        event.subject.sendMessage(builder.build())
    }

    /**
     * 删除权限组或从权限组中删除权限命令
     * @param event 消息事件对象
     */
    @MessageAuthorize(
        text = ["-perm \\S+( %?\\S+)*"],
        messageMatching = MessageMatchingEnum.REGULAR,
        userPermissions = [AuthPerm.OWNER]
    )
    suspend fun delPermGroup(event: MessageEvent) {
        val split = event.message.contentToString().split(" ")
        val groupName = split[1]
        val group = HibernateFactory.selectOne(PermGroup::class.java, "name", groupName) ?: run {
            sendMessageQuote(event, "权限组 $groupName 不存在!")
            return
        }

        if (split.size == 2) {
            if (HibernateFactory.selectList(PermGroup::class.java, "parentId", group.id).isNotEmpty()) {
                sendMessageQuote(event, "有权限组继承于本组, 禁止删除!")
                return
            }
            HibernateFactory.delete(group)
            PermCache.refresh()
            sendMessageQuote(event, "权限组 ${group.name} 已删除!")
            return
        }

        val builder = MessageChainBuilder()
        builder.append(QuoteReply(event.message))
        builder.append("为权限组 ${group.name} 执行:\n")

        for (i in 2 until split.size) {
            val code = split[i]
            val perm = group.perms.find { it.code == code }
            if (perm != null) {
                group.perms.remove(perm)
                builder.append("权限 $code 已删除!\n")
            } else {
                builder.append("权限 $code 不存在!\n")
            }
        }

        saveAndRefresh(group)
        updateChildren(group)
        builder.append("执行完成!")
        event.subject.sendMessage(builder.build())
    }

    /**
     * 查看权限组信息命令，支持查看所有权限组或指定权限组
     * @param event 消息事件对象
     */
    @MessageAuthorize(
        text = ["=perm( \\S+)?"],
        messageMatching = MessageMatchingEnum.REGULAR,
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN]
    )
    suspend fun viewPerm(event: MessageEvent) {
        val split = event.message.contentToString().split(" ")
        val permGroups = if (split.size == 2) {
            val one = HibernateFactory.selectOne(PermGroup::class.java, "name", split[1]) ?: run {
                sendMessageQuote(event, "权限组 ${split[1]} 不存在")
                return
            }
            val list = HibernateFactory.selectList(PermGroup::class.java, "parentId", one.parentId).toMutableList()
            list.add(one)
            list
        } else {
            HibernateFactory.selectList(PermGroup::class.java)
        }

        val groupTree = buildPermGroupTree(permGroups)
        val builder = ForwardMessageBuilder(event.subject).add(event.bot, PlainText("以下是所有权限组节点:"))
        appendChildNodes(builder, event.bot, event.subject, groupTree)
        event.subject.sendMessage(builder.build())
    }

    // ================= 用户管理 =================

    /**
     * 为全局用户添加权限组命令
     * @param event 消息事件对象
     */
    @MessageAuthorize(
        text = ["\\+global (\\[mirai:at:)?\\d+]? \\S+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN]
    )
    suspend fun addGlobalUser(event: MessageEvent) {
        handleUserOp(event, true) { userId, _ -> UserUtil.globalUser(userId) }
    }

    /**
     * 为群组添加权限组命令
     * @param event 群消息事件对象
     */
    @MessageAuthorize(
        text = ["\\+group \\S+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN]
    )
    suspend fun addGroup(event: GroupMessageEvent) {
        handleUserOp(event, true) { _, e -> UserUtil.group((e as GroupMessageEvent).group.id) }
    }

    /**
     * 为群管理员添加权限组命令
     * @param event 群消息事件对象
     */
    @MessageAuthorize(
        text = ["\\+admin \\S+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN]
    )
    suspend fun addGroupAdmin(event: GroupMessageEvent) {
        handleUserOp(event, true) { _, e -> UserUtil.groupAdmin((e as GroupMessageEvent).group.id) }
    }

    /**
     * 为群成员添加权限组命令
     * @param event 群消息事件对象
     */
    @MessageAuthorize(
        text = ["\\+member (\\[mirai:at:)?\\d+]? \\S+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN]
    )
    suspend fun addMember(event: GroupMessageEvent) {
        handleUserOp(event, true) { userId, e -> UserUtil.member((e as GroupMessageEvent).group.id, userId) }
    }

    /**
     * 从全局用户中移除权限组命令
     * @param event 消息事件对象
     */
    @MessageAuthorize(
        text = ["-global (\\[mirai:at:)?\\d+]? \\S+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN]
    )
    suspend fun delGlobalUser(event: MessageEvent) {
        handleUserOp(event, false) { userId, _ -> UserUtil.globalUser(userId) }
    }

    /**
     * 从群组中移除权限组命令
     * @param event 群消息事件对象
     */
    @MessageAuthorize(
        text = ["-group \\S+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN]
    )
    suspend fun delGroup(event: GroupMessageEvent) {
        handleUserOp(event, false) { _, e -> UserUtil.group((e as GroupMessageEvent).group.id) }
    }

    /**
     * 从群管理员中移除权限组命令
     * @param event 群消息事件对象
     */
    @MessageAuthorize(
        text = ["-admin \\S+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN]
    )
    suspend fun delGroupAdmin(event: GroupMessageEvent) {
        handleUserOp(event, false) { _, e -> UserUtil.groupAdmin((e as GroupMessageEvent).group.id) }
    }

    /**
     * 从群成员中移除权限组命令
     * @param event 群消息事件对象
     */
    @MessageAuthorize(
        text = ["-member (\\[mirai:at:)?\\d+]? \\S+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN]
    )
    suspend fun delMember(event: GroupMessageEvent) {
        handleUserOp(event, false) { userId, e -> UserUtil.member((e as GroupMessageEvent).group.id, userId) }
    }

    // ================= 辅助方法 =================

    /**
     * 处理用户权限组操作的通用方法
     * @param event 消息事件对象
     * @param isAdd 是否为添加操作，true为添加，false为删除
     * @param userProvider 用户提供者函数，用于获取用户对象
     */
    private suspend fun handleUserOp(
        event: MessageEvent,
        isAdd: Boolean,
        userProvider: (Long, MessageEvent) -> User
    ) {
        val split = event.message.contentToString().split(" ")
        val userId = if (event is GroupMessageEvent) EventUtil.getAtMember(event)?.id
            ?: split[1].toLongOrNull() else split[1].toLongOrNull()

        val groupName = if (split.size > 2) split[2] else split[1]

        if (groupName == "主人" && !PermUtil.checkOwner(event.sender.id)) {
            sendMessageQuote(event, "只有主人才能操作主人权限组!")
            return
        }

        val group = HibernateFactory.selectOne(PermGroup::class.java, "name", groupName) ?: run {
            sendMessageQuote(event, "权限组 $groupName 不存在!")
            return
        }

        val user = userProvider(userId ?: 0L, event)
        val result = if (isAdd) {
            if (group.users.contains(user)) {
                sendMessageQuote(event, "该用户已在权限组中!")
                return
            }
            group.users.add(user)
            "${user.toUserName()} 成功添加到权限组 ${group.name}"
        } else {
            if (!group.users.contains(user)) {
                sendMessageQuote(event, "该用户不在权限组中!")
                return
            }
            group.users.remove(user)
            "${user.toUserName()} 成功从权限组 ${group.name} 中删除"
        }

        saveAndRefresh(group)
        sendMessageQuote(event, result)
    }

    /**
     * 保存权限组并刷新缓存
     * @param group 要保存的权限组对象
     */
    private fun saveAndRefresh(group: PermGroup) {
        HibernateFactory.merge(group)
        PermCache.refresh()
    }

    /**
     * 更新子权限组的权限，将父权限组的权限同步给子权限组
     * @param parent 父权限组对象
     */
    private fun updateChildren(parent: PermGroup) {
        val children = HibernateFactory.selectList(PermGroup::class.java, "parentId", parent.id)
        for (child in children) {
            child.perms.addAll(parent.perms)
            HibernateFactory.merge(child)
        }
    }

    /**
     * 构建权限组树结构
     * @param permGroups 权限组列表
     * @return 权限组树结构集合
     */
    private fun buildPermGroupTree(permGroups: List<PermGroup>): Set<PermGroupTree> {
        val groupMutableMap = permGroups.associateBy({ it.id!! }, { PermGroupTree.fromPermGroup(it) })
        val result = mutableSetOf<PermGroupTree>()
        groupMutableMap.values.forEach {
            if (it.parentId == null || !groupMutableMap.containsKey(it.parentId)) {
                result.add(it)
            } else {
                groupMutableMap[it.parentId]?.children?.add(it)
            }
        }
        return result
    }

    /**
     * 递归添加权限组树节点到转发消息构建器中
     * @param builder 转发消息构建器
     * @param bot 机器人对象
     * @param subject 联系人对象
     * @param treeSet 权限组树集合
     */
    private fun appendChildNodes(
        builder: ForwardMessageBuilder,
        bot: Bot,
        subject: Contact,
        treeSet: Set<PermGroupTree>
    ) {
        for (tree in treeSet) {
            val perms = tree.perms.takeIf { it.isNotEmpty() }?.joinToString { it.code!! } ?: "无"
            val users = tree.users.takeIf { it.isNotEmpty() }?.joinToString { it.toUserName() } ?: "无"
            val text = "权限组:${tree.name}\n拥有权限:$perms\n拥有用户:$users\n"

            if (tree.children.isEmpty()) {
                builder.add(bot, PlainText(text + "拥有子组:无"))
            } else {
                builder.add(bot, PlainText(text + "拥有子组:↓"))
                val son = ForwardMessageBuilder(subject)
                appendChildNodes(son, bot, subject, tree.children)
                builder.add(bot, son.build())
            }
        }
    }
}

