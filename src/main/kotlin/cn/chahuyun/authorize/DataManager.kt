package cn.chahuyun.authorize



import cn.chahuyun.authorize.config.AuthorizeConfig.dataType
import cn.chahuyun.authorize.config.AuthorizeConfig.mysqlPassword
import cn.chahuyun.authorize.config.AuthorizeConfig.mysqlUrl
import cn.chahuyun.authorize.config.AuthorizeConfig.mysqlUser
import cn.chahuyun.authorize.utils.Log
import cn.chahuyun.hibernateplus.DriveType
import cn.chahuyun.hibernateplus.DriveType.*
import cn.chahuyun.hibernateplus.HibernatePlusService
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin


class DataManager {


    companion object {
        fun init(plugin: KotlinPlugin) {
            val configuration = HibernatePlusService.createConfiguration(plugin::class.java)

            configuration.classLoader = plugin::class.java.classLoader
            configuration.packageName = "cn.chahuyun.authorize.entity"

            configuration.driveType = dataType
            when (dataType) {
                MYSQL -> {
                    configuration.address = mysqlUrl
                    configuration.user = mysqlUser
                    configuration.password = mysqlPassword
                }
                H2 -> TODO()
                SQLITE -> TODO()
            }

            if (dataType == MYSQL) {

            }

            HibernatePlusService.loadingService(configuration)
            Log.info("HuYanAuthorize DateBase loaded success fully !")
        }
    }


}