package cn.chahuyun.authorize.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 群事件注解 预留
 *
 * @author Moyuyanli
 * @date 2023/8/8 9:44
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GroupAuthorize {
}
