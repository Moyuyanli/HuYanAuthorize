package cn.chahuyun.authorize.entity

import cn.chahuyun.authorize.constant.UserType
import cn.chahuyun.authorize.constant.UserType.*
import jakarta.persistence.*
import net.mamoe.mirai.Bot
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

    /**
     * 转换用户文本输出
     */
    fun toUserName(): String {
        val bot = Bot.instances.getOrNull(0)
        return when (type) {
            GLOBAL_USER -> "全局用户:${bot?.let { it.getFriend(userId!!)?.nick ?: "gl-$userId" }}"
            GROUP_MEMBER -> "群成员:${
                bot?.let {
                    it.getGroup(groupId!!)?.get(userId!!)
                        ?.let { ut -> "${ut.group.name}-${ut.nick}" } ?: "g$groupId-u$userId"
                }
            }"

            GROUP -> "群:${bot?.let { it.getGroup(groupId!!)?.name ?: "g$groupId" }}"
            GROUP_ADMIN -> "群管理:${bot?.let { it.getGroup(groupId!!)?.name ?: "g$groupId" }}"
            null -> throw RuntimeException("用户类型错误!")
        }
    }

}
