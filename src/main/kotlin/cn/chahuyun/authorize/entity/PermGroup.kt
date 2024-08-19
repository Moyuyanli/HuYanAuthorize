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
    override fun toString(): String {
        return "PermGroup(id=$id, parentId=$parentId, name=$name, perms=$perms, users=$users)"
    }
}