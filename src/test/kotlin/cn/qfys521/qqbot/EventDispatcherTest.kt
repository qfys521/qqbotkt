package cn.qfys521.qqbot

import cn.qfys521.qqbot.dsl.qqBot
import cn.qfys521.qqbot.event.*
import cn.qfys521.qqbot.model.api.GuildMemberWithGuildId
import cn.qfys521.qqbot.model.api.MessageAudited
import cn.qfys521.qqbot.model.api.MessageDelete
import cn.qfys521.qqbot.model.api.MessageReaction
import cn.qfys521.qqbot.model.interaction.Interaction
import cn.qfys521.qqbot.model.message.Message
import cn.qfys521.qqbot.model.user.User
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonNull

class EventDispatcherTest {

    @Test
    fun interactionResponsesAreRestrictedToAcknowledgementTypes() = runBlocking {
        val event = InteractionCreateEvent(Interaction(id = "interaction", type = 13))

        assertFailsWith<IllegalArgumentException> { event.respond() }
    }

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

    @Test
    fun newDocumentedEventsReachTheirTypedDslListeners() = runBlocking {
        val eventCount = 26
        val latch = CountDownLatch(eventCount)
        val bot = qqBot {
            appId = "test-app-id"
            clientSecret = "test-secret"
            onMessageDelete { latch.countDown() }
            onPublicMessageDelete { latch.countDown() }
            onDirectMessageDelete { latch.countDown() }
            onGuildMemberAdd { latch.countDown() }
            onGuildMemberUpdate { latch.countDown() }
            onGuildMemberRemove { latch.countDown() }
            onMessageReactionAdd { latch.countDown() }
            onMessageReactionRemove { latch.countDown() }
            onMessageAuditPass { latch.countDown() }
            onMessageAuditReject { latch.countDown() }
            onForumThreadCreate { latch.countDown() }
            onForumThreadUpdate { latch.countDown() }
            onForumThreadDelete { latch.countDown() }
            onForumPostCreate { latch.countDown() }
            onForumPostDelete { latch.countDown() }
            onForumReplyCreate { latch.countDown() }
            onForumReplyDelete { latch.countDown() }
            onForumPublishAuditResult { latch.countDown() }
            onGroupAddRobot { latch.countDown() }
            onGroupDelRobot { latch.countDown() }
            onAudioOrLiveChannelMemberEnter { latch.countDown() }
            onAudioOrLiveChannelMemberExit { latch.countDown() }
            onAudioStart { latch.countDown() }
            onAudioFinish { latch.countDown() }
            onAudioOnMic { latch.countDown() }
            onAudioOffMic { latch.countDown() }
        }
        val member = GuildMemberWithGuildId(guildId = "guild")
        val audited = MessageAudited(auditId = "audit")
        val deleted = MessageDelete(message = Message(id = "deleted"))
        val events: List<BotEvent> = listOf(
            MessageDeleteEvent(deleted),
            PublicMessageDeleteEvent(deleted),
            DirectMessageDeleteEvent(deleted),
            GuildMemberAddEvent(member),
            GuildMemberUpdateEvent(member),
            GuildMemberRemoveEvent(member),
            MessageReactionAddEvent(MessageReaction(), JsonNull),
            MessageReactionRemoveEvent(MessageReaction(), JsonNull),
            MessageAuditPassEvent(audited, JsonNull),
            MessageAuditRejectEvent(audited, JsonNull),
            ForumThreadCreateEvent("guild", "channel", "author", JsonNull),
            ForumThreadUpdateEvent("guild", "channel", "author", JsonNull),
            ForumThreadDeleteEvent("guild", "channel", "author", JsonNull),
            ForumPostCreateEvent("guild", "channel", "author", JsonNull),
            ForumPostDeleteEvent("guild", "channel", "author", JsonNull),
            ForumReplyCreateEvent("guild", "channel", "author", JsonNull),
            ForumReplyDeleteEvent("guild", "channel", "author", JsonNull),
            ForumPublishAuditResultEvent("guild", "channel", "author", 1, 0),
            GroupAddRobotEvent("group", "operator"),
            GroupDelRobotEvent("group", "operator"),
            AudioOrLiveChannelMemberEnterEvent("guild", "channel", 2, "user"),
            AudioOrLiveChannelMemberExitEvent("guild", "channel", 2, "user"),
            AudioStartEvent(JsonNull),
            AudioFinishEvent(JsonNull),
            AudioOnMicEvent(JsonNull),
            AudioOffMicEvent(JsonNull)
        )

        events.forEach(bot.eventDispatcher::dispatch)

        assertTrue(latch.await(3, TimeUnit.SECONDS), "所有新增类型化事件都应触发其 DSL 监听器")
    }
}
