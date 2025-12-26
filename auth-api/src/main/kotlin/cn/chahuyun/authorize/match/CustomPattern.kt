package cn.chahuyun.authorize.match

import net.mamoe.mirai.event.Event


/**
 * 自定义匹配
 *
 * @see SimpleCustomImpl
 */
interface CustomPattern {

    /**
     * 一个自定义匹配规则
     *
     * @param event 事件
     * @return boolean
     * @author Moyuyanli
     * @date 2023/8/7 18:20
     */
    fun custom(event: Event): Boolean

}

