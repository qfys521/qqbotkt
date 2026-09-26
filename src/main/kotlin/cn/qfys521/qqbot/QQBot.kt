package cn.qfys521.qqbot

import cn.qfys521.qqbot.auth.AccessTokenManager
import cn.qfys521.qqbot.config.QQBotConfig
import cn.qfys521.qqbot.event.EventDispatcher
import cn.qfys521.qqbot.gateway.QQBotGateway
import cn.qfys521.qqbot.http.QQBotApi
import cn.qfys521.qqbot.http.QQBotHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.Closeable

/**
 * QQ 机器人 SDK 的主门面总控核心类 (QQBot Facade)。
 *
 * 管理从网络连接客户端 (`HttpClient`)、凭据自动续期器 (`AccessTokenManager`)、
 * OpenAPI 业务操作类 (`QQBotApi`) 到 WebSocket 智能长连接网关 (`QQBotGateway`) 的全体系统架构，
 * 同时提供简捷的一站式事件分发管理引用。
 *
 * 建议在应用生命周期中仅持有单个或有限数量的 [QQBot] 实例。
 *
 * @property config 初始化并校验过的机器人配置项。
 * @property httpClient 支持 OkHttp Engine、WebSocket 以及 kotlinx.serialization JSON 协商的 Ktor [HttpClient]。
 * @property tokenManager 自动检测并在有效期结束前重新请求 AccessToken 的凭证中心。
 * @property api OpenAPI [QQBotApi] 功能实现客户端，用于调用群聊/私信/频道/文件处理接口。
 * @property eventDispatcher 事件分发核心执行器，负责接收事件帧并通过协程作用域将其调度给监听函数。
 * @property gateway 维护官方 WSS 连接、协议鉴权与断更自动会话修复 (`Resume`) 的长连接网关引擎。
 */
class QQBot(
    val config: QQBotConfig,
    private val httpClient: HttpClient = defaultHttpClient(config),
    val tokenManager: AccessTokenManager = AccessTokenManager(config, httpClient),
    val api: QQBotApi = QQBotHttpClient(config, tokenManager, httpClient),
    val eventDispatcher: EventDispatcher = EventDispatcher(api),
    val gateway: QQBotGateway = QQBotGateway(config, tokenManager, api, httpClient, eventDispatcher)
) : Closeable {

    private val logger = LoggerFactory.getLogger(QQBot::class.java)

    companion object {
        /**
         * 创建并配置自带 JSON 支持和时限管控的高性能 OkHttp 引擎 [HttpClient]。
         *
         * @param config 用于配置 HttpTimeout 的运行参数。
         * @return 准备完毕可马上用于 HTTP 与 WSS 调用的客户端实例。
         */
        fun defaultHttpClient(config: QQBotConfig): HttpClient {
            return HttpClient(OkHttp) {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        encodeDefaults = true
                    })
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = config.requestTimeoutMillis
                    connectTimeoutMillis = config.requestTimeoutMillis
                    socketTimeoutMillis = config.requestTimeoutMillis
                }
                install(WebSockets) {
                    pingIntervalMillis = -1L // 由网关业务层基于 Hello 下发的周期定制控制心跳
                }
            }
        }
    }

    init {
        config.validate()
        logger.info("QQBot 门面初始化完成: appId={}, sandbox={}, baseUrl={}", config.appId, config.sandbox, config.baseUrl)
    }

    /**
     * 在后台异步启动并连入平台 WebSocket 实时长连接网关。
     * 本方法为非阻塞执行，调起后立即返回调用方。
     */
    fun start() {
        gateway.start()
    }

    /**
     * 以阻塞当前线程/协程的形式连入 WebSocket 网关，持续响应在线并服务，直至应用被主动关闭。
     * 一般在服务端或者脚本项目的 `main()` 主函数末尾处调用。
     */
    suspend fun startBlocking() {
        gateway.startBlocking()
    }

    /**
     * 主动停止长连接网关运行，终止后台所有保约协程心跳并优雅关闭底层网路套接字。
     */
    fun stop() {
        gateway.stop()
    }

    /**
     * 释放与彻底重置所有网络套接字和 HTTP 引擎的底层系统文件资源。
     */
    override fun close() {
        stop()
        eventDispatcher.close()
        try {
            httpClient.close()
        } catch (e: Exception) {
            logger.warn("停止释放 Ktor HttpClient 时引发提示: {}", e.message)
        }
    }
}
