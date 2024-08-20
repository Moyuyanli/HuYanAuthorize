package cn.chahuyun.authorize

import cn.chahuyun.authorize.Interface.CustomPattern
import cn.chahuyun.authorize.constant.MessageMatchingEnum
import cn.chahuyun.authorize.constant.PermConstant
import cn.chahuyun.authorize.constant.PermissionMatchingEnum
import net.mamoe.mirai.event.ConcurrencyKind
import net.mamoe.mirai.event.EventPriority
import kotlin.reflect.KClass

/**
 * 声明这个类中有方法需要进行事件注册
 *
 * @author Moyuyanli
 * @date 2023/1/3 10:35
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EventComponent


/**
 * 授权信息注解
 *
 *
 *
 * @author Moyuyanli
 * @date 2023/1/3 9:07
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class MessageAuthorize(
    /**
     * 文本
     *
     * 默认:空
     *
     * 当你的消息匹配方式为正则的时候
     *
     * 只会识别第一条正则
     */
    val text: Array<String> = ["null"],

    /**
     * 自定义匹配规则
     * @see CustomPattern
     */
    val custom: KClass<out CustomPattern> = CustomPattern::class,

    /**
     * 消息内容匹配方式
     *
     * 默认:文本匹配
     *
     * @see MessageMatchingEnum
     */
    val messageMatching: MessageMatchingEnum = MessageMatchingEnum.TEXT,

    /**
     * 用户权限
     *
     * 默认:空
     *
     */
    val userPermissions: Array<String> = [PermConstant.NULL],

    /**
     * 用户权限匹配方式
     *
     * 默认:或
     *
     * @see PermissionMatchingEnum
     */
    val userPermissionsMatching: PermissionMatchingEnum = PermissionMatchingEnum.OR,

    /**
     * 群权限
     *
     * 默认:空
     *
     */
    val groupPermissions: Array<String> = [PermConstant.NULL],

    /**
     * 群权限匹配方式
     *
     * 默认:或
     *
     * @see PermissionMatchingEnum
     */
    val groupPermissionsMatching: PermissionMatchingEnum = PermissionMatchingEnum.OR,

    /**
     * 用户权限与群权限关联方式
     */
    val userInGroupPermissionsAssociation: PermissionMatchingEnum = PermissionMatchingEnum.AND,

    /**
     * 优先级
     *
     * 默认:正常
     *
     * @see EventPriority
     */
    val priority: EventPriority = EventPriority.NORMAL,

    /**
     * 消息处理方式
     *
     * 默认:使用 Mutex 保证同一时刻只处理一个事件.
     *
     * @see ConcurrencyKind
     */
    val concurrency: ConcurrencyKind = ConcurrencyKind.LOCKED,

    )