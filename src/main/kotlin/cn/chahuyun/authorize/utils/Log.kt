package cn.chahuyun.authorize.utils

import cn.chahuyun.authorize.HuYanAuthorize
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin
import net.mamoe.mirai.utils.MiraiLogger

/**
 * 日志操作
 *
 * @author Moyuyanli
 * @Date 2023/7/29 13:45
 */
object Log {

    private var LOGGER: MiraiLogger = HuYanAuthorize.logger

    /**
     * 替换日志打印插件
     * @param instance 插件本身
     */
    fun init(instance: JavaPlugin) {
        LOGGER = instance.logger
    }

    fun info(msg: String) {
        LOGGER.info(msg)
    }

    /**
     * 支持 String format 的日志打印
     *
     * @param msg 消息
     * @param params 参数
     * @author Moyuyanli
     * @date 2023/7/30 0:55
     */
    fun info(msg: String, vararg params: Any) {
        LOGGER.info(String.format(msg, *params))
    }


    fun warning(msg: String) {
        LOGGER.warning(msg)
    }

    /**
     * 支持 String format 的日志打印
     *
     * @param msg 消息
     * @param params 参数
     * @author Moyuyanli
     * @date 2023/7/30 0:55
     */
    fun warning(msg: String, vararg params: Any) {
        LOGGER.warning(String.format(msg, *params))
    }

    fun error(msg: String) {
        LOGGER.error(msg)
    }

    fun error(msg: String, error: Exception) {
        LOGGER.error(msg,error)
    }

    /**
     * 支持 String format 的日志打印
     *
     * @param msg 消息
     * @param params 参数
     * @author Moyuyanli
     * @date 2023/7/30 0:55
     */
    fun error(msg: String, vararg params: Any) {
        LOGGER.error(String.format(msg, *params))
    }



    fun debug(msg: String) {
        LOGGER.debug(msg)
    }

    /**
     * 支持 String format 的日志打印
     *
     * @param msg 消息
     * @param params 参数
     * @author Moyuyanli
     * @date 2023/7/30 0:55
     */
    fun debug(msg: String, vararg params: Any) {
        LOGGER.debug(String.format(msg, *params))
    }
}