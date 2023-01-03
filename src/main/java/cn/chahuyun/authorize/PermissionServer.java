package cn.chahuyun.authorize;

import cn.chahuyun.authorize.annotation.MessageAuthorize;
import cn.chahuyun.authorize.annotation.MessageComponent;
import cn.chahuyun.authorize.entity.PermissionInfo;
import cn.chahuyun.authorize.enums.MessageMatchingEnum;
import cn.chahuyun.authorize.enums.PermissionMatchingEnum;
import cn.chahuyun.authorize.manager.HibernateUtil;
import cn.chahuyun.authorize.manager.PermissionManager;
import cn.hutool.core.util.ClassUtil;
import kotlin.coroutines.EmptyCoroutineContext;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.EventChannel;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.ListeningStatus;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.utils.MiraiLogger;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 权限服务
 *
 * @author Moyuyanli
 * @date 2023/1/3 9:05
 */
public class PermissionServer {

    private static final PermissionServer instance = new PermissionServer();

    private final Map<JavaPlugin, List<String>> pluginListHashMap = new HashMap<>();

    private PermissionServer() {

    }

    /**
     * 获取权限插件服务单一实例
     *
     * @return cn.chahuyun.authorize.PermissionServer 权限管理服务
     * @author Moyuyanli
     * @date 2023/1/3 14:01
     */

    public static PermissionServer getInstance() {
        return instance;
    }

    public void init(EventChannel<Event> thisChannel) {
        MiraiLogger log = HuYanAuthorize.log;
        //遍历需要进行注册的类的方法
        for (Map.Entry<JavaPlugin, List<String>> entry : pluginListHashMap.entrySet()) {
            EventChannel<Event> eventEventChannel;
            if (entry.getKey().equals(HuYanAuthorize.INSTANCE)) {
                eventEventChannel = thisChannel;
            } else {
                //创建一个新的属于该插件的全局EventChannel
                eventEventChannel = GlobalEventChannel.INSTANCE.parentScope(entry.getKey());
            }

            //拿该插件下需要扫描注册的包信息
            List<String> value = entry.getValue();
            for (String s : value) {
                //扫描包下的类
                ClassUtil.scanPackage(s).stream()
                        //过滤不声明的类
                        .filter(aClass -> aClass.isAnnotationPresent(MessageComponent.class))
                        .forEach(aClass -> {
                            log.debug("已扫描到消息注册类->" + aClass.getName());
                            //尝试实例化该类
                            Object newInstance;
                            try {
                                newInstance = aClass.getConstructor().newInstance();
                            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                                     NoSuchMethodException e) {
                                log.error("注册类:" + aClass.getName() + "实例化失败!");
                                return;
                            }
                            //过滤类里面的方法
                            Arrays.stream(aClass.getMethods())
                                    //去掉不包括注册注解的
                                    .filter(method -> method.isAnnotationPresent(MessageAuthorize.class))
                                    .filter(method -> {
                                        //去掉参数数量不为1的
                                        if (method.getParameterCount() != 1) return false;
                                        //去掉参数类型不是消息事件的
                                        return method.getParameterTypes()[0].isAssignableFrom(MessageEvent.class);
                                    })
                                    .forEach(it -> execute(newInstance, it, eventEventChannel.filterIsInstance(MessageEvent.class)));
                        });

            }
        }
    }

    /**
     * 添加权限管理包路径
     *
     * @param packagePath 需要进行权限管理的包路径
     * @return boolean
     * @author Moyuyanli
     * @date 2023/1/3 9:39
     */
    public boolean addPackagePath(JavaPlugin plugin, String packagePath) {
        if (pluginListHashMap.containsKey(plugin)) {
            List<String> list = pluginListHashMap.get(plugin);
            if (list.contains(packagePath)) {
                return false;
            } else {
                list.add(packagePath);
                return true;
            }
        } else {
            pluginListHashMap.put(plugin, new ArrayList<>() {{
                add(packagePath);
            }});
            return true;
        }
    }

