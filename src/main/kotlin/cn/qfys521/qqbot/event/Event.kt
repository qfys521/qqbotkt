package cn.qfys521.qqbot.event

import cn.qfys521.qqbot.dsl.SendMessageRequestBuilder
import cn.qfys521.qqbot.dsl.message
import cn.qfys521.qqbot.http.QQBotApi
import cn.qfys521.qqbot.model.guild.Channel
import cn.qfys521.qqbot.model.guild.Guild
import cn.qfys521.qqbot.model.interaction.Interaction
import cn.qfys521.qqbot.model.interaction.InteractionResponseRequest
import cn.qfys521.qqbot.model.message.Message
import cn.qfys521.qqbot.model.message.MessageMarkdown
import cn.qfys521.qqbot.model.message.MessageResult
import cn.qfys521.qqbot.model.message.SendMessageRequest
import cn.qfys521.qqbot.model.gateway.ReadyData
import kotlinx.serialization.json.JsonElement

/**
 * QQ 机器人 SDK 分发事件的顶层封闭根基类。
 *
 * 当平台将数据帧交付网关并转派到事件分发器之后，所有特定的事件派生实例，
 * 都会在内部自动被注入当前合法的 [api] 请求实例，以便让开发者直接调用事件自身内聚的回发与操作方法。
 *
 * @property eventId 网关下发报文对应的平台事件全局标记 `id`（如果有）。
 * @property timestamp 事件生成的毫秒时间序列（如果有）。
 * @property rawJson 未经反序列化修整剥离的原始 JSON 字符文本字串。
 * @property api OpenAPI [QQBotApi] 客户端。
 */
sealed class BotEvent {
    open val eventId: String? = null
    open val timestamp: String? = null
    open val rawJson: String? = null

    lateinit var api: QQBotApi
        internal set
}

// ==================== 机器人生命周期与连接事件 ====================

/**
 * 成功连入平台 WebSocket 鉴权并获批接入 (`READY`) 生命周期登录就绪事件。
 *
 * @property data 网关直接下达回包中附送的用户基本属性和会话凭据定义体。
 */
class ReadyEvent(
    val data: ReadyData,
    override val eventId: String? = null,
    override val timestamp: String? = null,
    override val rawJson: String? = null
) : BotEvent()

/**
 * 长连断开但通过原有 `sessionId` 及序号无感 `RESUMED` 会话续订成功事件。
 */
class ResumedEvent(
    override val eventId: String? = null,
    override val timestamp: String? = null,
    override val rawJson: String? = null
) : BotEvent()

// ==================== 消息互动类事件 ====================

/**
 * 在 QQ 群组会话中普通的发信（不一定是 @ 机器人）所引起的 (`GROUP_MESSAGE_CREATE`) 群聊事件。
 *
 * @property message 包装好此条包含发言者、内容、群组标示与时间的完整 [Message] 实体对象。
 */
class GroupMessageEvent(
    val message: Message,
    override val eventId: String? = null,
    override val timestamp: String? = null,
    override val rawJson: String? = null
) : BotEvent() {

    /** 发信源头群成员的用户显示昵称快捷属性。 */
    val authorName: String get() = message.author.username
    /** 发信人具体标识 openId。 */
    val authorId: String get() = message.author.openId
    /** 所处群组的识别 ID。 */
    val groupOpenId: String get() = message.groupOpenId ?: ""

    /**
     * 直接在当前群内针对此事件以简明普通文字做出应答。
     *
     * @param content 你想要发送展示给成员的文字。
     * @param msgSeq 业务防重的序号代码，默认自定 `1`。
     */
    suspend fun reply(content: String, msgSeq: Int = 1): MessageResult {
        return api.sendGroupMessage(
            groupOpenId = groupOpenId,
            content = content,
            msgId = message.id,
            msgSeq = msgSeq
        )
    }

    /**
     * 以高度定制的声明式语法在当前群内对事件做出响应。
     *
     * @param block 声明式构建发送配置 [SendMessageRequestBuilder] 作用域函数。
     */
    suspend fun reply(block: SendMessageRequestBuilder.() -> Unit): MessageResult {
        val customRequest = message(block).copy(
            msgId = message.id
        )
        return api.sendGroupMessage(groupOpenId = groupOpenId, request = customRequest)
    }

    /**
     * 撤回本条触发事件的原始群消息（需要拥有管理员撤回权限）。
     */
    suspend fun delete() {
        if (groupOpenId.isNotBlank() && message.id.isNotBlank()) {
            api.deleteGroupMessage(groupOpenId, message.id)
        }
    }
}

/**
 * 在 QQ 群组会话中由于他人对机器人 @ 并发信所引起的 (`GROUP_AT_MESSAGE_CREATE`) 群聊事件。
 *
 * @property message 包装好此条包含发言者、内容、群组标示与时间的完整 [Message] 实体对象。
 */
