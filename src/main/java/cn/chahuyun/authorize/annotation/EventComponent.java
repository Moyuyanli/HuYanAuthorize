package cn.chahuyun.authorize.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明这个类中有方法需要进行事件注册
 *
 * @author Moyuyanli
 * @date 2023/1/3 10:35
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventComponent {
}
