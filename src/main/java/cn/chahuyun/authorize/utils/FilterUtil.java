package cn.chahuyun.authorize.utils;

import cn.chahuyun.authorize.annotation.EventAuthorize;
import net.mamoe.mirai.event.events.GroupEvent;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * 工具
 *
 * @author Moyuyanli
 * @date 2023/8/8 14:57
 */
public class FilterUtil {


    private FilterUtil() {
    }

    /**
     * 检查群动态授权注解
     *
     * @param method 方法
     * @return boolean
     */
    public static boolean methodCheckGroup(Method method) {
        return method.isAnnotationPresent(EventAuthorize.class) && method.getParameterCount() == 1;
    }

    /**
     * 检查群的权限
     *
     * @param event 群动态事件
     * @return boolean
     */
    public static boolean eventCheckPermission(GroupEvent event, Annotation annotation) {
        return false;
    }


}
