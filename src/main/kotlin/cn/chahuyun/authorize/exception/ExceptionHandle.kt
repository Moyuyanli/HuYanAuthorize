package cn.chahuyun.authorize.exception

import cn.chahuyun.authorize.HuYanAuthorize.log


interface ExceptionHandleApi {

    /**
     * 处理异常
     */
    fun handle(e: Throwable)

}

/**
 * 默认异常处理
 */
class ExceptionHandle : ExceptionHandleApi {

    /**
     * 处理异常
     */
    override fun handle(e: Throwable) {
        val message = e.cause?.message ?: e.message ?: "壶言鉴权:代理方法发生异常"
        val throwable = e.cause ?: e
        log.error(message, throwable)
    }

}