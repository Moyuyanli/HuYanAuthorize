package cn.chahuyun.authorize.Interface;

import net.mamoe.mirai.event.Event;

/**
 * 自定义匹配规则
 *
 * @author Moyuyanli
 * @date 2023/8/7 18:19
 */
public interface CustomPattern {

    /**
     * 一个自定义匹配规则
     *
     * @param event 事件
     * @return boolean
     * @author Moyuyanli
     * @date 2023/8/7 18:20
     */
    boolean custom(Event event);
}
