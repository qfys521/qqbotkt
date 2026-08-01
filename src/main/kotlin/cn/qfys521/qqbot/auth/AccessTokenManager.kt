package cn.qfys521.qqbot.auth

import cn.qfys521.qqbot.config.QQBotConfig
import cn.qfys521.qqbot.exception.QQBotAuthException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

/**
 * AccessToken 申请协议交互参数载体。
 *
 * @property appId 机器人 AppID。
 * @property clientSecret 机器人鉴权密钥。
 */
@Serializable
internal data class TokenRequest(
    val appId: String,
    val clientSecret: String
)

/**
 * 开放平台接口鉴权返回结果体。
 *
 * @property accessToken 平台核发下发的具体 Token 授权凭据串。
 * @property expiresIn 凭证有效生存总时长（秒），标准官方为 7200 秒（2 小时）。
 * @property errCode 接口错误码标识，`0` 表示调用顺利，其他数值代表特定平台鉴权拒绝或异常。
 * @property message 具体出错时服务器回显的说明文本说明。
 */
@Serializable
internal data class TokenResponse(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("expires_in") val expiresIn: Long = 7200,
    @SerialName("err_code") val errCode: Int = 0,
    val message: String = ""
)

/**
 * QQ 机器人 AccessToken 凭证自动申领、生命周期计算与并发保护引擎。
 *
 * 特性简介：
 * 1. 自动预留过期提前检测窗口（距离到期还剩 60 秒内时即主动判定为失效并向服务器发起更新请求）；
 * 2. 协程安全互斥锁 (Mutex) 设计：在数十百个并发网络请求遭遇统一鉴权超时过期瞬间，能够将争抢控制至仅单次请求落地，有效防范短时重发风暴；
 * 3. 具备主动重置强制更新凭据（如接口回包报错已失效）自适应处理能级。
 *
 * @property config 机器人运行环境配置。
 * @property httpClient 用以发送 API 凭证签发请求的 http 引擎。
 */
class AccessTokenManager(
    private val config: QQBotConfig,
    private val httpClient: HttpClient
) {
    private val logger = LoggerFactory.getLogger(AccessTokenManager::class.java)
    private val mutex = Mutex()

    private var cachedToken: String? = null
    /** Token 预计过期的系统毫秒时间戳 */
    private var expireTimeMillis: Long = 0L

    /**
     * 判断当前内存中持有的凭证是否有效且剩余有效时长远大于安全缓冲窗口期 (60秒)。
     */
    val isTokenValid: Boolean
        get() {
            val token = cachedToken ?: return false
            val remainingMillis = expireTimeMillis - System.currentTimeMillis()
            return token.isNotBlank() && remainingMillis > 60_000L
        }

    /**
     * 读取并获取最新的有效 AccessToken 字符串凭证。
     * 若凭证尚在有效期内将以非阻塞形式极速直接返回内存缓存；若将过期或缺失，会自动请求鉴权中心进行申请更新。
     *
     * @param forceRefresh 是否无视缓存状态直接发起新的凭证拉取命令（默认为 `false`）。
     * @return 准备完毕可直接组合于 Authorization Header 中的有效凭证字串。
     * @throws QQBotAuthException 申请失败（如网络中断或 Secret 有误）时抛出。
     */
    suspend fun getAccessToken(forceRefresh: Boolean = false): String {
        if (!forceRefresh && isTokenValid) {
            return cachedToken!!
        }
        return mutex.withLock {
            if (!forceRefresh && isTokenValid) {
                return@withLock cachedToken!!
            }
            logger.debug("正在申请或更新 QQ Bot AccessToken (appId={})...", config.appId)
            val token = fetchNewToken()
            cachedToken = token
            token
        }
    }

    /**
     * 强制无视时效缓存直接发起最新的 API Token 兑换（如发生 API 401/凭证鉴权拒绝等情况调用以实现自动恢复重试）。
     *
     * @return 刷新后得到的崭新 AccessToken 凭字。
     * @throws QQBotAuthException 若拉取异常引发抛出。
     */
    suspend fun forceRefresh(): String {
        return getAccessToken(forceRefresh = true)
    }

    private suspend fun fetchNewToken(): String {
        try {
            val response: HttpResponse = httpClient.post(config.tokenUrl) {
                contentType(ContentType.Application.Json)
                setBody(
                    TokenRequest(
                        appId = config.appId,
                        clientSecret = config.clientSecret
                    )
                )
            }
            if (!response.status.isSuccess()) {
                throw QQBotAuthException("获取 AccessToken 响应状态码非成功: httpStatus=${response.status.value}")
            }
            val body: TokenResponse = response.body()
            if (body.errCode != 0 || body.accessToken.isBlank()) {
                throw QQBotAuthException("获取 AccessToken 平台拒绝: errCode=${body.errCode}, message='${body.message}'")
            }
            expireTimeMillis = System.currentTimeMillis() + (body.expiresIn * 1000L)
            logger.info("成功获取 QQ 机器人 AccessToken, 有效期 {} 秒", body.expiresIn)
            return body.accessToken
        } catch (e: Exception) {
            if (e is QQBotAuthException) throw e
            throw QQBotAuthException("请求拉取 AccessToken 发生异常: ${e.message}", e)
        }
    }
}
