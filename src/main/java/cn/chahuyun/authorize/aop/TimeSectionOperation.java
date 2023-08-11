package cn.chahuyun.authorize.aop;

import cn.chahuyun.authorize.HuYanAuthorize;
import cn.hutool.aop.aspects.Aspect;
import cn.hutool.core.date.TimeInterval;

import java.lang.reflect.Method;

/**
 * 时间切面操作
 *
 * @author Moyuyanli
 * @date 2023/8/7 18:31
 */
public class TimeSectionOperation implements Aspect {


    private static final long serialVersionUID = 1L;

    private final TimeInterval interval = new TimeInterval();

    /**
     * @param target 目标对象
     * @param method 目标方法
     * @param args   参数
     * @return boolean
     */
    @Override
    public boolean before(Object target, Method method, Object[] args) {
        interval.start();
        return true;
    }

    /**
     * @param target    目标对象
     * @param method    目标方法
     * @param args      参数
     * @param returnVal 目标方法执行返回值
     * @return boolean
     */
    @Override
    public boolean after(Object target, Method method, Object[] args, Object returnVal) {
        String beanName = args[0].getClass().getName();
        Method beanMethod = (Method) args[1];
        String format = String.format("类:[%s]执行方法:[%s]用时:[%d]ms", beanName, beanMethod.getName(), interval.intervalMs());
        HuYanAuthorize.LOGGER.debug(format);
        return true;
    }

    /**
     * 目标方法抛出异常时的操作
     *
     * @param target 目标对象
     * @param method 目标方法
     * @param args   参数
     * @param e      异常
     * @return 是否允许抛出异常
     */
    @Override
    public boolean afterException(Object target, Method method, Object[] args, Throwable e) {
        String beanName = args[0].getClass().getName();
        Method beanMethod = (Method) args[1];
        String format = String.format("类:[%s]执行方法:[%s]错误!", beanName, beanMethod.getName());
        HuYanAuthorize.LOGGER.error(format);
        return false;
    }

}
