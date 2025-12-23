package cn.chahuyun.authorize


import cn.chahuyun.authorize.HuYanAuthorize.log
import cn.chahuyun.authorize.config.AuthorizeConfig.dataType
import cn.chahuyun.authorize.config.AuthorizeConfig.mysqlPassword
import cn.chahuyun.authorize.config.AuthorizeConfig.mysqlUrl
import cn.chahuyun.authorize.config.AuthorizeConfig.mysqlUser
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

                H2 -> configuration.address = HuYanAuthorize.dataFolderPath.resolve("authorize.h2.mv.db").toString()
                SQLITE -> configuration.address = HuYanAuthorize.dataFolderPath.resolve("authorize.mv.db").toString()
                HSQLDB -> configuration.address = HuYanAuthorize.dataFolderPath.resolve("authorize.hsqldb").toString()
                MARIADB -> {
                    configuration.address = mysqlUrl
                    configuration.user = mysqlUser
                    configuration.password = mysqlPassword
                }

                DUCKDB -> configuration.address = HuYanAuthorize.dataFolderPath.resolve("authorize.duckdb").toString()
            }

            HibernatePlusService.loadingService(configuration)
            log.info("HuYanAuthorize DateBase loaded success fully !")
        }
    }


}