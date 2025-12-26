package cn.chahuyun.authorize.listener

import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.constant.AuthPerm
import cn.chahuyun.authorize.constant.MessageConversionEnum.*
import cn.chahuyun.authorize.constant.PermissionMatchingEnum
import cn.chahuyun.authorize.constant.PermissionMatchingEnum.AND
import cn.chahuyun.authorize.constant.PermissionMatchingEnum.OR
import cn.chahuyun.authorize.constant.UserType
import cn.chahuyun.authorize.entity.User
import cn.chahuyun.authorize.utils.PermCache
import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChain.Companion.serializeToJsonString
import java.util.regex.Pattern

/**
 * 监听过滤器实现
 */
class ListenerFilter(private val prefix: String) {

    /**
     * 权限过滤
     */
    fun permFilter(
        messageEvent: MessageEvent,
        annotation: MessageAuthorize,
        methodType: Class<out MessageEvent>,
    ): Boolean {
        val userPermMatch: Boolean
        val groupPermMatch: Boolean

        val userPerms = annotation.userPermissions
        val groupPerms = annotation.groupPermissions
        val blackPerms = annotation.blackPermissions

        //匹配黑名单权限
        if (!blackPerms.contains(AuthPerm.NULL) && !PermUtil.checkOwner(messageEvent.sender.id)) {
            val u = userPermMatch(blackPerms, OR, messageEvent)
            val g = groupPermMatch(blackPerms, OR, messageEvent)

            if (u || g) {
                return false
            }
        }

        userPermMatch = if (userPerms.contains(AuthPerm.NULL)) {
            true
        } else {
            userPermMatch(userPerms, annotation.userPermissionsMatching, messageEvent)
        }

        if (!GroupMessageEvent::class.java.isAssignableFrom(methodType)) {
            return userPermMatch
        }

        groupPermMatch = if (groupPerms.contains(AuthPerm.NULL)) {
            true
        } else {
            groupPermMatch(groupPerms, annotation.groupPermissionsMatching, messageEvent)
        }

        return when (annotation.userInGroupPermissionsAssociation) {
            OR -> userPermMatch || groupPermMatch
            AND -> userPermMatch && groupPermMatch
        }
    }

    /**
     * 消息内容过滤
     */
    fun messageFilter(messageEvent: MessageEvent, annotation: MessageAuthorize): Boolean {
        var message = when (annotation.messageConversion) {
            CONTENT -> messageEvent.message.contentToString()
            MIRAI_CODE -> messageEvent.message.serializeToMiraiCode()
            JSON -> messageEvent.message.serializeToJsonString()
        }

        //匹配指令前缀
        if (prefix.isNotBlank()) {
            if (message.indexOf(prefix) == 0) {
                message = message.removePrefix(prefix)
            } else {
                return false
            }
        }

        return when (annotation.messageMatching) {
            cn.chahuyun.authorize.constant.MessageMatchingEnum.TEXT -> {
                for (s in annotation.text) {
                    if (s == message) return true
                }
                false
            }

            cn.chahuyun.authorize.constant.MessageMatchingEnum.REGULAR -> Pattern.matches(annotation.text[0], message)
            cn.chahuyun.authorize.constant.MessageMatchingEnum.CUSTOM -> {
                val instance = annotation.custom.java.getDeclaredConstructor().newInstance()
                return instance.custom(event = messageEvent)
            }
        }
    }

    private fun userPermMatch(
        perms: Array<String>,
        match: PermissionMatchingEnum,
        messageEvent: MessageEvent,
    ): Boolean {
        val globalUser = UserUtil.globalUser(messageEvent.sender.id)
        val isGroup = messageEvent is GroupMessageEvent
        val groupUser = if (isGroup) {
            UserUtil.member((messageEvent as GroupMessageEvent).subject.id, messageEvent.sender.id)
        } else null

        return when (match) {
            OR -> perms.any { checkSinglePerm(it, globalUser, groupUser, isGroup, messageEvent) }
            AND -> perms.all { checkSinglePerm(it, globalUser, groupUser, isGroup, messageEvent) }
        }
    }

    private fun checkSinglePerm(
        permCode: String,
        globalUser: User,
        groupUser: User?,
        isGroup: Boolean,
        messageEvent: MessageEvent
    ): Boolean {
        val perm = PermCache.get(permCode) ?: throw RuntimeException("权限 $permCode 未注册")
        for (group in perm.permGroup) {
            if (group.users.contains(globalUser)) return true
            if (isGroup && groupUser != null) {
                if (group.users.contains(groupUser)) return true
                for (admin in group.users.filter { it.type == UserType.GROUP_ADMIN }) {
                    val groupMessageEvent = messageEvent as GroupMessageEvent
                    if (admin.groupId == groupMessageEvent.group.id) {
                        if (groupMessageEvent.sender.permission != MemberPermission.MEMBER) return true
                    }
                }
            }
        }
        return false
    }

    private fun groupPermMatch(
        perms: Array<String>,
        match: PermissionMatchingEnum,
        messageEvent: MessageEvent,
    ): Boolean {
        if (messageEvent !is GroupMessageEvent) return false

        val groupUser = UserUtil.group(groupId = messageEvent.group.id)
        val permsMap = perms.associateWith { PermCache.get(it) }

        return when (match) {
            OR -> permsMap.any { (_, perm) ->
                perm?.permGroup?.any { group -> group.users.contains(groupUser) } == true
            }

            AND -> permsMap.all { (_, perm) ->
                perm?.permGroup?.any { group -> group.users.contains(groupUser) } == true
            }
        }
    }
}

