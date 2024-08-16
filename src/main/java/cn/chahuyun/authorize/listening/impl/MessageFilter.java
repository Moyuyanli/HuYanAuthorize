package cn.chahuyun.authorize.listening.impl;

import cn.chahuyun.authorize.annotation.MessageAuthorize;
import cn.chahuyun.authorize.aop.JavaBeanProxy;
import cn.chahuyun.authorize.config.AuthorizeConfig;
import cn.chahuyun.authorize.enums.MessageMatchingEnum;
import cn.chahuyun.authorize.enums.PermissionMatchingEnum;
import cn.chahuyun.authorize.listening.Filter;
import cn.chahuyun.authorize.manager.PermissionManager;
import cn.chahuyun.authorize.utils.Log;
import cn.hutool.core.exceptions.UtilException;
import cn.hutool.core.util.ReflectUtil;
import kotlin.coroutines.EmptyCoroutineContext;
import lombok.Data;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.EventChannel;
import net.mamoe.mirai.event.ListeningStatus;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static cn.chahuyun.authorize.HuYanAuthorize.LOGGER;

/**
 * 消息注册
 *
 * @author Moyuyanli
 * @date 2023/8/11 10:11
 */
@Data
public class MessageFilter implements Filter {

    private EventChannel<MessageEvent> channel;

    private MessageFilter() {
    }

