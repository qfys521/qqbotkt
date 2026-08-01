package cn.qfys521.qqbot

import cn.qfys521.qqbot.dsl.message
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MessageBuilderTest {

    @Test
    fun testSimpleTextMessage() {
        val req = message {
            content = "Hello World"
            msgId = "msg-123"
            msgSeq = 1
        }
        assertEquals("Hello World", req.content)
        assertEquals(0, req.msgType)
        assertEquals("msg-123", req.msgId)
        assertEquals(1, req.msgSeq)
        assertNull(req.markdown)
        assertNull(req.keyboard)
    }

    @Test
    fun testMarkdownMessage() {
        val req = message {
            markdown("# 标题\n这是一段 Markdown 文本")
            reference("ref-456")
        }
        assertEquals(2, req.msgType)
        assertNotNull(req.markdown)
        assertEquals("# 标题\n这是一段 Markdown 文本", req.markdown?.content)
        assertEquals("ref-456", req.messageReference?.messageId)
    }

    @Test
    fun testKeyboardMessage() {
        val req = message {
            content = "请选择一个选项："
            keyboard {
                row {
                    urlButton(id = "btn-web", label = "官方网站", url = "https://q.qq.com", style = 1)
                    commandButton(id = "btn-cmd", label = "再来一次", commandText = "/start", style = 0)
                }
                row {
                    callbackButton(id = "btn-cb", label = "同意授权", callbackData = "action=agree", style = 3)
                }
            }
        }
        assertNotNull(req.keyboard)
        val rows = req.keyboard?.content?.rows
        assertNotNull(rows)
        assertEquals(2, rows.size)
        assertEquals(2, rows[0].buttons?.size)
        assertEquals("官方网站", rows[0].buttons?.get(0)?.renderData?.label)
        assertEquals("https://q.qq.com", rows[0].buttons?.get(0)?.action?.data)
        assertEquals(0, rows[0].buttons?.get(0)?.action?.type)
        assertEquals(1, rows[1].buttons?.size)
        assertEquals(1, rows[1].buttons?.get(0)?.action?.type)
    }

    @Test
    fun testMediaMessage() {
        val req = message {
            media("file-uuid-xxx")
        }
        assertEquals(7, req.msgType)
        assertEquals("file-uuid-xxx", req.media?.fileInfo)
    }
}
