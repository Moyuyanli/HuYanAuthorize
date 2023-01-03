package cn.chahuyun.authorize.entity;

import jakarta.persistence.*;

/**
 * 用户信息
 *
 * @author Moyuyanli
 * @date 2023/1/3 9:04
 */
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
     * 对象id
     * 可以是用户qq
     * 可以是群号
     */
    private long qq;
    /**
     * 权限code
     */
    private String code;

    public UserPermissionInfo() {
    }

    public UserPermissionInfo(long qq, long bot, String code) {
        this.qq = qq;
        this.bot = bot;
        this.code = code;
    }

    public long getQq() {
        return qq;
    }

    public void setQq(long qq) {
        this.qq = qq;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getBot() {
        return bot;
    }

    public void setBot(long bot) {
        this.bot = bot;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
