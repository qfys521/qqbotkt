package cn.qfys521.qqbot.http

import cn.qfys521.qqbot.auth.AccessTokenManager
import cn.qfys521.qqbot.config.QQBotConfig
import cn.qfys521.qqbot.exception.QQBotApiException
import cn.qfys521.qqbot.exception.QQBotException
import cn.qfys521.qqbot.model.common.ApiErrorResponse
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
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.slf4j.LoggerFactory
import java.io.IOException

/**
 * [QQBotApi] 官方 HTTP OpenAPI 客户端的具体实现类。
 *
 * 内置功能：
 * 1. 鉴权头自动填充：在每个网络请求中附加 `Authorization: QQBot $accessToken`；
 * 2. Token 过期自我修复：发现返回码包含 `401` 或错误码 `11243` 时，自动请求刷新 Token 并无感重发请求；
 * 3. 全链路日志与追踪 ID (TraceId) 采集：出现平台异常抛出 `QQBotApiException` 时，附带服务器 Header 里的 `X-Tps-trace-ID`；
 * 4. 频率限制 (429 Too Many Requests) 与瞬态网络异常自动指数退避重试。
 *
 * @property config 整体客户端运行配置参数 [QQBotConfig]。
 * @property tokenManager AccessToken 管理引擎。
 * @property httpClient 经配置的基础 Ktor HttpClient 实例。
 * @property json kotlinx-serialization 序列化工具配置。
 */
