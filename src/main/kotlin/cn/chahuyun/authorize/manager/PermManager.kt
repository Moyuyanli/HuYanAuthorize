package cn.chahuyun.authorize.manager

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.PermissionServer
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.authorize.constant.PermConstant
import cn.chahuyun.authorize.entity.Perm
import cn.chahuyun.authorize.entity.PermGroup
import cn.chahuyun.authorize.entity.PermGroupTree
import cn.chahuyun.authorize.utils.MessageUtil.sendMessageQuery
import cn.chahuyun.authorize.utils.getSystemInfo
import cn.chahuyun.hibernateplus.HibernateFactory
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.QuoteReply

@EventComponent
class PermManager {

    @MessageAuthorize(
        text = ["进行测试"],
        userPermissions = [PermConstant.OWNER,PermConstant.ADMIN]
    )
    suspend fun test(messageEvent: MessageEvent) {
        messageEvent.subject.sendMessage(getSystemInfo())
    }

    /**
     * 添加权限组
     */
    @MessageAuthorize(
        text = ["\\+perm \\S+( %?\\S+)*"],
        messageMatching = MessageMatchingEnum.REGULAR,
        userPermissions = [PermConstant.OWNER]
    )
    suspend fun addPermGroup(event: MessageEvent) {
        val content = event.message.contentToString()

        val split = content.split(" ")

        val builder = MessageChainBuilder()
        builder.add(QuoteReply(event.message))

        val permSon = HibernateFactory.selectOne(PermGroup::class.java, "name", split[1]) ?: PermGroup(name = split[1])

        if (permSon.id == null) {
            builder.add("创建权限组 ${split[1]} ,并执行:\n")
        } else {
            builder.add("更新权限组 ${split[1]} ,并执行:\n")
        }

        for (i in 2 until split.size) {
            val s = split[i]

            if (s.startsWith("%")) {
                val substring = s.substring(1)
                val selectOne = HibernateFactory.selectOne(PermGroup::class.java, "name", substring)
                if (selectOne == null) {
                    sendMessageQuery(event, "父权限组 $substring 不存在!\n")
                    return
                }

                if (permSon.parentId != null) {
                    val parent = HibernateFactory.selectOne(PermGroup::class.java, permSon.parentId)
                    builder.add("已有父权限组 ${parent.name} ,添加父权限组失败!\n")
                } else {
                    permSon.parentId = selectOne.id
                    permSon.perms.addAll(selectOne.perms)
                    builder.add("继承父权限组 $substring 权限!\n")
                }
                continue
            }

            if (!PermissionServer.checkPermExist(s)) {
                builder.add("权限 $s 未注册,添加失败!\n")
                continue
            }

            val selectOne = HibernateFactory.selectOne(Perm::class.java, "code", s)!!

            if (permSon.perms.contains(selectOne)) {
                builder.add("权限 $s 已存在在权限组,添加失败!\n")
                continue
            }

            permSon.perms.add(selectOne)
            builder.add("权限 $s 添加成功!\n")
        }

        val selectList = HibernateFactory.selectList(PermGroup::class.java, "parentId", permSon.id)

        for (group in selectList) {
            group.perms.addAll(permSon.perms)
            HibernateFactory.merge(group)
        }

        HibernateFactory.merge(permSon)

        builder.add("执行完成!")
        event.subject.sendMessage(builder.build())
    }