    /**
     * 加载消息注册class实例
     *
     * @param classes
     * @param channel
     * @return void
     * @author Moyuyanli
     * @date 2023/8/11 15:17
     */
    public static void register(Set<Class<?>> classes, EventChannel<MessageEvent> channel) {
        MessageFilter register = new MessageFilter();
        register.setChannel(channel);
        /*
         * 类过滤
         */
        for (Class<?> aClass : classes) {
            LOGGER.debug("已扫描到消息注册类->" + aClass.getName());
            //尝试实例化该类
            Object newInstance;
            try {
                newInstance = aClass.getConstructor().newInstance();
            } catch (Exception e) {
                LOGGER.error("注册类:" + aClass.getName() + "实例化失败!");
                return;
            }
            Method[] methods = aClass.getDeclaredMethods();
            Stream<Method> stream = Arrays.stream(methods);
            register.filter(stream, newInstance);
        }
    }

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
        /*
        参数类型和方法过滤
         */
        stream.filter(it -> it.isAnnotationPresent(MessageAuthorize.class) && it.getParameterCount() == 1)
                .forEach(it -> {
                    Class<?> parameterType = it.getParameterTypes()[0];
                    if (MessageEvent.class.isAssignableFrom(parameterType)) {
                        Class<? extends MessageEvent> methodType = parameterType.asSubclass(MessageEvent.class);
                        execute(instance, it, channel.filterIsInstance(methodType), methodType);
                    } else {
                        Log.warning("类[%s]中方法[%s]的参数类型异常，请检查!", instance.getClass().getName(), it.getName());
                    }
                });
    }

    /**
     * 执行
     * 进行事件监听注册注册
     *
     * @param bean    实体
     * @param method  方法
     * @param channel 监听channel
     * @author Moyuyanli
     * @date 2023/8/11 10:11
     */
    public void execute(@NotNull Object bean, @NotNull Method method, @NotNull EventChannel<? extends MessageEvent> channel, Class<? extends MessageEvent> methodType) {
        LOGGER.debug("添加消息注册方法->" + method.getName() + " : 消息获取类型->" + method.getParameterTypes()[0].getSimpleName());
        //获取注解信息
        MessageAuthorize annotation = method.getAnnotation(MessageAuthorize.class);
        //过滤条件
        channel.filter(event -> eventCheckPermission(event, annotation))
                .filter(event -> messageMate(event, annotation))
                .subscribe(
                        methodType,
                        EmptyCoroutineContext.INSTANCE,
                        annotation.concurrency(),
                        annotation.priority(),
                        event -> {
                            if (AuthorizeConfig.INSTANCE.getProxySwitch()) {
                                return JavaBeanProxy.getInstance().register(bean, method, event) ?
                                        ListeningStatus.LISTENING :
                                        ListeningStatus.STOPPED;
                            } else {
                                try {
                                    method.invoke(bean, event);
                                } catch (Exception e) {
                                    Log.error("类[%s]方法[%s]执行错误:%s", bean.getClass().getName(), method.getName(), e.getMessage());
                                    e.printStackTrace();
                                }
                                return ListeningStatus.LISTENING;
                            }
                        });
    }


    /**
     * 检查消息的权限
     *
     * @param event 消息事件
     * @return boolean
     */
    private boolean eventCheckPermission(MessageEvent event, MessageAuthorize authorize) {
        /*
        权限过滤
         */
        Bot bot = event.getBot();
        User sender = event.getSender();

        //拿到群
        Contact subject = event.getSubject();
        //如果是群
        if (subject instanceof Group) {
            String[] groupPermissions = authorize.groupPermissions();
            if (!groupPermissions[0].equals(PermConstant.NULL)) {
                PermissionMatchingEnum groupPermissionsMatching = authorize.groupPermissionsMatching();
                //进行群权限判断,如果不过,直接过滤
                if (isPermission(bot.getId(), subject.getId(), subject.getId(), groupPermissionsMatching, groupPermissions)) {
                    return false;
                }
            }
        }

        //用户权限
        String[] permissions = authorize.userPermissions();
        PermissionMatchingEnum userPermissionsMatching = authorize.userPermissionsMatching();
        switch (permissions[0]) {
            case PermConstant.NULL:
                return true;
            case PermConstant.ADMIN:
            case PermConstant.OWNER:
                //如果需求主人权限
                if (AuthorizeConfig.INSTANCE.getOwner() == sender.getId()) {
                    return true;
                }
                //如果需求admin权限
                if (PermissionManager.checkPermission(bot.getId(), subject.getId(), sender.getId(), PermConstant.ADMIN)) {
                    return true;
                }
            default:
                //进行群权限判断,如果不过,直接过滤
                return !isPermission(bot.getId(), subject.getId(), sender.getId(), userPermissionsMatching, permissions);
        }
    }

    /**
     * 匹配消息
     *
     * @param event      消息事件
     * @param annotation 注解
     * @return boolean
     */
    private boolean messageMate(MessageEvent event, MessageAuthorize annotation) {
        //消息判断
        MessageChain message = event.getMessage();
        String code = message.serializeToMiraiCode();

        MessageMatchingEnum matching = annotation.messageMatching();
        String[] text = annotation.text();

        //消息匹配默认为否
        boolean messageMatching = false;

        switch (matching) {
            case TEXT:
                for (String messageString : text) {
                    //替换第一个#   [#] 请用 [##] 转意
                    messageString = messageString.replaceFirst("#", "");
                    if (code.equals(messageString)) {
                        messageMatching = true;
                        break;
                    }
                }
                break;
            case REGULAR:
                messageMatching = Pattern.matches(text[0], code);
                break;
            case CUSTOM:
                Class<? extends CustomPattern> custom = annotation.custom();
                try {
                    return ReflectUtil.invoke(ReflectUtil.newInstance(custom), "custom", event);
                } catch (UtilException e) {
                    HuYanAuthorize.LOGGER.error("使用自定义匹配异常!", e);
                    return false;
                }
            default:
                return false;
        }
            /*
            如果为 空(null)  则消息过滤直接通过
            如果需要判断[null] 请加 [#]  ->  [#null]
             */
        if (text.length == 1) {
            messageMatching = "null".equals(text[0]) || messageMatching;
        }
        return messageMatching;

    }

    /**
     * 权限判读，满足返回 false
     *
     * @param matching    匹配方式
     * @param permissions 权限
     * @return boolean  true 不满足条件
     * @author Moyuyanli
     * @date 2023/1/3 14:32
     */
    private static boolean isPermission(long bot, long group, long id, PermissionMatchingEnum matching, String[] permissions) {
        //是主人或者有admin或者有all权限
        if (AuthorizeConfig.INSTANCE.getOwner() == id || PermissionManager.checkPermission(bot, group, id, "admin") || PermissionManager.checkPermission(bot, group, id, "all")) {
            return false;
        }
        //如果为与(且)
        if (matching == PermissionMatchingEnum.AND) {
            boolean success = true;
            for (String permission : permissions) {
                //进行与运算
                success = success && PermissionManager.checkPermission(bot, group, id, permission);
            }
            return !success;
        } else {
            //如果为或,有一个满足即返回
            for (String permission : permissions) {
                if (PermissionManager.checkPermission(bot, group, id, permission)) {
                    return false;
                }
            }
            return true;
        }
    }


}
