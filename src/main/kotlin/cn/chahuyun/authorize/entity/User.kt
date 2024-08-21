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
    var createTime: Date? = Date(),

    ) {

    constructor(type: UserType, groupId: Long?, userId: Long?) : this() {
        this.type = type
        this.groupId = groupId
        this.userId = userId
    }


    override fun toString(): String {
        return "User(id=$id, type=$type, groupId=$groupId, userId=$userId, createTime=$createTime)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (type != other.type) return false
        if (groupId != other.groupId) return false
        if (userId != other.userId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type?.hashCode() ?: 0
        result = 31 * result + (groupId?.hashCode() ?: 0)
        result = 31 * result + (userId?.hashCode() ?: 0)
        return result
    }
}
