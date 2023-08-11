package cn.chahuyun.authorize.register;

import net.mamoe.mirai.event.Event;

import java.lang.reflect.Method;

/**
 * 注册接口，用于代理Bean
 *
 * @author Moyuyanli
 * @date 2023/8/11 15:13
 */
public interface Register {
    /**
     * 执行注册
     *
     * @param bean   实例
     * @param method 执行的方法
     * @param event  事件
     * @return boolean 是否成功
     * @author Moyuyanli
     * @date 2023/8/11 15:09
     */
    boolean register(Object bean, Method method, Event event);
}
