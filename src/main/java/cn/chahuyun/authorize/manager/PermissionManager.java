package cn.chahuyun.authorize.manager;

import cn.chahuyun.authorize.HuYanAuthorize;
import cn.chahuyun.authorize.annotation.MessageAuthorize;
import cn.chahuyun.authorize.annotation.MessageComponent;
import cn.chahuyun.authorize.entity.PermissionInfo;
import cn.chahuyun.authorize.entity.UserPermissionInfo;
import cn.chahuyun.authorize.enums.MessageMatchingEnum;
import cn.chahuyun.authorize.utils.HibernateUtil;
import cn.chahuyun.authorize.utils.QueryUtil;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.message.data.SingleMessage;

import java.util.Objects;

/**
 * 权限管理<p>
 *
 * @author Moyuyanli
 * @date 2023/1/3 9:05
 */
@MessageComponent
public class PermissionManager {


    /**
     * 添加/删除权限
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2023/1/4 22:58
     */
    @MessageAuthorize(
            text = "^[+|-]\\[mirai:at:\\d+]( +\\S+){1,5}",
            messageMatching = MessageMatchingEnum.REGULAR,
            userPermissions = {"admin"},
            messageEventType = GroupMessageEvent.class
    )
    public void addPermission(GroupMessageEvent event) {
        Contact subject = event.getSubject();
        Group group = event.getGroup();
        Bot bot = event.getBot();

        MessageChain message = event.getMessage();
        String code = message.serializeToMiraiCode();
        MessageChainBuilder builder = QueryUtil.quoteReply(message);

        long atId = 0;
        for (SingleMessage singleMessage : message) {
            if (singleMessage instanceof At) {
                atId = ((At) singleMessage).getTarget();
            }
        }
        if (atId == 0) {
            subject.sendMessage("权限添加识别失败!");
            return;
        }

        //是否添加
        boolean b = code.charAt(0) == '+';
        //权限code分割
        String[] split = code.split(" +");
        if (b) {
            builder.append(QueryUtil.formatMessage("为群成员%s(%s)添加以下权限:", Objects.requireNonNull(group.get(atId)).getNick(), atId));
            for (int i = 1; i < split.length; i++) {
                String perm = split[i];
                if (!isPermission(perm)) {
                    builder.append("\n权限:").append(perm).append("-权限不存在!");
                    continue;
                }
                //是否存在该权限
                if (checkPermission(bot.getId(), subject.getId(), atId, perm)) {
                    builder.append("\n权限:").append(perm).append("-已存在!");
                    continue;
                }
                UserPermissionInfo userPermissionInfo = new UserPermissionInfo(atId, subject.getId(), bot.getId(), false, perm);
                if (userPermissionInfo.save()) {
                    builder.append("\n权限:").append(perm).append("-添加成功!");
                } else {
                    builder.append("\n权限:").append(perm).append("-添加失败!");
                }
            }
        } else {
            builder.append(QueryUtil.formatMessage("为群成员%s(%s)删除以下权限:", Objects.requireNonNull(group.get(atId)).getNick(), atId));
            for (int i = 1; i < split.length; i++) {
                String perm = split[i];
                if (!isPermission(perm)) {
                    builder.append("\n权限:").append(perm).append("-权限不存在!");
                    continue;
                }
                //是否存在该权限
                if (checkPermission(bot.getId(), subject.getId(), atId, perm)) {
                    long finalAtId = atId;
                    Boolean aBoolean = HibernateUtil.factory.fromSession(session -> {
                        try {
                            UserPermissionInfo singleResult = session.createQuery(
                                    "select u.* from UserPermissionInfo u " +
                                            "left join PermissionInfo perm " +
                                            "and u.bot ='" + bot.getId() + "' " +
                                            "and u.qq = '" + finalAtId + "' " +
                                            "and u.groupId = '" + subject.getId() + "' " +
                                            "and perm.code = '" + code + "'", UserPermissionInfo.class).getSingleResult();
                            session.remove(singleResult);
                            return true;
                        } catch (Exception e) {
                            HuYanAuthorize.log.error("群成员基本权限删除失败:", e);
                            return false;
                        }
                    });
                    if (aBoolean) {
                        builder.append("\n权限:").append(perm).append("-删除成功!");
                    } else {
                        builder.append("\n权限:").append(perm).append("-删除失败!");
                    }
                } else {
                    builder.append("\n权限:").append(perm).append("-不存在!");
                }
            }
        }
        subject.sendMessage(builder.build());
    }


    @MessageAuthorize(
            text = "测试",
            userPermissions = "admin",
            messageEventType = GroupMessageEvent.class
    )
    public void test(GroupMessageEvent event) {
        event.getSubject().sendMessage("成功");
    }


    /**
     * 检查该用户有没有对应权限<p>
     *
     * @param bot   所属bot
     * @param group 群id
     * @param id    用户id或群id
     * @param code  权限
     * @return boolean true 用户有该权限
     * @author Moyuyanli
     * @date 2023/1/3 14:14
     */
    public static boolean checkPermission(long bot, long group, long id, String code) {
        return checkPermission(bot, group, id, code, true);
    }

    /**
     * 判断该权限是否存在<p>
     *
     * @param code 权限code
     * @return boolean  true 存在
     * @author Moyuyanli
     * @date 2023/1/6 3:37
     */
    public static boolean isPermission(String code) {
        try {
            return HibernateUtil.factory.fromSession(session -> session.createQuery("from PermissionInfo as perm where perm.code = '" + code + "'", PermissionInfo.class).getSingleResult() != null);
        } catch (Exception e) {
            return false;
        }
    }


    private static boolean checkPermission(long bot, long group, long id, String code, boolean number) {
        //第一次检查个人用户权限和群权限
        if (number) {
            try {
                boolean result = HibernateUtil.factory.fromSession(session -> session.createQuery(
                        "from UserPermissionInfo as u " +
                                "left join PermissionInfo as perm on u.code = perm.code " +
                                "where u.bot ='" + bot + "' " +
                                "and u.qq = '" + id + "' " +
                                "and u.global =  true " +
                                "and perm.code = '" + code + "'", UserPermissionInfo.class).getSingleResult()) != null;
                //如果不为空
                if (result) {
                    //返回true
                    return true;
                } else {
                    //如果是群
                    if (group == id) {
                        return false;
                    }
                }
                //为空则进行二次检查
                return checkPermission(bot, group, id, code, false);
            } catch (Exception e) {
                if (group == id) return false;
                return checkPermission(bot, group, id, code, false);
            }
        } else {
            try {
                //如果在群中
                return HibernateUtil.factory.fromSession(session -> session.createQuery(
                        "from UserPermissionInfo u " +
                                "left join PermissionInfo perm on u.code = perm.code " +
                                "where u.bot ='" + bot + "' " +
                                "and u.qq = '" + id + "' " +
                                "and u.groupId = '" + group + "' " +
                                "and perm.code = '" + code + "'", UserPermissionInfo.class).getSingleResult()) != null;
            } catch (Exception e) {
                return false;
            }
        }
    }

}