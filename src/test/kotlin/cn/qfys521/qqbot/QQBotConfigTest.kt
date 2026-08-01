package cn.qfys521.qqbot

import cn.qfys521.qqbot.config.QQBotConfig
import cn.qfys521.qqbot.model.common.Intent
import cn.qfys521.qqbot.model.common.ShardConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QQBotConfigTest {

    @Test
    fun testDefaultConfigUrl() {
        val config = QQBotConfig(appId = "12345", clientSecret = "secret")
        assertFalse(config.sandbox)
        assertEquals("https://api.bot.qq.com", config.baseUrl)
        assertEquals("https://bots.qq.com/app/getAppAccessToken", config.tokenUrl)
    }

    @Test
    fun testSandboxUrl() {
        val config = QQBotConfig(appId = "12345", clientSecret = "secret", sandbox = true)
        assertTrue(config.sandbox)
        assertEquals("https://sandbox.api.bot.qq.com", config.baseUrl)
    }

    @Test
    fun testCustomBaseUrl() {
        val config = QQBotConfig(appId = "12345", clientSecret = "secret", customBaseUrl = "https://my-proxy.com")
        assertEquals("https://my-proxy.com", config.baseUrl)
    }

    @Test
    fun testValidationFailure() {
        val emptyAppId = QQBotConfig(appId = "", clientSecret = "secret")
        assertThrows<IllegalArgumentException> {
            emptyAppId.validate()
        }

        val emptySecret = QQBotConfig(appId = "12345", clientSecret = "")
        assertThrows<IllegalArgumentException> {
            emptySecret.validate()
        }
    }

    @Test
    fun testIntentsCombination() {
        val combined = Intent.combine(Intent.GUILDS, Intent.PUBLIC_GUILD_MESSAGES, Intent.GROUP_AND_C2C_EVENT)
        assertTrue((combined and Intent.GUILDS) != 0)
        assertTrue((combined and Intent.PUBLIC_GUILD_MESSAGES) != 0)
        assertTrue((combined and Intent.GROUP_AND_C2C_EVENT) != 0)
    }

    @Test
    fun testShardConfig() {
        val shard = ShardConfig(0, 4)
        assertEquals(listOf(0, 4), shard.toList())
    }
}
