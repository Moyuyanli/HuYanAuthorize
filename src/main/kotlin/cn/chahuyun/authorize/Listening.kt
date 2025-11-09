package cn.chahuyun.authorize

import cn.chahuyun.authorize.HuYanAuthorize.log
import cn.chahuyun.authorize.constant.AuthPerm
import cn.chahuyun.authorize.constant.MessageConversionEnum.*
import cn.chahuyun.authorize.constant.PermissionMatchingEnum
import cn.chahuyun.authorize.constant.PermissionMatchingEnum.AND
import cn.chahuyun.authorize.constant.PermissionMatchingEnum.OR
import cn.chahuyun.authorize.constant.UserType
import cn.chahuyun.authorize.entity.User
import cn.chahuyun.authorize.exception.ExceptionHandleApi
import cn.chahuyun.authorize.utils.PermCache
import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import cn.hutool.core.date.DateUtil
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChain.Companion.serializeToJsonString
import java.lang.reflect.Method
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Stream

interface Filter {
    /**
     * 权限过滤
     *
     * @param messageEvent 消息事件
     * @param annotation 注解信息
     * @author Moyuyanli
     * @date 2024-8-10 14:38
     */
    fun permFilter(
        messageEvent: MessageEvent,
        annotation: MessageAuthorize,
        methodType: Class<out MessageEvent>,
    ): Boolean

    /**
     * 消息过滤
     *
     * @param messageEvent 消息事件
     * @param annotation 注解信息
     * @author Moyuyanli
     * @date 2024/8/20 15:27
     */
    fun messageFilter(messageEvent: MessageEvent, annotation: MessageAuthorize): Boolean
}


