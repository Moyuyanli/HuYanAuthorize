package cn.chahuyun.authorize

import cn.chahuyun.authorize.constant.MessageConversionEnum.*
import cn.chahuyun.authorize.constant.AuthPerm
import cn.chahuyun.authorize.constant.PermissionMatchingEnum
import cn.chahuyun.authorize.constant.PermissionMatchingEnum.AND
import cn.chahuyun.authorize.constant.PermissionMatchingEnum.OR
import cn.chahuyun.authorize.constant.UserType
import cn.chahuyun.authorize.entity.Perm
import cn.chahuyun.authorize.exception.ExceptionHandleApi
import cn.chahuyun.authorize.utils.ContinuationUtil
import cn.chahuyun.authorize.utils.Log
import cn.chahuyun.authorize.utils.Log.debug
import cn.chahuyun.authorize.utils.Log.error
import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.hutool.core.date.DateUtil
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
        ) {
            val register = MessageFilter(channel, handleApi, prefix)

            for (clazz in classList) {
                val name = clazz.name
                debug("已扫描到消息注册类-> $name ")

                var instance: Any
                try {
                    instance = clazz.getConstructor().newInstance()
                } catch (e: Exception) {
                    error("注册类: $name 实例化失败!", e)
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
                    Log.warning("类[${instance.javaClass.name}]中方法[${it.name}]的参数类型异常，请检查!")
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



        debug("注册消息事件方法-> ${method.name}")
        channel.exceptionHandler { handleApi.handle(it) }
            .filter {
                val time = DateUtil.timer()
                val result = permFilter(it, annotation, methodType) && messageFilter(it, annotation)
                if (result) debug("${method.name} 匹配用时 ${time.intervalMs()} ms")
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
                        debug("${method.name} 执行用时 ${timer.intervalMs()} ms")
                    } catch (e: Exception) {
                        handleApi.handle(e)
                    }
                } else {
                    // 创建 Continuation 实例
                    val continuation = ContinuationUtil.getContinuation()
                    try {
                        val timer = DateUtil.timer()
                        // 通过反射调用 suspend 函数
                        method.invoke(bean, it, continuation)
                        debug("${method.name} 执行用时 ${timer.intervalMs()} ms")
                    } catch (e: Exception) {
                        handleApi.handle(e)
                    }
                    // 等待协程完成
                    ContinuationUtil.closeContinuation(continuation)
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
                message = message.substring(1)
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
        } else {
            null
        }

        var result: Boolean? = null
        var temp: Boolean

        for (perm in perms) {
            val one = HibernateFactory.selectOne(Perm::class.java, "code", perm)
                ?: throw RuntimeException("权限 $perm 没有注册!")

            val permGroup = one.permGroup
            if (permGroup.isEmpty()) {
                if (match == AND) return false else continue
            }

            temp = false
            for (group in permGroup) {
                val users = group.users

                //全局用户权限校验
                if (users.contains(globalUser)) {
                    result = result ?: true
                    temp = true
                    break
                }

                if (isGroup) {
                    //群用户校验
                    if (users.contains(groupUser)) {
                        result = result ?: true
                        temp = true
                        break
                    }

                    //群管理权限校验
                    val filter = users.filter { it.type == UserType.GROUP_ADMIN }
                    if (filter.isNotEmpty()) {
                        for (user in filter) {
                            if (groupUser != null && user.groupId == groupUser.groupId) {
                                if (messageEvent is GroupMessageEvent) {
                                    val member = messageEvent.group[groupUser.userId!!]
                                    if (member?.permission == MemberPermission.OWNER || member?.permission == MemberPermission.ADMINISTRATOR) {
                                        result = result ?: true
                                        temp = true
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
            }

            result = when (match) {
                OR -> if (temp) return true else result == true
                AND -> result == true && temp
            }
        }

        return result == true
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

        val user = UserUtil.group(groupId = messageEvent.group.id)

        var result: Boolean? = null

        for (perm in perms) {
            val one = HibernateFactory.selectOne(Perm::class.java, "code", perm)
                ?: throw RuntimeException("权限 $perm 没有注册!")

            val permGroup = one.permGroup
            if (permGroup.isEmpty()) {
                if (match == AND) return false else continue
            }

            var temp = false

            for (group in permGroup) {
                if (group.users.contains(user)) {
                    result = result == true
                    temp = true
                    break
                }
            }

            result = when (match) {
                OR -> if (temp) return true else result == true
                AND -> result == true && temp
            }

        }
        return result == true
    }
}