package cn.qfys521.qqbot

import cn.qfys521.qqbot.auth.AccessTokenManager
import cn.qfys521.qqbot.config.QQBotConfig
import cn.qfys521.qqbot.exception.QQBotApiException
import cn.qfys521.qqbot.http.QQBotHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QQBotApiErrorResponseTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `maps documented err_code response into api exception`() = runTest {
        val tokenClient = HttpClient(MockEngine {
            respond(
                "{\"access_token\":\"token\",\"expires_in\":7200}",
                HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json")
            )
        }) { install(ContentNegotiation) { json(this@QQBotApiErrorResponseTest.json) } }
        val apiClient = HttpClient(MockEngine {
            respond(
                "{\"err_code\":11253,\"message\":\"permission denied\",\"trace_id\":\"trace-123\",\"data\":{\"reason\":\"approval required\"}}",
                HttpStatusCode.Forbidden
            )
        }) { install(ContentNegotiation) { json(this@QQBotApiErrorResponseTest.json) } }
        val config = QQBotConfig("app", "secret", customBaseUrl = "https://api.test")
        val api = QQBotHttpClient(config, AccessTokenManager(config, tokenClient), apiClient)

        val error = assertFailsWith<QQBotApiException> { api.getMe() }

        assertEquals(11253, error.errCode)
        assertEquals("permission denied", error.errMessage)
        assertEquals("trace-123", error.traceId)
        assertEquals(403, error.httpStatusCode)
        assertEquals("approval required", error.errorData?.let { (it as kotlinx.serialization.json.JsonObject)["reason"]?.toString()?.trim('"') })
        tokenClient.close()
        apiClient.close()
    }

    @Test
    fun `preserves audit data when async response is accepted`() = runTest {
        val tokenClient = HttpClient(MockEngine {
            respond(
                "{\"access_token\":\"token\",\"expires_in\":7200}",
                HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json")
            )
        }) { install(ContentNegotiation) { json(this@QQBotApiErrorResponseTest.json) } }
        val apiClient = HttpClient(MockEngine {
            respond(
                "{\"code\":304023,\"message\":\"push message is waiting for audit now\",\"data\":{\"message_audit\":{\"audit_id\":\"audit-1\"}}}",
                HttpStatusCode.Accepted
            )
        }) { install(ContentNegotiation) { json(this@QQBotApiErrorResponseTest.json) } }
        val config = QQBotConfig("app", "secret", customBaseUrl = "https://api.test")
        val api = QQBotHttpClient(config, AccessTokenManager(config, tokenClient), apiClient)

        val error = assertFailsWith<QQBotApiException> { api.getMe() }

        assertEquals(304023, error.errCode)
        assertEquals(true, error.isAsyncOperation)
        assertEquals("audit-1", error.errorData?.jsonObject?.get("message_audit")?.jsonObject?.get("audit_id")?.toString()?.trim('"'))
        tokenClient.close()
        apiClient.close()
    }
}
