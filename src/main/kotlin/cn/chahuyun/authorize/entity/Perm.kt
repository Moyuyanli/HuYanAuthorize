package cn.chahuyun.authorize.entity

import cn.chahuyun.authorize.utils.PermUtil
import jakarta.persistence.*

/**
 * 权限
 *
 * @see PermUtil
 */
@Entity(name = "auth_perm")
@Table(name = "auth_perm")
data class Perm(
    /**
     * id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    @Column(name = "create_plugin")
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

    fun setCreatePlugin(name: String): Perm {
        this.createPlugin = name
        return this
    }

    override fun toString(): String {
        return "Perm(id=$id, code=$code, description='$description', createPlugin=$createPlugin"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Perm

        if (id != other.id) return false
        if (code != other.code) return false
        if (description != other.description) return false
        if (createPlugin != other.createPlugin) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id ?: 0
        result = 31 * result + (code?.hashCode() ?: 0)
        result = 31 * result + description.hashCode()
        result = 31 * result + (createPlugin?.hashCode() ?: 0)
        return result
    }
}
