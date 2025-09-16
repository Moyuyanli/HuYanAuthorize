package cn.chahuyun.authorize.exception


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
        e.cause?.let {
            Log.error(it.message ?: "壶言鉴权代理方法出错!", it as Exception)
        } ?: e.printStackTrace()
    }

}