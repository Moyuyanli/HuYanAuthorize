package cn.chahuyun.authorize.manager

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.utils.getSystemInfo
import net.mamoe.mirai.event.events.MessageEvent

@EventComponent
class PermManager() {


    @MessageAuthorize(
        text = ["进行测试"]
    )
    suspend fun test(messageEvent: MessageEvent) {
        messageEvent.subject.sendMessage(getSystemInfo())
    }


}
