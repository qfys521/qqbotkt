package cn.qfys521.qqbot.http

import cn.qfys521.qqbot.auth.AccessTokenManager
import cn.qfys521.qqbot.config.QQBotConfig
import cn.qfys521.qqbot.exception.QQBotApiException
import cn.qfys521.qqbot.exception.QQBotException
import cn.qfys521.qqbot.model.common.ApiErrorResponse
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
import io.ktor.client.HttpClient
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
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

    private fun String.urlEncode(): String = java.net.URLEncoder.encode(this, Charsets.UTF_8).replace("+", "%20")

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
        body: Any? = null,
        query: Map<String, String> = emptyMap()
    ): T {
        var retries = 0
        var authRetryUsed = false
        var token = tokenManager.getAccessToken()
        val fullUrl = "${config.baseUrl.trimEnd('/')}$path"
        val requestUrl = if (query.isEmpty()) fullUrl else fullUrl + "?" + query.entries.joinToString("&") {
            "${it.key.urlEncode()}=${it.value.urlEncode()}"
        }

        while (true) {
            try {
                val response: HttpResponse = when (method) {
                    "GET" -> httpClient.get {
                        url(requestUrl)
                        header(HttpHeaders.Authorization, "QQBot $token")
                    }
                    "POST" -> httpClient.post {
                        url(requestUrl)
                        header(HttpHeaders.Authorization, "QQBot $token")
                        contentType(ContentType.Application.Json)
                        if (body != null) setBody(body)
                    }
                    "DELETE" -> httpClient.delete {
                        url(requestUrl)
                        header(HttpHeaders.Authorization, "QQBot $token")
                    }
                    "PATCH" -> httpClient.patch {
                        url(requestUrl)
                        header(HttpHeaders.Authorization, "QQBot $token")
                        contentType(ContentType.Application.Json)
                        if (body != null) setBody(body)
                    }
                    "PUT" -> httpClient.put {
                        url(requestUrl)
                        header(HttpHeaders.Authorization, "QQBot $token")
                        contentType(ContentType.Application.Json)
                        if (body != null) setBody(body)
                    }
                    else -> error("Unsupported HTTP method: $method")
                }
                val status = response.status.value
                val text = response.bodyAsText()
                val apiError = try {
                    json.decodeFromString<ApiErrorResponse>(text)
                } catch (_: Exception) {
                    null
                }
                val code = apiError?.code?.takeIf { it != 0 } ?: apiError?.errCode ?: 0

                if (response.status.isSuccess() && code == 0) {
                    if (T::class == Unit::class) {
                        @Suppress("UNCHECKED_CAST")
                        return Unit as T
                    }
                    if (text.isBlank()) {
                        throw QQBotException("Empty response for $method $path (HTTP $status)")
                    }
                    return json.decodeFromString<T>(text)
                }

                if ((status == 401 || code == 11243 || code == 11241) && !authRetryUsed) {
                    authRetryUsed = true
                    token = tokenManager.getAccessToken(forceRefresh = true, rejectedToken = token)
                    continue
                }

                if ((status == 429 || (method == "GET" && status in 500..599)) && retries < config.maxRetries) {
                    val retryAfter = response.headers[HttpHeaders.RetryAfter]?.toLongOrNull()
                        ?.coerceIn(0, 300)?.times(1000L)
                    delay(retryAfter ?: retryDelay(retries))
                    retries++
                    continue
                }

                throw QQBotApiException(
                    errCode = code.takeIf { it != 0 } ?: status,
                    errMessage = apiError?.message?.takeIf { it.isNotBlank() } ?: text,
                    traceId = apiError?.traceId ?: response.headers["X-Tps-trace-ID"],
                    httpStatusCode = status
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                currentCoroutineContext().ensureActive()
                if (method != "GET" || retries >= config.maxRetries) {
                    throw QQBotException("Network request failed: $method $path", e)
                }
                delay(retryDelay(retries))
                retries++
            }
        }
    }

    private fun retryDelay(retries: Int): Long =
        (500L * (1L shl retries.coerceAtMost(6))).coerceAtMost(30_000L)

    override suspend fun getMe(): UserMe = executeRequest("GET", "/users/@me")

    override suspend fun getMyGuilds(): List<GuildItem> = executeRequest("GET", "/users/@me/guilds")

    override suspend fun sendC2CMessage(userOpenId: String, request: SendMessageRequest): MessageResult {
        return executeRequest("POST", "/v2/users/$userOpenId/messages", request)
    }

    override suspend fun deleteC2CMessage(userOpenId: String, messageId: String) {
        executeRequest<Unit>("DELETE", "/v2/users/$userOpenId/messages/$messageId")
    }

    override suspend fun sendC2CStreamMessage(userOpenId: String, request: SendMessageRequest): MessageResult {
        return executeRequest("POST", "/v2/users/$userOpenId/stream_messages", request)
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
        return executeRequest("GET", "/v2/groups/$groupOpenId/bot_state")
    }

    override suspend fun uploadGroupMedia(groupOpenId: String, request: UploadMediaRequest): UploadMediaResponse {
        return executeRequest("POST", "/v2/groups/$groupOpenId/files", request)
    }

    override suspend fun generateShareLink(request: ShareLinkRequest): ShareLinkResponse =
        executeRequest("POST", "/v2/generate_url_link", request)

    override suspend fun getGroupMembers(groupOpenId: String, cursor: String?): GroupMemberPage =
        executeRequest("GET", "/v2/groups/$groupOpenId/members", query = cursorQuery(cursor))

    override suspend fun getGroupMember(groupOpenId: String, memberOpenId: String): GroupMemberDetail =
        executeRequest("GET", "/v2/groups/$groupOpenId/members/$memberOpenId")

    override suspend fun getGroupBlacklist(groupOpenId: String, cursor: String?, limit: Int?): BlacklistPage =
        executeRequest("GET", "/v2/groups/$groupOpenId/member_blacklist", query = buildMap {
            cursor?.let { put("cursor", it) }
            limit?.let { put("limit", it.toString()) }
        })

    override suspend fun updateGroupBlacklist(groupOpenId: String, request: BlacklistOperationRequest): BlacklistOperationResponse =
        executeRequest("POST", "/v2/groups/$groupOpenId/member_blacklist", request)

    override suspend fun getRestrictChatSetting(groupOpenId: String): RestrictChatSetting =
        executeRequest("GET", "/v2/groups/$groupOpenId/restrict_chat_setting")

    override suspend fun updateRestrictChatSetting(groupOpenId: String, request: RestrictChatSettingRequest) {
        executeRequest<Unit>("POST", "/v2/groups/$groupOpenId/restrict_chat_setting", request)
    }

    override suspend fun getJoinRequests(groupOpenId: String, cursor: String?, limit: Int?): JoinRequestPage =
        executeRequest("GET", "/v2/groups/$groupOpenId/join_request_list", query = buildMap {
            cursor?.let { put("cursor", it) }
            limit?.let { put("limit", it.toString()) }
        })

    override suspend fun approveJoinRequest(groupOpenId: String, memberOpenId: String, request: ApproveJoinRequest) {
        executeRequest<Unit>("POST", "/v2/groups/$groupOpenId/approval_join_request/$memberOpenId", request)
    }

    override suspend fun batchRemoveMembers(groupOpenId: String, request: BatchRemoveMembersRequest): BatchRemoveMembersResponse =
        executeRequest("POST", "/v2/groups/$groupOpenId/batch_remove_members", request)

    override suspend fun getJoinApprovalStrategies(cursor: String?, limit: Int?): JoinApprovalStrategyPage =
        executeRequest("GET", "/v2/groups/join_approval_strategy", query = buildMap {
            cursor?.let { put("cursor", it) }
            limit?.let { put("limit", it.toString()) }
        })

    override suspend fun createJoinApprovalStrategy(request: CreateJoinApprovalStrategyRequest): CreateJoinApprovalStrategyResponse =
        executeRequest("POST", "/v2/groups/join_approval_strategy", request)

    override suspend fun updateJoinApprovalStrategy(strategyId: String, request: UpdateJoinApprovalStrategyRequest): UpdateJoinApprovalStrategyResponse =
        executeRequest("PATCH", "/v2/groups/join_approval_strategy/$strategyId", request)

    override suspend fun deleteJoinApprovalStrategy(strategyId: String) {
        executeRequest<Unit>("DELETE", "/v2/groups/join_approval_strategy/$strategyId")
    }

    override suspend fun executeJoinApprovalStrategy(strategyId: String) {
        executeRequest<Unit>("POST", "/v2/groups/join_approval_strategy/$strategyId/execute")
    }

    override suspend fun updateJoinApprovalWhitelist(strategyId: String, request: WhitelistUsersRequest): WhitelistUsersResponse =
        executeRequest("POST", "/v2/groups/join_approval_strategy/$strategyId/whitelist_users", request)

    override suspend fun getMenu(): MenuResponse = executeRequest("GET", "/v2/menu")

    override suspend fun updateMenu(request: MenuRequest): MenuVersion =
        executeRequest("PUT", "/v2/menu", request)

    override suspend fun getPanels(scope: String, cursor: String?, limit: Int?): PanelPage =
        executeRequest("GET", "/v2/panels", query = buildMap {
            put("scope", scope)
            cursor?.let { put("cursor", it) }
            limit?.let { put("limit", it.toString()) }
        })

    override suspend fun createPanel(request: CreatePanelRequest): PanelId =
        executeRequest("POST", "/v2/panels", request)

    override suspend fun getPanel(panelId: String): PanelRecord =
        executeRequest("GET", "/v2/panels/$panelId")

    override suspend fun updatePanel(panelId: String, request: UpdatePanelRequest): MenuVersion =
        executeRequest("PUT", "/v2/panels/$panelId", request)

    override suspend fun updatePanelTargets(panelId: String, request: PanelTargetRequest) {
        executeRequest<Unit>("PUT", "/v2/panels/$panelId/target", request)
    }

    override suspend fun deletePanel(panelId: String) {
        executeRequest<Unit>("DELETE", "/v2/panels/$panelId")
    }

    override suspend fun prepareGroupUpload(groupOpenId: String, request: UploadPrepareRequest): UploadPrepareResponse =
        executeRequest("POST", "/v2/groups/$groupOpenId/upload_prepare", request)

    override suspend fun finishGroupUpload(groupOpenId: String, request: UploadPartFinishRequest) {
        executeRequest<Unit>("POST", "/v2/groups/$groupOpenId/upload_part_finish", request)
    }

    override suspend fun prepareC2CUpload(userOpenId: String, request: UploadPrepareRequest): UploadPrepareResponse =
        executeRequest("POST", "/v2/users/$userOpenId/upload_prepare", request)

    override suspend fun finishC2CUpload(userOpenId: String, request: UploadPartFinishRequest) {
        executeRequest<Unit>("POST", "/v2/users/$userOpenId/upload_part_finish", request)
    }

    private fun cursorQuery(cursor: String?): Map<String, String> =
        cursor?.let { mapOf("cursor" to it) } ?: emptyMap()

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
        executeRequest<Unit>("PUT", "/interactions/$interactionId", request)
    }

    override suspend fun getWssUrl(): WssUrlResponse {
        return executeRequest("GET", "/gateway")
    }

    override suspend fun getWssBotUrl(): WssUrlResponse {
        return executeRequest("GET", "/gateway/bot")
    }
}
