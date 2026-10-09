package cn.qfys521.qqbot

import cn.qfys521.qqbot.gateway.QQBotGateway
import cn.qfys521.qqbot.model.gateway.GatewayPayload
import cn.qfys521.qqbot.model.gateway.HelloData
import cn.qfys521.qqbot.model.api.GuildMemberWithGuildId
import cn.qfys521.qqbot.model.api.MessageAudited
import cn.qfys521.qqbot.model.api.MessageDelete
import cn.qfys521.qqbot.model.api.MessageReaction
import cn.qfys521.qqbot.model.guild.Channel
import cn.qfys521.qqbot.model.guild.Guild
import cn.qfys521.qqbot.model.interaction.Interaction
import cn.qfys521.qqbot.model.message.Message
import cn.qfys521.qqbot.model.message.MessageMarkdown
import cn.qfys521.qqbot.model.message.MessageMarkdownParam
import cn.qfys521.qqbot.model.message.SendMessageRequest
import cn.qfys521.qqbot.model.message.StreamMessageRequest
import cn.qfys521.qqbot.model.user.GuildItem
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

    @Test
    fun documentedGuildOwnerAndStreamRequestFieldsAreMapped() {
        val guild = json.decodeFromString<GuildItem>("""{"id":"g1","owner":true}""")
        assertTrue(guild.isOwner)

        val serialized = json.encodeToString(
            StreamMessageRequest(inputMode = "replace", inputState = 10, index = 2, contentType = "markdown", contentRaw = "done")
        )
        assertTrue(serialized.contains("\"input_mode\":\"replace\""))
        assertTrue(serialized.contains("\"input_state\":10"))
        assertTrue(serialized.contains("\"content_raw\":\"done\""))
        assertTrue(!serialized.contains("null"))
    }

    @Test
    fun documentedEventPayloadFieldsAreRetained() {
        val guild = json.decodeFromString<Guild>("""{"id":"g1","op_user_id":"operator"}""")
        val channel = json.decodeFromString<Channel>("""{"id":"c1","guild_id":"g1","op_user_id":"operator"}""")
        val member = json.decodeFromString<GuildMemberWithGuildId>("""{"guild_id":"g1","op_user_id":"operator","user":{"id":"u1"}}""")
        val interaction = json.decodeFromString<Interaction>(
            """{"type":11,"group_member_openid":"member-1","data":{"type":11,"resolved":{"button_id":"b1"}}}"""
        )
        val message = json.decodeFromString<Message>(
            """{"id":"m1","edited_timestamp":"2026-01-02T03:04:05Z","mention_everyone":true,"embeds":[{"title":"title"}],"member":{"nick":"nick"},"seq":7,"seq_in_channel":"8","message_reference":{"message_id":"m0"}}"""
        )
        val reaction = json.decodeFromString<MessageReaction>(
            """{"user_id":"u1","target":{"id":"m1","type":0},"emoji":{"id":"277","type":1}}"""
        )
        val deleted = json.decodeFromString<MessageDelete>(
            """{"message":{"id":"m1"},"op_user":{"id":"u1"}}"""
        )
        val audited = json.decodeFromString<MessageAudited>(
            """{"audit_id":"a1","message_id":"m1","seq_in_channel":"8"}"""
        )

        assertEquals("operator", guild.opUserId)
        assertEquals("operator", channel.opUserId)
        assertEquals("operator", member.opUserId)
        assertEquals("member-1", interaction.groupMemberOpenId)
        assertEquals(11, interaction.data?.type)
        assertTrue(interaction.data?.resolved.toString().contains("\"button_id\":\"b1\""))
        assertTrue(message.mentionEveryone == true)
        assertEquals("title", message.embeds?.firstOrNull()?.title)
        assertEquals("nick", message.member?.nick)
        assertEquals(7L, message.seq)
        assertEquals("8", message.seqInChannel)
        assertEquals("m0", message.messageReference?.messageId)
        assertEquals("\"277\"", reaction.emoji?.id.toString())
        assertEquals("u1", deleted.opUser?.id)
        assertEquals("8", audited.seqInChannel)
    }

    @Test
    fun markdownModelSupportsTemplatesWithoutEmittingConflictingEmptyParams() {
        val rawMarkdown = json.encodeToString(MessageMarkdown(content = "# title"))
        assertTrue(rawMarkdown.contains("\"content\":\"# title\""))
        assertTrue(!rawMarkdown.contains("\"params\""))

        val templatedMarkdown = json.encodeToString(
            MessageMarkdown(
                customTemplateId = "template-1",
                params = listOf(MessageMarkdownParam(key = "title", values = listOf("Hello")))
            )
        )
        assertTrue(templatedMarkdown.contains("\"custom_template_id\":\"template-1\""))
        assertTrue(templatedMarkdown.contains("\"params\":[{"))
    }
}
