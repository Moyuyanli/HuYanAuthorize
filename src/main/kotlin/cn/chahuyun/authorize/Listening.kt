package cn.chahuyun.authorize

import cn.chahuyun.authorize.exception.ExceptionHandleApi
import cn.chahuyun.authorize.listener.ListenerManager
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.events.MessageEvent

/**
 * 兼容旧版本的 Filter 接口
 */
interface Filter {
    fun permFilter(
        messageEvent: MessageEvent,
        annotation: MessageAuthorize,
        methodType: Class<out MessageEvent>,
    ): Boolean

    fun messageFilter(messageEvent: MessageEvent, annotation: MessageAuthorize): Boolean
}

/**
 * 兼容旧版本的 MessageFilter
 */
class MessageFilter {
    companion object {
        fun register(
            classList: Set<Class<*>>,
            channel: EventChannel<MessageEvent>,
            handleApi: ExceptionHandleApi,
            prefix: String,
            plugin: JvmPlugin
        ) {
            ListenerManager.register(classList, channel, handleApi, prefix, plugin)
        }
    }
}
