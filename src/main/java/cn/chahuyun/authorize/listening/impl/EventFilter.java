package cn.chahuyun.authorize.listening.impl;

import cn.chahuyun.authorize.listening.Filter;

import java.lang.reflect.Method;
import java.util.stream.Stream;

/**
 * 事件过滤
 *
 * @author Moyuyanli
 * @date 2023/8/11 17:04
 */
public class EventFilter implements Filter {
    /**
     * 过滤
     *
     * @param stream   方法过滤流
     * @param instance 实例
     * @author Moyuyanli
     * @date 2023/8/11 10:11
     */
    @Override
    public void filter(Stream<Method> stream, Object instance) {

    }
}