class GroupAtMessageEvent(
    val message: Message,
    override val eventId: String? = null,
    override val timestamp: String? = null,
    override val rawJson: String? = null
) : BotEvent() {

    /** 发信源头群成员的用户显示昵称快捷属性。 */
    val authorName: String get() = message.author.username
    /** 发信人具体标识 openId。 */
    val authorId: String get() = message.author.openId
    /** 所处群组的识别 ID。 */
    val groupOpenId: String get() = message.groupOpenId ?: ""

    /**
     * 直接在当前群内针对此事件以简明普通文字做出应答。
     *
     * @param content 你想要发送展示给成员的文字。
     * @param msgSeq 业务防重的序号代码，默认自定 `1`。
     * @return 返回调用后平台新消息分配回发收据。
     */
    suspend fun reply(content: String, msgSeq: Int = 1): MessageResult {
        return api.sendGroupMessage(
            groupOpenId = groupOpenId,
            request = SendMessageRequest(
                content = content,
                msgType = 0,
                msgId = message.id,
                msgSeq = msgSeq
            )
        )
    }

    /**
     * 以纯粹的 Markdown 语调排版并向指定群聊发送带有对应上下源引用的被动回发。
     *
     * @param markdownContent 准备发送的原生 Markdown 格式文本。
     * @param msgSeq 序列标识默认 `1`。
     * @return 请求落地后系统给到的收据响应。
     */
    suspend fun replyMarkdown(markdownContent: String, msgSeq: Int = 1): MessageResult {
        return api.sendGroupMessage(
            groupOpenId = groupOpenId,
            request = SendMessageRequest(
                msgType = 2,
                markdown = MessageMarkdown(content = markdownContent),
                msgId = message.id,
                msgSeq = msgSeq
            )
        )
    }

    /**
     * 便携版重载：使用声明式的高阶 DSL (`message { ... }`) 精细制定带有交互按键或附件引用的被动响应。
     *
     * @param block 声明式构建发送配置 [SendMessageRequestBuilder] 作用域函数。
     * @return 发信后平台生成的记录回执。
     */
    suspend fun reply(block: SendMessageRequestBuilder.() -> Unit): MessageResult {
        val customRequest = message {
            msgId = message.id
            msgSeq = 1
            block()
        }
        return api.sendGroupMessage(groupOpenId = groupOpenId, request = customRequest)
    }

    /**
     * 在群内尝试将此条触发本次事件的源消息彻底撤销（需应用获得对应群组的管理特权许可）。
     */
    suspend fun delete() {
        if (groupOpenId.isNotBlank() && message.id.isNotBlank()) {
            api.deleteGroupMessage(groupOpenId, message.id)
        }
    }
}

/**
 * 接收来自特定单个用户针对该应用私信下发的 (`C2C_MESSAGE_CREATE`) 单聊事件载体。
 *
 * @property message 一对一沟通报文映射。
 */
class C2CMessageEvent(
    val message: Message,
    override val eventId: String? = null,
    override val timestamp: String? = null,
    override val rawJson: String? = null
) : BotEvent() {

    /** 发信者昵称快捷指针。 */
    val authorName: String get() = message.author.username
    /** 对方标识 ID。 */
    val authorId: String get() = message.author.openId
    /** 单聊会话绑定的唯一对象 OpenID。 */
    val userOpenId: String get() = message.userOpenId ?: message.author.openId

    /**
     * 极简返回单聊文字应答。
     *
     * @param content 文字内容文本。
     * @param msgSeq 自增序号默认 `1`。
     * @return 消息回送后取得信息实体收据。
     */
    suspend fun reply(content: String, msgSeq: Int = 1): MessageResult {
        return api.sendC2CMessage(
            userOpenId = userOpenId,
            request = SendMessageRequest(
                content = content,
                msgType = 0,
                msgId = message.id,
                msgSeq = msgSeq
            )
        )
    }

    /**
     * 以 Markdown 为样式的格式向对方进行一对一被动回复。
     *
     * @param markdownContent Markdown 语法文段。
     * @param msgSeq 自增序号默认 `1`。
     * @return 服务端签收响应。
     */
    suspend fun replyMarkdown(markdownContent: String, msgSeq: Int = 1): MessageResult {
        return api.sendC2CMessage(
            userOpenId = userOpenId,
            request = SendMessageRequest(
                msgType = 2,
                markdown = MessageMarkdown(content = markdownContent),
                msgId = message.id,
                msgSeq = msgSeq
            )
        )
    }

    /**
     * 使用声明式 DSL 构建任意带按钮、卡片、引用的被动单聊回信。
     *
     * @param block 声明式构建规则函数体。
     * @return 交互成功结果。
     */
    suspend fun reply(block: SendMessageRequestBuilder.() -> Unit): MessageResult {
        val customRequest = message {
            msgId = message.id
            msgSeq = 1
            block()
        }
        return api.sendC2CMessage(userOpenId = userOpenId, request = customRequest)
    }

    /**
     * 针对该具体私有单聊记录发布消息删除撤销命令。
     */
    suspend fun delete() {
        if (userOpenId.isNotBlank() && message.id.isNotBlank()) {
            api.deleteC2CMessage(userOpenId, message.id)
        }
    }
}