class QQBotHttpClient(
    private val config: QQBotConfig,
    private val tokenManager: AccessTokenManager,
    private val httpClient: HttpClient,
    private val json: Json = defaultJson
) : QQBotApi {

    private val logger = LoggerFactory.getLogger(QQBotHttpClient::class.java)

    companion object {
        /**
         * 默认通用宽松 JSON 序列化器配置，能够忽略未知参数，允许非规范 JSON 并跳过默认值序列化。
         */
        val defaultJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            coerceInputValues = true
        }
    }

    /**
     * 内部通用 HTTP 请求派发函数，封装鉴权、错误解析、退避重试及 Token 刷新的基础流程。
     *
     * @param method HTTP 操作请求名称（例如 GET、POST、DELETE、PATCH、PUT）。
     * @param path API 相对路径，带开头的斜杠（比如 `/users/@me`）。
     * @param body 请求附带的有效载荷报文体。
     * @return 返回范型反序列化后的业务数据类实例 [T]。
     */
    private suspend inline fun <reified T> executeRequest(
        method: String,
        path: String,
        body: Any? = null
    ): T {
        var attempt = 0
        var forceRefresh = false
        val maxAttempts = config.maxRetries.coerceAtLeast(1)

        while (true) {
            attempt++
            val token = tokenManager.getAccessToken(forceRefresh = forceRefresh)
            val fullUrl = "${config.baseUrl}$path"
            try {
                val response: HttpResponse = when (method) {
                    "GET" -> httpClient.get {
                        url(fullUrl)
                        header(HttpHeaders.Authorization, "QQBot $token")
                    }
                    "POST" -> httpClient.post {
                        url(fullUrl)
                        header(HttpHeaders.Authorization, "QQBot $token")
                        contentType(ContentType.Application.Json)
                        if (body != null) setBody(body)
                    }
                    "DELETE" -> httpClient.delete {
                        url(fullUrl)
                        header(HttpHeaders.Authorization, "QQBot $token")
                    }
                    "PATCH" -> httpClient.patch {
                        url(fullUrl)
                        header(HttpHeaders.Authorization, "QQBot $token")
                        contentType(ContentType.Application.Json)
                        if (body != null) setBody(body)
                    }
                    "PUT" -> httpClient.put {
                        url(fullUrl)
                        header(HttpHeaders.Authorization, "QQBot $token")
                        contentType(ContentType.Application.Json)
                        if (body != null) setBody(body)
                    }
                    else -> throw IllegalArgumentException("不支持的 HTTP 方法: $method")
                }

                val traceId = response.headers["X-Tps-trace-ID"]
                val status = response.status

                if (status.isSuccess()) {
                    val bodyText = response.bodyAsText()
                    if (bodyText.isBlank() || status.value == 204) {
                        @Suppress("UNCHECKED_CAST")
                        return Unit as T
                    }
                    return json.decodeFromString<T>(bodyText)
                }

                // 错误回包解析
                val bodyText = response.bodyAsText()
                val apiError = try {
                    json.decodeFromString<ApiErrorResponse>(bodyText)
                } catch (e: Exception) {
                    ApiErrorResponse(errCode = status.value, message = bodyText, traceId = traceId)
                }
                val errorTraceId = apiError.traceId ?: traceId

                // 若遇到 Token 过期相关错误（401 / 11243 / 11241），且还未强制重刷过
                if ((status.value == 401 || apiError.errCode == 11243 || apiError.errCode == 11241) && !forceRefresh) {
                    logger.warn("检测到 AccessToken 可能已过期(errCode={})，正在尝试重刷凭证并重发...", apiError.errCode)
                    forceRefresh = true
                    continue
                }

                throw QQBotApiException(
                    errCode = apiError.errCode,
                    errMessage = apiError.message,
                    traceId = errorTraceId,
                    httpStatusCode = status.value
                )

            } catch (e: QQBotApiException) {
                // 若遇限流 429 且可尝试重试
                if (e.httpStatusCode == 429 && attempt < maxAttempts) {
                    val delayMs = 1000L * attempt
                    logger.warn("请求 {} 遇到频率限流(429)，将等待 {}ms 后继续第 {}/{} 次重试", fullUrl, delayMs, attempt, maxAttempts)
                    delay(delayMs)
                    continue
                }
                throw e
            } catch (e: IOException) {
                if (attempt >= maxAttempts) {
                    throw QQBotException("网络调用连续失败，已达到设定次数上界 ($maxAttempts): ${e.message}", e)
                }
                val delayMs = 500L * attempt
                logger.warn("发送 HTTP 请求出现 IO 异常({})，将等待 {}ms 后继续重试", e.message, delayMs)
                delay(delayMs)
                continue
            }
        }
    }

    override suspend fun getMe(): UserMe = executeRequest("GET", "/users/@me")

    override suspend fun getMyGuilds(): List<GuildItem> = executeRequest("GET", "/users/@me/guilds")

    override suspend fun sendC2CMessage(userOpenId: String, request: SendMessageRequest): MessageResult {
        return executeRequest("POST", "/v2/users/$userOpenId/messages", request)
    }

    override suspend fun deleteC2CMessage(userOpenId: String, messageId: String) {
        executeRequest<Unit>("DELETE", "/v2/users/$userOpenId/messages/$messageId")
    }

    override suspend fun sendC2CStreamMessage(userOpenId: String, request: SendMessageRequest): MessageResult {
        return executeRequest("POST", "/v2/users/$userOpenId/stream/messages", request)
    }

    override suspend fun uploadC2CMedia(userOpenId: String, request: UploadMediaRequest): UploadMediaResponse {
        return executeRequest("POST", "/v2/users/$userOpenId/files", request)
    }

    override suspend fun sendGroupMessage(groupOpenId: String, request: SendMessageRequest): MessageResult {
        return executeRequest("POST", "/v2/groups/$groupOpenId/messages", request)
    }

    override suspend fun deleteGroupMessage(groupOpenId: String, messageId: String) {
        executeRequest<Unit>("DELETE", "/v2/groups/$groupOpenId/messages/$messageId")
    }

    override suspend fun getGroupInfo(groupOpenId: String): JsonElement {
        return executeRequest("GET", "/v2/groups/$groupOpenId/info")
    }

    override suspend fun getGroupBotState(groupOpenId: String): GroupBotState {
        return executeRequest("GET", "/v2/groups/$groupOpenId/bot-state")
    }

    override suspend fun uploadGroupMedia(groupOpenId: String, request: UploadMediaRequest): UploadMediaResponse {
        return executeRequest("POST", "/v2/groups/$groupOpenId/files", request)
    }

    override suspend fun getGuild(guildId: String): Guild {
        return executeRequest("GET", "/guilds/$guildId")
    }

    override suspend fun getChannels(guildId: String): List<Channel> {
        return executeRequest("GET", "/guilds/$guildId/channels")
    }

    override suspend fun getChannel(channelId: String): Channel {
        return executeRequest("GET", "/channels/$channelId")
    }

    override suspend fun createChannel(guildId: String, request: CreateChannelRequest): Channel {
        return executeRequest("POST", "/guilds/$guildId/channels", request)
    }

    override suspend fun updateChannel(channelId: String, request: UpdateChannelRequest): Channel {
        return executeRequest("PATCH", "/channels/$channelId", request)
    }

    override suspend fun deleteChannel(channelId: String) {
        executeRequest<Unit>("DELETE", "/channels/$channelId")
    }

    override suspend fun putInteractionResponse(interactionId: String, request: InteractionResponseRequest) {
        executeRequest<Unit>("PUT", "/v2/interactions/$interactionId", request)
    }

    override suspend fun getWssUrl(): WssUrlResponse {
        return executeRequest("GET", "/gateway")
    }

    override suspend fun getWssBotUrl(): WssUrlResponse {
        return executeRequest("GET", "/gateway/bot")
    }
}
