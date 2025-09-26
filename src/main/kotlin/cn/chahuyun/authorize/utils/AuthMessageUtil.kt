package cn.chahuyun.authorize.utils

import cn.chahuyun.authorize.HuYanAuthorize
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.*

/**
 * 消息工具接口 - 为Java和Kotlin双平台提供兼容支持
 *
 * Kotlin开发者应优先使用suspend函数（如nextGroupMessage()）
 * Java开发者应使用以下明示方法：
 *   - nextGroupMessageAsync() / nextGroupMessageEventAsync() / nextMessageAsync()
 *   - nextGroupMessageSync() / nextGroupMessageEventSync() / nextMessageSync()
 *
 * 注意：Java开发者请勿直接调用Kotlin的suspend函数，会导致编译错误
 */
interface MessageUtil {
    /**
     * 事件通道（Kotlin开发者使用）
     * Kotlin开发者应通过此属性获取事件通道
     */
    val channel: EventChannel<MessageEvent>

    // ================ Java开发者专用方法（推荐使用回调式） ================

    /**
     * 异步等待群组消息（Java推荐方式）
     *
     * @param groupId 群组ID
     * @param timeout 超时时间（秒）
     * @param callback 回调函数，收到消息时调用，超时返回null
     *
     * Java示例：
     *   messageUtil.nextGroupMessageAsync(123456, 30, message -> {
     *       if (message != null) {
     *           System.out.println("收到消息: " + message);
     *       }
     *   });
     */
    fun nextGroupMessageAsync(groupId: Long, timeout: Int, callback: (MessageChain?) -> Unit)

    /**
     * 异步等待群组消息事件（Java推荐方式）
     *
     * @param groupId 群组ID
     * @param timeout 超时时间（秒）
     * @param callback 回调函数，收到消息事件时调用，超时返回null
     */
    fun nextGroupMessageEventAsync(groupId: Long, timeout: Int, callback: (GroupMessageEvent?) -> Unit)

    /**
     * 异步等待指定用户消息（Java推荐方式）
     *
     * @param senderId 用户ID
     * @param timeout 超时时间（秒）
     * @param callback 回调函数，收到消息时调用，超时返回null
     */
    fun nextMessageEventAsync(senderId: Long, timeout: Int, callback: (MessageEvent?) -> Unit)

    /**
     * 异步等待指定群组指定用户的下条消息（Java推荐方式）
     *
     * @param groupId 群组ID
     * @param userId 用户ID
     * @param timeout 超时时间（秒）
     * @param callback 回调函数，收到消息事件时调用，超时返回null
     */
    fun nextUserForGroupMessageEventAsync(
        groupId: Long,
        userId: Long,
        timeout: Int,
        callback: (GroupMessageEvent?) -> Unit
    )

    // ================ Java开发者专用方法（同步阻塞方式，不推荐在主线程使用） ================

    /**
     * 同步等待群组消息（Java同步方式，不推荐在主线程使用）
     *
     * @param groupId 群组ID
     * @param timeout 超时时间（秒）
     * @return 收到的消息，超时返回null
     *
     * 注意：此方法会阻塞当前线程，仅建议在后台线程调用
     */
    fun nextGroupMessageSync(groupId: Long, timeout: Int): MessageChain?

    /**
     * 同步等待群组消息事件（Java同步方式，不推荐在主线程使用）
     *
     * @param groupId 群组ID
     * @param timeout 超时时间（秒）
     * @return 收到的消息事件，超时返回null
     */
    fun nextGroupMessageEventSync(groupId: Long, timeout: Int): GroupMessageEvent?

    /**
     * 同步等待指定用户消息（Java同步方式，不推荐在主线程使用）
     *
     * @param senderId 用户ID
     * @param timeout 超时时间（秒）
     * @return 收到的消息事件，超时返回null
     */
    fun nextMessageEventSync(senderId: Long, timeout: Int): MessageEvent?

