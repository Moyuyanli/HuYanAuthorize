package cn.chahuyun.authorize.listener

import cn.chahuyun.authorize.HuYanAuthorize.log
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.exception.ExceptionHandleApi
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.events.MessageEvent
import java.lang.reflect.Method
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

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
                try {
                    // 仅对 test 方法打印入口来源，用于排查“双回复/重复订阅”
                    if (method.name == "test") {
                        val beanCl = bean.javaClass.classLoader
                        val methodCl = method.declaringClass.classLoader
                        val eventCl = it::class.java.classLoader
                        log.debug(
                            "ENTRY[test][REFLECTION] bean=${bean.javaClass.name}@${System.identityHashCode(bean)} " +
                                "method=${method.toGenericString()}@${System.identityHashCode(method)} " +
                                "beanCL=${beanCl?.javaClass?.name}@${System.identityHashCode(beanCl)} " +
                                "methodCL=${methodCl?.javaClass?.name}@${System.identityHashCode(methodCl)} " +
                                "eventCL=${eventCl?.javaClass?.name}@${System.identityHashCode(eventCl)} " +
                                "eventType=${it::class.java.name} sender=${it.sender.id} subject=${it.subject.id}"
                        )
                    }

                    val start = System.currentTimeMillis()

                    val paramTypes = method.parameterTypes
                    val isSuspend = paramTypes.isNotEmpty() && Continuation::class.java.isAssignableFrom(paramTypes.last())

                    if (!isSuspend) {
                        // 普通方法：fun onEvent(event) / fun onEvent(event, plugin)
                        if (method.parameterCount == 1) {
                            method.invoke(bean, it)
                        } else {
                            method.invoke(bean, it, plugin)
                        }
                    } else {
                        // suspend 方法：编译为 (event, [plugin], Continuation) -> Any?
                        val argCountWithoutContinuation = paramTypes.size - 1
                        val args: Array<Any?> = when (argCountWithoutContinuation) {
                            1 -> arrayOf(it)
                            2 -> arrayOf(it, plugin)
                            else -> throw IllegalStateException(
                                "不支持的方法签名: ${method.declaringClass.name}.${method.name} 参数数量=${paramTypes.size} (含 Continuation)"
                            )
                        }

                        // 在协程上下文中，把当前 continuation 传给编译后的 suspend 方法
                        suspendCoroutineUninterceptedOrReturn<Any?> { cont ->
                            val result = method.invoke(bean, *args, cont)
                            if (result === COROUTINE_SUSPENDED) COROUTINE_SUSPENDED else result
                        }
                    }

                    log.debug("${method.name} 执行用时 ${System.currentTimeMillis() - start} ms")
                } catch (e: Throwable) {
                    handleApi.handle(e)
                }
            }
    }
}

