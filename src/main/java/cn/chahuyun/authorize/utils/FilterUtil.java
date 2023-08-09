package cn.chahuyun.authorize.utils;

import cn.chahuyun.authorize.annotation.GroupAuthorize;
import cn.chahuyun.authorize.annotation.MessageAuthorize;
import cn.chahuyun.authorize.config.AuthorizeConfig;
import cn.chahuyun.authorize.enums.MessageMatchingEnum;
import cn.chahuyun.authorize.enums.PermissionMatchingEnum;
import cn.chahuyun.authorize.manager.PermissionManager;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.events.GroupEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

/**
 * 工具
 *
 * @author Moyuyanli
 * @date 2023/8/8 14:57
 */
public class FilterUtil {
    /**
     * 无权限
     */
    private static final String NULL = "null";
    /**
     * 管理权限
     */
    private static final String ADMIN = "admin";
    /**
     * 主人权限
     */
    private static final String OWNER = "owner";


    /**
     * 检查消息授权注解
     *
     * @param method 方法
     * @return boolean
     */
    public static boolean methodCheckMessage(Method method) {
        //去掉参数数量不为1的
        return method.isAnnotationPresent(MessageAuthorize.class) && method.getParameterCount() == 1;
    }

    /**
     * 检查群动态授权注解
     *
     * @param method 方法
     * @return boolean
     */
    public static boolean methodCheckGroup(Method method) {
        return method.isAnnotationPresent(GroupAuthorize.class) && method.getParameterCount() == 1;
    }

    /**
     * 检查消息的权限
     *
     * @param event 消息事件
     * @return boolean
     */
    public static boolean eventCheckPermission(MessageEvent event, MessageAuthorize authorize) {

        Bot bot = event.getBot();
        User sender = event.getSender();

        //拿到群
        Contact subject = event.getSubject();
        //如果是群
        if (subject instanceof Group) {
            String[] groupPermissions = authorize.groupPermissions();
            if (!groupPermissions[0].equals(NULL)) {
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
            case NULL:
                return true;
            case ADMIN:
            case OWNER:
                //如果需求主人权限
                if (AuthorizeConfig.INSTANCE.getOwner() == sender.getId()) {
                    return true;
                }
                //如果需求admin权限
                if (PermissionManager.checkPermission(bot.getId(), subject.getId(), sender.getId(), ADMIN)) {
                    return true;
                }
            default:
                //进行群权限判断,如果不过,直接过滤
                return !isPermission(bot.getId(), subject.getId(), sender.getId(), userPermissionsMatching, permissions);
        }
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
