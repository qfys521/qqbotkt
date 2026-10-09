package cn.qfys521.qqbot.model.interaction

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * 互动事件 (Interaction) 数据声明结构。
 *
 * 当用户在消息中点击了标记为回调 (type=1) 的交互按钮，或与相关应用组件发生交互行为时，
 * 系统网关将触发 `INTERACTION_CREATE` 事件并将此数据传递给客户端。
 *
 * @property id 平台针对当前这一笔交互分配的全局唯一互动识别 ID。
 * @property applicationId 该互动所属的应用或机器人的唯一应用编号。
 * @property type 互动动作类型标识：如按钮回调、选单更改等。
 * @property scene 交互触发源所属的特定场景标记。
 * @property chatType 聊天来源类型代码：单聊、群聊或频道。
 * @property eventId 关联上游产生的消息事件唯一凭证。
 * @property userOpenId 触发操作者的用户 OpenID（如果在单聊或群会话内）。
 * @property groupOpenId 触发操作所属 QQ 群聊 OpenID。
 * @property guildId 触发操作所属的主频道 ID。
 * @property channelId 触发操作所属的子频道序列号。
 * @property version 数据格式协议大版本号（当前默认 `1`）。
 * @property timestamp 用户完成点击触发事件操作时的服务器 UTC 时间戳。
 * @property data 经由按钮自定义上报回调或包含上下文的附加负载 JSON 结构。
 */
@Serializable
data class Interaction(
    val id: String = "",
    @SerialName("application_id") val applicationId: String = "",
    val type: Int = 0,
    val scene: String? = null,
    @SerialName("chat_type") val chatType: Int = 0,
    @SerialName("event_id") val eventId: String? = null,
    @SerialName("user_openid") val userOpenId: String? = null,
    @SerialName("group_openid") val groupOpenId: String? = null,
    @SerialName("guild_id") val guildId: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    val version: Int = 1,
    val timestamp: String? = null,
    val data: InteractionData? = null,
    @SerialName("group_member_openid") val groupMemberOpenId: String? = null
)

/**
 * 互动附随负载的具体数据封装。
 *
 * @property resolved 解析的业务动态载荷数据树对象。
 */
@Serializable
data class InteractionData(
    val resolved: JsonElement? = null,
    val type: Int? = null
)

/**
 * 回执应答客户端收到的 interaction 互动请求。
 *
 * 对应 HTTP 接口：`PUT /interactions/{interaction_id}`
 *
 * @property code 返回处理状态吗，默认为 `0` (成功应答)；如存在逻辑禁止或失败可以指定其它状态码。
 * @property isWakeup 选填，是否把此处回调视为唤醒会话的动作并获得主动发送下一条提示消息许可。
 */
@Serializable
data class InteractionResponseRequest(
    val code: Int = 0,
    @SerialName("is_wakeup") val isWakeup: Boolean? = null
)
