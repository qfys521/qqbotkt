package cn.qfys521.qqbot.dsl

import cn.qfys521.qqbot.QQBot
import cn.qfys521.qqbot.config.QQBotConfig
import cn.qfys521.qqbot.event.*
import cn.qfys521.qqbot.model.common.ShardConfig

/**
 * 构建并初始化一个高阶 Kotlin 异步 QQ 机器人的入口语法糖 (`qqBot { ... }`)。
 *
 * 开发者可在此 DSL 作用域内部直接完成运行项（如 `appId`、`clientSecret`、分片规则和环境选型等）的赋值配置，
 * 并以极为自然的方式直接声明各个平台网络事件的处理逻辑（诸如 `onReady`、`onGroupAtMessage`、`onC2CMessage` 等）。
 *
 * @param block 在 [QQBotBuilderScope] 作用域内运行的配置和侦听挂载回调表达式。
 * @return 创建并装配完好、随时可以使用 `startBlocking()` 或 `start()` 跑动的 [QQBot] 实例。
 */
fun qqBot(block: QQBotBuilderScope.() -> Unit): QQBot {
    val scope = QQBotBuilderScope().apply(block)
    return scope.build()
}

/**
 * 为 DSL (Domain Specific Language) 量身打造的专属作用域构建器类 [QQBotBuilderScope]。
 *
 * 通过委托属性设置与对底层 `eventDispatcher` 的内联转发绑定，为调用方创造直观且声明式的编程体验。
 */
class QQBotBuilderScope {
    private val config = QQBotConfig()
    private val eventListeners = mutableListOf<(EventDispatcher) -> Unit>()

    /** QQ 机器人的平台 AppID。 */
    var appId: String
        get() = config.appId
        set(value) { config.appId = value }

    /** QQ 机器人的客户端核心高密鉴权密钥 ClientSecret。 */
    var clientSecret: String
        get() = config.clientSecret
        set(value) { config.clientSecret = value }

    /** 是否切换连接开放平台沙箱环境地址 (Sandbox)。 */
    var sandbox: Boolean
        get() = config.sandbox
        set(value) { config.sandbox = value }

    /** 需要订阅与侦听的具体官方网关事件意图合集 (Intents 位掩码)。 */
    var intents: Int
        get() = config.intents
        set(value) { config.intents = value }

    /** 本实例在整个集群系统所分配的分片策略参数。 */
    var shard: ShardConfig
        get() = config.shard
        set(value) { config.shard = value }

    /** 单个 HTTP 网络请求超时等待毫秒总数。 */
    var requestTimeoutMillis: Long
        get() = config.requestTimeoutMillis
        set(value) { config.requestTimeoutMillis = value }

    /** 遇 HTTP 429 或网络波动最大指数退避尝试次数。 */
    var maxRetries: Int
        get() = config.maxRetries
        set(value) { config.maxRetries = value }

    /** 连接出现挂断时否自动重连及通过 SessionId 实施会话无感 Resume 恢复。 */
    var autoReconnect: Boolean
        get() = config.autoReconnect
        set(value) { config.autoReconnect = value }

    /** 自定义测试或网关中转所采用的 API 根路径地址。 */
    var customBaseUrl: String?
        get() = config.customBaseUrl
        set(value) { config.customBaseUrl = value }

    // ==================== 事件响应侦听 DSL ====================

    /**
     * 绑定长连网关连接就绪事件 (`READY`) 回调监听。
     *
     * @param listener 自定监听协程执行体。
     */
    fun onReady(listener: suspend (ReadyEvent) -> Unit) {
        eventListeners.add { it.onReady(listener) }
    }

    /**
     * 绑定会话成功通过 Resume 找回并重续后触发 (`RESUMED`) 的回调函数。
     *
     * @param listener 自定监听协程执行体。
     */
    fun onResumed(listener: suspend (ResumedEvent) -> Unit) {
        eventListeners.add { it.onResumed(listener) }
    }
    /**
     * 绑定 QQ 群聊中普通发话（包括无 @ 消息） (`GROUP_MESSAGE_CREATE`) 的响应监听回调。
     *
     * @param listener 自定监听协程执行体。
     */
    fun onGroupMessage(listener: suspend (GroupMessageEvent) -> Unit) {
        eventListeners.add { it.onGroupMessage(listener) }
    }

    /**
     * 绑定 QQ 群聊中他人 @ 机器人发话 (`GROUP_AT_MESSAGE_CREATE`) 的响应监听回调。
     *
     * @param listener 自定监听协程执行体。
     */
    fun onGroupAtMessage(listener: suspend (GroupAtMessageEvent) -> Unit) {
        eventListeners.add { it.onGroupAtMessage(listener) }
    }

