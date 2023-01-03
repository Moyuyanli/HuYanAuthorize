package cn.chahuyun.authorize.annotation;

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
     * 文本
     */
    String[] text();

    /**
     * 消息内容匹配方式
     */
    MessageMatchingEnum messageMatching() default MessageMatchingEnum.TEXT;

    /**
     * 用户权限
     */
    String[] userPermissions() default "null";

    /**
     * 用户权限匹配方式
     */
    PermissionMatchingEnum userPermissionsMatching() default PermissionMatchingEnum.OR;

    /**
     * 群权限
     */
    String[] groupPermissions() default "null";

    /**
     * 群权限匹配方式
     */
    PermissionMatchingEnum groupPermissionsMatching() default PermissionMatchingEnum.OR;

    /**
     * 优先级
     */
    EventPriority priority() default EventPriority.NORMAL;

    /**
     * 消息处理方式
     */
    ConcurrencyKind concurrency() default ConcurrencyKind.LOCKED;
}