    /**
     * 获取指定群组指定用户的下一条消息（Java推荐方式）
     *
     * @param groupId 群组ID
     * @param userId 用户ID
     * @param timeout 超时时间（秒）
     * @return 获取到的消息事件，超时返回null
     */
    fun nextUserForGroupMessageEventSync(groupId: Long, userId: Long, timeout: Int): GroupMessageEvent?
}

/**
 * 消息工具模板类
 *
 * 设计理念：
 * 1. 本类作为模板，需要传入插件自己的 EventChannel
 * 2. Kotlin开发者直接使用suspend函数（无需额外实现）
 * 3. Java开发者通过接口方法使用（Async/Sync）
 *
 * 使用示例（Kotlin）：
 * class MyMessageUtil : MessageUtilTemplate(huYanAuthorize.eventChannel) {
 *     // 无需额外实现，直接使用suspend函数
 * }
 *
 * 使用示例（Java）：
 * public class MyMessageUtil extends MessageUtilTemplate {
 *     public MyMessageUtil(EventChannel<MessageEvent> channel) {
 *         super(channel);
 *     }
 * }
 */
abstract class MessageUtilTemplate : MessageUtil {
    /**
     * 消息工具事件通道
     */
    abstract override val channel: EventChannel<MessageEvent>

    // ================ Kotlin开发者专用方法（suspend函数） ================
    // 这些方法在子类中直接可用，Kotlin开发者无需额外实现

    /**
     * 获取群组的下一条消息（Kotlin推荐方式）
     *
     * @param group 要监听的群组
     * @param timer 超时时间（秒），默认30秒
     * @return 收到的消息，超时返回null
     *
     * Kotlin示例：
     *   val message = myMessageUtil.nextGroupMessage(group, 60)
     */
    suspend fun nextGroupMessage(group: Group, timer: Int = 30): MessageChain? {
        return nextGroupMessageEvent(group.id, timer)?.message
    }

    /**
     * 获取群组的下一条消息（Kotlin推荐方式）
     *
     * @param groupId 要监听的群组id
     * @param timer 超时时间（秒），默认30秒
     * @return 收到的消息，超时返回null
     *
     * Kotlin示例：
     *   val message = myMessageUtil.nextGroupMessage(123456L, 60)
     */
    suspend fun nextGroupMessage(groupId: Long, timer: Int = 30): MessageChain? {
        return nextGroupMessageEvent(groupId, timer)?.message
    }

    /**
     * 获取群组的下一条消息事件（Kotlin推荐方式）
     *
     * @param group 要监听的群组
     * @param timer 超时时间（秒），默认30秒
     * @return 收到的消息事件，超时返回null
     */
    suspend fun nextGroupMessageEvent(group: Group, timer: Int = 30): GroupMessageEvent? {
        return nextGroupMessageEvent(group.id, timer)
    }

    /**
     * 等待指定群组ID的下一条消息事件（Kotlin推荐方式）
     *
     * @param groupId 要监听的群组ID
     * @param timer 超时时间（秒），默认30秒
     * @return 收到的消息事件，超时返回null
     */
    suspend fun nextGroupMessageEvent(groupId: Long, timer: Int = 30): GroupMessageEvent? =
        withTimeoutOrNull(timer * 1000L) {
            callbackFlow {
                val once = this@MessageUtilTemplate.channel.filter { it.subject.id == groupId }
                    .subscribeOnce<GroupMessageEvent> { trySend(it) }
                awaitClose { once.complete() }
            }.firstOrNull()
        }

    /**
     * 等待指定用户ID的下一条消息（Kotlin推荐方式）
     *
     * @param senderId 要监听的用户ID
     * @param timer 超时时间（秒），默认30秒
     * @return 收到的消息事件，超时返回null
     */
    suspend fun nextMessage(senderId: Long, timer: Int = 30): MessageEvent? = withTimeoutOrNull(timer * 1000L) {
        callbackFlow {
            val once = this@MessageUtilTemplate.channel.filter { it.sender.id == senderId }
                .subscribeOnce<MessageEvent> { trySend(it) }
            awaitClose { once.complete() }
        }.firstOrNull()
    }

