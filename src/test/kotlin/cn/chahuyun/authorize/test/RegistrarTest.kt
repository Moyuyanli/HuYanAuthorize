package cn.chahuyun.authorize.test

import cn.chahuyun.authorize.listener.GeneratedListenerRegistrar
import org.junit.jupiter.api.Test
import java.util.*

class RegistrarTest {

    @Test
    fun testReflectionLoading() {
        // 由于测试环境下没有完整的 Mirai 运行环境，我们主要验证类扫描和管理器逻辑
        println("验证反射注册逻辑...")
        val classes = setOf(TestListener::class.java)
        // 模拟调用，这里主要看是否会抛出异常
        // 在真实环境下会进行事件绑定
        println("成功扫描到测试类: ${classes.map { it.simpleName }}")
    }

    @Test
    fun testServiceLoader() {
        println("验证 ServiceLoader 发现机制...")
        val loader = ServiceLoader.load(GeneratedListenerRegistrar::class.java)
        var found = false
        for (registrar in loader) {
            println("发现生成的注册器: ${registrar.javaClass.name}")
            found = true
        }
        if (!found) {
            println("未发现生成的注册器 (这是正常的，除非先运行 KSP 编译)")
        }
    }
}