    /**
     * 添加一个种[权限]<p>
     * 如果存在，则返回添加失败<p>
     * 默认存在几个关键字权限:<p>
     * owner : 主人<p>
     * null : 不需要权限<p>
     * admin : 权限跟改权限<p>
     * all : 所有权限<p>
     *
     * @param code        权限id
     * @param description 权限描述
     * @return boolean
     * @author Moyuyanli
     * @date 2023/1/3 15:08
     */
    public boolean addPermission(String code, String description) {
        List<String> defaultPermissions = new ArrayList<>() {{
            add("owner");
            add("null");
            add("admin");
            add("all");
        }};
        if (defaultPermissions.contains(code)) return false;
        try {
            HibernateUtil.factory.fromSession(session -> session.get(PermissionInfo.class, code));
            return false;
        } catch (Exception e) {
            PermissionInfo permissionInfo = new PermissionInfo(code, description);
            try {
                HibernateUtil.factory.fromTransaction(session -> session.merge(permissionInfo));
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }


    /**
     * 过滤消息，进行事件监听注册注册
     *
     * @param bean    实体
     * @param method  方法
     * @param channel 监听channel
     * @author Moyuyanli
     * @date 2023/1/3 11:15
     */

    private static void execute(Object bean, Method method, @NotNull EventChannel<MessageEvent> channel) {
        //获取注解信息
        MessageAuthorize annotation = method.getAnnotation(MessageAuthorize.class);
        //过滤条件
        channel.filter(event -> {
            //统一保留
            boolean quit = false;
            //拿到群
            Contact subject = event.getSubject();
            //如果是群
            if (subject instanceof Group) {
                String[] groupPermissions = annotation.groupPermissions();
                //如果权限的第一个为null,则将保留改为true
                if (groupPermissions[0].equals("null")) {
                    quit = true;
                } else {
                    PermissionMatchingEnum groupPermissionsMatching = annotation.groupPermissionsMatching();
                    //进行群权限判断,如果不过,直接过滤
                    if (isPermission(event.getBot().getId(), subject.getId(), groupPermissionsMatching, groupPermissions)) {
                        return false;
                    }
                }
            }

            //用户权限
            String[] permissions = annotation.userPermissions();
            if (permissions[0].equals("null")) {
                quit = quit && true;
            } else {
                PermissionMatchingEnum permissionsMatching = annotation.userPermissionsMatching();
                //进行群权限判断,如果不过,直接过滤
                if (isPermission(event.getBot().getId(), event.getSender().getId(), permissionsMatching, permissions)) {
                    return false;
                }
            }

            //消息判断
            MessageChain message = event.getMessage();
            String code = message.serializeToMiraiCode();

            MessageMatchingEnum matching = annotation.messageMatching();
            String[] text = annotation.text();

            if (matching == MessageMatchingEnum.TEXT) {
                for (String s : text) {
                    if (code.equals(s)) {
                        quit = quit && true;
                        break;
                    }
                }
            } else {
                quit = quit && Pattern.matches(text[0], code);
            }
            return quit;
        }).subscribe(MessageEvent.class,
                EmptyCoroutineContext.INSTANCE,
                annotation.concurrency(),
                annotation.priority(),
                event -> {
                    try {
                        method.invoke(bean, event);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        HuYanAuthorize.log.error("权限消息注册失败");
                        e.printStackTrace();
                    }
                    return ListeningStatus.LISTENING;
                });
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
    private static boolean isPermission(long bot, long group, PermissionMatchingEnum matching, String[] permissions) {
        //如果为与(且)
        if (matching == PermissionMatchingEnum.AND) {
            boolean success = true;
            for (String permission : permissions) {
                //进行与运算
                success = success && PermissionManager.isPermission(bot, group, permission);
            }
            return !success;
        } else {
            //如果为或,有一个满足即返回
            for (String permission : permissions) {
                if (PermissionManager.isPermission(bot, group, permission)) {
                    return false;
                }
            }
            return true;
        }
    }

}
