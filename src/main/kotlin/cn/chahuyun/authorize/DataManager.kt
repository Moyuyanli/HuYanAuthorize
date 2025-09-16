package cn.chahuyun.authorize



import cn.chahuyun.authorize.config.AuthorizeConfig.dataType
import cn.chahuyun.authorize.config.AuthorizeConfig.mysqlPassword
import cn.chahuyun.authorize.config.AuthorizeConfig.mysqlUrl
import cn.chahuyun.authorize.config.AuthorizeConfig.mysqlUser
import cn.chahuyun.hibernateplus.DriveType.*
import cn.chahuyun.hibernateplus.HibernatePlusService
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.utils.SilentLogger.info


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
            }

            HibernatePlusService.loadingService(configuration)
            info("HuYanAuthorize DateBase loaded success fully !")
        }
    }


}