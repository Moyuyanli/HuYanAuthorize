package cn.chahuyun.authorize

import java.lang.reflect.Method
import java.util.*
import java.util.stream.Stream

interface Filter {
    /**
     * 过滤
     *
     * @param method 方法过滤流
     * @param instance 实例
     * @author Moyuyanli
     * @date 2024-8-10 14:38:07
     */
    fun filter(method: Stream<Method>, instance: Objects)
}

class MessageFilter:Filter{
    override fun filter(method: Stream<Method>, instance: Objects) {
        TODO("Not yet implemented")
    }

}