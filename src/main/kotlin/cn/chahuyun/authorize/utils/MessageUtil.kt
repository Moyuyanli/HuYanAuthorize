package cn.chahuyun.authorize.utils

import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.*

object MessageUtil {

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
    suspend fun sendMessageQuery(event: MessageEvent, format: String, vararg params: Any?): MessageReceipt<Contact> {
        return event.subject.sendMessage(QuoteReply(event.message).plus(PlainText(String.format(format, *params))))
    }

    suspend fun MessageEvent.sendMessageQuery(message: MessageChain): MessageReceipt<Contact> {
        return this.subject.sendMessage(QuoteReply(this.message).plus(message))
    }

    suspend fun MessageEvent.sendMessageQuery(text: String): MessageReceipt<Contact> {
        return this.subject.sendMessage(QuoteReply(this.message).plus(PlainText(text)))
    }
}