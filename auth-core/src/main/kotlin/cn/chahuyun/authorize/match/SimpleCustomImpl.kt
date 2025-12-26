package cn.chahuyun.authorize.match

import net.mamoe.mirai.event.Event


/**
 * 简单自定义实现类
 * 实现CustomPattern接口，提供自定义匹配规则
 */
class SimpleCustomImpl : CustomPattern {

    /**
     * 一个自定义匹配规则
     *
     * @param event 事件对象，用于匹配判断的输入参数
     * @return Boolean 返回匹配结果，true表示匹配成功，false表示匹配失败
     * @author Moyuyanli
     * @date 2023/8/7 18:20
     */
    override fun custom(event: Event): Boolean {
        return true
    }

}
