package cn.qfys521.qqbot.model.message

import cn.qfys521.qqbot.model.user.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * 接收或返回的通用消息描述（包含单聊、群聊及频道消息）。
 *
 * @property id 消息唯一标识 ID，可用于被动回复或调用撤回消息接口。
 * @property author 消息发送者基本资料。
 * @property content 消息纯文本内容（对于群聊 @ 消息，平台自动去除了 @ 机器人的文本前缀）。
 * @property groupOpenId 目标群聊 OpenID（群聊场景下存在）。
 * @property userOpenId 目标用户 OpenID（单聊场景下存在）。
 * @property guildId 频道 ID（频道场景下存在）。
 * @property channelId 子频道 ID（频道场景下存在）。
 * @property timestamp 消息发送的系统毫秒级 RFC3339 时间戳字符串。
 * @property messageType 消息具体类型：0=纯文本/普通消息, 2=Markdown, 3=ARK 结构化卡片, 7=富媒体消息。
 * @property messageScene 消息业务场景上下文描述。
 * @property attachments 消息附带的文件/图片/语音等多媒体附件列表。
 * @property mentions 消息中文本提及 (@) 的用户列表（不包含机器人自身）。
 * @property arkData 结构化卡片 (ARK) 详细数据。
 * @property msgElements 组合嵌套式的富文本消息元素列表。
 */
@Serializable
data class Message(
    val id: String = "",
    val author: User = User(),
    val content: String = "",
    @SerialName("group_openid") val groupOpenId: String? = null,
    @SerialName("user_openid") val userOpenId: String? = null,
    @SerialName("guild_id") val guildId: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    val timestamp: String? = null,
    @SerialName("message_type") val messageType: Int = 0,
    @SerialName("message_scene") val messageScene: MessageScene? = null,
    val attachments: List<MessageAttachment>? = null,
    val mentions: List<User>? = null,
    @SerialName("ark_data") val arkData: ArkData? = null,
    @SerialName("msg_elements") val msgElements: List<MsgElement>? = null
) {
    /**
     * 快速将此消息转换为回复上下文引用实体 [MessageReference]。
     *
     * @param ignoreError 是否忽略获取被引用消息失败的报错（默认 `false`）。
     * @return 构建完善的 [MessageReference] 实例。
     */
    fun toReference(ignoreError: Boolean = false): MessageReference {
        return MessageReference(messageId = id, ignoreGetMessageError = ignoreError)
    }
}

/**
 * 消息业务场景上下文描述。
 *
 * @property source 场景来源名称（默认 `"default"`）。
 * @property ext 额外拓展业务标签字符串列表。
 */
@Serializable
data class MessageScene(
    val source: String = "default",
    val ext: List<String>? = null
)

/**
 * 消息携带的附件实体（如图片、视频、语音、普通文件等）。
 *
 * @property url 附件网络直接下载地址。
 * @property filename 文件原始文件名。
 * @property width 图片像素宽度。
 * @property height 图片像素高度。
 * @property size 文件总大小（字节）。
 * @property contentType 附件 MIME 类型（例如 `image/jpeg`、`video/mp4`、`voice`）。
 * @property voiceWavUrl 语音消息转换后的 WAV 格式网络下载链接。
 * @property asrReferText 语音消息自动识别 (ASR) 转写出的文本内容。
 */
@Serializable
data class MessageAttachment(
    val url: String? = null,
    val filename: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val size: Long? = null,
    @SerialName("content_type") val contentType: String? = null,
    @SerialName("voice_wav_url") val voiceWavUrl: String? = null,
    @SerialName("asr_refer_text") val asrReferText: String? = null
)

/**
 * ARK 结构化模板卡片数据实体。
 *
 * @property prompt 卡片推送通知栏的弱提示语。
 * @property arkType 结构化卡片特定业务类型标记。
 * @property arkName 卡片模板名称。
 * @property fields 卡片内嵌套字段 KV 数据源。
 */
@Serializable
data class ArkData(
    val prompt: String? = null,
    @SerialName("ark_type") val arkType: String? = null,
    @SerialName("ark_name") val arkName: String? = null,
    val fields: JsonElement? = null
)

/**
 * 复合消息元素实体，支持描述单条复杂富文本消息。
 *
 * @property msgIdx 元素序号索引。
 * @property author 元素对应的发布作者。
 * @property messageType 子元素类型编码。
 * @property content 子文本内容。
 * @property attachments 元素关联媒体附件列表。
 * @property arkData 元素关联结构化卡片。
 * @property msgElements 递归子元素列表。
 */
