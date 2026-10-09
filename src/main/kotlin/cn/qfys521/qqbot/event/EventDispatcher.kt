package cn.qfys521.qqbot.event

import cn.qfys521.qqbot.http.QQBotApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 机器人事件全功能调度与协程派发引擎 (EventDispatcher)。
 *
 * 负责统一收集处理由底层 WebSocket 网关反序列化生成的各类实时业务事件 [BotEvent]，
 * 并安全匹配类型并发布调起已注册的相关具体协程监听函数。
 *
 * 内置安全屏障与异常隔离机制：
 * 任何侦听器函数因逻辑错误抛出的异常将被全局安全截断，且打印于日志中，确保不会引发网关主循环崩溃挂掉。
 *
 * @property api 当派发事件时，该注入用于给各个事件载体传入操作引用指针的 OpenAPI [QQBotApi] 对象。
 */
class EventDispatcher(
    internal var api: QQBotApi? = null
) {
    private val logger = LoggerFactory.getLogger(EventDispatcher::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val readyListeners = CopyOnWriteArrayList<suspend (ReadyEvent) -> Unit>()
    private val resumedListeners = CopyOnWriteArrayList<suspend (ResumedEvent) -> Unit>()
    private val groupMessageListeners = CopyOnWriteArrayList<suspend (GroupMessageEvent) -> Unit>()
    private val groupAtMessageListeners = CopyOnWriteArrayList<suspend (GroupAtMessageEvent) -> Unit>()
    private val c2cMessageListeners = CopyOnWriteArrayList<suspend (C2CMessageEvent) -> Unit>()
    private val groupJoinRequestListeners = CopyOnWriteArrayList<suspend (GroupJoinRequestEvent) -> Unit>()
    private val groupMemberAddListeners = CopyOnWriteArrayList<suspend (GroupMemberAddEvent) -> Unit>()
    private val groupMemberRemoveListeners = CopyOnWriteArrayList<suspend (GroupMemberRemoveEvent) -> Unit>()
    private val friendAddListeners = CopyOnWriteArrayList<suspend (FriendAddEvent) -> Unit>()
    private val friendDelListeners = CopyOnWriteArrayList<suspend (FriendDelEvent) -> Unit>()
    private val groupMsgReceiveListeners = CopyOnWriteArrayList<suspend (GroupMsgReceiveEvent) -> Unit>()
    private val groupMsgRejectListeners = CopyOnWriteArrayList<suspend (GroupMsgRejectEvent) -> Unit>()
    private val c2cMsgReceiveListeners = CopyOnWriteArrayList<suspend (C2CMsgReceiveEvent) -> Unit>()
    private val c2cMsgRejectListeners = CopyOnWriteArrayList<suspend (C2CMsgRejectEvent) -> Unit>()
    private val messageDeleteListeners = CopyOnWriteArrayList<suspend (MessageDeleteEvent) -> Unit>()
    private val publicMessageDeleteListeners = CopyOnWriteArrayList<suspend (PublicMessageDeleteEvent) -> Unit>()
    private val directMessageDeleteListeners = CopyOnWriteArrayList<suspend (DirectMessageDeleteEvent) -> Unit>()
    private val guildMemberAddListeners = CopyOnWriteArrayList<suspend (GuildMemberAddEvent) -> Unit>()
    private val guildMemberUpdateListeners = CopyOnWriteArrayList<suspend (GuildMemberUpdateEvent) -> Unit>()
    private val guildMemberRemoveListeners = CopyOnWriteArrayList<suspend (GuildMemberRemoveEvent) -> Unit>()
    private val reactionAddListeners = CopyOnWriteArrayList<suspend (MessageReactionAddEvent) -> Unit>()
    private val reactionRemoveListeners = CopyOnWriteArrayList<suspend (MessageReactionRemoveEvent) -> Unit>()
    private val auditPassListeners = CopyOnWriteArrayList<suspend (MessageAuditPassEvent) -> Unit>()
    private val auditRejectListeners = CopyOnWriteArrayList<suspend (MessageAuditRejectEvent) -> Unit>()
    private val forumThreadCreateListeners = CopyOnWriteArrayList<suspend (ForumThreadCreateEvent) -> Unit>()
    private val forumThreadUpdateListeners = CopyOnWriteArrayList<suspend (ForumThreadUpdateEvent) -> Unit>()
    private val forumThreadDeleteListeners = CopyOnWriteArrayList<suspend (ForumThreadDeleteEvent) -> Unit>()
    private val forumPostCreateListeners = CopyOnWriteArrayList<suspend (ForumPostCreateEvent) -> Unit>()
    private val forumPostDeleteListeners = CopyOnWriteArrayList<suspend (ForumPostDeleteEvent) -> Unit>()
    private val forumReplyCreateListeners = CopyOnWriteArrayList<suspend (ForumReplyCreateEvent) -> Unit>()
    private val forumReplyDeleteListeners = CopyOnWriteArrayList<suspend (ForumReplyDeleteEvent) -> Unit>()
    private val forumAuditListeners = CopyOnWriteArrayList<suspend (ForumPublishAuditResultEvent) -> Unit>()
    private val groupAddRobotListeners = CopyOnWriteArrayList<suspend (GroupAddRobotEvent) -> Unit>()
    private val groupDelRobotListeners = CopyOnWriteArrayList<suspend (GroupDelRobotEvent) -> Unit>()
    private val audioMemberEnterListeners = CopyOnWriteArrayList<suspend (AudioOrLiveChannelMemberEnterEvent) -> Unit>()
    private val audioMemberExitListeners = CopyOnWriteArrayList<suspend (AudioOrLiveChannelMemberExitEvent) -> Unit>()
    private val audioStartListeners = CopyOnWriteArrayList<suspend (AudioStartEvent) -> Unit>()
    private val audioFinishListeners = CopyOnWriteArrayList<suspend (AudioFinishEvent) -> Unit>()
    private val audioOnMicListeners = CopyOnWriteArrayList<suspend (AudioOnMicEvent) -> Unit>()
    private val audioOffMicListeners = CopyOnWriteArrayList<suspend (AudioOffMicEvent) -> Unit>()
    private val guildAtMessageListeners = CopyOnWriteArrayList<suspend (GuildAtMessageEvent) -> Unit>()
    private val guildMessageListeners = CopyOnWriteArrayList<suspend (GuildMessageEvent) -> Unit>()
    private val directMessageListeners = CopyOnWriteArrayList<suspend (DirectMessageEvent) -> Unit>()
    private val interactionListeners = CopyOnWriteArrayList<suspend (InteractionCreateEvent) -> Unit>()
    private val guildCreateListeners = CopyOnWriteArrayList<suspend (GuildCreateEvent) -> Unit>()
    private val guildUpdateListeners = CopyOnWriteArrayList<suspend (GuildUpdateEvent) -> Unit>()
    private val guildDeleteListeners = CopyOnWriteArrayList<suspend (GuildDeleteEvent) -> Unit>()
    private val channelCreateListeners = CopyOnWriteArrayList<suspend (ChannelCreateEvent) -> Unit>()
    private val channelUpdateListeners = CopyOnWriteArrayList<suspend (ChannelUpdateEvent) -> Unit>()
    private val channelDeleteListeners = CopyOnWriteArrayList<suspend (ChannelDeleteEvent) -> Unit>()
    private val genericListeners = CopyOnWriteArrayList<suspend (GenericEvent) -> Unit>()
    private val anyEventListeners = CopyOnWriteArrayList<suspend (BotEvent) -> Unit>()

    // ==================== DSL 侦听器注册方法 ====================

    /**
     * 注册面向 `READY`（长连连入完成鉴权登录成功）事件的侦听回调。
     *
     * @param listener 具备 `suspend` 协程能力的处理函数体。
     */
    fun onReady(listener: suspend (ReadyEvent) -> Unit) {
        readyListeners.add(listener)
    }

    /**
     * 注册面向 `RESUMED`（长连接临时中断后成功使用 SessionId 进行无缝会话恢复）事件的侦听回调。
     *
     * @param listener 协程监听函数体。
     */
    fun onResumed(listener: suspend (ResumedEvent) -> Unit) {
        resumedListeners.add(listener)
    }
    /**
     * 注册面向 QQ 群组里他人发信（包括无 @ 消息） (`GROUP_MESSAGE_CREATE`) 的专属回调。
     *
     * @param listener 协程监听函数体。
     */
    fun onGroupMessage(listener: suspend (GroupMessageEvent) -> Unit) {
        groupMessageListeners.add(listener)
    }

    /**
     * 注册面向 QQ 群组里他人发信艾特 @ 此机器人 (`GROUP_AT_MESSAGE_CREATE`) 的专属回调。
     *
     * @param listener 协程监听函数体。
     */
    fun onGroupAtMessage(listener: suspend (GroupAtMessageEvent) -> Unit) {
        groupAtMessageListeners.add(listener)
    }

    /**
     * 注册面向用户从一对一私人界面投寄单聊会话 (`C2C_MESSAGE_CREATE`) 的专席监听。
     *
     * @param listener 协程监听函数体。
     */
    fun onC2CMessage(listener: suspend (C2CMessageEvent) -> Unit) {
        c2cMessageListeners.add(listener)
    }

    /** 注册用户申请加入群聊 (`GROUP_JOIN_REQUEST`) 事件。 */
    fun onGroupJoinRequest(listener: suspend (GroupJoinRequestEvent) -> Unit) {
        groupJoinRequestListeners.add(listener)
    }

    /** 注册群成员加入 (`GROUP_MEMBER_ADD`) 事件。 */
    fun onGroupMemberAdd(listener: suspend (GroupMemberAddEvent) -> Unit) {
        groupMemberAddListeners.add(listener)
    }

    /** 注册群成员退出或被移出 (`GROUP_MEMBER_REMOVE`) 事件。 */
    fun onGroupMemberRemove(listener: suspend (GroupMemberRemoveEvent) -> Unit) {
        groupMemberRemoveListeners.add(listener)
    }

    /** 注册用户添加好友 (`FRIEND_ADD`) 事件。 */
    fun onFriendAdd(listener: suspend (FriendAddEvent) -> Unit) {
        friendAddListeners.add(listener)
    }

    /** 注册用户删除好友 (`FRIEND_DEL`) 事件。 */
    fun onFriendDel(listener: suspend (FriendDelEvent) -> Unit) {
        friendDelListeners.add(listener)
    }

    /** 注册开启群聊消息接收 (`GROUP_MSG_RECEIVE`) 事件。 */
    fun onGroupMsgReceive(listener: suspend (GroupMsgReceiveEvent) -> Unit) {
        groupMsgReceiveListeners.add(listener)
    }

    /** 注册关闭群聊消息接收 (`GROUP_MSG_REJECT`) 事件。 */
    fun onGroupMsgReject(listener: suspend (GroupMsgRejectEvent) -> Unit) {
        groupMsgRejectListeners.add(listener)
    }

    /** 注册开启单聊主动消息接收 (`C2C_MSG_RECEIVE`) 事件。 */
    fun onC2CMsgReceive(listener: suspend (C2CMsgReceiveEvent) -> Unit) {
        c2cMsgReceiveListeners.add(listener)
    }

    /** 注册关闭单聊主动消息接收 (`C2C_MSG_REJECT`) 事件。 */
    fun onC2CMsgReject(listener: suspend (C2CMsgRejectEvent) -> Unit) {
        c2cMsgRejectListeners.add(listener)
    }

    fun onMessageDelete(listener: suspend (MessageDeleteEvent) -> Unit) {
        messageDeleteListeners.add(listener)
    }

    fun onPublicMessageDelete(listener: suspend (PublicMessageDeleteEvent) -> Unit) {
        publicMessageDeleteListeners.add(listener)
    }

    fun onDirectMessageDelete(listener: suspend (DirectMessageDeleteEvent) -> Unit) {
        directMessageDeleteListeners.add(listener)
    }

    fun onGuildMemberAdd(listener: suspend (GuildMemberAddEvent) -> Unit) {
        guildMemberAddListeners.add(listener)
    }

    fun onGuildMemberUpdate(listener: suspend (GuildMemberUpdateEvent) -> Unit) {
        guildMemberUpdateListeners.add(listener)
    }

    fun onGuildMemberRemove(listener: suspend (GuildMemberRemoveEvent) -> Unit) {
        guildMemberRemoveListeners.add(listener)
    }

    fun onMessageReactionAdd(listener: suspend (MessageReactionAddEvent) -> Unit) {
        reactionAddListeners.add(listener)
    }

    fun onMessageReactionRemove(listener: suspend (MessageReactionRemoveEvent) -> Unit) {
        reactionRemoveListeners.add(listener)
    }

    fun onMessageAuditPass(listener: suspend (MessageAuditPassEvent) -> Unit) {
        auditPassListeners.add(listener)
    }

    fun onMessageAuditReject(listener: suspend (MessageAuditRejectEvent) -> Unit) {
        auditRejectListeners.add(listener)
    }

    fun onForumThreadCreate(listener: suspend (ForumThreadCreateEvent) -> Unit) {
        forumThreadCreateListeners.add(listener)
    }

    fun onForumThreadUpdate(listener: suspend (ForumThreadUpdateEvent) -> Unit) {
        forumThreadUpdateListeners.add(listener)
    }

    fun onForumThreadDelete(listener: suspend (ForumThreadDeleteEvent) -> Unit) {
        forumThreadDeleteListeners.add(listener)
    }

    fun onForumPostCreate(listener: suspend (ForumPostCreateEvent) -> Unit) {
        forumPostCreateListeners.add(listener)
    }

    fun onForumPostDelete(listener: suspend (ForumPostDeleteEvent) -> Unit) {
        forumPostDeleteListeners.add(listener)
    }

    fun onForumReplyCreate(listener: suspend (ForumReplyCreateEvent) -> Unit) {
        forumReplyCreateListeners.add(listener)
    }

    fun onForumReplyDelete(listener: suspend (ForumReplyDeleteEvent) -> Unit) {
        forumReplyDeleteListeners.add(listener)
    }

    fun onForumPublishAuditResult(listener: suspend (ForumPublishAuditResultEvent) -> Unit) {
        forumAuditListeners.add(listener)
    }

    fun onGroupAddRobot(listener: suspend (GroupAddRobotEvent) -> Unit) {
        groupAddRobotListeners.add(listener)
    }

    fun onGroupDelRobot(listener: suspend (GroupDelRobotEvent) -> Unit) {
        groupDelRobotListeners.add(listener)
    }

    fun onAudioOrLiveChannelMemberEnter(listener: suspend (AudioOrLiveChannelMemberEnterEvent) -> Unit) {
        audioMemberEnterListeners.add(listener)
    }

    fun onAudioOrLiveChannelMemberExit(listener: suspend (AudioOrLiveChannelMemberExitEvent) -> Unit) {
        audioMemberExitListeners.add(listener)
    }

    fun onAudioStart(listener: suspend (AudioStartEvent) -> Unit) {
        audioStartListeners.add(listener)
    }

    fun onAudioFinish(listener: suspend (AudioFinishEvent) -> Unit) {
        audioFinishListeners.add(listener)
    }

    fun onAudioOnMic(listener: suspend (AudioOnMicEvent) -> Unit) {
        audioOnMicListeners.add(listener)
    }

    fun onAudioOffMic(listener: suspend (AudioOffMicEvent) -> Unit) {
        audioOffMicListeners.add(listener)
    }

    /**
     * 注册针对特定公域频道的 @ 消息 (`AT_MESSAGE_CREATE`) 回调函数。
     *
     * @param listener 协程监听函数体。
     */
    fun onGuildAtMessage(listener: suspend (GuildAtMessageEvent) -> Unit) {
        guildAtMessageListeners.add(listener)
    }

    /**
     * 注册接收所加频道全部消息流量事件 (`MESSAGE_CREATE`) 回调（平台规范限制私有独裁特权应用可接）。
     *
     * @param listener 协程监听函数体。
     */
    fun onGuildMessage(listener: suspend (GuildMessageEvent) -> Unit) {
        guildMessageListeners.add(listener)
    }

    /**
     * 注册处理在频道社区中向应用发起私人私聊 (`DIRECT_MESSAGE_CREATE`) 的事件侦听。
     *
     * @param listener 协程监听函数体。
     */
    fun onDirectMessage(listener: suspend (DirectMessageEvent) -> Unit) {
        directMessageListeners.add(listener)
    }

    /**
     * 注册捕捉按钮被击打点击、或菜单被触发下发回调等 (`INTERACTION_CREATE`) 的交互动作监听。
     *
     * @param listener 协程监听函数体。
     */
    fun onInteractionCreate(listener: suspend (InteractionCreateEvent) -> Unit) {
        interactionListeners.add(listener)
    }

    /**
     * 注册当应用被拉入某全新频道社区或频道被新建时的 (`GUILD_CREATE`) 回调。
     *
     * @param listener 协程监听函数体。
     */
    fun onGuildCreate(listener: suspend (GuildCreateEvent) -> Unit) {
        guildCreateListeners.add(listener)
    }

    /**
     * 注册监听当自身已加入之频道配置或信息改写时的 (`GUILD_UPDATE`) 回调。
     *
     * @param listener 协程监听函数体。
     */
    fun onGuildUpdate(listener: suspend (GuildUpdateEvent) -> Unit) {
        guildUpdateListeners.add(listener)
    }

    /**
     * 注册监听应用自身退群、或者主频道遭到整体撤消解散后的 (`GUILD_DELETE`) 回调。
     *
     * @param listener 协程监听函数体。
     */
    fun onGuildDelete(listener: suspend (GuildDeleteEvent) -> Unit) {
        guildDeleteListeners.add(listener)
    }

    /**
     * 注册响应某一频道之下建构了新子频道时的 (`CHANNEL_CREATE`) 监听函数。
     *
     * @param listener 协程监听函数体。
     */
    fun onChannelCreate(listener: suspend (ChannelCreateEvent) -> Unit) {
        channelCreateListeners.add(listener)
    }

    /**
     * 注册响应已有某特定子频道发生分类改名改动之 (`CHANNEL_UPDATE`) 监听。
     *
     * @param listener 协程监听函数体。
     */
    fun onChannelUpdate(listener: suspend (ChannelUpdateEvent) -> Unit) {
        channelUpdateListeners.add(listener)
    }

    /**
     * 注册当某条子频道被用户主动废弃删除事件 (`CHANNEL_DELETE`) 时引发的回调函数。
     *
     * @param listener 协程监听函数体。
     */
    fun onChannelDelete(listener: suspend (ChannelDeleteEvent) -> Unit) {
        channelDeleteListeners.add(listener)
    }

    /**
     * 注册泛化事件侦听，对暂时未细分的平台推送，以 [GenericEvent] 标准形态被截获。
     *
     * @param listener 协程监听函数体。
     */
    fun onGenericEvent(listener: suspend (GenericEvent) -> Unit) {
        genericListeners.add(listener)
    }

    /**
     * 注册全局通用全事件侦听，每一次不管何种合规事件触发，首先均会被此类别所有监查者捕捞。
     *
     * @param listener 协程监听函数体。
     */
    fun onAnyEvent(listener: suspend (BotEvent) -> Unit) {
        anyEventListeners.add(listener)
    }

    // ==================== 派发执行 ====================

    /**
     * 将已经完成协议层层反序列化创建的事件实例 [event] 精准按类型分派至已驻留注册的所有匹配监听函数。
     *
     * @param event 派生的具体类型化事件对象体。
     */
    fun dispatch(event: BotEvent) {
        scope.launch { dispatchAndAwait(event) }
    }

    internal suspend fun dispatchAndAwait(event: BotEvent) {
        api?.let { event.api = it }
        try {
            for (listener in anyEventListeners) {
                safeInvoke { listener(event) }
            }

            when (event) {
                is ReadyEvent -> readyListeners.forEach { safeInvoke { it(event) } }
                is ResumedEvent -> resumedListeners.forEach { safeInvoke { it(event) } }
                is GroupMessageEvent -> groupMessageListeners.forEach { safeInvoke { it(event) } }
                is GroupAtMessageEvent -> groupAtMessageListeners.forEach { safeInvoke { it(event) } }
                is C2CMessageEvent -> c2cMessageListeners.forEach { safeInvoke { it(event) } }
                is GroupJoinRequestEvent -> groupJoinRequestListeners.forEach { safeInvoke { it(event) } }
                is GroupMemberAddEvent -> groupMemberAddListeners.forEach { safeInvoke { it(event) } }
                is GroupMemberRemoveEvent -> groupMemberRemoveListeners.forEach { safeInvoke { it(event) } }
                is FriendAddEvent -> friendAddListeners.forEach { safeInvoke { it(event) } }
                is FriendDelEvent -> friendDelListeners.forEach { safeInvoke { it(event) } }
                is GroupMsgReceiveEvent -> groupMsgReceiveListeners.forEach { safeInvoke { it(event) } }
                is GroupMsgRejectEvent -> groupMsgRejectListeners.forEach { safeInvoke { it(event) } }
                is C2CMsgReceiveEvent -> c2cMsgReceiveListeners.forEach { safeInvoke { it(event) } }
                is C2CMsgRejectEvent -> c2cMsgRejectListeners.forEach { safeInvoke { it(event) } }
                is MessageDeleteEvent -> messageDeleteListeners.forEach { safeInvoke { it(event) } }
                is PublicMessageDeleteEvent -> publicMessageDeleteListeners.forEach { safeInvoke { it(event) } }
                is DirectMessageDeleteEvent -> directMessageDeleteListeners.forEach { safeInvoke { it(event) } }
                is GuildMemberAddEvent -> guildMemberAddListeners.forEach { safeInvoke { it(event) } }
                is GuildMemberUpdateEvent -> guildMemberUpdateListeners.forEach { safeInvoke { it(event) } }
                is GuildMemberRemoveEvent -> guildMemberRemoveListeners.forEach { safeInvoke { it(event) } }
                is MessageReactionAddEvent -> reactionAddListeners.forEach { safeInvoke { it(event) } }
                is MessageReactionRemoveEvent -> reactionRemoveListeners.forEach { safeInvoke { it(event) } }
                is MessageAuditPassEvent -> auditPassListeners.forEach { safeInvoke { it(event) } }
                is MessageAuditRejectEvent -> auditRejectListeners.forEach { safeInvoke { it(event) } }
                is ForumThreadCreateEvent -> forumThreadCreateListeners.forEach { safeInvoke { it(event) } }
                is ForumThreadUpdateEvent -> forumThreadUpdateListeners.forEach { safeInvoke { it(event) } }
                is ForumThreadDeleteEvent -> forumThreadDeleteListeners.forEach { safeInvoke { it(event) } }
                is ForumPostCreateEvent -> forumPostCreateListeners.forEach { safeInvoke { it(event) } }
                is ForumPostDeleteEvent -> forumPostDeleteListeners.forEach { safeInvoke { it(event) } }
                is ForumReplyCreateEvent -> forumReplyCreateListeners.forEach { safeInvoke { it(event) } }
                is ForumReplyDeleteEvent -> forumReplyDeleteListeners.forEach { safeInvoke { it(event) } }
                is ForumPublishAuditResultEvent -> forumAuditListeners.forEach { safeInvoke { it(event) } }
                is GroupAddRobotEvent -> groupAddRobotListeners.forEach { safeInvoke { it(event) } }
                is GroupDelRobotEvent -> groupDelRobotListeners.forEach { safeInvoke { it(event) } }
                is AudioOrLiveChannelMemberEnterEvent -> audioMemberEnterListeners.forEach { safeInvoke { it(event) } }
                is AudioOrLiveChannelMemberExitEvent -> audioMemberExitListeners.forEach { safeInvoke { it(event) } }
                is AudioStartEvent -> audioStartListeners.forEach { safeInvoke { it(event) } }
                is AudioFinishEvent -> audioFinishListeners.forEach { safeInvoke { it(event) } }
                is AudioOnMicEvent -> audioOnMicListeners.forEach { safeInvoke { it(event) } }
                is AudioOffMicEvent -> audioOffMicListeners.forEach { safeInvoke { it(event) } }
                is GuildAtMessageEvent -> guildAtMessageListeners.forEach { safeInvoke { it(event) } }
                is GuildMessageEvent -> guildMessageListeners.forEach { safeInvoke { it(event) } }
                is DirectMessageEvent -> directMessageListeners.forEach { safeInvoke { it(event) } }
                is InteractionCreateEvent -> interactionListeners.forEach { safeInvoke { it(event) } }
                is GuildCreateEvent -> guildCreateListeners.forEach { safeInvoke { it(event) } }
                is GuildUpdateEvent -> guildUpdateListeners.forEach { safeInvoke { it(event) } }
                is GuildDeleteEvent -> guildDeleteListeners.forEach { safeInvoke { it(event) } }
                is ChannelCreateEvent -> channelCreateListeners.forEach { safeInvoke { it(event) } }
                is ChannelUpdateEvent -> channelUpdateListeners.forEach { safeInvoke { it(event) } }
                is ChannelDeleteEvent -> channelDeleteListeners.forEach { safeInvoke { it(event) } }
                is GenericEvent -> genericListeners.forEach { safeInvoke { it(event) } }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("事件全局调度派发流程遭遇未预期异常: {}", e.message, e)
        }
    }

    private inline fun safeInvoke(block: () -> Unit) {
        try {
            block()
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            logger.error("在执行自定义具体事件回调监听期间发生异常, 已成功安全隔离: {}", t.message, t)
        }
    }

    /** Cancel pending listener callbacks when the owning bot is closed. */
    fun close() {
        scope.cancel()
    }
}
