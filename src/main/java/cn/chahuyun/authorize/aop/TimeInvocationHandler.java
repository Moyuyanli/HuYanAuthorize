package cn.chahuyun.authorize.aop;

import cn.chahuyun.authorize.register.Register;
import cn.chahuyun.authorize.utils.Log;
import cn.hutool.core.date.TimeInterval;
import net.sf.cglib.proxy.InvocationHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 时间操作
 *
 * @author Moyuyanli
 * @date 2023/8/11 18:50
 */
public class TimeInvocationHandler implements InvocationHandler {

    private final Register register;
    public TimeInvocationHandler(Register register){
        this.register = register;
    }

    private final TimeInterval interval = new TimeInterval();

    /**
     * 执行切面操作
     *
     * @param proxy   实例
     * @param method 执行的方法
     * @param args  参数
     * @return obj 类
     * @author Moyuyanli
     * @date 2023/8/11 15:09
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String beanName = args[0].getClass().getName();
        Method beanMethod = (Method) args[1];
        try {
            interval.start();
            Object invoke = method.invoke(register, args);
            String format = String.format("类:[%s]执行方法:[%s]用时:[%d]ms", beanName, beanMethod.getName(), interval.intervalMs());
            HuYanAuthorize.LOGGER.debug(format);
            return invoke;
        } catch (IllegalAccessException | InvocationTargetException e) {
            Log.error("类[%s]方法[%s]执行失败!",proxy.getClass().getName(),method.getName());
            return null;
        }
    }
}
