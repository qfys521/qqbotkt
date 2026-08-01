package cn.qfys521.qqbot.exception

/**
 * QQ 机器人 Kotlin SDK 异常根基类。
 * 所有由本 SDK 抛出的异常类均派生自此。
 *
 * @param message 异常概况性解释。
 * @param cause 触发源底层的真实异常堆栈根源（可选）。
 */
open class QQBotException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * 平台 OpenAPI 业务交互异常。
 * 当请求成功到达平台服务，但由于鉴权不足、参数错误、频率超载等业务逻辑引发平台接口返回非成功回包时抛出。
 *
 * @property errCode QQ 开放平台下发的具体 OpenAPI 错误码（比如 `11243` 代表 Token 失效）。
 * @property errMessage 接口返回的文字报错描述解释。
 * @property traceId 服务端 Header 或报文中随同响应生成的链路日志追踪识别凭证 (`X-Tps-trace-ID`)，协助定位工单。
 * @property httpStatusCode 底层通讯关联的 HTTP Status 响应状态代码（例 `401`、`429`、`500` 等）。
 */
class QQBotApiException(
    val errCode: Int,
    val errMessage: String,
    val traceId: String? = null,
    val httpStatusCode: Int = 0
) : QQBotException(
    message = "QQ Bot API 业务错误 [httpStatus=$httpStatusCode, errCode=$errCode]: $errMessage" +
            if (traceId != null) " (traceId=$traceId)" else ""
)

/**
 * 平台长连接网关协议及帧交互处理异常。
 * 当通过 WebSocket 与开放平台网关进行长连建立、鉴权鉴别 (`Identify`)、重登 (`Resume`)、心跳交互出现断连或阻碍抛出。
 */
class QQBotGatewayException(
    message: String,
    cause: Throwable? = null
) : QQBotException(message, cause)

/**
 * 账号鉴权及 AccessToken 续期存取异常。
 * 发生在从腾讯鉴权中心申请获取或者重新签发 AccessToken 时，由于应用 AppID/ClientSecret 有误或权限被禁时抛出。
 */
class QQBotAuthException(
    message: String,
    cause: Throwable? = null
) : QQBotException(message, cause)
