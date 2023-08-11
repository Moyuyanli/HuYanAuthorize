package cn.chahuyun.authorize.manager;

import cn.chahuyun.authorize.annotation.EventAuthorize;
import cn.chahuyun.authorize.annotation.MessageAuthorize;

import java.lang.annotation.Annotation;
import java.util.ArrayList;

/**
 * 注解管理
 *
 * @author Moyuyanli
 * @date 2023/8/7 19:03
 */
public class AnnotationsManager extends ArrayList<Class<? extends Annotation>> {

    private static final AnnotationsManager INSTANCE = new AnnotationsManager();

    static {
        INSTANCE.add(MessageAuthorize.class);
        INSTANCE.add(EventAuthorize.class);
    }

    public static AnnotationsManager getInstance() {
        return INSTANCE;
    }


}
