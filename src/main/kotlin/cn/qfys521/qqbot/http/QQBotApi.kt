package cn.qfys521.qqbot.http

import cn.qfys521.qqbot.model.api.*
import cn.qfys521.qqbot.model.guild.Channel
import cn.qfys521.qqbot.model.guild.CreateChannelRequest
import cn.qfys521.qqbot.model.guild.Guild
import cn.qfys521.qqbot.model.guild.UpdateChannelRequest
import cn.qfys521.qqbot.model.interaction.InteractionResponseRequest
import cn.qfys521.qqbot.model.message.GroupBotState
import cn.qfys521.qqbot.model.message.MessageResult
import cn.qfys521.qqbot.model.message.SendMessageRequest
import cn.qfys521.qqbot.model.message.UploadMediaRequest
import cn.qfys521.qqbot.model.message.UploadMediaResponse
import cn.qfys521.qqbot.model.user.GuildItem
import cn.qfys521.qqbot.model.user.UserMe
import cn.qfys521.qqbot.model.gateway.WssUrlResponse
import kotlinx.serialization.json.JsonElement

/**
 * QQ 机器人 OpenAPI v2 高阶操作接口抽象门面。
 * 覆盖基础状态自查、单聊私信及群聊消息发送撤回、多媒体文件上传转存、
 * 频道与子频道配置及交互管理、互动回调确认和长连网关获取。
 */
interface QQBotApi {

    // ==================== 机器人自身信息 ====================

    /**
     * 查询机器人自身公开信息。
     *
     * 对应 HTTP 接口：`GET /users/@me`
     *
     * @return 当前认证账号对应机器人的全量基本属性。
     */
    suspend fun getMe(): UserMe

    /**
     * 查询当前机器人已经获准加入的频道社区列表。
     *
     * 对应 HTTP 接口：`GET /users/@me/guilds`
     *
     * @return 频道简略描述条目合集。
     */
    suspend fun getMyGuilds(): List<GuildItem>

    // ==================== 单聊消息 (C2C) ====================

    /**
     * 向目标用户的私聊界面主动发信或被动回信。
     *
     * 对应 HTTP 接口：`POST /v2/users/{user_openid}/messages`
     *
     * @param userOpenId 目标单聊对象的唯一标识 (`user_openid`)。
     * @param request 准备下发的完整发信结构（如纯文本、Markdown、交互按钮等）。
     * @return 发送响应回包，附带消息分配的 ID 及系统时间戳。
     */
    suspend fun sendC2CMessage(userOpenId: String, request: SendMessageRequest): MessageResult

    /**
     * 便携版重载：直接向目标用户单聊下发普通文本字串。
     *
     * @param userOpenId 目标用户 OpenID。
     * @param content 待展示文字文本字串。
     * @param msgId 如果是响应回复对应的那条源消息 ID，非必填。
     * @param msgSeq 发信防重发唯一自增序号，默认为 `null` (SDK 自定为 `1`)。
     * @return 消息投递执行反馈。
     */
    suspend fun sendC2CMessage(
        userOpenId: String,
        content: String,
        msgId: String? = null,
        msgSeq: Int? = null
    ): MessageResult = sendC2CMessage(
        userOpenId,
        SendMessageRequest(
            content = content,
            msgType = 0,
            msgId = msgId,
            msgSeq = msgSeq
        )
    )

    /**
     * 撤回指定的单聊消息。
     *
     * 对应 HTTP 接口：`DELETE /v2/users/{user_openid}/messages/{message_id}`
     *
     * @param userOpenId 目标会话对象的 OpenID。
     * @param messageId 需要撤销的具体消息 ID。
     */
    suspend fun deleteC2CMessage(userOpenId: String, messageId: String)

    /**
     * 采用类似打字机或流式分块协议，向目标用户会话持续发送流式消息。
     *
     * 对应 HTTP 接口：`POST /v2/users/{user_openid}/stream_messages`
     *
     * @param userOpenId 目标用户会话 OpenID。
     * @param request 准备投递流式的特定消息体。
     * @return 消息分发任务应答。
     */
    suspend fun sendC2CStreamMessage(userOpenId: String, request: SendMessageRequest): MessageResult

    /**
     * 在单聊场景中上传媒体文件（根据 URL 下载或预签转存）并换回发消息所需的 [UploadMediaResponse]。
     *
     * 对应 HTTP 接口：`POST /v2/users/{user_openid}/files`
     *
     * @param userOpenId 对应私聊会话关联用户 OpenID。
     * @param request 上传媒体资源的请求规范。
     * @return 包含有效期限及关键凭证字串 [UploadMediaResponse.fileInfo] 的响应。
     */
    suspend fun uploadC2CMedia(userOpenId: String, request: UploadMediaRequest): UploadMediaResponse

