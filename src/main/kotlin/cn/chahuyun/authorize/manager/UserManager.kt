package cn.chahuyun.authorize.manager

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.AuthPerm
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.authorize.entity.PermGroup
import cn.chahuyun.authorize.utils.EventUtil
import cn.chahuyun.authorize.utils.MessageUtil.sendMessageQuote
import cn.chahuyun.authorize.utils.PermCache
import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import cn.chahuyun.hibernateplus.HibernateFactory
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent

@EventComponent
class UserManager {


    @MessageAuthorize(
        text = ["\\+global (\\[mirai:at:)?\\d+]? \\S+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN]
    )
    suspend fun addGlobalUser(event: MessageEvent) {
        val split = content(event)

        val userId = if (event is GroupMessageEvent) {
            EventUtil.getAtMember(event)?.id
        } else {
            split[1].toLong()
        }

        if (userId == null) {
            throw RuntimeException("获取at用户错误!")
        }

        val group = split[2]

        //主人才能往主人权限组添加用户
        if (group == "主人") {
            if (!PermUtil.checkOwner(event.subject.id)) return
        }

        val one = HibernateFactory.selectOne(PermGroup::class.java, "name", group)

        if (one == null) {
            sendMessageQuote(event, "权限组 $group 不存在!")
            return
        }

        val user = UserUtil.globalUser(userId)

        if (one.users.contains(user)) {
            sendMessageQuote(event, "该用户已经存在于该权限组了!")
            return
        }

        one.users.add(user)
        HibernateFactory.merge(one)

        PermCache.refresh()
        sendMessageQuote(event, "${user.toUserName()} 成功添加到权限组 ${one.name}")
    }

    @MessageAuthorize(
        text = ["\\+group \\S+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN]
    )
    suspend fun addGroup(event: GroupMessageEvent) {
        val split = content(event)

        val s = split[1]

        if (s == "主人") {
            if (!PermUtil.checkOwner(event.subject.id)) return
        }

        val one = HibernateFactory.selectOne(PermGroup::class.java, "name", s)

        if (one == null) {
            sendMessageQuote(event, "权限组 ${split[1]} 不存在!")
            return
        }

        val user = UserUtil.group(event.group.id)

        if (one.users.contains(user)) {
            sendMessageQuote(event, "该群已经存在于该权限组了!")
            return
        }

        one.users.add(user)
        HibernateFactory.merge(one)

        PermCache.refresh()
        sendMessageQuote(event, "${user.toUserName()} 成功添加到权限组 ${one.name}")
    }

    @MessageAuthorize(
        text = ["\\+admin \\S+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN]
    )
    suspend fun addGroupAdmin(event: GroupMessageEvent) {
        val split = content(event)

        val s = split[1]

        if (s == "主人") {
            if (!PermUtil.checkOwner(event.subject.id)) return
        }

        val one = HibernateFactory.selectOne(PermGroup::class.java, "name", s)

        if (one == null) {
            sendMessageQuote(event, "权限组 $s 不存在!")
            return
        }

        val user = UserUtil.groupAdmin(event.group.id)

        if (one.users.contains(user)) {
            sendMessageQuote(event, "该管理用户已经存在于该权限组了!")
            return
        }

        one.users.add(user)
        HibernateFactory.merge(one)

        PermCache.refresh()
        sendMessageQuote(event, "${user.toUserName()} 成功添加到权限组 ${one.name}")
    }


    @MessageAuthorize(
        text = ["\\+member (\\[mirai:at:)?\\d+]? \\S+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN]
    )
    suspend fun addMember(event: GroupMessageEvent) {
        val split = content(event)

        val userId = EventUtil.getAtMember(event)?.id ?: split[1].toLong()

        val permGroup = split[2]

        if (permGroup == "主人") {
            if (!PermUtil.checkOwner(event.subject.id)) return
        }

        val one = HibernateFactory.selectOne(PermGroup::class.java, "name", permGroup)

        if (one == null) {
            sendMessageQuote(event, "权限组 $permGroup 不存在!")
            return
        }

        val user = UserUtil.member(event.group.id, userId)

        if (one.users.contains(user)) {
            sendMessageQuote(event, "该群成员已经存在于该权限组了!")
            return
        }

        one.users.add(user)
        HibernateFactory.merge(one)

        PermCache.refresh()
        sendMessageQuote(event, "${user.toUserName()} 成功添加到权限组 ${one.name}")
    }


    @MessageAuthorize(
        text = ["-global (\\[mirai:at:)?\\d+]? \\S+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN]
    )
    suspend fun delGlobalUser(event: MessageEvent) {
        val split = content(event)

        val userId = if (event is GroupMessageEvent) {
            EventUtil.getAtMember(event)?.id
        } else {
            split[1].toLong()
        }

        if (userId == null) {
            throw RuntimeException("获取at用户错误!")
        }

        val one = HibernateFactory.selectOne(PermGroup::class.java, "name", split[2])

        if (one == null) {
            event.sendMessageQuote("权限组 ${split[2]} 不存在!")
            return
        }

        val user = UserUtil.globalUser(userId)

        if (!one.users.contains(user)) {
            event.sendMessageQuote("该用户不存在于该权限组!")
            return
        }

        one.users.remove(user)
        HibernateFactory.merge(one)

        PermCache.refresh()
        event.sendMessageQuote("${user.toUserName()} 成功删除于权限组 ${one.name}")
    }

    @MessageAuthorize(
        text = ["-group \\S+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN]
    )
    suspend fun delGroup(event: GroupMessageEvent) {
        val split = content(event)

        val s = split[1]
        val one = HibernateFactory.selectOne(PermGroup::class.java, "name", s)

        if (one == null) {
            event.sendMessageQuote("权限组 ${split[1]} 不存在!")
            return
        }

        val user = UserUtil.group(event.group.id)

        if (!one.users.contains(user)) {
            event.sendMessageQuote("该群不存在于该权限组!")
            return
        }

        one.users.remove(user)
        HibernateFactory.merge(one)

        PermCache.refresh()
        event.sendMessageQuote("${user.toUserName()} 成功删除于权限组 ${one.name}")
    }

    @MessageAuthorize(
        text = ["-admin \\S+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN]
    )
    suspend fun delGroupAdmin(event: GroupMessageEvent) {
        val split = content(event)

        val one = HibernateFactory.selectOne(PermGroup::class.java, "name", split[1])

        if (one == null) {
            event.sendMessageQuote("权限组 ${split[1]} 不存在!")
            return
        }

        val user = UserUtil.groupAdmin(event.group.id)

        if (!one.users.contains(user)) {
            event.sendMessageQuote("该管理用户不存在于该权限组!")
            return
        }

        one.users.remove(user)
        HibernateFactory.merge(one)

        PermCache.refresh()
        event.sendMessageQuote("${user.toUserName()} 成功删除于权限组 ${one.name}")
    }


    @MessageAuthorize(
        text = ["\\-member (\\[mirai:at:)?\\d+]? \\S+"],
        messageMatching = MessageMatchingEnum.REGULAR,
        userPermissions = [AuthPerm.OWNER, AuthPerm.ADMIN]
    )
    suspend fun delMember(event: GroupMessageEvent) {
        val split = content(event)


        val userId = EventUtil.getAtMember(event)?.id ?: split[1].toLong()

        val one = HibernateFactory.selectOne(PermGroup::class.java, "name", split[2])

        if (one == null) {
            event.sendMessageQuote("权限组 ${split[2]} 不存在!")
            return
        }

        val user = UserUtil.member(event.group.id, userId)

        if (!one.users.contains(user)) {
            event.sendMessageQuote("该群成员不存在于该权限组!")
            return
        }

        one.users.remove(user)
        HibernateFactory.merge(one)

        PermCache.refresh()
        event.sendMessageQuote("${user.toUserName()} 成功删除于权限组 ${one.name}")
    }

    //todo 删除用户，查看用户

    /**
     * 按照空格分割消息
     */
    private fun content(event: MessageEvent): List<String> {
        val content = event.message.contentToString()

        val split = content.split(" ")
        return split
    }


}