# QQBotKt - QQ 官方机器人 Kotlin 异步高阶 SDK

> 参考官方开发者文档 `tools/qq_bot_docs`（开放平台 OpenAPI v2 & WebSocket 网关规范）精心匠制。
> 采用 **Kotlin 2.1 + Coroutines 协程 + Ktor Client 3.x + kotlinx-serialization**，兼具 **高阶异步扩展性** 与 **极致声明式 DSL 优雅体验**。

---

## ✨ 核心特性与架构亮点

### 1. ⚡ 纯异步非阻塞协程体系
- **底层驱动**：采用 **Ktor Client 3.x**（配合 OkHttp 引擎）与 **Kotlin Coroutines**，不管是 HTTP 调用还是 WebSocket TCP/TLS 通信，均实现全异步非阻塞，并发承载力极强。
- **线程安全与重流控制**：为多协程并发量身打造。

### 2. 🔐 AccessToken 自动托管与并发保护 (AccessTokenManager)
- **无感生命周期管理**：自动计算 `expires_in` 到期窗口，提前 **60 秒** 安全自动续期。
- **协程锁 (Mutex) 双重检查**：若出现上百个并发请求同时遭遇 Token 过期，`Mutex` 保障**仅由第一个协程**发起 HTTPS 凭证更新，其余请求复用最新凭证，告别频繁触发鉴权错误。
- **401/过期错误自动纠偏**：如果网络请求返回 Token 过期（如 `errCode = 11243` 或 HTTP 401），客户端会自动刷取最新 Token 并进行一次无感重试。

### 3. 🛡️ 稳健的异常分层与智能重试机制 (QQBotException Hierarchy)
- **分层明确**：
  - `QQBotException`（基类）
  - `QQBotApiException`（开放平台 OpenAPI 业务异常，封装 `errCode`、`httpStatusCode` 与平台 `trace_id`）
  - `QQBotAuthException`（鉴权与 AccessToken 异常）
  - `QQBotGatewayException`（WebSocket 网关连接与协议异常）
- **指数退避重试 (Exponential Backoff)**：对于 HTTP `429 Too Many Requests` 限流与短暂网络抖动，内置自动按次退避重试，保障网络通信稳如泰山。
- **全链路追踪 (TraceId)**：无论调用成功还是业务返回异常，均自动采集并在异常信息中附带官方 HTTP Response Header 的 `X-Tps-trace-ID`，协助排查平台报错。

### 4. 🌐 全功能 OpenAPI v2 高阶封装 (QQBotApi)
- 覆盖单聊私信 (C2C)、群聊消息 (Group)、频道全量消息与公域 @ 消息、子频道 (Channel) 增删改查、以及互动交互回调应答 (Interaction)。
- 内置方法重载与可选参数，无需繁琐构造大结构体即可快速发起发信命令。

### 5. 🔌 智能 WebSocket 网关 (QQBotGateway)
- **完整遵守 OpCode 协议**：严格遵循官方规范（`10 Hello`、`2 Identify`、`1 Heartbeat`、`11 Heartbeat ACK`、`6 Resume`、`7 Reconnect`、`9 Invalid Session` 等）。
- **心跳与序列号保活**：基于服务端下发的 `heartbeat_interval` 自动管理心跳协程；精准追踪序列号 (`s`)。
- **断线无缝会话恢复**：在发生异常断线（网络中断、`OpCode 7 Reconnect` 等）时，自动尝试凭 `sessionId` 和最新 `s` 发起 `OpCode 6 Resume`，无需丢失未处理消息，保障高可用。

### 6. 🎨 极致优雅的声明式 Kotlin DSL
- **一站式配置与回调注册**：通过 `qqBot { ... }` 即可在单代码块内完成参数配置与所有类型事件响应监听。
- **消息发送 DSL (`message { ... }`)**：优雅构建纯文本、Markdown、交互按钮键盘 (`keyboard { row { ... } }`)、多媒体消息与引用回复。
- **被动回复便携拓展**：每个事件对象（如 `GroupAtMessageEvent`、`C2CMessageEvent`）皆内置 `event.reply(...)` 与 `event.delete()` 操作，自动绑定目标会话 ID 与消息引用，省去重复写会话参数的繁琐。

---

## 📦 安装与依赖引入 (Installation)

本项目已被支持构建并发布至 Maven Local 仓库。你可以直接在本机通过以下方式在其它 Java/Kotlin 项目中集成：

### Gradle (Kotlin DSL)
```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("cn.qfys521:qqbotkt:1.0.0")
}
```

### Gradle (Groovy DSL)
```groovy
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'cn.qfys521:qqbotkt:1.0.0'
}
```

### Maven (`pom.xml`)
```xml
<dependency>
    <groupId>cn.qfys521</groupId>
    <artifactId>qqbotkt</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## 🚀 快速上手 (Quick Start)

### 1. 基础机器人应用示例

```kotlin
import cn.qfys521.qqbot.dsl.qqBot
import cn.qfys521.qqbot.model.common.Intent