    // ==================== 群聊消息 (Group) ====================

    /**
     * 针对指定群聊推送公开文本、卡片、交互键盘及引用回帖。
     *
     * 对应 HTTP 接口：`POST /v2/groups/{group_openid}/messages`
     *
     * @param groupOpenId 待下发的 QQ 群聊标识。
     * @param request 发送具体报文。
     * @return 附随的新增群聊天记录应答详情。
     */
    suspend fun sendGroupMessage(groupOpenId: String, request: SendMessageRequest): MessageResult

    /**
     * 便携版重载：以精炼的文本字串直接对 QQ 群聊会话发布文字消息或直接答复被动会话。
     *
     * @param groupOpenId 目标群 OpenID。
     * @param content 文字内容。
     * @param msgId 引用的事件源头消息 ID。
     * @param msgSeq 为防止群组吞信息或重复的防重校验字。
     * @return 服务端生成的发信收据。
     */
    suspend fun sendGroupMessage(
        groupOpenId: String,
        content: String,
        msgId: String? = null,
        msgSeq: Int? = null
    ): MessageResult = sendGroupMessage(
        groupOpenId,
        SendMessageRequest(
            content = content,
            msgType = 0,
            msgId = msgId,
            msgSeq = msgSeq
        )
    )

    /**
     * 从群聊界面主动撤销已发的消息。
     *
     * 对应 HTTP 接口：`DELETE /v2/groups/{group_openid}/messages/{message_id}`
     *
     * @param groupOpenId 群聊的标示 ID。
     * @param messageId 必须撤回的原纪录 ID。
     */
    suspend fun deleteGroupMessage(groupOpenId: String, messageId: String)

    /**
     * 查询并解析特定群聊的简易公共资料。
     *
     * 对应 HTTP 接口：`GET /v2/groups/{group_openid}/info`
     *
     * @param groupOpenId 目标群标识。
     * @return 返回含该群的简明属性描述 JSON 对象。
     */
    suspend fun getGroupInfo(groupOpenId: String): JsonElement

    /**
     * 探查并确认自身在此群中被设定的管理与互动许可状态（包括频率管控与发言特权）。
     *
     * 对应 HTTP 接口：`GET /v2/groups/{group_openid}/bot_state`
     *
     * @param groupOpenId 群会话 OpenID。
     * @return 机器人群状态描述快照。
     */
    suspend fun getGroupBotState(groupOpenId: String): GroupBotState

    /**
     * 为给定的群聊会话快速转存外部网路文件并取得有效发消息富媒体令牌。
     *
     * 对应 HTTP 接口：`POST /v2/groups/{group_openid}/files`
     *
     * @param groupOpenId 群聊 ID 凭证。
     * @param request 指定待提取并托管的网络 URL 与媒体分类。
     * @return 换得 `file_info` 文件令牌的结构。
     */
    suspend fun uploadGroupMedia(groupOpenId: String, request: UploadMediaRequest): UploadMediaResponse

    // ==================== 群管理与扩展能力 ====================

    suspend fun generateShareLink(request: ShareLinkRequest = ShareLinkRequest()): ShareLinkResponse

    suspend fun getGroupMembers(groupOpenId: String, cursor: String? = null): GroupMemberPage
    suspend fun getGroupMember(groupOpenId: String, memberOpenId: String): GroupMemberDetail
    suspend fun getGroupBlacklist(groupOpenId: String, cursor: String? = null, limit: Int? = null): BlacklistPage
    suspend fun updateGroupBlacklist(groupOpenId: String, request: BlacklistOperationRequest): BlacklistOperationResponse
    suspend fun getRestrictChatSetting(groupOpenId: String): RestrictChatSetting
    suspend fun updateRestrictChatSetting(groupOpenId: String, request: RestrictChatSettingRequest)
    suspend fun getJoinRequests(groupOpenId: String, cursor: String? = null, limit: Int? = null): JoinRequestPage
    suspend fun approveJoinRequest(groupOpenId: String, memberOpenId: String, request: ApproveJoinRequest)
    suspend fun batchRemoveMembers(groupOpenId: String, request: BatchRemoveMembersRequest): BatchRemoveMembersResponse

    suspend fun getJoinApprovalStrategies(cursor: String? = null, limit: Int? = null): JoinApprovalStrategyPage
    suspend fun createJoinApprovalStrategy(request: CreateJoinApprovalStrategyRequest): CreateJoinApprovalStrategyResponse
    suspend fun updateJoinApprovalStrategy(strategyId: String, request: UpdateJoinApprovalStrategyRequest): UpdateJoinApprovalStrategyResponse
    suspend fun deleteJoinApprovalStrategy(strategyId: String)
    suspend fun executeJoinApprovalStrategy(strategyId: String)
    suspend fun updateJoinApprovalWhitelist(strategyId: String, request: WhitelistUsersRequest): WhitelistUsersResponse

