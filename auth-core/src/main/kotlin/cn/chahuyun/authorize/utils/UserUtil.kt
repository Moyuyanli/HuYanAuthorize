package cn.chahuyun.authorize.utils

import cn.chahuyun.authorize.constant.UserType
import cn.chahuyun.authorize.entity.User
import cn.chahuyun.hibernateplus.HibernateFactory

object UserUtil {
    /**
     * 查询或创建一个 全局用户
     *
     * @see UserType.GLOBAL_USER
     */
    fun globalUser(userId: Long): User {
        val map = mapOf("type" to UserType.GLOBAL_USER, "userId" to userId)
        return HibernateFactory.selectOne(User::class.java, map) ?: User(type = UserType.GLOBAL_USER, userId = userId)
    }

    /**
     * 查询或创建一个 群用户
     *
     * @see UserType.GROUP
     */
    fun group(groupId: Long): User {
        val map = mapOf("type" to UserType.GROUP, "groupId" to groupId)
        return HibernateFactory.selectOne(User::class.java, map) ?: User(type = UserType.GROUP, groupId = groupId)
    }

    /**
     * 查询或创建一个 群管理用户
     *
     * @see UserType.GROUP_ADMIN
     */
    fun groupAdmin(groupId: Long): User {
        val map = mapOf("type" to UserType.GROUP_ADMIN, "groupId" to groupId)
        return HibernateFactory.selectOne(User::class.java, map) ?: User(type = UserType.GROUP_ADMIN, groupId = groupId)
    }

    /**
     * 查询或创建一个 群成员用户
     *
     * @see UserType.GROUP_MEMBER
     */
    fun member(groupId: Long, userId: Long): User {
        val map = mapOf(
            "type" to UserType.GROUP_MEMBER,
            "groupId" to groupId,
            "userId" to userId
        )
        return HibernateFactory.selectOne(User::class.java, map) ?: User(
            type = UserType.GROUP_MEMBER,
            groupId = groupId,
            userId = userId
        )
    }
}