fun main() {
    // 创建并配置 QQ 机器人
    val bot = qqBot {
        // 1. 基础鉴权与环境配置
        appId = "YOUR_APP_ID"
        clientSecret = "YOUR_CLIENT_SECRET"
        sandbox = false                         // 是否连入沙箱测试环境
        intents = Intent.DEFAULT_PUBLIC_INTENTS // 默认订阅推荐的公开事件合集
        autoReconnect = true                    // 开启网关断开自动重新鉴权/会话恢复

        // 2. 监听网关连接成功就绪事件 (READY)
        onReady { event ->
            println("✅ 机器人就绪！登录昵称: ${event.data.user.username}, SessionID: ${event.data.sessionId}")
        }

        // 3. 监听群聊 @ 机器人消息事件
        onGroupAtMessage { event ->
            val content = event.message.content.trim()
            println("📩 收到群消息 [来自 ${event.authorName}]: $content")

            when {
                content == "ping" -> {
                    // 快捷简单文本回复
                    event.reply("pong! 我在~")
                }
                content == "菜单" -> {
                    // 优雅使用 DSL 构造 Markdown + 内嵌交互键盘按钮
                    event.reply {
                        markdown(
                            content = """
                                # 🤖 机器人帮助功能表
                                请点击下方控制按钮进行操作：
                            """.trimIndent()
                        )
                        keyboard {
                            row {
                                urlButton("btn_doc", "官方文档", "https://bot.q.qq.com", style = 1)
                                commandButton("btn_cmd", "查询天气", "/天气 北京", style = 0)
                            }
                            row {
                                callbackButton("btn_cb", "立即打卡", "action=checkin", style = 3)
                            }
                        }
                    }
                }
                else -> {
                    event.reply("你发送了: $content")
                }
            }
        }

        // 4. 监听单聊私信事件
        onC2CMessage { event ->
            event.reply("已收到私信：${event.message.content}")
        }

        // 5. 监听内嵌按钮互动事件 (例如用户点击了 callbackButton)
        onInteractionCreate { event ->
            println("交互回调触发: interactionId=${event.interaction.id}")
            // 响应事件（必须应答）
            event.respond(code = 0)
        }
    }

    // 阻塞主线程开始连接官方网关并持久服务
    bot.startBlocking()
}
```

---

## 🛠️ 模块化包结构速览

```
cn.qfys521.qqbot
├── QQBot.kt                       // 核心驱动 SDK 门面主类
├── auth
│   └── AccessTokenManager.kt      // Token 申请/并发锁/到期无感自动更新
├── config
│   └── QQBotConfig.kt             // 运行配置项 (AppID, Secret, Intents, 路由等)
├── dsl
│   ├── MessageBuilder.kt          // Markdown/键盘按钮/引用的声明式构建 DSL
│   └── QQBotDSL.kt                // qqBot { ... } 配置与回调顶层函数
├── event
│   ├── Event.kt                   // 丰富层级的事件体系类与便携操作拓展 (reply / delete / respond)
│   └── EventDispatcher.kt         // 事件路由与协程安全回调调度器
├── exception
│   └── QQBotException.kt          // 业务错误码/TraceID/鉴权/网关统一异常分层
├── gateway
│   └── QQBotGateway.kt            // WSS 协议处理 / Heartbeat 心跳保活 / Identify / Resume 会话恢复
├── http
│   ├── QQBotApi.kt                // OpenAPI v2 官方全栈业务接口抽象
│   └── QQBotHttpClient.kt         // 支持鉴权注入与异常智能重试的 HTTP 引擎
└── model
    ├── common
    │   └── OpCode.kt              // OpCode / Intents 常量与分片模型
    ├── gateway
    │   └── GatewayPayload.kt      // 上下行通信帧报文实体
    ├── guild
    │   └── Guild.kt               // 频道与子频道操作实体
    ├── interaction
    │   └── Interaction.kt         // 内嵌键盘按钮交互回调实体
    ├── message
    │   └── Message.kt             // 消息发送/解析/多媒体/Markdown/键盘按钮实体
    └── user
        └── User.kt                // 机器人与用户信息模型
```

---

## 🧪 单元测试与验证

本项目内置完善的单元测试覆盖，验证各类配置、JSON 解析、DSL 语义和事件派发兼容性：

```bash
# 执行全部测试用例
./gradlew test

# 编译主包
./gradlew build
```

---

## 📚 文档与官方参考资料

- **[全量 API 开发速查手册 (API_REFERENCE.md)](file:///E:/Code/qqbotkt/API_REFERENCE.md)**：包含本 SDK 支持的全部 OpenAPI v2 HTTPS 接口、WebSocket 网关所有事件、DSL 语法规范及数据字典速查表。
- [QQ 开放平台官方开发者文档](https://bot.q.qq.com/wiki)
- 本仓库内置原始官方规范文档根目录：`tools/qq_bot_docs/`
