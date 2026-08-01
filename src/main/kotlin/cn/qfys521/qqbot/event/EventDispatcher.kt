package cn.qfys521.qqbot.event

import cn.qfys521.qqbot.http.QQBotApi
import kotlinx.coroutines.CoroutineScope
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
    private val groupAtMessageListeners = CopyOnWriteArrayList<suspend (GroupAtMessageEvent) -> Unit>()
    private val c2cMessageListeners = CopyOnWriteArrayList<suspend (C2CMessageEvent) -> Unit>()
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
        api?.let { event.api = it }

        scope.launch {
            try {
                for (listener in anyEventListeners) {
                    safeInvoke { listener(event) }
                }

                when (event) {
                    is ReadyEvent -> readyListeners.forEach { safeInvoke { it(event) } }
                    is ResumedEvent -> resumedListeners.forEach { safeInvoke { it(event) } }
                    is GroupAtMessageEvent -> groupAtMessageListeners.forEach { safeInvoke { it(event) } }
                    is C2CMessageEvent -> c2cMessageListeners.forEach { safeInvoke { it(event) } }
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
            } catch (e: Exception) {
                logger.error("事件全局调度派发流程遭遇未预期异常: {}", e.message, e)
            }
        }
    }

    private inline fun safeInvoke(block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            logger.error("在执行自定义具体事件回调监听期间发生异常, 已成功安全隔离: {}", t.message, t)
        }
    }
}
