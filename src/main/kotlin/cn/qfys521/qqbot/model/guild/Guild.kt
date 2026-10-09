package cn.qfys521.qqbot.model.guild

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * QQ 频道主信息全貌描述实体类。
 *
 * @property id 频道全局 ID。
 * @property name 频道名称。
 * @property icon 频道封面或图标 URL。
 * @property ownerId 频道所有者的 ID。
 * @property owner 标示查询人自身是否即为此频道创建者。
 * @property memberCount 频道中目前共有多少成员。
 * @property maxMembers 当前能够支持的最大人员规模。
 * @property description 频道备注介绍。
 * @property joinedAt 加入时间戳。
 */
@Serializable
data class Guild(
    val id: String = "",
    val name: String = "",
    val icon: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    val owner: Boolean = false,
    @SerialName("member_count") val memberCount: Int = 0,
    @SerialName("max_members") val maxMembers: Int = 0,
    val description: String? = null,
    @SerialName("joined_at") val joinedAt: String? = null,
    @SerialName("op_user_id") val opUserId: String? = null
)

/**
 * QQ 频道内部的子频道 (Channel) 具体属性。
 *
 * @property id 子频道唯一序列号。
 * @property guildId 归属的父级主频道 ID。
 * @property name 子频道名称。
 * @property type 子频道业务属性编码：`0` = 文本子频道, `2` = 语音频道, `10005` = 直播子频道等。
 * @property subType 频道子分类编码。
 * @property position 频道在左侧导航排版中的顺序下标。
 * @property parentId 分组类目录或上级节点 ID。
 * @property ownerId 频道的管理者/所有者标识。
 */
@Serializable
data class Channel(
    val id: String = "",
    @SerialName("guild_id") val guildId: String = "",
    val name: String = "",
    val type: Int = 0,
    @SerialName("sub_type") val subType: Int = 0,
    val position: Int = 0,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("op_user_id") val opUserId: String? = null
)

/**
 * 新建子频道的参数声明请求报文。
 *
 * @property name 准备为该新频道创建的展示名。
 * @property type 频道类别：`0` = 文本频道, `2` = 语音频道等。
 * @property subType 子类别标记。
 * @property position 排序位号。
 * @property parentId 隶属分组 ID。
 */
@Serializable
data class CreateChannelRequest(
    val name: String,
    val type: Int = 0,
    @SerialName("sub_type") val subType: Int = 0,
    val position: Int = 0,
    @SerialName("parent_id") val parentId: String? = null
)

/**
 * 更新或覆盖修改现有子频道的请求。
 *
 * @property name 要更新为的目标显示名称。
 * @property position 新的排序位置。
 * @property parentId 要变更迁移到目标分组分类的 ID。
 */
@Serializable
data class UpdateChannelRequest(
    val name: String? = null,
    val position: Int? = null,
    @SerialName("parent_id") val parentId: String? = null
)
