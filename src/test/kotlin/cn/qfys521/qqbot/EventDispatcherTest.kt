package cn.qfys521.qqbot

import cn.qfys521.qqbot.dsl.qqBot
import cn.qfys521.qqbot.event.GroupAtMessageEvent
import cn.qfys521.qqbot.model.message.Message
import cn.qfys521.qqbot.model.user.User
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventDispatcherTest {

    @Test
    fun testEventDispatchingAndListenerDSL() = runBlocking {
        val latch = CountDownLatch(1)
        var receivedContent = ""

        val bot = qqBot {
            appId = "test-app-id"
            clientSecret = "test-secret"

            onGroupAtMessage { event ->
                receivedContent = event.message.content
                latch.countDown()
            }
        }

        val testMessage = Message(
            id = "test-msg-1",
            author = User(id = "u1", username = "tester"),
            content = "测试消息内容",
            groupOpenId = "g1001"
        )
        val event = GroupAtMessageEvent(message = testMessage)

        bot.eventDispatcher.dispatch(event)

        val completed = latch.await(3, TimeUnit.SECONDS)
        assertTrue(completed, "监听器回调应在 3 秒内执行完毕")
        assertEquals("测试消息内容", receivedContent)
    }
}
