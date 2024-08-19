package cn.chahuyun.authorize.entity

import jakarta.persistence.*

/**
 * 权限组
 *
 * @author moyuyanli
 */
@Entity(name = "perm_group")
@Table(name = "perm_group")
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
     * 权限列表
     */
    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var code: MutableList<Perm> = mutableListOf(),
    /**
     * 用户列表
     */
    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var user: MutableList<User> = mutableListOf(),

)