/**
 * 频道全域范围所有消息触发下的 (`MESSAGE_CREATE`) 事件监听模型（专属特权机器人应用选用）。
 *
 * @property message 频道消息详情。
 */
class GuildMessageEvent(
    val message: Message,
    override val eventId: String? = null,
    override val timestamp: String? = null,
    override val rawJson: String? = null
) : BotEvent()

/**
 * 特定公共频道内所收到的 @ 此机器人的专属消息 (`AT_MESSAGE_CREATE`) 事件。
 *
 * @property message 包含该 @ 记录的详细内容模型。
 */
class GuildAtMessageEvent(
    val message: Message,
    override val eventId: String? = null,
    override val timestamp: String? = null,
    override val rawJson: String? = null
) : BotEvent()

/**
 * 自频道发起的与应用之间的私信消息 (`DIRECT_MESSAGE_CREATE`) 交互。
 *
 * @property message 详情实体。
 */
class DirectMessageEvent(
    val message: Message,
    override val eventId: String? = null,
    override val timestamp: String? = null,
    override val rawJson: String? = null
) : BotEvent()

// ==================== 互动响应类事件 ====================

/**
 * 页面卡片或内嵌交互操作组件触发产生的动作 (`INTERACTION_CREATE`) 回调实体事件。
 *
 * @property interaction 记录来源应用 ID、触发者、场景与被点击具体值定义的完整描述对象。
 */
class InteractionCreateEvent(
    val interaction: Interaction,
    override val eventId: String? = null,
    override val timestamp: String? = null,
    override val rawJson: String? = null
) : BotEvent() {

    /**
     * 对该次具体的平台界面按压行为实施合规应答（在特定阈值毫秒中务必回覆防止按钮出现客户端报错样式）。
     *
     * @param code 状态提示返回状态：成功一般设置默认为 `0`。
     */
    suspend fun respond(code: Int = 0) {
        if (interaction.id.isNotBlank()) {
            api.putInteractionResponse(interaction.id, InteractionResponseRequest(code = code))
        }
    }
}

// ==================== 频道与子频道基础事件 ====================

/**
 * 机器人获准加入一条全新大频道社区时的通知广播事件 (`GUILD_CREATE`)。
 *
 * @property guild 该新涉足大频道的完整组织与人员信息。
 */
class GuildCreateEvent(
    val guild: Guild,
    override val eventId: String? = null,
    override val timestamp: String? = null,
    override val rawJson: String? = null
) : BotEvent()

/**
 * 某一已参加之频道基本背景或描述内容更改时的变化同步事件 (`GUILD_UPDATE`)。
 *
 * @property guild 频道更改后的最新元数据。
 */
class GuildUpdateEvent(
    val guild: Guild,
    override val eventId: String? = null,
    override val timestamp: String? = null,
    override val rawJson: String? = null
) : BotEvent()

/**
 * 机器人退出或所在社区注销解散后分发的移除宣告事件 (`GUILD_DELETE`)。
 *
 * @property guild 被解约废止的大频道 ID 或残留对象。
 */
class GuildDeleteEvent(
    val guild: Guild,
    override val eventId: String? = null,
    override val timestamp: String? = null,
    override val rawJson: String? = null
) : BotEvent()

/**
 * 在某一隶属频道之下列为新增子频道项目时的创建宣告事件 (`CHANNEL_CREATE`)。
 *
 * @property channel 该刚建立子频道的详情实体。
 */
class ChannelCreateEvent(
    val channel: Channel,
    override val eventId: String? = null,
    override val timestamp: String? = null,
    override val rawJson: String? = null
) : BotEvent()

/**
 * 在某特定子频道经过名称、顺序或类别重整配置后的更新宣告事件 (`CHANNEL_UPDATE`)。
 *
 * @property channel 该频道变更为最新状态的信息表示。
 */
class ChannelUpdateEvent(
    val channel: Channel,
    override val eventId: String? = null,
    override val timestamp: String? = null,
    override val rawJson: String? = null
) : BotEvent()

/**
 * 某条子频道遭到从大社区节点删除后的告警清理事件 (`CHANNEL_DELETE`)。
 *
 * @property channel 原有即将注销的子频道简讯。
 */
class ChannelDeleteEvent(
    val channel: Channel,
    override val eventId: String? = null,
    override val timestamp: String? = null,
    override val rawJson: String? = null
) : BotEvent()

// ==================== 通用/后备事件 ====================

/**
 * 代表任何其余或面向未来的未知及新增定制平台推送下发数据的兜底包装类事件。
 *
 * @property eventType 实际收到的原始平台事件分类名字符串字样（类似 `"MESSAGE_AUDIT_PASS"` 等）。
 * @property payload 没有做特定强范式转换的有效下行参数树 `d` 的原始 JSON 元节点对象。
 */
class GenericEvent(
    val eventType: String,
    val payload: JsonElement?,
    override val eventId: String? = null,
    override val timestamp: String? = null,
    override val rawJson: String? = null
) : BotEvent()
