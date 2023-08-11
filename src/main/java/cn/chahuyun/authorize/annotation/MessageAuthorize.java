package cn.chahuyun.authorize.annotation;

import cn.chahuyun.authorize.Interface.CustomPattern;
import cn.chahuyun.authorize.Interface.impl.SimpleCustom;
import cn.chahuyun.authorize.enums.MessageMatchingEnum;
import cn.chahuyun.authorize.enums.PermissionMatchingEnum;
import net.mamoe.mirai.event.ConcurrencyKind;
import net.mamoe.mirai.event.EventPriority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 授权信息注解<p>
 *
 * @author Moyuyanli
 * @date 2023/1/3 9:07
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageAuthorize {
    /**
     * 文本<p>
     * 默认:空<p>
     * 当你的消息匹配方式为正则的时候<p>
     * 只会识别第一条正则<p>
     */
    String[] text() default "null";

    /**
     * 自定义匹配规则
     * @see CustomPattern
     */
    Class<? extends CustomPattern> custom() default SimpleCustom.class;

    /**
     * 消息内容匹配方式<p>
     * 默认:文本匹配<p>
     *
     * @see MessageMatchingEnum
     */
    MessageMatchingEnum messageMatching() default MessageMatchingEnum.TEXT;

    /**
     * 用户权限<p>
     * 默认:空<p>
     */
    String[] userPermissions() default "null";

    /**
     * 用户权限匹配方式<p>
     * 默认:或<p>
     *
     * @see PermissionMatchingEnum
     */
    PermissionMatchingEnum userPermissionsMatching() default PermissionMatchingEnum.OR;

    /**
     * 群权限<p>
     * 默认:空<p>
     */
    String[] groupPermissions() default "null";

    /**
     * 群权限匹配方式<p>
     * 默认:或<p>
     *
     * @see PermissionMatchingEnum
     */
    PermissionMatchingEnum groupPermissionsMatching() default PermissionMatchingEnum.OR;

    /**
     * 优先级<p>
     * 默认:正常<p>
     *
     * @see EventPriority
     */
    EventPriority priority() default EventPriority.NORMAL;

    /**
     * 消息处理方式<p>
     * 默认:使用 Mutex 保证同一时刻只处理一个事件.<p>
     *
     * @see ConcurrencyKind
     */
    ConcurrencyKind concurrency() default ConcurrencyKind.LOCKED;
}
