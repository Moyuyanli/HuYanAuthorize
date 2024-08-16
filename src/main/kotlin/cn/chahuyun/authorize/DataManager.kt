package cn.chahuyun.authorize


import cn.chahuyun.authorize.utils.Log
import cn.chahuyun.hibernateplus.DriveType.MYSQL
import cn.chahuyun.hibernateplus.HibernatePlusService
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin


class DataManager {


    companion object {
        fun init(plugin: KotlinPlugin) {
            val config = HuYanAuthorize.CONFIG
            val configuration = HibernatePlusService.createConfiguration(plugin::class.java)

            configuration.classLoader = plugin::class.java.classLoader
            configuration.packageName = "cn.chahuyun.authorize.entity"

            if (config.dataType == MYSQL) {
                configuration.driveType = config.dataType
                configuration.address = config.mysqlUrl
                configuration.user = config.mysqlUser
                configuration.password = config.mysqlPassword
            } else configuration.driveType = config.dataType

            HibernatePlusService.loadingService(configuration)
            Log.info("HuYanAuthorize DateBase loaded success fully !")
        }
    }


}