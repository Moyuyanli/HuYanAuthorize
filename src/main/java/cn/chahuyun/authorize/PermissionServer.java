package cn.chahuyun.authorize;

import cn.chahuyun.authorize.annotation.EventComponent;
import cn.chahuyun.authorize.annotation.MessageAuthorize;
import cn.chahuyun.authorize.entity.PermissionInfo;
import cn.chahuyun.authorize.listening.impl.MessageFilter;
import cn.chahuyun.authorize.manager.PermissionManager;
import cn.chahuyun.authorize.utils.HibernateUtil;
import cn.chahuyun.authorize.utils.Log;
import cn.hutool.core.collection.EnumerationIter;
import cn.hutool.core.lang.ClassScanner;
import cn.hutool.core.util.URLUtil;
import lombok.SneakyThrows;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.EventChannel;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.utils.MiraiLogger;
import org.hibernate.query.Query;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.jar.JarFile;

/**
 * 权限服务
 *
 * @author Moyuyanli
 * @date 2023/1/3 9:05
 */
public class PermissionServer {

    private static final PermissionServer INSTANCE = new PermissionServer();

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
        return INSTANCE;
    }

    /**
     * 加载插件中消息监听方法<p><p>
     * 在一个类上添加 {@link EventComponent} 声明这个类是一个[消息监听指向类]<p>
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
        ClassScanner classScanner = null;
        ClassLoader classLoader = null;
        Set<Class<?>> scan = new HashSet<>();

        try {
            //获取插件的classloader
            classLoader = instance.getClass().getClassLoader();
            //扫描包下的类
            classScanner = new ClassScanner(packagePath, aClass -> aClass.isAnnotationPresent(EventComponent.class));
            classScanner.setClassLoader(classLoader);
            scan = classScanner.scan();
        } catch (Exception e) {
            log.warning("类扫描错误!");
            e.printStackTrace();
        }

        if (classLoader != null && scan.isEmpty()) {
            log.debug("使用旧的类扫描方式");
            scan = reflectiveScan(classLoader, classScanner, packagePath);
        }

        if (scan == null || scan.isEmpty()) {
            log.error("包扫描严重错误，请检查包路径或注解!");
            return;
        }
        MessageFilter.register(scan, eventEventChannel.filterIsInstance(MessageEvent.class));
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
        if (defaultPermissions.contains(code)) {
            Log.warning("禁止使用插件权限种！");
            return false;
        }
        try {
            Query<PermissionInfo> query = HibernateUtil.factory.fromSession(session -> session.createQuery("select * from PERMISSIONINFO as u where u.code = '" + code +"'", PermissionInfo.class));
            if (query.getSingleResult() == null) {
                Log.debug("新权限%s,进行添加",code);
            } else {
                Log.debug("权限%s已存在",code);
                return false;
            }
        } catch (Exception ignored) {
            Log.debug("查询权限%s出错!",code);
        }
        PermissionInfo permissionInfo = new PermissionInfo(code, description);
        try {
            HibernateUtil.factory.fromTransaction(session -> session.merge(permissionInfo));
            Log.debug("权限%s添加成功!",code);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    //  ====================================   private   ==========================================


    /**
     * 获指定路径扫描包信息(旧)
     *
     * @param classLoader  类加载器
     * @param classScanner 类扫描器
     * @param packagePath  包路径
     * @return set类集合
     */
    @Deprecated
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
