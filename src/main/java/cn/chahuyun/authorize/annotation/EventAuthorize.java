package cn.chahuyun.authorize.annotation;

import cn.chahuyun.authorize.Interface.impl.SimpleCustom;
import net.mamoe.mirai.event.ConcurrencyKind;
import net.mamoe.mirai.event.EventPriority;

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
public @interface EventAuthorize {

    /**
     * 自定义匹配规则
     * @see CustomPattern
     */
    Class<? extends CustomPattern> custom() default SimpleCustom.class;

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
