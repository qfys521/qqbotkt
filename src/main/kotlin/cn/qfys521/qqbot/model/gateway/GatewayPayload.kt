package cn.qfys521.qqbot.model.gateway

import cn.qfys521.qqbot.model.user.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * 平台网关接入点基础地址与分片配额信息返回类。
 *
 * 通过调用 `GET /gateway` 或 `GET /gateway/bot` 获得。
 *
 * @property url 可用于 WebSocket 连接的标准 wss/ws 统一协议地址。
 * @property shards 建议或配置支持的推荐分片数目。
 * @property sessionStartLimit 当前账号的网关鉴权及会话建立限频限制规则说明。
 */
@Serializable
data class WssUrlResponse(
    val url: String = "",
    val shards: Int = 1,
    @SerialName("session_start_limit") val sessionStartLimit: SessionStartLimit? = null
)

/**
 * 限制凭证在某周期内发起 Identify 连接的上限和剩余次数。
 *
 * @property total 该账号每日周期内能够执行 Identify 的总次数上限。
 * @property remaining 当前周期内剩余可用的会话初始化配额数目。
 * @property resetAfter 距离该统计周期重置释放所剩下的毫秒数。
 * @property maxConcurrency 被允许在同一个短窗口内平行进行连接鉴权的最大并发数。
 */
@Serializable
data class SessionStartLimit(
    val total: Int = 1000,
    val remaining: Int = 1000,
    @SerialName("reset_after") val resetAfter: Long = 86400000,
    @SerialName("max_concurrency") val maxConcurrency: Int = 1
)

/**
 * WebSocket 网关上下行通用数据包框架实体。
 *
 * @property op 操作指示识别码 (OpCode)，用来标明此桢处于会话建立、心跳还是事件推流。
 * @property s 服务端单向递增分发的序列号 (Sequence number)，客户端在心跳中回传并在异常断开 Resume 时用于数据定位补发。
 * @property t 事件分类标记文字 (Event type)，例如 `"GROUP_AT_MESSAGE_CREATE"` 等。
 * @property d 报文有效载荷树对象，对应各个子事件或参数的详细属性流。
 * @property id 平台针对当前这一次事件分派的唯一标记 UUID。
 */
@Serializable
data class GatewayPayload(
    val op: Int = 0,
    val s: Long? = null,
    val t: String? = null,
    val d: JsonElement? = null,
    val id: String? = null
)

/**
 * 发送会话初始认证 (OpCode 2 Identify) 报文规范体。
 *
 * @property op 固定等于 `2`。
 * @property d 鉴权参数载荷。
 */
@Serializable
data class IdentifyPayload(
    val op: Int = 2,
    val d: IdentifyData
)

/**
 * Identify 鉴权参数描述实体。
 *
 * @property token 完整的鉴权头字符串（例如 `"QQBot {accessToken}"`）。
 * @property intents 按位或 [or] 计算后的需要监听的事件类型掩码 (Intents)。
 * @property shard 分片配置数组，形如 `[shard_id, total_shards]`。
 * @property properties 客户端环境上报标记。
 */
@Serializable
data class IdentifyData(
    val token: String,
    val intents: Int,
    val shard: List<Int>,
    val properties: Map<String, String> = defaultProperties
) {
    companion object {
        val defaultProperties = mapOf(
            "\$os" to "Linux",
            "\$browser" to "QQBotKt",
            "\$device" to "QQBotKt"
        )
    }
}

/**
 * 恢复连接会话请求 (OpCode 6 Resume) 报文。
 *
 * @property op 固定等于 `6`。
 * @property d 恢复参数对象。
 */
@Serializable
data class ResumePayload(
    val op: Int = 6,
    val d: ResumeData
)

/**
 * 会话恢复传输的具体凭证与序号定位体。
 *
 * @property token 上次会话使用的 API Token。
 * @property sessionId 需要续期的已有回传会话 ID。
 * @property seq 上一次长连接中断前最终收到的有效下发报文序号 `s`。
 */
@Serializable
data class ResumeData(
    val token: String,
    @SerialName("session_id") val sessionId: String,
    val seq: Long
)

/**
 * 接到 `OpCode 10 Hello` 报文时的回包参数对象。
 *
 * @property heartbeatInterval 网关要求的必须每隔此毫秒数发送一次心跳保活的周期。
 */
@Serializable
data class HelloData(
    @SerialName("heartbeat_interval") val heartbeatInterval: Long = 45000
)

/**
 * 网关连接与鉴权全部准备完结 `READY` 事件的载体。
 *
 * @property version 网关协议数据结构当前使用的版本。
 * @property sessionId 此会话连接生成的全局识别 ID，后续重连时以它做为凭靠。
 * @property user 账号所关联认证机器人的账号完整属性。
 * @property shard 该次登录的有效分片下标序列信息。
 */
@Serializable
data class ReadyData(
    val version: Int = 1,
    @SerialName("session_id") val sessionId: String = "",
    val user: User = User(),
    val shard: List<Int>? = null
)
