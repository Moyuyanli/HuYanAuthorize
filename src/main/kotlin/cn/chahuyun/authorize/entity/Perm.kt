package cn.chahuyun.authorize.entity

import jakarta.persistence.*

/**
 * 权限
 */
@Entity(name = "auth_perm")
@Table(name = "auth_perm")
data class Perm(
    /**
     * id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int? = null,
    /**
     * 权限code
     */
    @Column(unique = true)
    var code: String? = null,
    /**
     * 描述
     */
    var description: String = "无",

    /**
     * 注册插件
     */
    var createPlugin: String? = null,

    /**
     * 权限组
     */
    @ManyToMany(mappedBy = "perms", fetch = FetchType.EAGER)
    var permGroup: MutableList<PermGroup> = mutableListOf(),
) {
    constructor(code: String, description: String) : this() {
        this.code = code
        this.description = description
    }

    override fun toString(): String {
        return "Perm(id=$id, code=$code, description='$description', createPlugin=$createPlugin"
    }
}
