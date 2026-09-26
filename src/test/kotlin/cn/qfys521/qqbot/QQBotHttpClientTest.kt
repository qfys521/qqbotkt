package cn.qfys521.qqbot

import cn.qfys521.qqbot.auth.AccessTokenManager
import cn.qfys521.qqbot.config.QQBotConfig
import cn.qfys521.qqbot.http.QQBotHttpClient
import cn.qfys521.qqbot.model.api.ShareLinkResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
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
        val apiClient = HttpClient(MockEngine { request ->
            requests += request.url.toString()
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
                else -> respond("", HttpStatusCode.NoContent)
            }
        }) { install(ContentNegotiation) { json(this@QQBotHttpClientTest.json) } }
        val config = QQBotConfig("app", "secret", customBaseUrl = "https://api.test")
        val api = QQBotHttpClient(config, AccessTokenManager(config, tokenClient), apiClient)

        val result: ShareLinkResponse = api.generateShareLink()
        api.getGroupMembers("group", "a cursor&space")
        api.sendC2CStreamMessage("user", cn.qfys521.qqbot.model.message.SendMessageRequest(content = "hello"))
        api.putInteractionResponse("interaction")

        assertEquals("https://example.test/invite", result.data.url)
        assertTrue(requests.any { it == "https://api.test/v2/groups/group/members?cursor=a%20cursor%26space" })
        assertTrue(requests.any { it == "https://api.test/v2/users/user/stream_messages" })
        assertTrue(requests.any { it == "https://api.test/interactions/interaction" })
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
            if (apiRequests == 1) respond("{\"code\":11243,\"message\":\"expired\"}", HttpStatusCode.Unauthorized)
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
}
