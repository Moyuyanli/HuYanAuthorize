package cn.chahuyun.authorize.register.impl;

import cn.chahuyun.authorize.register.Register;
import cn.chahuyun.authorize.utils.Log;
import net.mamoe.mirai.event.Event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 执行事件注册
 *
 * @author Moyuyanli
 * @date 2023/8/11 15:00
 */
public class EventRegister implements Register {

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
    @Override
    public boolean register(Object bean, Method method, Event event) {
        try {
            method.invoke(bean, event);
            return true;
        } catch (IllegalAccessException | InvocationTargetException e) {
            Log.error("类[%s]方法[%s]执行失败!",bean.getClass().getName(),method.getName());
            return false;
        }
    }

}
