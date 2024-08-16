package cn.chahuyun.authorize.Interface

import net.mamoe.mirai.event.Event


class SimpleCustomImpl:CustomPattern {

    /**
     * 一个自定义匹配规则
     *
     * @param event 事件
     * @return boolean
     * @author Moyuyanli
     * @date 2023/8/7 18:20
     */
    override fun custom(event: Event): Boolean {
        return true
    }

}