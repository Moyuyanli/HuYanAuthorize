package cn.chahuyun.test

import cn.chahuyun.authorize.constant.UserType
import cn.chahuyun.authorize.entity.Perm
import cn.chahuyun.authorize.entity.PermGroup
import cn.chahuyun.authorize.entity.User
import cn.chahuyun.hibernateplus.HibernateFactory
import java.util.*


fun test() {

    var code = Perm(
        code = "admin",
        description = "管理员",
        createPlugin = "HuYanAuthorize"
    )

    val mergeCode = HibernateFactory.merge(code)

    println("code -> $mergeCode")

    var user = User(
        type = UserType.GROUP,
        userId = 572490972,
        createTime = Date()
    )

    val mergeUser = HibernateFactory.merge(user)

    println("user -> $mergeUser")


    code = HibernateFactory.selectOne(Perm::class.java, 1)

    val permGroup1 = PermGroup(
        name = "默认",
        perms = mutableSetOf(code),
        users = mutableSetOf(mergeUser)
    )

    val group = HibernateFactory.merge(permGroup1)

    println("group -> $group")

    user = User(
        type = UserType.GLOBAL_USER,
        userId = 572490972,
        createTime = Date()
    )

    val user1 = HibernateFactory.merge(user)

    println("user1 -> $user1")

    val permGroup2 = PermGroup(
        name = "默认",
        perms = mutableSetOf(code),
        users = mutableSetOf(user1)
    )

    val group1 = HibernateFactory.merge(permGroup2)

    val selectOne1 = HibernateFactory.selectOne(PermGroup::class.java, group.id)
    val selectOne2 = HibernateFactory.selectOne(PermGroup::class.java, group1.id)
    println("select one1 -> $selectOne1")
    println("select one2 -> $selectOne2")


    val one1 = HibernateFactory.selectOne(User::class.java, 1)
    val one2 = HibernateFactory.selectOne(User::class.java, 2)
    println("one1 -> $one1")
    println("one2 -> $one2")

//        selectOne.user.removeAt(0)
//
//        val group = HibernateFactory.merge(selectOne)
//
//        println(group)

}


fun test1() {
//        val one = HibernateFactory.selectOne(Perm::class.java, "code", "admin") ?: throw RuntimeException("权限不存在!")
//
//        println("one -> $one")
//        println("one.permGroup -> ${one.permGroup}")


}
