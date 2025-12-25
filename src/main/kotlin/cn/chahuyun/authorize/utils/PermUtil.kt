package cn.chahuyun.authorize.utils

import cn.chahuyun.authorize.HuYanAuthorize.log
import cn.chahuyun.authorize.constant.AuthPerm
import cn.chahuyun.authorize.entity.Perm
import cn.chahuyun.authorize.entity.PermGroup
import cn.chahuyun.authorize.entity.User
import cn.chahuyun.hibernateplus.HibernateFactory
import java.util.concurrent.ConcurrentHashMap


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
    fun selectPermGroupOneById(id: Long): PermGroup? {
        return HibernateFactory.selectOneById(id)
    }

    /**
     * 根据名称查询权限组
     */
    fun selectPermGroupOneByName(name: String): PermGroup? {
        return HibernateFactory.selectOne(PermGroup::class.java, "name", name)
    }

    /**
     * 取一个权限组，如果不存在则新建
     */
    fun takePermGroupByName(name: String): PermGroup {
        return HibernateFactory.selectOne(PermGroup::class.java, "name", name)
            ?: HibernateFactory.merge(PermGroup(name))
    }

    /**
     * 获取已经注册的权限
     */
    fun takePerm(code: String): Perm {
        return HibernateFactory.selectOne(Perm::class.java, "code", code) ?: throw RuntimeException("改权限未注册!")
    }

    /**
     * 检查这个用户，有没有这个权限
     *
     * @return true 有权限
     */
    fun checkUserHasPerm(user: User, code: String): Boolean {
        val perm = PermCache.get(code)
        if (perm == null) {
            log.debug("权限码 $code 未注册!")
            return false
        }
        return perm.permGroup.any { group ->
            group.users.contains(user)
        }
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
        PermCache.refresh() // 用户变更，刷新缓存以确保鉴权准确

        return true
    }

    /**
     * 将这个权限添加到对应的权限组
     */
    fun addPermToPermGroupByPermGroup(perm: Perm, permGroup: PermGroup): Boolean {
        permGroup.perms.add(perm)
        HibernateFactory.merge(permGroup)
        PermCache.refresh() // 权限组变更，刷新缓存
        return true
    }

    /**
     * 将这个权限添加到对应的权限组
     */
    fun addPermToPermGroupByName(perm: Perm, name: String): Boolean {
        val group = takePermGroupByName(name)
        group.perms.add(perm)
        HibernateFactory.merge(group)
        PermCache.refresh() // 权限组变更，刷新缓存
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
        PermCache.refresh() // 用户变更，刷新缓存

        return true
    }

    /**
     * 检查这个人是否主人
     *
     * @return true 是
     */
    fun checkOwner(qq: Long): Boolean {
        return checkUserHasPerm(UserUtil.globalUser(qq), AuthPerm.OWNER)
    }

}

/**
 * 权限缓存
 * 懒加载
 */
object PermCache {

    @Volatile
    private var loaded = false

    private val cache = ConcurrentHashMap<String, Perm>()

    fun get(code: String): Perm? {
        // 快路径：已加载，直接查缓存
        if (loaded) return cache[code]

        // 慢路径：未加载，加锁初始化
        synchronized(this) {
            if (!loaded) {
                loadAllPermsFromDatabase()
                loaded = true
            }
        }
        return cache[code]
    }

    private fun loadAllPermsFromDatabase() {
        // 从 DB 查询所有已注册的权限
        val perms = HibernateFactory.selectList(Perm::class.java)
        for (perm in perms) {
            cache[perm.code!!] = perm
        }
        log.debug("权限缓存已加载，共 ${perms.size} 项")
    }

    // 可选：提供手动刷新接口
    fun refresh() {
        synchronized(this) {
            cache.clear()
            loadAllPermsFromDatabase()
        }
    }
}