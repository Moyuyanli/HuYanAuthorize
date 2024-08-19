package cn.chahuyun.authorize

import cn.chahuyun.authorize.utils.Log
import cn.chahuyun.authorize.utils.Log.debug
import cn.chahuyun.authorize.utils.Log.error
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.events.MessageEvent
import java.lang.reflect.Method
import java.util.*
import java.util.stream.Stream

interface Filter {
    /**
     * 过滤
     *
     * @param method 方法过滤流
     * @param instance 实例
     * @author Moyuyanli
     * @date 2024-8-10 14:38:07
     */
    fun filter(method: Stream<Method>, instance: Any)

}


class MessageFilter(private val channel: EventChannel<MessageEvent>) : Filter {

    companion object {
        fun register(classList: Set<Class<*>>, channel: EventChannel<MessageEvent>) {
            val register = MessageFilter(channel)

            for (clazz in classList) {
                val name = clazz.name
                debug("已扫描到消息注册类-> $name ")

                var instance: Any
                try {
                    instance = clazz.getConstructor().newInstance()
                } catch (e: Exception) {
                    error("注册类: $name 实例化失败!", e)
                    continue
                }

                val methods: Array<Method> = clazz.getDeclaredMethods()
                val stream = Arrays.stream(methods)
                register.filter(stream, instance)
            }
        }
    }

    /**
     * 过滤
     *
     * @param method 方法过滤流
     * @param instance 实例
     * @author Moyuyanli
     * @date 2024-8-10 14:38:07
     */
    override fun filter(method: Stream<Method>, instance: Any) {
        method.filter { it.isAnnotationPresent(MessageAuthorize::class.java) && it.parameterCount == 1 }
            .forEach {
                val paramsType = it.parameterTypes[0]
                if (MessageEvent::class.java.isAssignableFrom(paramsType)) {
                    val methodType = paramsType.asSubclass(MessageEvent::class.java)
                    execute(instance, it, channel.filterIsInstance(methodType), methodType)
                } else {
                    Log.warning("类[${instance.javaClass.name}]中方法[${it.name}]的参数类型异常，请检查!")
                }
            }
    }

    /**
     * 执行
     * 进行事件监听注册注册
     *
     * @param bean    实体
     * @param method  方法
     * @param channel 监听channel
     * @author Moyuyanli
     * @date 2023/8/11 10:11
     */
    fun execute(
        bean: Any,
        method: Method,
        channel: EventChannel<MessageEvent>,
        methodType: Class<out MessageEvent>,
    ) {
        
    }


}