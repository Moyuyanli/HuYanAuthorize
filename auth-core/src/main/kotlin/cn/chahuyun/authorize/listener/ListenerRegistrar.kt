package cn.chahuyun.authorize.listener

import cn.chahuyun.authorize.exception.ExceptionHandleApi
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.events.MessageEvent

/**
 * 监听器注册器接口
 */
interface ListenerRegistrar {
    fun register(
        channel: EventChannel<MessageEvent>,
        filter: ListenerFilter,
        handleApi: ExceptionHandleApi,
        plugin: JvmPlugin
    )
}

/**
 * KSP 辅助类接口
 * 生成的代码将实现此接口
 */
interface GeneratedListenerRegistrar : ListenerRegistrar

