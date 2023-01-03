package cn.chahuyun.authorize.manager;

import cn.chahuyun.authorize.annotation.MessageComponent;
import cn.chahuyun.authorize.entity.UserPermissionInfo;

/**
 * 权限管理
 *
 * @author Moyuyanli
 * @date 2023/1/3 9:05
 */
@MessageComponent
public class PermissionManager {

    /**
     * 检查该用户有没有对应权限
     *
     * @param bot 所属bot
     * @param id 用户id或群id
     * @param code 权限
     * @return boolean true 用户有该权限
     * @author Moyuyanli
     * @date 2023/1/3 14:14
     */
    public static boolean isPermission(long bot, long id, String code) {
        try {
            HibernateUtil.factory.fromSession(session -> session.createQuery(
                    "SELECT perm from UserPermissionInfo perm " +
                            "where perm.bot ='" + bot + "' " +
                            "and perm.qq = '" + id + "' " +
                            "and perm.code = '" + code + "'", UserPermissionInfo.class).getSingleResult());
            return true;
        } catch (Exception e) {
            return false;
        }
    }


}
