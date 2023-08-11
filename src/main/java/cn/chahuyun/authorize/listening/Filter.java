package cn.chahuyun.authorize.listening;

import java.lang.reflect.Method;
import java.util.stream.Stream;

/**
 * 事件监听注册基本方法
 *
 * @author Moyuyanli
 * @date 2023/8/11 10:10
 */
public interface Filter {

    /**
     * 过滤
     *
     * @param stream 方法过滤流
     * @param instance 实例
     * @author Moyuyanli
     * @date 2023/8/11 10:11
     */
    void filter(Stream<Method> stream,Object instance);

}
