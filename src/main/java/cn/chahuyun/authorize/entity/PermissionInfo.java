package cn.chahuyun.authorize.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 权限信息<p>
 *
 * @author Moyuyanli
 * @date 2023/1/3 11:24
 */
@Data
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

}
