package cn.chahuyun.authorize.manager

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.PermissionServer
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.authorize.entity.Perm
import cn.chahuyun.authorize.entity.PermGroup
import cn.chahuyun.authorize.utils.MessageUtil.sendMessage
import cn.chahuyun.authorize.utils.MessageUtil.sendMessageQuery
import cn.chahuyun.authorize.utils.getSystemInfo
import cn.chahuyun.hibernateplus.HibernateFactory
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.QuoteReply

@EventComponent
class PermManager {

    @MessageAuthorize(
        text = ["进行测试"],
        userPermissions = ["owner"]
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
        userPermissions = ["owner"]
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

        event.subject.sendMessage(builder.build())
    }


    @MessageAuthorize(
        text = ["-perm \\S+( %?\\S+)*"],
        messageMatching = MessageMatchingEnum.REGULAR,
        userPermissions = ["owner"]
    )
    suspend fun delPermGroup(event: MessageEvent) {
        val content = event.message.contentToString()

        val split = content.split(" ")

        val builder = MessageChainBuilder()
        builder.add(QuoteReply(event.message))

        val one = HibernateFactory.selectOne(PermGroup::class.java, "name", split[1])
        if (one == null) {
            sendMessageQuery(event,"权限组 ${split[1]} 不存在!")
            return
        }

        if (split.size == 2) {
            if (HibernateFactory.selectList(PermGroup::class.java,"parentId",one.id).isNotEmpty()) {
                sendMessageQuery(event,"有权限组继承于本组,禁止删除!")
                return
            }

            HibernateFactory.delete(one)
            sendMessageQuery(event, "权限组 ${one.name} 已删除!")
            return
        }




    }

}
