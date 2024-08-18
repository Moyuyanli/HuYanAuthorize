package cn.chahuyun.authorize.entity

import cn.chahuyun.authorize.constant.UserType
import jakarta.persistence.*


@Entity(name = "auth_user_permission")
@Table(name = "auth_user_permission")
data class UserPermissionInfo(
    /**
     * id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int? = null,
    /**
     *  bot
     */
    var bot: Long? = null,
    /**
     * 群号
     */
    var groupId: Long? = null,
    /**
     * qq
     */
    var qq: Long? = null,
    /**
     * 用户类型
     */
    var type: UserType = UserType.GLOBAL,
    /**
     * 权限code
     */
    var code: String? = null,
) {

}