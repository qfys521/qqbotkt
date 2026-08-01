package cn.qfys521.qqbot

import cn.qfys521.qqbot.gateway.QQBotGateway
import cn.qfys521.qqbot.model.gateway.GatewayPayload
import cn.qfys521.qqbot.model.gateway.HelloData
import cn.qfys521.qqbot.model.message.Message
import cn.qfys521.qqbot.model.message.SendMessageRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromJsonElement
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ModelSerializationTest {

    private val json = QQBotGateway.defaultJson

    @Test
    fun testHelloPayloadDeserialization() {
        val helloJson = """
            {
              "op": 10,
              "d": {
                "heartbeat_interval": 45000
              }
            }
        """.trimIndent()

        val payload = json.decodeFromString<GatewayPayload>(helloJson)
        assertEquals(10, payload.op)
        assertNotNull(payload.d)
        val helloData = json.decodeFromJsonElement<HelloData>(payload.d)
        assertEquals(45000L, helloData.heartbeatInterval)
    }

    @Test
    fun testGroupAtMessagePayloadDeserialization() {
        val dispatchJson = """
            {
              "id": "event_123456",
              "op": 0,
              "s": 100,
              "t": "GROUP_AT_MESSAGE_CREATE",
              "d": {
                "id": "msg_abc123",
                "author": {
                  "id": "A1B2C3D4",
                  "member_openid": "A1B2C3D4",
                  "username": "测试用户"
                },
                "content": "你好，测试机器人",
                "group_openid": "G12345678",
                "timestamp": "2026-08-01T21:00:00+08:00"
              }
            }
        """.trimIndent()

        val payload = json.decodeFromString<GatewayPayload>(dispatchJson)
        assertEquals(0, payload.op)
        assertEquals(100L, payload.s)
        assertEquals("GROUP_AT_MESSAGE_CREATE", payload.t)
        assertNotNull(payload.d)

        val msg = json.decodeFromJsonElement<Message>(payload.d)
        assertEquals("msg_abc123", msg.id)
        assertEquals("测试用户", msg.author.username)
        assertEquals("A1B2C3D4", msg.author.openId)
        assertEquals("你好，测试机器人", msg.content)
        assertEquals("G12345678", msg.groupOpenId)
    }

    @Test
    fun testSendMessageRequestSerialization() {
        val req = SendMessageRequest(
            content = "Hello",
            msgType = 0,
            msgId = "msg_123",
            msgSeq = 1
        )
        val serialized = json.encodeToString(req)
        assertTrue(serialized.contains("\"content\":\"Hello\""))
        assertTrue(serialized.contains("\"msg_type\":0"))
        assertTrue(serialized.contains("\"msg_id\":\"msg_123\""))
        assertTrue(serialized.contains("\"msg_seq\":1"))
    }
}
