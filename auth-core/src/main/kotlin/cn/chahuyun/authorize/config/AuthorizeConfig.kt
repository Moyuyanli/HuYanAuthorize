package cn.chahuyun.authorize.config

import cn.chahuyun.hibernateplus.DriveType
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
    var owner: Long by value(123456L)

    @ValueDescription("是否启用 KSP 编译期代码生成监听器(建议开启,需要下游开发者自行配置 KSP)")
    var useKsp: Boolean by value(true)

    @ValueDescription("数据库类型(H2,SQLITE,MYSQL,HSQLDB)")
    var dataType: DriveType by value(DriveType.HSQLDB)

    @ValueDescription("mysql数据库连接地址")
    val mysqlUrl: String by value("127.0.0.1:3306/authorize")

    @ValueDescription("mysql数据库用户名")
    val mysqlUser: String by value("root")

    @ValueDescription("mysql数据库密码")
    val mysqlPassword: String by value("123456")


}