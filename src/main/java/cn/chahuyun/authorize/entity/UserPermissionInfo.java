package cn.chahuyun.authorize.entity;

import cn.chahuyun.authorize.HuYanAuthorize;
import cn.chahuyun.authorize.utils.HibernateUtil;
import jakarta.persistence.*;
import lombok.Data;

/**
 * 用户信息<p>
 *
 * @author Moyuyanli
 * @date 2023/1/3 9:04
 */
@Data
@Entity
@Table(name = "UserPermissionInfo")
public class UserPermissionInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    /**
     * 所属bot
     */
    private long bot;
    /**
     * 群号id
     */
    private long groupId;
    /**
     * 对象id
     * 可以是用户qq
     * 可以是群号
     */
    private long qq;
    /**
     * 是否全局
     */
    private boolean global;
    /**
     * 权限code
     */
    private String code;

    /**
     * 权限信息
     */
    @Transient
    private PermissionInfo permissionInfo;

    public UserPermissionInfo() {
    }

    public UserPermissionInfo(long qq, long groupId, long bot, boolean global, String code) {
        this.qq = qq;
        this.bot = bot;
        this.groupId = groupId;
        this.global = global;
        this.code = code;
    }

    /**
     * 保存
     *
     * @return true 成功
     */
    public boolean save() {
        try {
            HibernateUtil.factory.fromTransaction(session -> session.merge(this));
            return true;
        } catch (Exception e) {
            HuYanAuthorize.LOGGER.error("群成员基本权限添加失败:", e);
            return false;
        }
    }

    /**
     * 获取权限信息
     *
     * @return 权限信息
     */
    public PermissionInfo getPermissionInfo() {
        return HibernateUtil.factory.fromSession(session -> session.createQuery("from PermissionInfo as prem where prem.code = '" + code + "'", PermissionInfo.class).getSingleResult());
    }
}