    /**
     * 获取指定群组指定用户的下条消息事件（Kotlin推荐方式）
     *
     * @param groupId 群组ID
     * @param userId 用户ID
     * @param timer 超时时间（秒），默认30秒
     * @return 获取到的消息事件，超时返回null
     */
    suspend fun nextUserForGroupMessageEvent(groupId: Long, userId: Long, timer: Int = 30): GroupMessageEvent? =
        withTimeoutOrNull(timer * 1000L) {
            callbackFlow {
                val once =
                    this@MessageUtilTemplate.channel.filter { it.subject.id == groupId && it.sender.id == userId }
                        .subscribeOnce<GroupMessageEvent> { trySend(it) }
                awaitClose { once.complete() }
            }.firstOrNull()
        }

    // ================ Java开发者专用方法（实现接口） ================
    // 这些方法由模板类实现，Java开发者通过接口调用

    /**
     * 异步等待群组消息（Java推荐方式）
     *
     * @param groupId 群组ID
     * @param timeout 超时时间（秒）
     * @param callback 回调函数，收到消息时调用，超时返回null
     */
    override fun nextGroupMessageAsync(groupId: Long, timeout: Int, callback: (MessageChain?) -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            callback(nextGroupMessage(groupId, timeout))
        }
    }

    /**
     * 异步等待群组消息事件（Java推荐方式）
     *
     * @param groupId 群组ID
     * @param timeout 超时时间（秒）
     * @param callback 回调函数，收到消息事件时调用，超时返回null
     */
    override fun nextGroupMessageEventAsync(groupId: Long, timeout: Int, callback: (GroupMessageEvent?) -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            callback(nextGroupMessageEvent(groupId, timeout))
        }
    }

    /**
     * 异步等待指定用户消息（Java推荐方式）
     *
     * @param senderId 用户ID
     * @param timeout 超时时间（秒）
     * @param callback 回调函数，收到消息时调用，超时返回null
     */
    override fun nextMessageEventAsync(senderId: Long, timeout: Int, callback: (MessageEvent?) -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            callback(nextMessage(senderId, timeout))
        }
    }

    /**
     * 异步等待指定群组指定用户的下条消息（Java推荐方式）
     *
     * @param groupId 群组ID
     * @param userId 用户ID
     * @param timeout 超时时间（秒）
     * @param callback 回调函数，收到消息事件时调用，超时返回null
     */
    override fun nextUserForGroupMessageEventAsync(
        groupId: Long,
        userId: Long,
        timeout: Int,
        callback: (GroupMessageEvent?) -> Unit
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            callback(nextUserForGroupMessageEvent(groupId, userId, timeout))
        }
    }

    /**
     * 获取指定群组指定用户的下一条消息（Java推荐方式）
     *
     * @param groupId 群组ID
     * @param userId 用户ID
     * @param timeout 超时时间（秒）
     * @return 获取到的消息事件，超时返回null
     */
    override fun nextUserForGroupMessageEventSync(
        groupId: Long,
        userId: Long,
        timeout: Int
    ): GroupMessageEvent? {
        return runBlocking { nextUserForGroupMessageEvent(groupId, userId, timeout) }
    }

    /**
     * 同步等待群组消息（Java同步方式，不推荐在主线程使用）
     *
     * @param groupId 群组ID
     * @param timeout 超时时间（秒）
     * @return 收到的消息，超时返回null
     */
    override fun nextGroupMessageSync(groupId: Long, timeout: Int): MessageChain? {
        return runBlocking { nextGroupMessage(groupId, timeout) }
    }

    /**
     * 同步等待群组消息事件（Java同步方式，不推荐在主线程使用）
     *
     * @param groupId 群组ID
     * @param timeout 超时时间（秒）
     * @return 收到的消息事件，超时返回null
     */
    override fun nextGroupMessageEventSync(groupId: Long, timeout: Int): GroupMessageEvent? {
        return runBlocking { nextGroupMessageEvent(groupId, timeout) }
    }

    /**
     * 同步等待指定用户消息（Java同步方式，不推荐在主线程使用）
     *
     * @param senderId 用户ID
     * @param timeout 超时时间（秒）
     * @return 收到的消息事件，超时返回null
     */
    override fun nextMessageEventSync(senderId: Long, timeout: Int): MessageEvent? {
        return runBlocking { nextMessage(senderId, timeout) }
    }

    // ================ 其他实用工具方法 ================
    // 这些方法在Kotlin中可用，Java中通过单例调用

    /**
     * 构建转发消息（Kotlin推荐方式）
     *
     * @param titleGenerator 标题生成器
     * @param briefGenerator 简介生成器
     * @param previewGenerator 预览消息列表
     * @param summarySize 摘要显示条数
     * @param summaryGenerator 摘要生成器
     * @param sourceGenerator 来源描述
     * @param block 自定义构建
     * @return 构建完成的转发消息
     */
    fun buildForwardMessage(
        event: MessageEvent,
        titleGenerator: String = "群聊的聊天记录",
        briefGenerator: String = "[聊天记录]",
        previewGenerator: List<String> = listOf("放空:消息A", "放空:消息B"),
        summarySize: Int = 10,
        summaryGenerator: String = "查看${summarySize}条转发消息",
        sourceGenerator: String = "聊天记录",
        block: ForwardMessageBuilder.() -> Unit = {}
    ): ForwardMessage = ForwardMessageBuilder(event.subject).apply {
        this.displayStrategy = CustomForwardDisplayStrategy(
            titleGenerator,
            briefGenerator,
            previewGenerator,
            summarySize,
            summaryGenerator,
            sourceGenerator
        )
    }.apply(block).build()


    // ================ 辅助类 ================

    /**
     * @param titleGenerator 显示标题
     * @param briefGenerator 消息栏显示简介
     * @param previewGenerator 消息栏预览消息，默认两条
     * @param summarySize 查看消息数量
     * @param summaryGenerator 消息栏总结，查看消息
     * @param sourceGenerator 未知显示
     */
    class CustomForwardDisplayStrategy(
        private val titleGenerator: String,
        private val briefGenerator: String,
        private val previewGenerator: List<String>,
        private val summarySize: Int,
        private val summaryGenerator: String,
        private val sourceGenerator: String
    ) : ForwardMessage.DisplayStrategy {

        override fun generateTitle(forward: RawForwardMessage): String = titleGenerator
        override fun generateBrief(forward: RawForwardMessage): String = briefGenerator
        override fun generateSource(forward: RawForwardMessage): String = sourceGenerator
        override fun generatePreview(forward: RawForwardMessage): List<String> = previewGenerator
        override fun generateSummary(forward: RawForwardMessage): String = summaryGenerator
    }
}