    /**
     * 绑定单聊私人界面发向此机器人的消息 (`C2C_MESSAGE_CREATE`) 回调。
     *
     * @param listener 自定监听协程执行体。
     */
    fun onC2CMessage(listener: suspend (C2CMessageEvent) -> Unit) {
        eventListeners.add { it.onC2CMessage(listener) }
    }

    /**
     * 绑定公域频道中被用户 @ 时 (`AT_MESSAGE_CREATE`) 的侦听回调。
     *
     * @param listener 自定监听协程执行体。
     */
    fun onGuildAtMessage(listener: suspend (GuildAtMessageEvent) -> Unit) {
        eventListeners.add { it.onGuildAtMessage(listener) }
    }

    /**
     * 绑定获得所有普通频道发文消息推流 (`MESSAGE_CREATE`) 的独裁特权回调。
     *
     * @param listener 自定监听协程执行体。
     */
    fun onGuildMessage(listener: suspend (GuildMessageEvent) -> Unit) {
        eventListeners.add { it.onGuildMessage(listener) }
    }

    /**
     * 绑定频道用户对应用私信下达 (`DIRECT_MESSAGE_CREATE`) 的消息监听。
     *
     * @param listener 自定监听协程执行体。
     */
    fun onDirectMessage(listener: suspend (DirectMessageEvent) -> Unit) {
        eventListeners.add { it.onDirectMessage(listener) }
    }

    /**
     * 绑定内嵌页面按钮被触打或者自定义指令点击触发 (`INTERACTION_CREATE`) 的处理侦听。
     *
     * @param listener 自定监听协程执行体。
     */
    fun onInteractionCreate(listener: suspend (InteractionCreateEvent) -> Unit) {
        eventListeners.add { it.onInteractionCreate(listener) }
    }

    /**
     * 绑定加入某个新大频道或是群组架构刚被建立时 (`GUILD_CREATE`) 的通知函数。
     *
     * @param listener 自定监听协程执行体。
     */
    fun onGuildCreate(listener: suspend (GuildCreateEvent) -> Unit) {
        eventListeners.add { it.onGuildCreate(listener) }
    }

    /**
     * 绑定大频道背景与属性改变更新 (`GUILD_UPDATE`) 回调。
     *
     * @param listener 自定监听协程执行体。
     */
    fun onGuildUpdate(listener: suspend (GuildUpdateEvent) -> Unit) {
        eventListeners.add { it.onGuildUpdate(listener) }
    }

    /**
     * 绑定主频道注销废除或机器人退群时的 (`GUILD_DELETE`) 声明。
     *
     * @param listener 自定监听协程执行体。
     */
    fun onGuildDelete(listener: suspend (GuildDeleteEvent) -> Unit) {
        eventListeners.add { it.onGuildDelete(listener) }
    }

    /**
     * 绑定其频道中某一分类/文本或语音子频道新创建时 (`CHANNEL_CREATE`) 的广播回调。
     *
     * @param listener 自定监听协程执行体。
     */
    fun onChannelCreate(listener: suspend (ChannelCreateEvent) -> Unit) {
        eventListeners.add { it.onChannelCreate(listener) }
    }

    /**
     * 绑定某一个既有子频道发生名字位置改写时的 (`CHANNEL_UPDATE`) 回调。
     *
     * @param listener 自定监听协程执行体。
     */
    fun onChannelUpdate(listener: suspend (ChannelUpdateEvent) -> Unit) {
        eventListeners.add { it.onChannelUpdate(listener) }
    }

    /**
     * 绑定该特定子频道自父级网络被清理移除 (`CHANNEL_DELETE`) 的事件函数。
     *
     * @param listener 自定监听协程执行体。
     */
    fun onChannelDelete(listener: suspend (ChannelDeleteEvent) -> Unit) {
        eventListeners.add { it.onChannelDelete(listener) }
    }

    /**
     * 绑定所有泛型或由平台最新增发且暂无特定结构解析化定义的通用定制事件 (`GenericEvent`) 侦听器。
     *
     * @param listener 自定监听协程执行体。
     */
    fun onGenericEvent(listener: suspend (GenericEvent) -> Unit) {
        eventListeners.add { it.onGenericEvent(listener) }
    }

    /**
     * 绑定监听任何经由网关下发的合法且有效之全部平台事件的通用侦听函数。
     *
     * @param listener 自定监听协程执行体。
     */
    fun onAnyEvent(listener: suspend (BotEvent) -> Unit) {
        eventListeners.add { it.onAnyEvent(listener) }
    }

    /**
     * 在作用域完成声明式赋值配置与所有监听挂载后，内部执行实例化及装配组装的方法。
     *
     * @return 构建完善可供执行的 [QQBot] 对外业务控制门面实例。
     */
    internal fun build(): QQBot {
        val bot = QQBot(config)
        eventListeners.forEach { it(bot.eventDispatcher) }
        return bot
    }
}