@Serializable
data class MsgElement(
    @SerialName("msg_idx") val msgIdx: String? = null,
    val author: User? = null,
    @SerialName("message_type") val messageType: Int = 0,
    val content: String? = null,
    val attachments: List<MessageAttachment>? = null,
    @SerialName("ark_data") val arkData: ArkData? = null,
    @SerialName("msg_elements") val msgElements: List<MsgElement>? = null
)

/**
 * 发送消息的通用 HTTP 请求体。
 * 兼容单聊 (`POST /v2/users/{id}/messages`)、群聊 (`POST /v2/groups/{id}/messages`) 等标准接口。
 *
 * @property content 普通文本消息体（当 `msgType=0` 时设置；如使用 `markdown` 则设为 `null`）。
 * @property msgType 消息格式代码：`0` = 普通文本, `2` = Markdown 文本, `6` = 输入状态提醒, `7` = 富媒体文件消息。
 * @property markdown Markdown 渲染消息实体（`msgType=2` 时使用）。
 * @property keyboard 随消息下发的交互按钮键盘。
 * @property msgId 被动回复目标消息 ID（在事件发生 5 分钟内有效，与 [eventId] 互斥）。
 * @property eventId 被动回复目标互动事件 ID（与 [msgId] 互斥）。
 * @property msgSeq 消息序号，在多次调用时自增配置以防被平台拦截重发，默认为 `1`。
 * @property media 富媒体消息信息体（通过 `/files` 接口成功上传返回的 `file_info` 构造）。
 * @property messageReference 消息回复引用关系配置（会在聊天窗口展示引用气泡）。
 * @property isWakeup 是否作为互动召回主动推送消息发出。
 * @property inputNotify 正在输入状态提醒配置（`msgType=6` 时使用）。
 */
@Serializable
data class SendMessageRequest(
    val content: String? = null,
    @SerialName("msg_type") val msgType: Int? = null,
    val markdown: MessageMarkdown? = null,
    val keyboard: Keyboard? = null,
    @SerialName("msg_id") val msgId: String? = null,
    @SerialName("event_id") val eventId: String? = null,
    @SerialName("msg_seq") val msgSeq: Int? = null,
    val media: MediaInfo? = null,
    @SerialName("message_reference") val messageReference: MessageReference? = null,
    @SerialName("is_wakeup") val isWakeup: Boolean? = null,
    @SerialName("input_notify") val inputNotify: InputNotify? = null
)

/**
 * Markdown 格式文本内容声明实体。
 *
 * @property content 原生 Markdown 字符串内容（部分平台原生语法）。
 * @property templateId 平台内置 Markdown 模板 ID（当引用标准模板时）。
 * @property customTemplateId 开发者在后台配置的自定义模板 ID。
 */
@Serializable
data class MessageMarkdown(
    val content: String? = null,
    @SerialName("template_id") val templateId: Int? = null,
    @SerialName("custom_template_id") val customTemplateId: String? = null
)

/**
 * 交互按钮键盘 (Keyboard) 配置类。
 *
 * @property id 自定义或模板键盘唯一标识。
 * @property content 动态声明的键盘行与按钮布局内容。
 */
@Serializable
data class Keyboard(
    val id: String? = null,
    val content: KeyboardContent? = null
)

/**
 * 键盘布局内容声明类。
 *
 * @property rows 按钮行列表（一般支持多行展示）。
 */
@Serializable
data class KeyboardContent(
    val rows: List<Row>? = null
)

/**
 * 键盘单行声明。
 *
 * @property buttons 当前行内包含的交互按钮列表（行内自适应排布）。
 */
@Serializable
data class Row(
    val buttons: List<Button>? = null
)

/**
 * 单个交互按钮声明实体。
 *
 * @property id 按钮自身唯一的识别 ID。
 * @property renderData 按钮渲染属性与样式定义。
 * @property action 按钮被点击触发的操作事件行为声明。
 */
@Serializable
data class Button(
    val id: String? = null,
    @SerialName("render_data") val renderData: RenderData? = null,
    val action: Action? = null
)

/**
 * 按钮展示渲染参数。
 *
 * @property label 默认显示的文字提示。
 * @property visitedLabel 用户点击过之后显示为已点击状态的说明说明文本。
 * @property style 样式：`0` = 灰框, `1` = 蓝框, `2` = 白字, `3` = 蓝底白字。
 */
