package cn.chahuyun.authorize.entity

import jakarta.persistence.*

/**
 * 权限组
 *
 * @author moyuyanli
 */
@Entity(name = "auth_perm_group")
@Table(name = "auth_perm_group")
data class PermGroup(
    /**
     * id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int? = null,
    /**
     * 父id
     */
    var parentId: Int? = null,
    /**
     * 分组名称
     */
    @Column(unique = true)
    var name: String? = null,
    /**
     * 权限列表
     */
    @ManyToMany(cascade = [CascadeType.PERSIST, CascadeType.MERGE], fetch = FetchType.EAGER)
    @JoinTable(
        name = "auth_perm_group_map_perm",
        joinColumns = [JoinColumn(name = "perm_group_id")],
        inverseJoinColumns = [JoinColumn(name = "code_id")]
    )
    var perms: MutableSet<Perm> = mutableSetOf(),
    /**
     * 用户列表
     */
    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    @JoinColumn(name = "permGroup_id")
    var users: MutableSet<User> = mutableSetOf(),
) {

    constructor(name: String, parentId: Int? = null) : this() {
        this.name = name
        this.parentId = parentId
    }

    /**
     * 这个权限code是否存在于该权限组
     */
    fun contains(code: String): Boolean {
        for (perm in perms) {
            if (perm.code == code) return true
        }
        return false
    }

    override fun toString(): String {
        return "PermGroup(id=$id, parentId=$parentId, name=$name, perms=$perms, users=$users)"
    }
}

/**
 * 权限组树
 */
data class PermGroupTree(
    val id: Int?,
    val parentId: Int?,
    val name: String?,
    val perms: MutableSet<Perm>,
    val users: MutableSet<User>,
    var children: MutableSet<PermGroupTree> = mutableSetOf(),
) {

    companion object {
        fun fromPermGroup(permGroup: PermGroup): PermGroupTree {
            return PermGroupTree(
                id = permGroup.id,
                parentId = permGroup.parentId,
                name = permGroup.name,
                perms = permGroup.perms,
                users = permGroup.users
            )
        }
    }
}