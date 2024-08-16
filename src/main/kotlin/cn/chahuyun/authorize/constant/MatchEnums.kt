package cn.chahuyun.authorize.constant

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

object LogTopic {

    /**
     * topic
     */
    const val topic = "HuYanAuthorize"

}