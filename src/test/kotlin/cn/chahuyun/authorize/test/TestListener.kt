package cn.chahuyun.authorize.test

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent

@EventComponent
class TestListener {

    @MessageAuthorize(text = ["test"])
    fun onMessage(event: MessageEvent) {
        println("收到消息: ${event.message.contentToString()}")
    }

    @MessageAuthorize(text = ["group test"])
    fun onGroupMessage(event: GroupMessageEvent) {
        println("收到群消息: ${event.message.contentToString()}")
    }
}

