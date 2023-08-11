package cn.chahuyun.authorize.aop;

import cn.chahuyun.authorize.register.impl.EventRegister;
import cn.hutool.aop.ProxyUtil;

/**
 * bean代理
 *
 * @author Moyuyanli
 * @date 2023/8/11 15:22
 */
public class JavaBeanProxy {

    private static EventRegister eventRegister;

    private JavaBeanProxy() {}

    /**
     * 加载一次代理就够
     * 懒加载
     * @return cn.chahuyun.authorize.register.impl.EventRegister
     */
    private static EventRegister init() {
        eventRegister = ProxyUtil.proxy(new EventRegister(), TimeSectionOperation.class);
        return eventRegister;
    }

    /**
     * 代理实例
     *
     * @return cn.chahuyun.authorize.register.impl.EventRegister
     * @author Moyuyanli
     * @date 2023/8/11 16:29
     */
    public static EventRegister getInstance() {
        return eventRegister != null ? eventRegister : init();
    }
}
