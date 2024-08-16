package cn.chahuyun.authorize.Interface.impl;

import net.mamoe.mirai.event.Event;

/**
 * 简单自定义过滤
 *
 * @author Moyuyanli
 * @date 2023/8/8 9:46
 */
public class SimpleCustom implements CustomPattern {

    /**
     * 一个自定义匹配规则
     *
     * @param event 事件
     * @return boolean
     * @author Moyuyanli
     * @date 2023/8/7 18:20
     */
    @Override
    public boolean custom(Event event) {
        return true;
    }
}
