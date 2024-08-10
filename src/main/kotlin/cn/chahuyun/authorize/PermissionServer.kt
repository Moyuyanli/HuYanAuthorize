package cn.chahuyun.authorize

import lombok.extern.slf4j.Slf4j
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel

@Slf4j
class PermissionServer private constructor() {
    companion object{
         val instance = PermissionServer()
    }


    fun init(plugin: KotlinPlugin) {
        val eventChannel = GlobalEventChannel.parentScope(plugin)
    }


}


