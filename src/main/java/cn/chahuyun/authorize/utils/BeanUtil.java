package cn.chahuyun.authorize.utils;

import cn.chahuyun.authorize.aop.TimeSectionOperation;
import cn.hutool.aop.ProxyUtil;

/**
 * Bean工具
 *
 * @author Moyuyanli
 * @date 2023/8/11 10:21
 */
public class BeanUtil {


    private BeanUtil() {}


    /**
     * 通过代理构建实例
     *
     * @param aClass 类
     * @return 实例
     */
    @Deprecated
    public static Object proxy(Class<?> aClass) {
        try {
            return ProxyUtil.proxy(aClass.getConstructor().newInstance(), TimeSectionOperation.class);
        } catch (Exception e) {
            return null;
        }
    }
}
