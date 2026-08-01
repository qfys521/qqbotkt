package cn.qfys521.qqbot.model.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * QQ 用户、群聊成员或频道成员信息通用实体类。
 * 能够兼容承载单聊、QQ 群组、频道内的用户资料字段。
 *
 * @property id 平台分配的用户 ID 标识。
 * @property username 账号昵称/显示名。
 * @property bot 是否是机器人的标记变量。
 * @property avatar 用户头像的网络 URL（部分接口或特权场景返回）。
 * @property unionOpenId 跨开发者不同应用的统一 OpenID（如已打通开放平台应用）。
 * @property unionUserAccount 跨应用统一用户账户标示。
 * @property userOpenId 单聊场景下对应的用户 OpenID。
 * @property memberOpenId 群聊或频道成员场景下的群内成员 OpenID。
 * @property memberRole 成员角色类型字串：`"member"` = 普通成员, `"admin"` = 管理员, `"owner"` = 群主/频道主。
 */
@Serializable
data class User(
    val id: String = "",
    val username: String = "",
    val bot: Boolean = false,
    val avatar: String? = null,
    @SerialName("union_openid") val unionOpenId: String? = null,
    @SerialName("union_user_account") val unionUserAccount: String? = null,
    @SerialName("user_openid") val userOpenId: String? = null,
    @SerialName("member_openid") val memberOpenId: String? = null,
    @SerialName("member_role") val memberRole: String? = null
) {
    /** 是否为当前会话（群聊或频道）的群主或建立者。 */
    val isOwner: Boolean get() = memberRole == "owner"

    /** 是否为当前群聊或频道的管理员/群主。 */
    val isAdminOrOwner: Boolean get() = memberRole == "admin" || memberRole == "owner"

    /**
     * 智能获取当前场景下最适用的 OpenID（依次尝试 `memberOpenId` -> `userOpenId` -> `id`）。
     */
    val openId: String get() = memberOpenId ?: userOpenId ?: id
}

/**
 * 当前认证机器人自身的完整公开资料。
 *
 * 通过调用 `GET /users/@me` 接口获得。
 *
 * @property id 机器人自身唯一 ID 标识。
 * @property username 机器人的显示名称。
 * @property avatar 机器人的官方头像图标链接。
 * @property bot 标示自身是否为机器人（恒等于 `true`）。
 * @property unionOpenId 跨应用统一的 OpenID。
 */
@Serializable
data class UserMe(
    val id: String = "",
    val username: String = "",
    val avatar: String? = null,
    val bot: Boolean = true,
    @SerialName("union_openid") val unionOpenId: String? = null
)

/**
 * 机器人被拉入或所参加的频道社区简要信息条目。
 *
 * 通过调用 `GET /users/@me/guilds` 接口返回列表单项。
 *
 * @property id 频道全局 ID。
 * @property name 频道显示的名称。
 * @property icon 频道的图标图片 URL。
 * @property ownerId 频道所有者的 OpenID。
 * @property isOwner 当前查询机器人自己是否就是该频道的拥有者。
 * @property memberCount 频道内当前成员数量统计。
 * @property maxMembers 该频道目前允许容纳的成员最大上限容量。
 * @property description 频道的对外简介阐述。
 * @property joinedAt 机器人被邀请加入此频道的系统时间戳 (RFC3339)。
 */
@Serializable
data class GuildItem(
    val id: String = "",
    val name: String = "",
    val icon: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("is_owner") val isOwner: Boolean = false,
    @SerialName("member_count") val memberCount: Int = 0,
    @SerialName("max_members") val maxMembers: Int = 0,
    val description: String? = null,
    @SerialName("joined_at") val joinedAt: String? = null
)
