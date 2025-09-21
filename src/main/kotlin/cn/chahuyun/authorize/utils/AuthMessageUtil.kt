package cn.chahuyun.authorize.utils

import cn.chahuyun.authorize.HuYanAuthorize
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.*

abstract class MessageUtilTemplate {


    protected val channel: EventChannel<MessageEvent> by lazy {
        GlobalEventChannel.parentScope(HuYanAuthorize).filterIsInstance(MessageEvent::class)
    }

    //== 接受消息的方法工具 ==

    /**
     * 获取群组的下一条消息,不区分环境,默认30秒超时
     * 优先使用此方法
     *
     * @see MessageUtilTemplate.nextGroupMessage
     */
    suspend fun Group.nextMessage(): MessageChain? = nextGroupMessage(this, 30)

    /**
     * 获取群组的下条消息,不区分环境
     * 优先使用此方法
     *
     * @param timer 超时时间(秒)
     * @see MessageUtilTemplate.nextGroupMessage
     */
    suspend fun Group.nextMessage(timer: Int): MessageChain? = nextGroupMessage(this, timer)


    /**
     * 等待指定群组中的下一条消息。
     *
     * @param group 要监听的群组对象
     * @return 收到的消息（[MessageChain]），如果没有收到则挂起直到有消息
     */
    suspend fun nextGroupMessage(group: Group): MessageChain? {
        return nextGroupMessageEvent(group.id)?.message
    }

    /**
     * 等待指定群组中的下一条消息事件。
     *
     * @param group 要监听的群组对象
     * @return 收到的消息事件（[MessageEvent]），如果没有收到则挂起直到有消息
     */
    suspend fun nextGroupMessageEvent(group: Group): GroupMessageEvent? {
        return nextGroupMessageEvent(group.id)
    }

    /**
     * 等待指定群组在一定时间内发送的消息（带超时机制）。
     *
     * @param group 要监听的群组对象
     * @param timer 超时时间（单位：秒）
     * @return 如果在规定时间内收到消息则返回 [MessageChain]，否则返回 null
     */
    suspend fun nextGroupMessage(group: Group, timer: Int): MessageChain? = withTimeoutOrNull(timer * 1000L) {
        nextGroupMessageEvent(group.id)?.message
    }


    /**
     * 等待指定群组在一定时间内发送的消息事件（带超时机制）。
     *
     * @param group 要监听的群组对象
     * @param timer 超时时间（单位：秒）
     * @return 如果在规定时间内收到消息则返回 [MessageEvent]，否则返回 null
     */
    suspend fun nextGroupMessageEvent(group: Group, timer: Int): GroupMessageEvent? = withTimeoutOrNull(timer * 1000L) {
        nextGroupMessageEvent(group.id)
    }

    /**
     * 等待指定群组 ID 的下一条消息事件。
     *
     * @param groupId 要监听的群组 ID
     * @return 收到的消息事件（[MessageEvent]），如果没有收到则挂起直到有消息
     */
    suspend fun nextGroupMessageEvent(groupId: Long): GroupMessageEvent? = callbackFlow {
        val once = this@MessageUtilTemplate.channel.filter { it.subject.id == groupId }
            .subscribeOnce<GroupMessageEvent> { trySend(it) }
        // 当 callbackFlow 结束时取消监听器
        awaitClose {
            once.complete()
        }
    }.firstOrNull()


    /**
     * 等待指定用户 ID 发送的下一条消息。
     *
     * @param senderId 要监听的用户 ID
     * @return 收到的消息事件（[MessageEvent]），如果没有收到则挂起直到有消息
     */
    suspend fun nextMessage(senderId: Long): MessageEvent? = callbackFlow {
        val once = this@MessageUtilTemplate.channel.filter { it.sender.id == senderId }
            .subscribeOnce<MessageEvent> { trySend(it) }
        awaitClose {
            once.complete()
        }
    }.firstOrNull()


    // == 其他消息工具 ==

    /**
     * 构建一条转发消息，支持自定义显示策略。
     *
     * 此方法基于 [ForwardMessageBuilder] 和自定义的 [CustomForwardDisplayStrategy]，
     * 允许开发者灵活设置转发消息的标题、摘要、预览内容等信息。
     *
     * @param titleGenerator 转发消息的标题，默认为 "群聊的聊天记录"
     * @param briefGenerator 转发消息的简要描述，默认为 "[聊天记录]"
     * @param previewGenerator 转发消息的预览内容列表，默认包含两条示例消息
     * @param summarySize 摘要中显示的消息条数，默认为 10 条
     * @param summaryGenerator 摘要内容生成器，默认为 "查看${summarySize}条转发消息"
     * @param sourceGenerator 转发消息来源描述，默认为 "聊天记录"
     * @param block 可选的构建回调，用于进一步自定义 [ForwardMessageBuilder]
     * @return 返回构建完成的 [ForwardMessage]
     */
    @JvmSynthetic
    fun MessageEvent.buildForwardMessage(
        titleGenerator: String = "群聊的聊天记录",
        briefGenerator: String = "[聊天记录]",
        previewGenerator: List<String> = mutableListOf("放空:消息A", "放空:消息B"),
        summarySize: Int = 10,
        summaryGenerator: String = "查看${summarySize}条转发消息",
        sourceGenerator: String = "聊天记录",
        block: ForwardMessageBuilder.() -> Unit
    ): ForwardMessage = ForwardMessageBuilder(this.subject).apply {
        this.displayStrategy = CustomForwardDisplayStrategy(
            titleGenerator,
            briefGenerator,
            previewGenerator,
            summarySize,
            summaryGenerator,
            sourceGenerator
        )
    }.apply(block).build()

}

/**
 * @param titleGenerator 显示标题
 * @param briefGenerator 消息栏显示简介
 * @param previewGenerator 消息栏预览消息，默认两条
 * @param summarySize 查看消息数量
 * @param summaryGenerator 消息栏总结，查看消息
 * @param sourceGenerator 未知显示
 */
class CustomForwardDisplayStrategy(
    private val titleGenerator: String = "群聊的聊天记录",
    private val briefGenerator: String = "[聊天记录]",
    private val previewGenerator: List<String> = mutableListOf("放空:1", "放空:2"),
    private val summarySize: Int = 10,
    private val summaryGenerator: String = "查看${summarySize}条转发消息",
    private val sourceGenerator: String = "聊天记录",
) : ForwardMessage.DisplayStrategy {

    override fun generateTitle(forward: RawForwardMessage): String = titleGenerator
    override fun generateBrief(forward: RawForwardMessage): String = briefGenerator
    override fun generateSource(forward: RawForwardMessage): String = sourceGenerator
    override fun generatePreview(forward: RawForwardMessage): List<String> = previewGenerator
    override fun generateSummary(forward: RawForwardMessage): String = summaryGenerator
}

object AuthMessageUtil : MessageUtilTemplate(){

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