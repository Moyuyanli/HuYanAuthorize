package cn.chahuyun.authorize.exception

import cn.chahuyun.authorize.HuYanAuthorize.log


/**
 * 异常处理API接口
 * 定义了处理异常的统一接口规范
 */
interface ExceptionHandleApi {

    /**
     * 处理异常
     *
     * @param e 需要被处理的异常对象，类型为Throwable
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