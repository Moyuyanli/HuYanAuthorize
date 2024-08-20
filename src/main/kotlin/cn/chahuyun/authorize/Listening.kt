package cn.chahuyun.authorize

import cn.chahuyun.authorize.constant.PermConstant
import cn.chahuyun.authorize.constant.PermissionMatchingEnum
import cn.chahuyun.authorize.constant.PermissionMatchingEnum.AND
import cn.chahuyun.authorize.constant.PermissionMatchingEnum.OR
import cn.chahuyun.authorize.constant.UserType
import cn.chahuyun.authorize.entity.Perm
import cn.chahuyun.authorize.entity.User
import cn.chahuyun.authorize.utils.Log
import cn.chahuyun.authorize.utils.Log.debug
import cn.chahuyun.authorize.utils.Log.error
import cn.chahuyun.hibernateplus.HibernateFactory
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import java.lang.reflect.Method
import java.util.*
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


class MessageFilter(private val channel: EventChannel<MessageEvent>) : Filter {

    companion object {
        fun register(classList: Set<Class<*>>, channel: EventChannel<MessageEvent>) {
            val register = MessageFilter(channel)

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
                register.permFilter(stream, instance)
            }
        }
    }

    /**
     * 过滤
     *
     * @param method 方法过滤流
     * @param instance 实例
     * @author Moyuyanli
     * @date 2024-8-10 14:38:07
     */
    fun permFilter(method: Stream<Method>, instance: Any) {
        method.filter { it.isAnnotationPresent(MessageAuthorize::class.java) && it.parameterCount == 1 }
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

        channel.filter { permFilter(it, annotation, methodType) && messageFilter(it, annotation) }
            .subscribeAlways(
                methodType,
                concurrency = annotation.concurrency,
                priority = annotation.priority
            ) {
                method.invoke(bean, it)
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

        userPermMatch = if (userPerms.contains(PermConstant.NULL)) {
            true
        } else {
            userPermMatch(userPerms, annotation.userPermissionsMatching, messageEvent)
        }

        if (!GroupMessageEvent::class.java.isAssignableFrom(methodType)) {
            return userPermMatch
        }

        groupPermMatch = if (groupPerms.contains(PermConstant.NULL)) {
            true
        } else {
            groupPermMatch(groupPerms, messageEvent)
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
        return true
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

        val globalUser = User(
            type = UserType.GLOBAL_USER,
            userId = messageEvent.sender.id
        )

        val isGroup = messageEvent is GroupMessageEvent
        val groupUser = if (isGroup) {
            User(
                type = UserType.GROUP_MEMBER,
                userId = messageEvent.sender.id,
                groupId = messageEvent.subject.id
            )
        } else {
            null
        }

        var result: Boolean? = null
        var temp:Boolean

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
                val filter = users.filter { it.type == UserType.GROUP_ADMIN }
                if (filter.isNotEmpty()) {

                }
                if (users.contains(globalUser) || (isGroup && users.contains(groupUser))) {
                    result?.let { result = true }
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

    /**
     * 群权限过滤
     * @param perms 群权限
     * @param messageEvent 消息事件
     */
    private fun groupPermMatch(perms: Array<String>, messageEvent: MessageEvent): Boolean {
        return false
    }

}