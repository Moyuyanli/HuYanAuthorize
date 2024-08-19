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
    var code:String? = null,
    /**
     * 描述
     */
    var description:String = "无",

    /**
     * 注册插件
     */
    var createPlugin:String? = null
)
