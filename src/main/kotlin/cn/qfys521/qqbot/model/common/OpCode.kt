package cn.qfys521.qqbot.model.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * QQ OpenAPI 标准接口请求报错或者鉴权拒绝时服务器返回的数据包结构。
 *
 * @property errCode 平台下发的错误编号（例如 `11243` 代表凭据无效或过期）。
 * @property message 具体的错误说明文本。
 * @property traceId 平台服务的 TraceID，用于查验工单排障。
 */
@Serializable
data class ApiErrorResponse(
    @SerialName("errcode") val errCode: Int = 0,
    val message: String = "",
    @SerialName("trace_id") val traceId: String? = null
)

/**
 * QQ 机器人官方 WebSocket 网关操作码常量定义体 (`OpCode`)。
 *
 * 用于标识当前传输报文的作用类型（如业务心跳、登录鉴权、会话续约等）。
 */
object OpCode {
    /** 服务端下发：表示这是一个事件派发推流帧。 */
    const val DISPATCH: Int = 0
    /** 客户端/服务端：心跳交互报文。 */
    const val HEARTBEAT: Int = 1
    /** 客户端发出：初始化鉴权连接 (`Identify`)。 */
    const val IDENTIFY: Int = 2
    /** 客户端发出：恢复会话请求 (`Resume`)。 */
    const val RESUME: Int = 6
    /** 服务端下发：要求客户端立即断开当前 Socket 并进行平滑重连 (`Reconnect`)。 */
    const val RECONNECT: Int = 7
    /** 服务端下发：当前会话 ID `SessionId` 已无效，必须重新 `Identify`。 */
    const val INVALID_SESSION: Int = 9
    /** 服务端向全新接入者下发的欢迎报文，声明周期心跳时间间隔 `heartbeat_interval`。 */
    const val HELLO: Int = 10
    /** 服务端响应客户端的心跳答复 (`Heartbeat ACK`)。 */
    const val HEARTBEAT_ACK: Int = 11
}

/**
 * 官方 WebSocket 网关连接事件意图掩码常量合集 (`Intents`)。
 *
 * 使用按位或 (`or` / `|`) 计算多种订阅权限的组合掩码。
 */
object Intent {
    /** 监听公有大频道全域基础管理事件 (GUILDS)。 */
    const val GUILDS: Int = 1 shl 0
    /** 监听大频道内全体成员进出变动事件 (GUILD_MEMBERS)。 */
    const val GUILD_MEMBERS: Int = 1 shl 1
    /** 监听频道内全部普通文本及附件发信事件 (GUILD_MESSAGES - 私有特权专用)。 */
    const val GUILD_MESSAGES: Int = 1 shl 9
    /** 监听公有频道和子频道 @ 机器人消息事件 (GUILD_AT_MESSAGES)。 */
    const val GUILD_AT_MESSAGES: Int = 1 shl 30
    /** 监听用户在群聊里 @ 机器人或者发送一对一私聊事件 (PUBLIC_GUILD_MESSAGES / USER_MESSAGES)。 */
    const val PUBLIC_GUILD_MESSAGES: Int = 1 shl 30
    /** 监听内嵌按钮互动点击与菜单操作事件 (INTERACTION)。 */
    const val INTERACTION: Int = 1 shl 26
    /** 监听群聊与单聊私信事件掩码 (GROUP_AND_C2C_EVENT)。 */
    const val GROUP_AND_C2C_EVENT: Int = 1 shl 25

    /**
     * 针对公域普通推荐的常规默认开箱集齐配置掩码：
     * 包含频道生命周期、群 @ 机器人与私信会话及交互回调。
     */
    const val DEFAULT_PUBLIC_INTENTS: Int = GUILDS or GUILD_MEMBERS or GUILD_AT_MESSAGES or INTERACTION or GROUP_AND_C2C_EVENT

    /**
     * 组合多个 Intent 位掩码为一个综合掩码 Int。
     *
     * @param intents 要并联订阅的位掩码合集。
     * @return 组合后的掩码 Int。
     */
    fun combine(vararg intents: Int): Int {
        var result = 0
        for (intent in intents) {
            result = result or intent
        }
        return result
    }
}

/**
 * QQ 机器人网关长连接分片策略配置描述类 (`Shard`)。
 *
 * 如果机器人业务服务需承载大规模高并发流量，建议采用多进程/多实例连接多个不同分片，
 * 以便分担并水平扩展事件处理吞吐能力。
 *
 * @property shardId 当前实例要承载的具体分片下标（从 `0` 开始）。
 * @property totalShards 集群计划划分的全局分片总数（最少为 `1`）。
 */
data class ShardConfig(
    val shardId: Int = 0,
    val totalShards: Int = 1
) {
    companion object {
        /**
         * 默认单分片单机规范 (`[0, 1]`)。
         */
        val DEFAULT = ShardConfig(0, 1)
    }

    /**
     * 转换为官方网关 `IdentifyData.shard` 参数接受的两位整型数组表达：`[shardId, totalShards]`。
     */
    fun toList(): List<Int> = listOf(shardId, totalShards)
}