class MessageFilter(
    private val plugin: JvmPlugin,
    private val channel: EventChannel<MessageEvent>,
    private val handleApi: ExceptionHandleApi,
    private val prefix: String,
) : Filter {

    companion object {
        fun register(
            classList: Set<Class<*>>,
            channel: EventChannel<MessageEvent>,
            handleApi: ExceptionHandleApi,
            prefix: String,
            plugin: JvmPlugin
        ) {
            val register = MessageFilter(plugin, channel, handleApi, prefix)

            for (clazz in classList) {
                val name = clazz.name
                log.debug("已扫描到消息注册类-> $name ")

                var instance: Any
                try {
                    instance = clazz.getConstructor().newInstance()
                } catch (e: Throwable) {
                    log.error("注册类: $name 实例化失败!", e)
                    continue
                }

                val methods: Array<Method> = clazz.getDeclaredMethods()
                val stream = Arrays.stream(methods)
                register.frontFilter(stream, instance)
            }
        }
    }

    /**
     * 前置过滤
     *
     * @param method 方法过滤流
     * @param instance 实例
     * @author Moyuyanli
     * @date 2024-8-10 14:38:07
     */
    fun frontFilter(method: Stream<Method>, instance: Any) {
        method.filter {
            it.isAnnotationPresent(MessageAuthorize::class.java) && it.parameterTypes.isNotEmpty()
        }
            .forEach {
                val paramsType = it.parameterTypes[0]
                if (MessageEvent::class.java.isAssignableFrom(paramsType)) {
                    val methodType = paramsType.asSubclass(MessageEvent::class.java)
                    execute(instance, it, channel.filterIsInstance(methodType), methodType)
                } else {
                    log.warning("类[${instance.javaClass.name}]中方法[${it.name}]的参数类型异常，请检查!")
                }
            }
    }

    /**
     * 执行
     * 进行事件监听注册注册
     *
     * @param bean    实体
     * @param method  方法
     * @param channel 监听channel
     * @author Moyuyanli
     * @date 2023/8/11 10:11
     */
    private fun execute(
        bean: Any,
        method: Method,
        channel: EventChannel<MessageEvent>,
        methodType: Class<out MessageEvent>,
    ) {
        val annotation = method.getAnnotation(MessageAuthorize::class.java)


        log.debug("注册消息事件方法-> ${method.name}")
        channel.exceptionHandler { handleApi.handle(it) }
            .filter {
                val time = DateUtil.timer()
                val result = permFilter(it, annotation, methodType) && messageFilter(it, annotation)
                if (result) log.debug("${method.name} 匹配用时 ${time.intervalMs()} ms")
                result
            }
            .subscribeAlways<MessageEvent>(
                concurrency = annotation.concurrency,
                priority = annotation.priority
            ) {
                if (method.parameterCount == 1) {
                    try {
                        val timer = DateUtil.timer()
                        method.invoke(bean, it)
                        log.debug("${method.name} 执行用时 ${timer.intervalMs()} ms")
                    } catch (e: Throwable) {
                        handleApi.handle(e)
                    }
                } else {
                    // 创建 Continuation 实例
                    try {
                        val timer = DateUtil.timer()
                        // 通过反射调用 suspend 函数
                        method.invoke(bean, it, plugin)
                        log.debug("${method.name} 执行用时 ${timer.intervalMs()} ms")
                    } catch (e: Exception) {
                        handleApi.handle(e)
                    }
                }
            }
    }

    /**
     * 权限过滤
     *
     * @param messageEvent 消息事件
     * @param annotation 注解信息
     * @author Moyuyanli
     * @date 2024-8-20 16:17
     */
    override fun permFilter(
        messageEvent: MessageEvent, annotation: MessageAuthorize,
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
     * 消息过滤
     *
     * @param messageEvent 消息事件
     * @param annotation 注解信息
     * @author Moyuyanli
     * @date 2024-8-20 16:18
     */
    override fun messageFilter(messageEvent: MessageEvent, annotation: MessageAuthorize): Boolean {
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
                return false
            }

            cn.chahuyun.authorize.constant.MessageMatchingEnum.REGULAR -> Pattern.matches(annotation.text[0], message)
            cn.chahuyun.authorize.constant.MessageMatchingEnum.CUSTOM -> {
                val instance = annotation.custom.java.getDeclaredConstructor().newInstance()
                return instance.custom(event = messageEvent)
            }
        }
    }

    /**
     * 用户权限过滤
     * @param perms 用户权限列表
     * @param messageEvent 消息事件
     */
    private fun userPermMatch(
        perms: Array<String>,
        match: PermissionMatchingEnum,
        messageEvent: MessageEvent,
    ): Boolean {
        val globalUser = UserUtil.globalUser(messageEvent.sender.id)
        val isGroup = messageEvent is GroupMessageEvent
        val groupUser = if (isGroup) {
            UserUtil.member(messageEvent.subject.id, messageEvent.sender.id)
        } else null

        return when (match) {
            OR -> {
                for (perm in perms) {
                    if (checkSinglePerm(perm, globalUser, groupUser, isGroup, messageEvent)) {
                        return true
                    }
                }
                false
            }

            AND -> {
                for (perm in perms) {
                    if (!checkSinglePerm(perm, globalUser, groupUser, isGroup, messageEvent)) {
                        return false
                    }
                }
                true
            }
        }
    }

    /**
     * 单个权限过滤
     * @param permCode 权限码
     * @param globalUser 全局用户
     * @param groupUser 群组用户
     * @param isGroup 是否是群组
     * @param messageEvent 消息事件
     */
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

    /**
     * 群权限过滤
     * @param perms 群权限
     * @param messageEvent 消息事件
     */
    private fun groupPermMatch(
        perms: Array<String>,
        match: PermissionMatchingEnum,
        messageEvent: MessageEvent,
    ): Boolean {
        if (messageEvent !is GroupMessageEvent) return false

        val groupUser = UserUtil.group(groupId = messageEvent.group.id)
        val permCodes = perms.toList()


        val permsMap = permCodes.associateWith { PermCache.get(it) }

        val missing = permsMap.filterValues { it == null }.keys
        if (missing.isNotEmpty()) {
            log.warning("以下群权限未注册: ${missing.joinToString()}")
            return if (match == AND) false else false // OR 模式下，有缺失但可能其他命中
        }

        return when (match) {
            OR -> {
                permsMap.any { (_, perm) ->
                    perm?.permGroup?.any { group -> group.users.contains(groupUser) } == true
                }
            }

            AND -> {
                permsMap.all { (_, perm) ->
                    perm?.permGroup?.any { group -> group.users.contains(groupUser) } == true
                }
            }
        }
    }
}