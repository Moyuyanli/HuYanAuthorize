package cn.chahuyun.authorize.command

import cn.chahuyun.authorize.HuYanAuthorize
import cn.chahuyun.authorize.config.AuthorizeConfig
import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.plugin.version


object AuthorizeCommand : CompositeCommand(
    HuYanAuthorize, "hya",
    description = "壶言权限管理指令"
) {


    @SubCommand("owner") // 可以设置多个子指令名。此时函数名会被忽略。
    @Description("设置主人")
    suspend fun CommandSender.setOwner(owner: Long) {
        AuthorizeConfig.owner = owner
        val permGroup = PermUtil.talkPermGroupByName("主人")
        permGroup.users.clear()
        val ownerUser = UserUtil.globalUser(owner)
        permGroup.users.add(ownerUser)
        permGroup.save()
        sendMessage("主人设置成功!")
    }


    @SubCommand("v")
    @Description("查询当前插件版本")
    suspend fun CommandSender.version() {
        val version = HuYanAuthorize.version
        sendMessage("壶言鉴权当前版本: $version")
    }

}