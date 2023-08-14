package cn.chahuyun.authorize.config

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

/**
 * 插件配置文件类
 *
 * @author Moyuyanli
 * @date 2022/11/14 12:49
 */
object AuthorizeConfig : AutoSavePluginConfig("AuthorizeConfig") {

    @ValueDescription("主人\n")
    var owner: Long by value()

    @ValueDescription("是否开启方法代理")
    var proxySwitch : Boolean by value(false)

}