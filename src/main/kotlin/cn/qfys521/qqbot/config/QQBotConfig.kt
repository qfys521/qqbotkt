package cn.qfys521.qqbot.config

import cn.qfys521.qqbot.model.common.Intent
import cn.qfys521.qqbot.model.common.ShardConfig

/**
 * QQ 机器人 SDK 运行全局配置选项实体类。
 *
 * 用于统一声明和保管鉴权参数、网络选项、网关重连与分片和订阅事件意图集合。
 *
 * @property appId 在 QQ 开放平台管理端获得的开发者机器人 AppID。
 * @property clientSecret 在 QQ 开放平台管理端获得的开发者机器人 ClientSecret（高密鉴权凭证）。
 * @property sandbox 是否开启沙箱开发测试环境（若设为 `true`，SDK 将自动连入 `https://sandbox.api.bot.qq.com` 路径）。
 * @property intents 事件订阅 Intents 位掩码，默认为公开事件推荐合集 [Intent.DEFAULT_PUBLIC_INTENTS]。
 * @property shard 集群分片参数配置，默认为不分片规则 [ShardConfig.DEFAULT]（即 `[0, 1]`）。
 * @property requestTimeoutMillis API 单个 HTTP 网络请求超时等待时间（默认 `10000L` 毫秒）。
 * @property maxRetries 遇到接口限频 (429) 或临时性网络 IO 异常时的指数退避最大可重试次数（默认 `3` 次）。
 * @property autoReconnect 当 WebSocket 网关意外挂断或收到远端重连信号时，是否自动进行连接重连与凭证 Resume 会话恢复（默认 `true`）。
 * @property customBaseUrl 用户可指定用于内网代理或专线中转测试的自定义根服务地址（当设置时将优于 [sandbox] 设置被选用）。
 */
data class QQBotConfig(
    var appId: String = "",
    var clientSecret: String = "",
    var sandbox: Boolean = false,
    var intents: Int = Intent.GROUP_AND_C2C_EVENT,
    var shard: ShardConfig = ShardConfig.DEFAULT,
    var requestTimeoutMillis: Long = 10_000L,
    var maxRetries: Int = 3,
    var autoReconnect: Boolean = true,
    var customBaseUrl: String? = null
) {
    /**
     * 智能获取当前激活生效的 OpenAPI 根服务地址。
     */
    val baseUrl: String
        get() = customBaseUrl ?: if (sandbox) {
            "https://sandbox.api.bot.qq.com"
        } else {
            "https://api.bot.qq.com"
        }

    /**
     * QQ 机器人 AccessToken 鉴权接口请求根路径 (`https://api.bot.qq.com/app/getAppAccessToken`)。
     */
    val tokenUrl: String = "https://api.bot.qq.com/app/getAppAccessToken"

    /**
     * 针对当前实例进行基础必填项合规自检。
     *
     * @throws IllegalArgumentException 当 `appId` 或 `clientSecret` 存在空白或未配置时抛出异常。
     */
    fun validate() {
        require(appId.isNotBlank()) { "appId 不能为空！请检查 QQBotConfig.appId 是否正确填写。" }
        require(clientSecret.isNotBlank()) { "clientSecret 不能为空！请检查 QQBotConfig.clientSecret 是否正确填写。" }
    }
}
