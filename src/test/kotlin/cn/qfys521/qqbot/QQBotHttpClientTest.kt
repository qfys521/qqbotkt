package cn.qfys521.qqbot

import cn.qfys521.qqbot.auth.AccessTokenManager
import cn.qfys521.qqbot.config.QQBotConfig
import cn.qfys521.qqbot.http.QQBotHttpClient
import cn.qfys521.qqbot.model.api.ChannelMessageImageFile
import cn.qfys521.qqbot.model.api.ChannelMessageSendRequest
import cn.qfys521.qqbot.model.api.RemoveGuildMemberRequest
import cn.qfys521.qqbot.model.api.ShareLinkResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpHeaders
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QQBotHttpClientTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `uses documented routes and encodes query parameters`() = runTest {
        val tokenClient = HttpClient(MockEngine { request ->
            respond("{\"access_token\":\"token\",\"expires_in\":7200}",
                HttpStatusCode.OK, headers = io.ktor.http.headersOf("Content-Type", "application/json"))
        }) { install(ContentNegotiation) { json(this@QQBotHttpClientTest.json) } }
        val requests = mutableListOf<String>()
        val requestBodies = mutableMapOf<String, Any>()
        val contentTypes = mutableMapOf<String, String?>()
        val apiClient = HttpClient(MockEngine { request ->
            requests += request.url.toString()
            requestBodies[request.url.encodedPath] = request.body
            contentTypes[request.url.encodedPath] =
                request.body.contentType?.toString() ?: request.headers[HttpHeaders.ContentType]
            when {
                request.url.encodedPath == "/v2/generate_url_link" -> respond(
                    "{\"data\":{\"url\":\"https://example.test/invite\"}}",
                    HttpStatusCode.OK, headers = io.ktor.http.headersOf("Content-Type", "application/json")
                )
                request.url.encodedPath == "/v2/groups/group/members" -> respond(
                    "{\"members\":[],\"next_cursor\":\"\"}", HttpStatusCode.OK,
                    headers = io.ktor.http.headersOf("Content-Type", "application/json")
                )
                request.url.encodedPath == "/v2/users/user/stream_messages" -> respond(
                    "{\"id\":\"stream-1\"}", HttpStatusCode.OK,
                    headers = io.ktor.http.headersOf("Content-Type", "application/json")
                )
                request.url.encodedPath == "/users/@me/guilds" -> respond(
                    "[]", HttpStatusCode.OK, headers = io.ktor.http.headersOf("Content-Type", "application/json")
                )
                request.url.encodedPath == "/channels/channel/pins/message" -> respond(
                    "{\"guild_id\":\"guild\",\"channel_id\":\"channel\",\"message_ids\":[\"message\"]}",
                    HttpStatusCode.OK, headers = io.ktor.http.headersOf("Content-Type", "application/json")
                )
                request.url.encodedPath == "/channels/channel/messages" -> respond(
                    """{"id":"channel-message","attachments":[{"url":"https://example.test/image.png"}],"mentions":[{"id":"user","username":"name"}],"member":{"nick":"member","roles":["role"]},"ark":{"template_id":1,"kv":[{"key":"title","value":"hello"}]},"seq":4,"seq_in_channel":"5","message_reference":{"message_id":"parent"}}""",
                    HttpStatusCode.OK,
                    headers = io.ktor.http.headersOf("Content-Type", "application/json")
                )
                else -> respond("", HttpStatusCode.NoContent)
            }
        }) { install(ContentNegotiation) { json(this@QQBotHttpClientTest.json) } }
        val config = QQBotConfig("app", "secret", customBaseUrl = "https://api.test")
        val api = QQBotHttpClient(config, AccessTokenManager(config, tokenClient), apiClient)

        val result: ShareLinkResponse = api.generateShareLink()
        api.getGroupMembers("group", "a cursor&space")
        api.sendC2CStreamMessage("user", cn.qfys521.qqbot.model.message.StreamMessageRequest(
            inputState = 10,
            index = 0,
            contentType = "text",
            contentRaw = "hello"
        ))
        api.putInteractionResponse("interaction")
        api.getMyGuilds(before = "last id/+", after = "first?", limit = 10)
        api.removeGuildMember(
            "guild/id",
            "user id",
            RemoveGuildMemberRequest(addBlacklist = true, deleteHistoryMsgDays = 3)
        )
        val pins = api.addChannelPin("channel", "message")
        val sentMessage = api.sendChannelMessage(
            "channel",
            ChannelMessageSendRequest(content = "file caption"),
            ChannelMessageImageFile("photo.png", byteArrayOf(1, 2, 3), "image/png")
        )

        assertEquals("https://example.test/invite", result.data.url)
        assertTrue(requests.any { it == "https://api.test/v2/groups/group/members?cursor=a%20cursor%26space" })
        assertTrue(requests.any { it == "https://api.test/v2/users/user/stream_messages" })
        assertTrue(requests.any { it == "https://api.test/interactions/interaction" })
        assertTrue(requests.any { it == "https://api.test/users/@me/guilds?before=last%20id%2F%2B&after=first%3F&limit=10" })
        assertTrue(requestBodies.containsKey("/guilds/guild%2Fid/members/user%20id"), requestBodies.keys.toString())
        val deleteBody = requestBodies["/guilds/guild%2Fid/members/user%20id"] as OutgoingContent.ByteArrayContent
        val deleteJson = deleteBody.bytes().decodeToString()
        assertTrue(deleteJson.contains("\"add_blacklist\":true"))
        assertTrue(deleteJson.contains("\"delete_history_msg_days\":3"))
        assertEquals(listOf("message"), pins.messageIds)
        assertEquals("https://example.test/image.png", sentMessage.attachments.single().url)
        assertEquals("user", sentMessage.mentions.single().id)
        assertEquals("member", sentMessage.member?.nick)
        assertEquals(1, sentMessage.ark?.templateId)
        assertEquals("5", sentMessage.seqInChannel)
        assertEquals("parent", sentMessage.messageReference?.messageId)
        assertEquals("multipart/form-data", contentTypes["/channels/channel/messages"]?.substringBefore(';'))
        assertTrue(requestBodies["/channels/channel/messages"] is MultiPartFormDataContent)
        tokenClient.close()
        apiClient.close()
    }

    @Test
    fun `refreshes token once after unauthorized response`() = runTest {
        var tokenRequests = 0
        var apiRequests = 0
        val tokenClient = HttpClient(MockEngine {
            tokenRequests++
            respond("{\"access_token\":\"token$tokenRequests\",\"expires_in\":7200}",
                HttpStatusCode.OK, headers = io.ktor.http.headersOf("Content-Type", "application/json"))
        }) { install(ContentNegotiation) { json(this@QQBotHttpClientTest.json) } }
        val apiClient = HttpClient(MockEngine {
            apiRequests++
            if (apiRequests == 1) respond("{\"message\":\"unauthorized\"}", HttpStatusCode.Unauthorized)
            else respond("{\"id\":\"message-1\"}", HttpStatusCode.OK, headers = io.ktor.http.headersOf("Content-Type", "application/json"))
        }) { install(ContentNegotiation) { json(this@QQBotHttpClientTest.json) } }
        val config = QQBotConfig("app", "secret", customBaseUrl = "https://api.test")
        val api = QQBotHttpClient(config, AccessTokenManager(config, tokenClient), apiClient)

        val result = api.sendC2CMessage("user", cn.qfys521.qqbot.model.message.SendMessageRequest(content = "hello"))

        assertEquals("message-1", result.id)
        assertEquals(2, tokenRequests)
        assertEquals(2, apiRequests)
        tokenClient.close()
        apiClient.close()
    }

    @Test
    fun `does not refresh for explicit invalid token business error`() = runTest {
        var tokenRequests = 0
        var apiRequests = 0
        val tokenClient = HttpClient(MockEngine {
            tokenRequests++
            respond("{\"access_token\":\"token$tokenRequests\",\"expires_in\":7200}",
                HttpStatusCode.OK, headers = io.ktor.http.headersOf("Content-Type", "application/json"))
        }) { install(ContentNegotiation) { json(this@QQBotHttpClientTest.json) } }
        val apiClient = HttpClient(MockEngine {
            apiRequests++
            respond("{\"err_code\":11243,\"message\":\"invalid token\"}", HttpStatusCode.Unauthorized)
        }) { install(ContentNegotiation) { json(this@QQBotHttpClientTest.json) } }
        val config = QQBotConfig("app", "secret", customBaseUrl = "https://api.test")
        val api = QQBotHttpClient(config, AccessTokenManager(config, tokenClient), apiClient)

        val error = kotlin.test.assertFailsWith<cn.qfys521.qqbot.exception.QQBotApiException> { api.getMe() }

        assertEquals(11243, error.errCode)
        assertEquals(1, tokenRequests)
        assertEquals(1, apiRequests)
        tokenClient.close()
        apiClient.close()
    }
}