@Serializable
data class RenderData(
    val label: String = "",
    @SerialName("visited_label") val visitedLabel: String? = null,
    val style: Int = 0
)

/**
 * 交互按钮点击响应操作。
 *
 * @property type 操作触发的动作分类：`0` = 跳转链接/小程序, `1` = 回调后台接收 Interaction 事件, `2` = 将特定命令字直接粘贴或自动向机器人发送。
 * @property permission 按钮的触达操作白名单控制限制。
 * @property data 跳转目标 URL 链接、或者自定义回调标识数据、或者触发指令文本。
 * @property unsupportTips 客户端版本不支持此交互按钮类型时的兜底提示文本。
 */
@Serializable
data class Action(
    val type: Int = 0,
    val permission: Permission? = null,
    val data: String? = null,
    @SerialName("unsupport_tips") val unsupportTips: String? = null
)

/**
 * 交互按钮触发权限管控配置。
 *
 * @property type 白名单过滤类型：`0` = 全体允许, `1` = 指定用户列表可操作, `2` = 指定管理权限或身份组可触发。
 * @property specifyUserIds 允许操作的具体用户 OpenID/ID 列表。
 * @property specifyRoleIds 允许操作的具体角色 ID 列表。
 */
@Serializable
data class Permission(
    val type: Int = 0,
    @SerialName("specify_user_ids") val specifyUserIds: List<String>? = null,
    @SerialName("specify_role_ids") val specifyRoleIds: List<String>? = null
)

/**
 * 消息回复引用上下文配置。
 *
 * @property messageId 引用的目标消息唯一标识。
 * @property ignoreGetMessageError 是否在此引用消息不存在或因权限无法拉取时自动降级发送主文本而忽略错误。
 */
@Serializable
data class MessageReference(
    @SerialName("message_id") val messageId: String = "",
    @SerialName("ignore_get_message_error") val ignoreGetMessageError: Boolean = false
)

/**
 * 正在输入中通知配置体。
 *
 * @property type 提醒类型（默认 `1` 表示正在输入）。
 * @property status 当前提醒状态编码。
 */
@Serializable
data class InputNotify(
    val type: Int = 1,
    val status: Int = 1
)

/**
 * 富媒体文件发送载体包装。
 *
 * @property fileInfo 经由 `/files` 上传成功返回的有效签名文件信息令牌 (`file_info`)。
 */
@Serializable
data class MediaInfo(
    @SerialName("file_info") val fileInfo: String
)

/**
 * API 调用成功发送消息的标准化返回。
 *
 * @property id 平台刚分配创建的全新消息 ID。
 * @property timestamp 消息写入服务端的 UTC 时间戳。
 */
@Serializable
data class MessageResult(
    val id: String? = null,
    val timestamp: String? = null
)

/**
 * 机器人在特定群聊内的在线状态统计。
 *
 * @property groupOpenId 所查询群聊的 OpenID。
 * @property state 当前机器人状态编码（详见平台开发准则）。
 */
@Serializable
data class GroupBotState(
    @SerialName("group_openid") val groupOpenId: String? = null,
    val state: Int = 0
)

/**
 * 富媒体文件直接上传请求参数 (POST /v2/users/{user_openid}/files 或 POST /v2/groups/{group_openid}/files)。
 *
 * @property fileType 待上传媒体文件类别：`1` = 图片 (jpg/png), `2` = 视频 (mp4), `3` = 语音 (silk), `4` = 任意普通文件。
 * @property url 公网可达的原始媒体资源 URL，平台服务器将自动远程下载提取此文件。
 * @property srvSendMsg 设为 `true` 时将在完成上传的瞬间直接把文件下发往目标会话中（跳过发消息请求接口）。
 */
@Serializable
data class UploadMediaRequest(
    @SerialName("file_type") val fileType: Int,
    val url: String,
    @SerialName("srv_send_msg") val srvSendMsg: Boolean? = null
)

/**
 * 富媒体文件资源成功向腾讯云端转存得到的凭证应答。
 *
 * @property fileInfo 构造 `msg_type=7` 发送消息请求体所必需的核心媒体凭证文本。
 * @property ttl 当前凭证处于可用状态的剩余存活时长（秒）。
 * @property id 仅当配置 `srv_send_msg=true` 自主下发成功时一并返回的信息 ID。
 */
@Serializable
data class UploadMediaResponse(
    @SerialName("file_info") val fileInfo: String? = null,
    val ttl: Int? = null,
    val id: String? = null
)