object AuthMessageUtil : MessageUtilTemplate() {

    override val channel: EventChannel<MessageEvent> by lazy {
        GlobalEventChannel.parentScope(HuYanAuthorize).filterIsInstance(MessageEvent::class)
    }

    /**
     * 格式化的消息
     *
     *
     *
     * @param format 消息格式
     * @param params 参数
     * @return PlainText 文本消息
     * @author Moyuyanli
     * @date 2022/12/21 11:30
     */
    fun formatMessage(format: String, vararg params: Any?): PlainText {
        return PlainText(String.format(format, *params))
    }

    /**
     * 格式化的消息
     *
     * @param format 消息格式
     * @param params 参数
     * @return MessageChain 消息链
     * @author Moyuyanli
     * @date 2022/12/21 11:30
     */
    fun formatMessageChain(format: String, vararg params: Any?): MessageChain {
        return MessageChainBuilder().append(String.format(format, *params)).build()
    }

    /**
     * 带引用的格式化的消息
     *
     * @param citation 引用消息
     * @param format   消息格式
     * @param params   参数
     * @return MessageChain 消息链
     * @author Moyuyanli
     * @date 2022/12/21 11:30
     */
    fun formatMessageChain(citation: MessageChain, format: String, vararg params: Any?): MessageChain {
        return MessageChainBuilder().append(QuoteReply(citation)).append(String.format(format, *params)).build()
    }