    suspend fun getMenu(): MenuResponse
    suspend fun updateMenu(request: MenuRequest): MenuVersion
    suspend fun getPanels(scope: String, cursor: String? = null, limit: Int? = null): PanelPage
    suspend fun createPanel(request: CreatePanelRequest): PanelId
    suspend fun getPanel(panelId: String): PanelRecord
    suspend fun updatePanel(panelId: String, request: UpdatePanelRequest): MenuVersion
    suspend fun updatePanelTargets(panelId: String, request: PanelTargetRequest)
    suspend fun deletePanel(panelId: String)

    suspend fun prepareGroupUpload(groupOpenId: String, request: UploadPrepareRequest): UploadPrepareResponse
    suspend fun finishGroupUpload(groupOpenId: String, request: UploadPartFinishRequest)
    suspend fun prepareC2CUpload(userOpenId: String, request: UploadPrepareRequest): UploadPrepareResponse
    suspend fun finishC2CUpload(userOpenId: String, request: UploadPartFinishRequest)

    // ==================== 频道与子频道 (Guild & Channel) ====================

    /**
     * 读取特定频道的架构等详情信息。
     *
     * 对应 HTTP 接口：`GET /guilds/{guild_id}`
     *
     * @param guildId 被查询对象频道标识 ID。
     * @return [Guild] 频道全属性映射对象。
     */
    suspend fun getGuild(guildId: String): Guild

    /**
     * 遍历并查询某频道下的所有子频道清单。
     *
     * 对应 HTTP 接口：`GET /guilds/{guild_id}/channels`
     *
     * @param guildId 目标主频道的 ID。
     * @return [Channel] 该频道拥有的各个分类、文本与语音子频道列表。
     */
    suspend fun getChannels(guildId: String): List<Channel>

    /**
     * 获取某一特定子频道的具体设置与属性结构。
     *
     * 对应 HTTP 接口：`GET /channels/{channel_id}`
     *
     * @param channelId 被查询的子频道 ID。
     * @return 有关该子频道具体信息的模型对象。
     */
    suspend fun getChannel(channelId: String): Channel

    /**
     * 在目标大频道内部新增并建立一条新的子频道（需要明确配置名称与类型）。
     *
     * 对应 HTTP 接口：`POST /guilds/{guild_id}/channels`
     *
     * @param guildId 主频道的标识。
     * @param request 建立子频道的规格参数要求声明。
     * @return 服务端创建成功后分配的完整子频道信息体。
     */
    suspend fun createChannel(guildId: String, request: CreateChannelRequest): Channel

    /**
     * 调整、改名或对既有的子频道做特定属性覆盖修饰。
     *
     * 对应 HTTP 接口：`PATCH /channels/{channel_id}`
     *
     * @param channelId 要改动更新的目标子频道 ID。
     * @param request 待覆盖应用的变更内容声明。
     * @return 更新落地完毕后最新的 [Channel] 数据实体。
     */
    suspend fun updateChannel(channelId: String, request: UpdateChannelRequest): Channel

    /**
     * 删除指定的子频道会话。
     *
     * 对应 HTTP 接口：`DELETE /channels/{channel_id}`
     *
     * @param channelId 欲移除处理的目标频道标识。
     */
    suspend fun deleteChannel(channelId: String)

    // ==================== 互动事件应答 (Interaction) ====================

    /**
     * 在接到用户与页面按钮或指令交互的 `INTERACTION_CREATE` 事件后，将反馈应答送往此网关路径。
     *
     * 对应 HTTP 接口：`PUT /interactions/{interaction_id}`
     *
     * @param interactionId 上传派发中下发的交互事件 `id`。
     * @param request 应答附随的返回码与会话召回标签（默认为成功的 `code=0`）。
     */
    suspend fun putInteractionResponse(
        interactionId: String,
        request: InteractionResponseRequest = InteractionResponseRequest(code = 0)
    )

    // ==================== WebSocket 网关接入点 ====================

    /**
     * 查询适用于当前通用会话与鉴权策略的基础 WebSocket 接入地址。
     *
     * 对应 HTTP 接口：`GET /gateway`
     *
     * @return WSS 连接信息基础说明体。
     */
    suspend fun getWssUrl(): WssUrlResponse

    /**
     * 检索面向该具体机器人的专用长连接网关地址，并且能够一并获得关于此账号并发数量限制和推荐分片规模的指标。
     *
     * 对应 HTTP 接口：`GET /gateway/bot`
     *
     * @return 承载有效连接 URL 与并发配额约束说明数据返回。
     */
    suspend fun getWssBotUrl(): WssUrlResponse
}
