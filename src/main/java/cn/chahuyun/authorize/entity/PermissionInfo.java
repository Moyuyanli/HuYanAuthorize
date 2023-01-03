package cn.chahuyun.authorize.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 权限信息
 *
 * @author Moyuyanli
 * @date 2023/1/3 11:24
 */
@Entity
@Table(name = "PermissionInfo")
public class PermissionInfo {

    /**
     * 权限id
     */
    @Id
    private String code;
    /**
     * 权限描述
     */
    private String description;

    public PermissionInfo() {
    }

    public PermissionInfo(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