    /**
     * 带at的格式化的消息
     *
     * @param at     at用户
     * @param format 消息格式
     * @param params 参数
     * @return MessageChain 消息链
     * @author Moyuyanli
     * @date 2022/12/21 11:30
     */
    fun formatMessageChain(at: Long, format: String, vararg params: Any?): MessageChain {
        return MessageChainBuilder().append(At(at)).append(String.format(format, *params)).build()
    }


    /**
     * 带引用的消息构造器
     *
     * @param citation 引用消息
     * @return MessageChainBuild 构造消息器
     * @author Moyuyanli
     * @date 2022/12/21 11:30
     */
    fun quoteReply(citation: MessageChain): MessageChainBuilder {
        return MessageChainBuilder().append(QuoteReply(citation))
    }

    /**
     * 格式化的消息
     *
     * @param format 消息格式
     * @param params 参数
     * @return MessageChainBuild 构造消息器
     * @author Moyuyanli
     * @date 2022/12/21 11:30
     */
    fun formatMessageBuild(format: String, vararg params: Any?): MessageChainBuilder {
        return MessageChainBuilder().append(String.format(format, *params))
    }

    /**
     * 带引用的格式化的消息
     *
     * @param citation 引用消息
     * @param format   消息格式
     * @param params   参数
     * @return MessageChainBuild 构造消息器
     * @author Moyuyanli
     * @date 2022/12/21 11:30
     */
    fun formatMessageBuild(citation: MessageChain, format: String, vararg params: Any?): MessageChainBuilder {
        return MessageChainBuilder().append(QuoteReply(citation)).append(String.format(format, *params))
    }

    /**
     * 带at的格式化的消息
     *
     * @param at     at用户
     * @param format 消息格式
     * @param params 参数
     * @return MessageChainBuild 构造消息器
     * @author Moyuyanli
     * @date 2022/12/21 11:30
     */
    fun formatMessageBuild(at: Long, format: String, vararg params: Any?): MessageChainBuilder {
        return MessageChainBuilder().append(At(at)).append(String.format(format, *params))
    }

    /**
     * 直接发送消息
     *
     * @param event 消息事件
     * @param format 格式化消息
     * @param params 格式化参数
     * @return 返回消息
     * @author Moyuyanli
     * @date 2024/8/29 11:09
     */
    suspend fun sendMessage(event: MessageEvent, format: String, vararg params: Any?): MessageReceipt<Contact> {
        return event.subject.sendMessage(String.format(format, *params))
    }

    /**
     * 直接发送消息，同时at这个人
     *
     * @param event 消息事件
     * @param format 格式化消息
     * @param params 格式化参数
     * @return 返回消息
     * @author Moyuyanli
     * @date 2024/8/29 11:09
     */
    suspend fun sendMessageAt(event: MessageEvent, format: String, vararg params: Any?): MessageReceipt<Contact> {
        return event.subject.sendMessage(At(event.sender).plus(PlainText(String.format(format, *params))))
    }

    /**
     * 直接发送消息，同时引用这个消息
     *
     * @param event 消息事件
     * @param format 格式化消息
     * @param params 格式化参数
     * @return 返回消息
     * @author Moyuyanli
     * @date 2024/8/29 11:09
     */
    suspend fun sendMessageQuote(event: MessageEvent, format: String, vararg params: Any?): MessageReceipt<Contact> {
        return event.subject.sendMessage(QuoteReply(event.message).plus(PlainText(String.format(format, *params))))
    }

    suspend fun MessageEvent.sendMessageQuote(message: MessageChain): MessageReceipt<Contact> {
        return this.subject.sendMessage(QuoteReply(this.message).plus(message))
    }

    suspend fun MessageEvent.sendMessageQuote(text: String): MessageReceipt<Contact> {
        return this.subject.sendMessage(QuoteReply(this.message).plus(PlainText(text)))
    }
}