package cn.chahuyun.authorize;

import cn.chahuyun.authorize.annotation.MessageAuthorize;
import cn.chahuyun.authorize.annotation.MessageComponent;
import cn.chahuyun.authorize.config.AuthorizeConfig;
import cn.chahuyun.authorize.entity.PermissionInfo;
import cn.chahuyun.authorize.enums.MessageMatchingEnum;
import cn.chahuyun.authorize.enums.PermissionMatchingEnum;
import cn.chahuyun.authorize.manager.PermissionManager;
import cn.chahuyun.authorize.utils.HibernateUtil;
import cn.hutool.core.collection.EnumerationIter;
import cn.hutool.core.lang.ClassScanner;
import cn.hutool.core.util.URLUtil;
import kotlin.coroutines.EmptyCoroutineContext;
import lombok.SneakyThrows;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.EventChannel;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.ListeningStatus;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.utils.MiraiLogger;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/**
 * 权限服务
 *
 * @author Moyuyanli
 * @date 2023/1/3 9:05
 */
public class PermissionServer {

    private static final PermissionServer instance = new PermissionServer();

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

    /**
     * 加载插件中消息监听方法<p><p>
     * 在一个类上添加 {@link MessageComponent} 声明这个类是一个[消息监听指向类]<p>
     * 表面这个类中有方法需要进行消息监听注册<p>
     * 再在需要进行消息监听的方法上添加 {@link MessageAuthorize} 进行消息监听<p><p>
     * 需要注意的:<p>
     * 没有在所指包中的 [消息监听指向类] 是不会被检测到的<p><p>
     * 包名的写法:<p>
     * cn.xxx<p><p>
     * 如果还不懂使用如何，请参考本插件自身的案例 {@link PermissionManager}<p>
     *
     * @param instance    插件本身唯一实例
     * @param packagePath 所扫描的包
     * @author Moyuyanli
     * @date 2023/1/4 21:43
     */
    @SneakyThrows
    public void init(JavaPlugin instance, String packagePath) {
        MiraiLogger log = HuYanAuthorize.LOGGER;
        //创建一个新的属于该插件的全局EventChannel
        EventChannel<Event> eventEventChannel = GlobalEventChannel.INSTANCE.parentScope(instance);
        //替换包信息
        packagePath = packagePath.replace(".", "/");

        //扫描包下的类
        ClassScanner classScanner = new ClassScanner(packagePath, aClass -> aClass.isAnnotationPresent(MessageComponent.class));
        //获取插件的classloader
        ClassLoader classLoader = instance.getClass().getClassLoader();
        classScanner.setClassLoader(classLoader);
        Set<Class<?>> scan = classScanner.scan();

        if (scan.isEmpty()) {
            log.debug("使用旧的类扫描方式");
            scan = reflectiveScan(classLoader, classScanner, packagePath);
        }

        //获取到对应的类
        scan.forEach(aClass -> {
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
                        if (method.getParameterCount() != 1) {
                            return false;
                        }
                        //去掉参数类型不是消息事件的
                        return method.getParameterTypes()[0].isAssignableFrom(method.getAnnotation(MessageAuthorize.class).messageEventType());
                    })
                    //检查权限消息后注册
                    .forEach(it -> execute(newInstance, it, eventEventChannel.filterIsInstance(MessageEvent.class)));
        });
        log.info("HuYanAuthorize message event registration succeeded !");
    }

    /**
     * 添加一个种 [权限]<p>
     * 如果存在，则返回添加失败<p>
     * <p>
     * 注意:<p>
     * 权限关键字不能携带空格和换行<p>
     * 如果被其他插件占领了你插件需要的权限<p>
     * 你可以在你注册的权限名前面添加前缀<p>
     * 例:<p>
     * hy.admin<p>
     * <p>
     * 默认存在几个关键字权限:<p>
     * owner : 主人<p>
     * null : 不需要权限<p>
     * admin : 权限修改权限<p>
     * all : 所有权限(不会替代其他权限,只会让有这个权限的人或群约等于拥有(除admin)所有权限)<p>
     * <p>
     * 权限等级排序:<p>
     * owner>admin>all>其他<p>
     *
     * @param code        权限id
     * @param description 权限描述
     * @return boolean
     * @author Moyuyanli
     * @date 2023/1/3 15:08
     * @see PermissionInfo
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

    //  ====================================   private   ==========================================

    /**
     * 过滤消息，进行事件监听注册注册
     *
     * @param bean    实体
     * @param method  方法
     * @param channel 监听channel
     * @author Moyuyanli
     * @date 2023/1/3 11:15
     */

    private static void execute(Object bean, @NotNull Method method, @NotNull EventChannel<MessageEvent> channel) {
        HuYanAuthorize.LOGGER.debug("添加消息注册方法->" + method.getName() + " : 消息获取类型->" + method.getParameterTypes()[0].getSimpleName());
        //获取注解信息
        MessageAuthorize annotation = method.getAnnotation(MessageAuthorize.class);
        //过滤条件
        channel.filter(event -> {
            //统一不保留
            boolean quit = false;

            Bot bot = event.getBot();
            User sender = event.getSender();

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
                    if (isPermission(bot.getId(), subject.getId(), subject.getId(), groupPermissionsMatching, groupPermissions)) {
                        return false;
                    }
                }
            }


            //用户权限
            String[] permissions = annotation.userPermissions();
            switch (permissions[0]) {
                case "null":
                    quit = quit;
                    break;
                case "owner":
                    //如果需求主人权限
                    quit = quit && AuthorizeConfig.INSTANCE.getOwner() == sender.getId();
                    break;
                case "admin":
                    //如果需求admin权限
                    quit = quit && (AuthorizeConfig.INSTANCE.getOwner() == sender.getId()
                            || PermissionManager.checkPermission(bot.getId(), subject.getId(), sender.getId(), "admin"));
                    break;
                default:
                    PermissionMatchingEnum permissionsMatching = annotation.userPermissionsMatching();
                    //进行群权限判断,如果不过,直接过滤
                    if (isPermission(bot.getId(), subject.getId(), sender.getId(), permissionsMatching, permissions)) {
                        return false;
                    }
                    break;
            }
            //如果权限判断失败，则不进行消息判断，优化效率
            if (!quit) {
                return false;
            }

            //消息判断
            MessageChain message = event.getMessage();
            String code = message.serializeToMiraiCode();

            MessageMatchingEnum matching = annotation.messageMatching();
            String[] text = annotation.text();

            //消息匹配默认为否
            boolean messageMatching = false;

            if (matching == MessageMatchingEnum.TEXT) {
                for (String messageString : text) {
                    //替换第一个#   [#] 请用 [##] 转意
                    messageString = messageString.replaceFirst("#", "");
                    if (code.equals(messageString)) {
                        messageMatching = true;
                        break;
                    }
                }
            } else {
                messageMatching = Pattern.matches(text[0], code);
            }
            /*
            如果为 空(null)  则消息过滤直接通过
            如果需要判断[null] 请加 [#]  ->  [#null]
             */
            if (text.length == 1) {
                messageMatching = text[0].equals("null") || messageMatching;
            }
            return messageMatching;
        }).subscribe(annotation.messageEventType(),
                EmptyCoroutineContext.INSTANCE,
                annotation.concurrency(),
                annotation.priority(),
                event -> {
                    try {
                        method.invoke(bean, event);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        HuYanAuthorize.LOGGER.error("消息事件方法执行失败!", e);
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

    /**
     * 获指定路径扫描包信息(旧)
     *
     * @param classLoader  类加载器
     * @param classScanner 类扫描器
     * @param packagePath  包路径
     * @return set类集合
     */
    @SneakyThrows
    private Set<Class<?>> reflectiveScan(ClassLoader classLoader, ClassScanner classScanner, String packagePath) {
        //拿到包扫描反射类
        Class<ClassScanner> classScannerClass = ClassScanner.class;
        //获取classload加载的信息
        Enumeration<URL> resources = classLoader.getResources(packagePath);
        //进行类扫描
        EnumerationIter<URL> enumerationIter = new EnumerationIter<>(resources);
        for (URL url : enumerationIter) {
            try {
                Method scanJar = classScannerClass.getDeclaredMethod("scanJar", JarFile.class);
                scanJar.setAccessible(true);
                scanJar.invoke(classScanner, URLUtil.getJarFile(url));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Field classes = classScannerClass.getDeclaredField("classes");
        classes.setAccessible(true);
        return (Set<Class<?>>) classes.get(classScanner);
    }
}
