package cn.chahuyun.authorize

import cn.chahuyun.authorize.constant.LogTopic
import lombok.extern.slf4j.Slf4j
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder


class PermissionServer private constructor() {
    companion object {
        val instance = PermissionServer()
    }


    fun init(plugin: KotlinPlugin, packageName: String) {
        val eventChannel = GlobalEventChannel.parentScope(plugin)
        val classes = scanPackage(plugin.javaClass.classLoader, packageName)

        if (classes.isEmpty()) throw RuntimeException("注册类扫描为空!")

        MessageFilter.register(classes, eventChannel.filterIsInstance(MessageEvent::class))
    }

    private fun scanPackage(loader: ClassLoader, packageName: String): MutableSet<Class<*>> {
        // 创建ConfigurationBuilder并设置自定义ClassLoader
        val reflections = Reflections(
            ConfigurationBuilder()
                .forPackage(packageName, loader)
                .addClassLoaders(loader)
        )

        val queryFunction = Scanners.TypesAnnotated.of(
            EventComponent::class.java
        ).asClass<Any>(loader)

        return queryFunction.apply(reflections.store)
    }

}


