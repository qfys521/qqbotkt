package cn.qfys521.qqbot.gateway

import cn.qfys521.qqbot.auth.AccessTokenManager
import cn.qfys521.qqbot.config.QQBotConfig
import cn.qfys521.qqbot.event.*
import cn.qfys521.qqbot.exception.QQBotGatewayException
import cn.qfys521.qqbot.http.QQBotApi
import cn.qfys521.qqbot.model.common.OpCode
import cn.qfys521.qqbot.model.guild.Channel
import cn.qfys521.qqbot.model.guild.Guild
import cn.qfys521.qqbot.model.interaction.Interaction
import cn.qfys521.qqbot.model.message.Message
import cn.qfys521.qqbot.model.gateway.*
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.url
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * QQ 机器人 WebSocket 实时长连网关客户端核心引擎 (QQBotGateway)。
 *
 * 底层架构特性：
 * 1. 严格遵守官方 OpCode 协议交互流程（`OpCode 10 Hello` 握手 -> `OpCode 2 Identify` / `OpCode 6 Resume` 登录会话）；
 * 2. 具备独立常驻心跳协程：依据网关下发的 `heartbeat_interval` 定时主动提交心跳报文 (`OpCode 1 Heartbeat`)；
 * 3. 序列号与会话高可用：自动追踪保存最新事件序列号 `s` 与 Session ID，即使遇到网络重连或 `OpCode 7 Reconnect` 命令，
 *    也能发起无损续约请求 (`Resume`)，极大减轻重连造成的信息漏抓与频率封锁风险。
 *
 * @property config 整体客户端运行配置参数。
 * @property tokenManager AccessToken 自动管理器。
 * @property api OpenAPI [QQBotApi] 对象，在接收并派发事件时为各个业务对象提供接口操作上下文指针。
 * @property httpClient 支持 WebSocket 的 HttpClient。
 * @property eventDispatcher 事件分发核心引擎。
 * @property json JSON 编解码工具配置。
 */
