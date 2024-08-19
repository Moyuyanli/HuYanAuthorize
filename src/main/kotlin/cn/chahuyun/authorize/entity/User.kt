package cn.chahuyun.authorize.entity

import cn.chahuyun.authorize.constant.UserType
import jakarta.persistence.*
import java.util.*

/**
 * 用户
 *
 */
@Entity(name = "auth_user")
@Table(name = "auth_user")
data class User(
    /**
     * id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int? = null,
    /**
     * 类型
     * @see UserType
     */
    var type: UserType? = null,
    /**
     * 群id
     */
    var groupId: Long? = null,
    /**
     * 用户id(qq)
     */
    var userId: Long? = null,
    /**
     * 创建时间
     */
    var createTime: Date? = null,
    /**
     * 权限组
     */
    @ManyToOne
    var permGroup: PermGroup? = null,
)
