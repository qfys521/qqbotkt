package cn.qfys521.qqbot.demo

import cn.qfys521.qqbot.dsl.qqBot
import cn.qfys521.qqbot.model.common.Intent

/**
 * QQ 机器人 Kotlin SDK 完整使用指南与优雅特性示例 (Demo)。
 *
 * 运行或参考此文件，体验极简的 Kotlin 协程 DSL 与高度封装的 OpenAPI。
 */
object QQBotDemo {

    @JvmStatic
    fun main(args: Array<String>) {
        // 1. 使用 DSL 构建器初始化 QQBot
        val bot = qqBot {
            appId = System.getenv("QQ_BOT_APP_ID") ?: "YOUR_APP_ID"
            clientSecret = System.getenv("QQ_BOT_CLIENT_SECRET") ?: "YOUR_CLIENT_SECRET"
            sandbox = true // 开发调试可启用沙箱环境
            intents = Intent.DEFAULT_PUBLIC_INTENTS // 默认订阅推荐的公开事件合集
            autoReconnect = true // 断开时自动通过 Resume / Reconnect 恢复连接与会话

            // 2. 监听网关连接成功事件 (READY)
            onReady { event ->
                println(">>> [READY] 机器人成功登录！当前昵称: ${event.data.user.username}, 会话ID: ${event.data.sessionId}")
            }

            // 3. 监听群聊 @ 机器人消息事件 (GROUP_AT_MESSAGE_CREATE)
            onGroupAtMessage { event ->
                val content = event.message.content.trim()
                val authorName = event.authorName
                println(">>> 收到群 [@机器人] 消息 | 来自: $authorName | 内容: $content")

                when {
                    content == "ping" -> {
                        // 3.1 极简响应文本
                        event.reply("pong! 你好 $authorName，我在正常运行中~")
                    }

                    content == "菜单" -> {
                        // 3.2 使用 Markdown 与交互键盘按钮 DSL
                        event.reply {
                            markdown(
                                content = """
                                    # 🤖 功能指南菜单
                                    请点击下方按钮体验 SDK 交互能力：
                                """.trimIndent()
                            )
                            keyboard {
                                row {
                                    urlButton("btn_web", "官网开发文档", "https://bot.q.qq.com", style = 1)
                                    commandButton("btn_cmd", "查天气", "/天气 北京", style = 0)
                                }
                                row {
                                    callbackButton("btn_cb", "点我打卡", "action=checkin", style = 3)
                                }
                            }
                        }
                    }

                    content.startsWith("/天气") -> {
                        // 3.3 带引用回复
                        event.reply("正在查询天气中... (示例)", msgSeq = 1)
                    }

                    else -> {
                        event.reply("收到你的消息: $content")
                    }
                }
            }

            // 4. 监听单聊私信消息 (C2C_MESSAGE_CREATE)
            onC2CMessage { event ->
                println(">>> 收到私信消息 | 来自: ${event.authorName} | 内容: ${event.message.content}")
                event.reply("收到私信啦！谢谢你发送：${event.message.content}")
            }

            // 5. 监听内嵌按钮互动事件 (INTERACTION_CREATE)
            onInteractionCreate { event ->
                println(">>> 收到互动交互按钮事件: id=${event.interaction.id}")
                // 必须在规定时间内做出回调应答
                event.respond(code = 0)
            }
        }

        // 6. 阻塞主线程运行机器人网关
        // bot.startBlocking() // 取消注释以直接连接网关运行
        println("QQBotDemo 初始化成功！可以在主函数中调用 bot.startBlocking() 连入平台网关。")
    }
}
