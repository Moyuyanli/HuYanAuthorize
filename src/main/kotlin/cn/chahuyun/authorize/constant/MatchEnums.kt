package cn.chahuyun.authorize.constant


import lombok.Getter

/**
 * 匹配方式枚举
 *
 *
 * @author Moyuyanli
 * @Date 2023/1/2 22:59
 */
enum class MessageMatchingEnum {

    /**
     * 文本直接匹配
     */
    TEXT,

    /**
     * 正则匹配
     */
    REGULAR,

    /**
     * 自定义
     */
    CUSTOM
}


/**
 * 权限匹配方式
 *
 *
 * @author Moyuyanli
 * @date 2023/1/3 9:18
 */
enum class PermissionMatchingEnum {

    /**
     * 或
     */
    OR,

    /**
     * 与(且)
     */
    AND
}


/**
 * 作用域类型
 */
@Getter
enum class UserType(private val valueTemplate: String) {
    /**
     * 全局
     */
    GLOBAL("global"),

    /**
     * 全局用户
     */
    GLOBAL_USER("global-%d"),

    /**
     * 群
     */
    GROUP("group-%d"),

    /**
     * 群用户
     */
    GROUP_MEMBER("group-%d-member-%d"),

    /**
     * 群分组用户
     */
    GROUP_MEMBERS("group-%d-membersName-%s"),

    /**
     * 分组群
     */
    LIST("list-%s"),

    /**
     * 分组用户
     */
    USERS("users-%s")
}


/**
 * 权限常量
 *
 * @author Moyuyanli
 * @date 2023/8/11 16:40
 */
object PermConstant {

    /**
     * 无权限
     */
    const val NULL: String = "null"

    /**
     * 管理权限
     */
    const val ADMIN: String = "admin"

    /**
     * 主人权限
     */
    const val OWNER: String = "owner"

}

/**
 * 日志常量
 */
object LogTopic {

    /**
     * topic
     */
    const val topic = "HuYanAuthorize"

}