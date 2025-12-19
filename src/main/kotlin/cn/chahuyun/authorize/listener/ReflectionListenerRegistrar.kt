package cn.chahuyun.authorize.listener

import cn.chahuyun.authorize.HuYanAuthorize.log
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.exception.ExceptionHandleApi
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.events.MessageEvent
import java.lang.reflect.Method
import java.util.*

/**
 * 反射实现的监听注册器
 */
class ReflectionListenerRegistrar(private val classList: Set<Class<*>>) : ListenerRegistrar {

    override fun register(
        channel: EventChannel<MessageEvent>,
        filter: ListenerFilter,
        handleApi: ExceptionHandleApi,
        plugin: JvmPlugin
    ) {
        for (clazz in classList) {
            val name = clazz.name
            log.debug("已扫描到消息注册类(反射)-> $name ")

            val instance: Any
            try {
                instance = clazz.getConstructor().newInstance()
            } catch (e: Throwable) {
                log.error("注册类: $name 实例化失败!", e)
                continue
            }

            val methods: Array<Method> = clazz.getDeclaredMethods()
            val stream = Arrays.stream(methods)

            stream.filter {
                it.isAnnotationPresent(MessageAuthorize::class.java) && it.parameterTypes.isNotEmpty()
            }.forEach { method ->
                val paramsType = method.parameterTypes[0]
                if (MessageEvent::class.java.isAssignableFrom(paramsType)) {
                    val methodType = paramsType.asSubclass(MessageEvent::class.java)
                    execute(
                        instance,
                        method,
                        channel.filterIsInstance(methodType),
                        methodType,
                        filter,
                        handleApi,
                        plugin
                    )
                } else {
                    log.warning("类[${instance.javaClass.name}]中方法[${method.name}]的参数类型异常，请检查!")
                }
            }
        }
    }

    private fun execute(
        bean: Any,
        method: Method,
        channel: EventChannel<MessageEvent>,
        methodType: Class<out MessageEvent>,
        filter: ListenerFilter,
        handleApi: ExceptionHandleApi,
        plugin: JvmPlugin
    ) {
        val annotation = method.getAnnotation(MessageAuthorize::class.java)

        log.debug("注册消息事件方法(反射)-> ${method.name}")
        channel.exceptionHandler { handleApi.handle(it) }
            .filter {
                val start = System.currentTimeMillis()
                val result = filter.permFilter(it, annotation, methodType) && filter.messageFilter(it, annotation)
                if (result) log.debug("${method.name} 匹配用时 ${System.currentTimeMillis() - start} ms")
                result
            }
            .subscribeAlways<MessageEvent>(
                concurrency = annotation.concurrency,
                priority = annotation.priority
            ) {
                if (method.parameterCount == 1) {
                    try {
                        val start = System.currentTimeMillis()
                        method.invoke(bean, it)
                        log.debug("${method.name} 执行用时 ${System.currentTimeMillis() - start} ms")
                    } catch (e: Throwable) {
                        handleApi.handle(e)
                    }
                } else {
                    try {
                        val start = System.currentTimeMillis()
                        method.invoke(bean, it, plugin)
                        log.debug("${method.name} 执行用时 ${System.currentTimeMillis() - start} ms")
                    } catch (e: Exception) {
                        handleApi.handle(e)
                    }
                }
            }
    }
}

