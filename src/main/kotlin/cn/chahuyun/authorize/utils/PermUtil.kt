package cn.chahuyun.authorize.utils

import cn.chahuyun.authorize.entity.Perm
import cn.chahuyun.authorize.entity.PermGroup
import cn.chahuyun.authorize.entity.User
import cn.chahuyun.hibernateplus.HibernateFactory


/**
 * 权限操作工具
 *
 * @author Moyuyanli
 * @date 2024/9/2 9:39
 */
object PermUtil {

    /**
     * 根据id查询权限组
     */
    fun selectOneById(id: Long): PermGroup? {
        return HibernateFactory.selectOne(PermGroup::class.java, id)
    }

    /**
     * 根据名称查询权限组
     */
    fun selectOneByName(name: String): PermGroup {
        return HibernateFactory.selectOne(PermGroup::class.java, "name", name)
    }

    /**
     * 检查这个用户，有没有这个权限
     */
    fun checkUserHasPerm(user: User, code: String): Boolean {
        val selectOne = HibernateFactory.selectOne(Perm::class.java, "code", code)
        if (selectOne == null) {
            Log.debug("权限不存在!")
            return false
        }
        selectOne.permGroup.forEach {
            if (it.users.contains(user)) return true
        }
        return false
    }

    /**
     * 将这个用户添加到对应的权限组
     */
    fun addUserToPermGroupByName(user: User, name: String): Boolean {
        val selectOne =
            HibernateFactory.selectOne(PermGroup::class.java, "name", name) ?: throw RuntimeException("权限组不存在!")

        if (selectOne.users.contains(user)) {
            return false
        }

        selectOne.users.add(user)
        HibernateFactory.merge(selectOne)

        return true
    }

    /**
     * 将这个用户从对应权限组删除
     */
    fun delUserFromPermGroupByName(user: User, name: String): Boolean {
        val selectOne =
            HibernateFactory.selectOne(PermGroup::class.java, "name", name) ?: throw RuntimeException("权限组不存在!")

        if (!selectOne.users.contains(user)) {
            return false
        }

        selectOne.users.remove(user)
        HibernateFactory.merge(selectOne)

        return true
    }
}