    @MessageAuthorize(
        text = ["-perm \\S+( %?\\S+)*"],
        messageMatching = MessageMatchingEnum.REGULAR,
        userPermissions = [PermConstant.OWNER]
    )
    suspend fun delPermGroup(event: MessageEvent) {
        val content = event.message.contentToString()

        val split = content.split(" ")

        val builder = MessageChainBuilder()
        builder.add(QuoteReply(event.message))

        val one = HibernateFactory.selectOne(PermGroup::class.java, "name", split[1])
        if (one == null) {
            sendMessageQuery(event, "权限组 ${split[1]} 不存在!")
            return
        }

        if (split.size == 2) {
            if (HibernateFactory.selectList(PermGroup::class.java, "parentId", one.id).isNotEmpty()) {
                sendMessageQuery(event, "有权限组继承于本组,禁止删除!")
                return
            }

            HibernateFactory.delete(one)
            sendMessageQuery(event, "权限组 ${one.name} 已删除!")
            return
        }

        builder.add("为权限组 ${one.name} 执行以下操作:\n")
        for (i in 2 until split.size) {
            val s = split[i]

            if (one.contains(s)) {
                val find = one.perms.find { it.code == s }!!

                one.perms.remove(find)

                val selectList = HibernateFactory.selectList(PermGroup::class.java, "parentId", one.id)

                for (permGroup in selectList) {
                    permGroup.perms.remove(find)
                    HibernateFactory.merge(permGroup)
                }

                HibernateFactory.merge(one)
                builder.add("权限 $s 已删除!\n")
            } else {
                builder.add("权限 $s 不存在!\n")
            }
        }

        builder.add("执行完成!")
        event.subject.sendMessage(builder.build())
    }


    @MessageAuthorize(
        text = ["=perm( \\S+)?"],
        messageMatching = MessageMatchingEnum.REGULAR,
        userPermissions = [PermConstant.OWNER]
    )
    suspend fun viewPerm(event: MessageEvent) {
        val subject = event.subject
        val bot = event.bot

        val split = event.message.contentToString().split(" ")

        val permGroups: List<PermGroup>
        if (split.size == 2) {
            val one = HibernateFactory.selectOne(PermGroup::class.java, "name", split[1])
            if (one == null) {
                sendMessageQuery(event, "权限组 ${split[1]} 不存在")
                return
            }
            val list = HibernateFactory.selectList(PermGroup::class.java, "parentId", one.parentId)
            list.add(one)
            permGroups = list
        } else {
            permGroups = HibernateFactory.selectList(PermGroup::class.java)
        }

        val groupTree = buildPermGroupTree(permGroups)

        val builder = ForwardMessageBuilder(subject)
        builder.add(bot, PlainText("以下是所有权限组节点:"))


        appendChildNodes(builder, bot, subject, groupTree)

        subject.sendMessage(builder.build())
    }


    /**
     * 构建权限组树
     */
    private fun buildPermGroupTree(permGroups: List<PermGroup>): Set<PermGroupTree> {
        val groupMutableMap = mutableMapOf<Int, PermGroupTree>()

        permGroups.forEach { groupMutableMap[it.id!!] = PermGroupTree.fromPermGroup(it) }

        val result = mutableSetOf<PermGroupTree>()

        groupMutableMap.values.forEach {
            if (it.parentId == null || !groupMutableMap.containsKey(it.parentId)) {
                result.add(it)
            } else {
                val permGroupTree = groupMutableMap[it.parentId]
                permGroupTree?.children?.add(it)
            }
        }

        return result
    }


    /**
     * 递归构建子组信息
     */
    private fun appendChildNodes(
        builder: ForwardMessageBuilder,
        bot: Bot,
        subject: Contact,
        groupTree: Set<PermGroupTree>,
    ) {
        for (tree in groupTree) {
            val permsString = tree.perms.takeIf { it.isNotEmpty() }?.joinToString { it.code!! } ?: "无"
            val usersString = tree.users.takeIf { it.isNotEmpty() }?.joinToString { it.toUserName() } ?: "无"
            val text = PlainText(
                "权限组:${tree.name}\n" +
                        "拥有权限:$permsString\n" +
                        "拥有用户:$usersString\n"
            )
            if (tree.children.isEmpty()) {
                builder.add(bot, text.plus("拥有子组:无"))
            } else {
                builder.add(bot, text.plus("拥有子组:↓"))
                val son = ForwardMessageBuilder(subject)

                appendChildNodes(son, bot, subject, tree.children)

                builder.add(bot, son.build())
            }
        }
    }

}
