package cn.chahuyun.authorize.command

import cn.chahuyun.authorize.HuYanAuthorize
import cn.chahuyun.authorize.config.AuthorizeConfig
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand

object AuthorizeCommand : CompositeCommand(
    HuYanAuthorize.INSTANCE, "hya",
    description = "壶言权限管理指令"
) {


    @SubCommand("owner") // 可以设置多个子指令名。此时函数名会被忽略。
    @Description("设置主人")
    suspend fun CommandSender.setOwner(owner: Long) {
        AuthorizeConfig.owner = owner
        sendMessage("主人设置成功!")
    }


    @SubCommand("v") // 可以设置多个子指令名。此时函数名会被忽略。
    @Description("查询当前插件版本")
    suspend fun CommandSender.version() {
        val version = HuYanAuthorize.version
        sendMessage("壶言会话当前版本: $version")
    }

}