class QQBotGateway(
    private val config: QQBotConfig,
    private val tokenManager: AccessTokenManager,
    private val api: QQBotApi,
    private val httpClient: HttpClient,
    private val eventDispatcher: EventDispatcher,
    private val json: Json = defaultJson
) {
    private val logger = LoggerFactory.getLogger(QQBotGateway::class.java)

    companion object {
        /**
         * 用于 WebSocket 网关报文解析的专用的 JSON 序列化器配置实例。
         */
        val defaultJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }

    private val isRunning = AtomicBoolean(false)
    private var gatewayScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var heartbeatJob: Job? = null
    private var wsSession: DefaultClientWebSocketSession? = null

    /** 缓存当前连接成功的 Session ID，主要用于断开连接时的会话续订及网络层恢复。 */
    private var cachedSessionId: String? = null
    /** 记录由平台推送过来的最近下行报文的有效数字序号 `s`。 */
    private var latestSeq: Long? = null

    /**
     * 以全异步且不挂断调用者工作线程的方式向平台网关启动监听循环。
     */
    fun start() {
        if (!isRunning.compareAndSet(false, true)) {
            logger.warn("QQBotGateway 已处于正常连接运行中，避免重复触发启动。")
            return
        }
        gatewayScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        gatewayScope.launch {
            connectionLoop()
        }
    }

    /**
     * 发动网关监听连入，并持久化阻塞当前函数调起方所在的主协程/主调用线程直至执行 [stop]。
     */
    suspend fun startBlocking() {
        start()
        while (isRunning.get()) {
            delay(500)
        }
    }

    /**
     * 主动断开当前与官方长连接网关会话，并停止取消后台所有的心跳及连接保活协程任务。
     */
    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            logger.info("收到中止信号，准备正常终止断开 QQBotGateway 连接并释放协程资源...")
            heartbeatJob?.cancel()
            gatewayScope.launch {
                try {
                    wsSession?.close(CloseReason(CloseReason.Codes.NORMAL, "Bot Stop"))
                } catch (ignored: Exception) {
                }
            }
            gatewayScope.cancel()
        }
    }

    private suspend fun connectionLoop() {
        var retryAttempt = 0
        while (isRunning.get()) {
            try {
                // 1. 获取 WSS 网关接入地址
                val wssInfo = try {
                    api.getWssBotUrl()
                } catch (e: Exception) {
                    logger.debug("请求 /gateway/bot 接入点未成功，现降级请求普通 /gateway 通用端点...")
                    api.getWssUrl()
                }
                val wssUrl = wssInfo.url
                logger.info("准备连入 QQ 官方 WebSocket 网关连接点: {}", wssUrl)

                // 2. 建立长连
                httpClient.webSocketSession {
                    url(wssUrl)
                }.let { session ->
                    wsSession = session
                    retryAttempt = 0
                    logger.info("与网关服务器建立 TCP/TLS 通讯成功，当前等待服务端发送 OpCode 10 Hello...")
                    sessionLoop(session)
                }
                if (isRunning.get()) {
                    logger.warn("与远端 WebSocket 网关会话断开/结束，3 秒后重新进入连接循环...")
                    delay(3000L)
                }

            } catch (e: Exception) {
                if (!isRunning.get()) break
                if (e is ClosedReceiveChannelException || e is CancellationException) {
                    logger.warn("当前 WebSocket 连接已被重断或主动关闭。")
                } else {
                    logger.error("在维护 WebSocket 长期存活链路时遭遇异常: {}", e.message)
                }

                if (!config.autoReconnect) {
                    logger.info("由于未开启自动重新鉴权及重连 (autoReconnect=false)，网关退出本次连接循环。")
                    isRunning.set(false)
                    break
                }

                retryAttempt++
                val backoffMs = (2000L * retryAttempt).coerceAtMost(60_000L)
                logger.warn("网关将在等待 {} 毫秒后尝试执行第 {} 次自动重连动作...", backoffMs, retryAttempt)
                delay(backoffMs)
            }
        }
    }

    private suspend fun sessionLoop(session: DefaultClientWebSocketSession) {
        try {
            for (frame in session.incoming) {
                if (frame !is Frame.Text) continue
                val text = frame.readText()
                val payload = try {
                    json.decodeFromString<GatewayPayload>(text)
                } catch (e: Exception) {
                    logger.error("解读解析网关下行的帧数据期间发生反序列化异常: {}, 载文: {}", e.message, text)
                    continue
                }

                if (payload.s != null) {
                    latestSeq = payload.s
                }

                when (payload.op) {
                    OpCode.HELLO -> {
                        val helloData = payload.d?.let { json.decodeFromJsonElement<HelloData>(it) }
                            ?: HelloData()
                        val interval = helloData.heartbeatInterval
                        logger.info("已接到 Hello (op=10), 心跳发送间隔要求: {} ms", interval)
                        startHeartbeatLoop(session, interval)

                        if (cachedSessionId != null && latestSeq != null) {
                            logger.info("检测到缓存会话 SessionId ({}) 与已追溯序列号 ({}) 依然完好，尝试发出 Resume (op=6)...", cachedSessionId, latestSeq)
                            sendResume(session)
                        } else {
                            logger.info("未发现可复用的上期有效凭靠，开始正式提交 Identify (op=2) 进行新鉴权...")
                            sendIdentify(session)
                        }
                    }

                    OpCode.HEARTBEAT_ACK -> {
                        logger.debug("顺利收到心跳请求答复 Heartbeat ACK (op=11)")
                    }

                    OpCode.HEARTBEAT -> {
                        sendHeartbeat(session)
                    }

                    OpCode.RECONNECT -> {
                        logger.warn("平台主动发起 OpCode 7 Reconnect 会话重置指令，现切断当前套接字以触发平滑续订重接...")
                        session.close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "Server Reconnect"))
                        return
                    }

                    OpCode.INVALID_SESSION -> {
                        logger.warn("遇到平台回发 OpCode 9 Invalid Session 警报，原有会话 SessionId 已完全失效！自动清理旧会话，立即重新 Identify...")
                        cachedSessionId = null
                        latestSeq = null
                        sendIdentify(session)
                    }

                    OpCode.DISPATCH -> {
                        handleDispatchEvent(payload)
                    }

                    else -> {
                        logger.debug("捕获到其它未特异分派的 OpCode={} 数据类型: {}", payload.op, payload.t)
                    }
                }
            }
            val reason = session.closeReason.await()
            if (reason != null) {
                logger.warn("WebSocket 官方网关连接已关闭/中断: code={}, message='{}'", reason.code, reason.message)
                when (reason.code) {
                    4014.toShort() -> {
                        logger.error("❌ [错误 4014 Disallowed Intent]: 当前请求的 intents ({}) 包含了未在 QQ 机器人后台启用的事件权限！", config.intents)
                        logger.error("👉 解决建议: 大多数 QQ 官方机器仅开通了【群聊与单聊】，请修改 qqbot.json 将 intents 设为 33554432 (GROUP_AND_C2C_EVENT)。")
                    }
                    4004.toShort() -> {
                        logger.error("❌ [错误 4004 Authentication Failed]: Token 或 AppID 鉴权失败，请检查开放平台应用凭证。")
                    }
                    4001.toShort() -> {
                        logger.error("❌ [错误 4001 Invalid Opcode]: 发向服务端的 OpCode 或 payload 格式有误。")
                    }
                    else -> {
                        logger.error("⚠️ [网关断开 code={}] - 服务端连接关闭消息: {}", reason.code, reason.message)
                    }
                }
            }
        } finally {
            heartbeatJob?.cancel()
            heartbeatJob = null
        }
    }

    private fun startHeartbeatLoop(session: DefaultClientWebSocketSession, intervalMs: Long) {
        heartbeatJob?.cancel()
        heartbeatJob = gatewayScope.launch {
            while (isActive && isRunning.get()) {
                delay(intervalMs)
                sendHeartbeat(session)
            }
        }
    }

    private suspend fun sendHeartbeat(session: DefaultClientWebSocketSession) {
        try {
            val packet = GatewayPayload(op = OpCode.HEARTBEAT, d = latestSeq?.let { json.parseToJsonElement(it.toString()) })
            val text = json.encodeToString(packet)
            session.send(Frame.Text(text))
            logger.debug("定时发往服务端心跳报文 Heartbeat (op=1, s={})", latestSeq)
        } catch (e: Exception) {
            logger.error("向服务端投递心跳帧数据发生异常: {}", e.message)
        }
    }

    private suspend fun sendIdentify(session: DefaultClientWebSocketSession) {
        val token = tokenManager.getAccessToken()
        val identifyPacket = IdentifyPayload(
            op = OpCode.IDENTIFY,
            d = IdentifyData(
                token = "QQBot $token",
                intents = config.intents,
                shard = config.shard.toList()
            )
        )
        val text = json.encodeToString(identifyPacket)
        logger.info("已向官方网关发送 Identify 鉴权报文 (op=2) -> intents={}, shard={}", config.intents, config.shard.toList())
        logger.debug("完整鉴权报文内容: {}", text.replace(token, "******"))
        session.send(Frame.Text(text))
    }

    private suspend fun sendResume(session: DefaultClientWebSocketSession) {
        val token = tokenManager.getAccessToken()
        val resumePacket = ResumePayload(
            op = OpCode.RESUME,
            d = ResumeData(
                token = "QQBot $token",
                sessionId = cachedSessionId ?: "",
                seq = latestSeq ?: 0L
            )
        )
        val text = json.encodeToString(resumePacket)
        session.send(Frame.Text(text))
    }

    private fun handleDispatchEvent(payload: GatewayPayload) {
        val eventType = payload.t ?: return
        val rawJson = payload.d?.toString()

        try {
            when (eventType) {
                "READY" -> {
                    val readyData = payload.d?.let { json.decodeFromJsonElement<ReadyData>(it) } ?: ReadyData()
                    cachedSessionId = readyData.sessionId
                    logger.info("网关 READY！恭喜机器人连接鉴权成功，当前登录昵称: {}, 凭依 SessionId: {}", readyData.user.username, readyData.sessionId)
                    eventDispatcher.dispatch(ReadyEvent(readyData, payload.id, payload.s?.toString(), rawJson))
                }

                "RESUMED" -> {
                    logger.info("会话续订确认成功 (RESUMED)！一切既往连接上下文完全恢复，无缝运行中！")
                    eventDispatcher.dispatch(ResumedEvent(payload.id, payload.s?.toString(), rawJson))
                }

                "GROUP_AT_MESSAGE_CREATE" -> {
                    val message = payload.d?.let { json.decodeFromJsonElement<Message>(it) } ?: return
                    eventDispatcher.dispatch(GroupAtMessageEvent(message, payload.id, payload.s?.toString(), rawJson))
                }

                "C2C_MESSAGE_CREATE" -> {
                    val message = payload.d?.let { json.decodeFromJsonElement<Message>(it) } ?: return
                    eventDispatcher.dispatch(C2CMessageEvent(message, payload.id, payload.s?.toString(), rawJson))
                }

                "AT_MESSAGE_CREATE" -> {
                    val message = payload.d?.let { json.decodeFromJsonElement<Message>(it) } ?: return
                    eventDispatcher.dispatch(GuildAtMessageEvent(message, payload.id, payload.s?.toString(), rawJson))
                }

                "MESSAGE_CREATE" -> {
                    val message = payload.d?.let { json.decodeFromJsonElement<Message>(it) } ?: return
                    eventDispatcher.dispatch(GuildMessageEvent(message, payload.id, payload.s?.toString(), rawJson))
                }

                "DIRECT_MESSAGE_CREATE" -> {
                    val message = payload.d?.let { json.decodeFromJsonElement<Message>(it) } ?: return
                    eventDispatcher.dispatch(DirectMessageEvent(message, payload.id, payload.s?.toString(), rawJson))
                }

                "INTERACTION_CREATE" -> {
                    val interaction = payload.d?.let { json.decodeFromJsonElement<Interaction>(it) } ?: return
                    eventDispatcher.dispatch(InteractionCreateEvent(interaction, payload.id, payload.s?.toString(), rawJson))
                }

                "GUILD_CREATE" -> {
                    val guild = payload.d?.let { json.decodeFromJsonElement<Guild>(it) } ?: return
                    eventDispatcher.dispatch(GuildCreateEvent(guild, payload.id, payload.s?.toString(), rawJson))
                }

                "GUILD_UPDATE" -> {
                    val guild = payload.d?.let { json.decodeFromJsonElement<Guild>(it) } ?: return
                    eventDispatcher.dispatch(GuildUpdateEvent(guild, payload.id, payload.s?.toString(), rawJson))
                }

                "GUILD_DELETE" -> {
                    val guild = payload.d?.let { json.decodeFromJsonElement<Guild>(it) } ?: return
                    eventDispatcher.dispatch(GuildDeleteEvent(guild, payload.id, payload.s?.toString(), rawJson))
                }

                "CHANNEL_CREATE" -> {
                    val channel = payload.d?.let { json.decodeFromJsonElement<Channel>(it) } ?: return
                    eventDispatcher.dispatch(ChannelCreateEvent(channel, payload.id, payload.s?.toString(), rawJson))
                }

                "CHANNEL_UPDATE" -> {
                    val channel = payload.d?.let { json.decodeFromJsonElement<Channel>(it) } ?: return
                    eventDispatcher.dispatch(ChannelUpdateEvent(channel, payload.id, payload.s?.toString(), rawJson))
                }

                "CHANNEL_DELETE" -> {
                    val channel = payload.d?.let { json.decodeFromJsonElement<Channel>(it) } ?: return
                    eventDispatcher.dispatch(ChannelDeleteEvent(channel, payload.id, payload.s?.toString(), rawJson))
                }

                else -> {
                    eventDispatcher.dispatch(GenericEvent(eventType, payload.d, payload.id, payload.s?.toString(), rawJson))
                }
            }
        } catch (e: Exception) {
            logger.error("反序列化且派发处理事件分类 {} 出错: {}", eventType, e.message, e)
        }
    }
}
