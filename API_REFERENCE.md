# QQBotKt API Reference (全量 API 开发速查手册)

> 本参考手册整理自 SDK 内核源码 ([`QQBotApi.kt`](file:///E:/Code/qqbotkt/src/main/kotlin/cn/qfys521/qqbot/http/QQBotApi.kt)) 与官方文档 `tools/qq_bot_docs/develop/api-v2/`，提供**全部 OpenAPI v2 接口、WebSocket 网关事件、DSL 语法规范及数据字典**，是基于 `QQBotKt` 进行机器人开发的标准 API 文档。

---

## 目录 (Table of Contents)
1. [OpenAPI v2 接口速查 (QQBotApi)](#1-openapi-v2-接口速查-qqbotapi)
   - [1.1 机器人账号及频道基本信息](#11-机器人账号及频道基本信息)
   - [1.2 单聊私信会话 (C2C Messages & Media)](#12-单聊私信会话-c2c-messages--media)
   - [1.3 QQ 群聊会话 (Group Messages & Media)](#13-qq-群聊会话-group-messages--media)
   - [1.4 频道与子频道管理 (Guild & Channel API)](#14-频道与子频道管理-guild--channel-api)
   - [1.5 交互回调应答 (Interaction Response)](#15-交互回调应答-interaction-response)
   - [1.6 WebSocket 网关寻址 (Gateway URL)](#16-websocket-网关寻址-gateway-url)
2. [WebSocket 网关事件体系 (BotEvent & DSL 侦听器)](#2-websocket-网关事件体系-botevent--dsl-侦听器)
3. [高阶声明式 DSL 规范 (Builder DSL)](#3-高阶声明式-dsl-规范-builder-dsl)
4. [核心数据模型词典 (Model Dictionary)](#4-核心数据模型词典-model-dictionary)
5. [错误码与异常继承关系 (Exceptions & Error Codes)](#5-错误码与异常继承关系-exceptions--error-codes)

---

## 1. OpenAPI v2 接口速查 (QQBotApi)

在 `QQBotKt` 中，你可以通过 `bot.api` 对象调用所有开放平台 HTTPS 接口。

### 1.1 机器人账号及频道基本信息
| SDK 方法名 | 对应 HTTP 方法与相对路径 | 业务功能 | 返回结果类型 |
| :--- | :--- | :--- | :--- |
| `getMe()` | `GET /users/@me` | 查验认证账号机器人的头像、ID 与名称 | `UserMe` |
| `getMyGuilds()` | `GET /users/@me/guilds` | 拉取机器人目前已获准加入的频道合集列表 | `List<GuildItem>` |

---

### 1.2 单聊私信会话 (C2C Messages & Media)
| SDK 方法名 | 对应 HTTP 方法与相对路径 | 业务功能 | 参数说明 | 返回结果类型 |
| :--- | :--- | :--- | :--- | :--- |
| `sendC2CMessage(userOpenId, request)` | `POST /v2/users/{id}/messages` | 针对特定用户的私聊发信与被动回信 | `userOpenId`: 用户 OpenID<br>`request`: 发信结构体 | `MessageResult` |
| `sendC2CMessage(userOpenId, content, ...)` | `POST /v2/users/{id}/messages` | 重载便携版：发送普通纯文本字串 | `content`: 文字内容<br>`msgId`: 被引用消息 ID | `MessageResult` |
| `deleteC2CMessage(userOpenId, messageId)` | `DELETE /v2/users/{id}/messages/{msg_id}` | 撤销已经由自身发送过的私聊会话记录 | `messageId`: 目标消息 ID | `Unit` |
| `uploadC2CMedia(userOpenId, request)` | `POST /v2/users/{id}/files` | **富媒体上传**：转存图片/视频/语音到服务器 | `request`: `UploadMediaRequest(fileType, url)` | `UploadMediaResponse` (含 `file_info`) |

#### 💡 富媒体发信最佳实践示例：
```kotlin
// 1. 提交 URL 供腾讯云自动提取上传
val uploadRes = bot.api.uploadC2CMedia(
    userOpenId = "USER_OPEN_ID",
    request = UploadMediaRequest(fileType = 1, url = "https://example.com/demo.png")
)
// 2. 携带凭证 file_info，设定 msg_type = 7 进行发图
val fileInfo = uploadRes.fileInfo!!
bot.api.sendC2CMessage(
    userOpenId = "USER_OPEN_ID",
    request = message {
        media(fileInfo)
    }
)
```

---

### 1.3 QQ 群聊会话 (Group Messages & Media)
| SDK 方法名 | 对应 HTTP 方法与相对路径 | 业务功能 | 参数说明 | 返回结果类型 |
| :--- | :--- | :--- | :--- | :--- |
| `sendGroupMessage(groupOpenId, request)` | `POST /v2/groups/{id}/messages` | 向指定 QQ 群发送公开消息或答复 @ 回复 | `groupOpenId`: 群聊 ID<br>`request`: 消息配置体 | `MessageResult` |
| `sendGroupMessage(groupOpenId, content, ...)`| `POST /v2/groups/{id}/messages` | 重载便携版：快捷向群聊投放文本消息 | `content`: 文字文本 | `MessageResult` |
| `deleteGroupMessage(groupOpenId, messageId)`| `DELETE /v2/groups/{id}/messages/{msg_id}`| 从群聊窗口中主动撤回既往发送消息 | `messageId`: 待撤回消息 ID | `Unit` |
| `getGroupInfo(groupOpenId)` | `GET /v2/groups/{id}/info` | 探查指定群组的基本展示元数据 | `groupOpenId`: 群聊 ID | `JsonElement` |
| `getGroupBotState(groupOpenId)` | `GET /v2/groups/{id}/bot-state` | 查验自己在群内所属的管理状态和特权 | `groupOpenId`: 群聊 ID | `GroupBotState` |
| `uploadGroupMedia(groupOpenId, request)` | `POST /v2/groups/{id}/files` | **群富媒体转存**：把群媒体文件交由服务器托管 | `request`: 文件类型及资源下载 URL | `UploadMediaResponse` |

---

### 1.4 频道与子频道管理 (Guild & Channel API)
| SDK 方法名 | 对应 HTTP 方法与相对路径 | 业务功能 | 返回结果类型 |
| :--- | :--- | :--- | :--- |
| `getGuild(guildId)` | `GET /guilds/{guild_id}` | 获取大频道的成员容积与简介 | `Guild` |
| `getChannels(guildId)` | `GET /guilds/{guild_id}/channels` | 罗列查询此社区中所有子频道项目 | `List<Channel>` |
| `getChannel(channelId)` | `GET /channels/{channel_id}` | 精确探知某条子频道的名称、排序位号 | `Channel` |
| `createChannel(guildId, request)` | `POST /guilds/{guild_id}/channels` | 在社区内新设文本/语音子频道会话 | `Channel` |
| `updateChannel(channelId, request)` | `PATCH /channels/{channel_id}` | 修改特定子频道的名称或分组层级 | `Channel` |
| `deleteChannel(channelId)` | `DELETE /channels/{channel_id}` | 将目标子频道直接清理废弃 | `Unit` |

---

### 1.5 交互回调应答 (Interaction Response)
| SDK 方法名 | 对应 HTTP 方法与相对路径 | 业务功能 | 参数说明 |
| :--- | :--- | :--- | :--- |
| `putInteractionResponse(interactionId, request)` | `PUT /v2/interactions/{id}` | 在收到内嵌键盘按钮回调点击后进行回显确认 | `request`: `InteractionResponseRequest(code=0)` |

---

### 1.6 WebSocket 网关寻址 (Gateway URL)
| SDK 方法名 | 对应 HTTP 方法与相对路径 | 业务功能 | 返回结果类型 |
| :--- | :--- | :--- | :--- |
| `getWssUrl()` | `GET /gateway` | 获得标准通用版 WSS 连接地址 | `WssUrlResponse` |
| `getWssBotUrl()` | `GET /gateway/bot` | **推荐使用**：获取自带建议分片及并发控制配额的网关信息 | `WssUrlResponse` |

---

## 2. WebSocket 网关事件体系 (BotEvent & DSL 侦听器)

当使用 `qqBot { ... }` 作用域配置机器人时，SDK 内置了统一的协程回调生命周期与消息分类侦听：

| 平台事件代码 (Event Type) | 监听器 DSL 方法 | 对应处理事件类 (`BotEvent`) | 核心负载描述 |
| :--- | :--- | :--- | :--- |
| `READY` | `onReady { event -> }` | `ReadyEvent` | 网关登录鉴权与 `SessionId` 分配完结事件 |
| `RESUMED` | `onResumed { event -> }` | `ResumedEvent` | 发生断线后根据缓存及 `s` 成功恢复会话 |
| `GROUP_AT_MESSAGE_CREATE` | `onGroupAtMessage { event -> }` | `GroupAtMessageEvent` | **群组被 @ 艾特发话**。内置 `event.reply(...)` |
| `C2C_MESSAGE_CREATE` | `onC2CMessage { event -> }` | `C2CMessageEvent` | **单聊私聊消息**。内置 `event.reply(...)` |
| `AT_MESSAGE_CREATE` | `onGuildAtMessage { event -> }` | `GuildAtMessageEvent` | 频道中特定子频道向机器人发出的 @ 消息 |
| `MESSAGE_CREATE` | `onGuildMessage { event -> }` | `GuildMessageEvent` | 监听所处频道的全体发言流水（需专属特权）|
| `DIRECT_MESSAGE_CREATE` | `onDirectMessage { event -> }` | `DirectDirectMessageEvent`| 频道内部私聊消息沟通 |
| `INTERACTION_CREATE` | `onInteractionCreate { event -> }`| `InteractionCreateEvent`| 用户在窗口点击回调型按钮或操作面板引发的回应 |
| `GUILD_CREATE` | `onGuildCreate { event -> }` | `GuildCreateEvent` | 机器人刚加入某全新频道时推送通知 |
| `GUILD_UPDATE` | `onGuildUpdate { event -> }` | `GuildUpdateEvent` | 大频道信息更新和属性变化监控 |
| `GUILD_DELETE` | `onGuildDelete { event -> }` | `GuildDeleteEvent` | 机器人退出或者频道废除告警 |
| `CHANNEL_CREATE` | `onChannelCreate { event -> }` | `ChannelCreateEvent` | 有子频道建立 |
| `CHANNEL_UPDATE` | `onChannelUpdate { event -> }` | `ChannelUpdateEvent` | 有子频道名称或排序修改 |
| `CHANNEL_DELETE` | `onChannelDelete { event -> }` | `ChannelDeleteEvent` | 有子频道关闭注销 |
| `(自定义/泛化)` | `onGenericEvent { event -> }` | `GenericEvent` | 匹配其它还未细化的未定义事件载荷字样 |
| `(全局兜底)` | `onAnyEvent { event -> }` | `BotEvent` | **每一个合规事件**触发时，首先调用该处侦听函数 |

#### 💡 便携被动回信快捷 API：
所有如 `GroupAtMessageEvent` 和 `C2CMessageEvent` 都内聚了：
- `event.reply(content: String)` - 一行纯文字会话气泡响应。
- `event.replyMarkdown(markdownContent: String)` - 发送 Markdown。
- `event.reply { markdown("..."); keyboard { ... } }` - 结合 DSL 发送图文/按键卡片。
- `event.delete()` - 主动撤消本条触达发信记录。

---

## 3. 高阶声明式 DSL 规范 (Builder DSL)

### 3.1 机器人主配置域 (`qqBot { ... }`)
```kotlin
val bot = qqBot {
    appId = "APP_ID"
    clientSecret = "CLIENT_SECRET"
    sandbox = false // 生产环境为 false，调试为 true
    intents = Intent.DEFAULT_PUBLIC_INTENTS // 事件订阅位掩码
    autoReconnect = true // 断线或 Reconnect 时自动发送 Resume 续约
    requestTimeoutMillis = 10_000L // HTTP 超时控制

    onReady { event -> /* 登录就绪 */ }
    onGroupAtMessage { event -> /* 群聊互动 */ }
}
```

### 3.2 组合消息与按键布局构建 DSL (`message { ... }`)
```kotlin
val request = message {
    content = "文本备选说明"
    markdown(content = "# 主标题\n正文及说明字段")
    keyboard {
        row {
            urlButton(id = "b1", label = "打开网站", url = "https://q.qq.com", style = 1)
            commandButton(id = "b2", label = "点我打卡", command = "/checkin", style = 0)
        }
        row {
            callbackButton(id = "b3", label = "授权同意", data = "action=agree", style = 3)
        }
    }
    reference(messageId = "SOURCE_MESSAGE_ID", ignoreGetMessageError = true)
}
```

---

## 4. 核心数据模型词典 (Model Dictionary)

| 模型类 | 所在包 | 关键属性描述 |
| :--- | :--- | :--- |
| `Message` | `cn.qfys521.qqbot.model.message` | 涵盖发送方 `author: User`、`content: String`、`id: String`、`attachments` 附件及 `mentions` |
| `User` | `cn.qfys521.qqbot.model.user` | 用户、群成员通用体。提供 `openId`、`username`、`memberRole`、`isOwner`、`isAdminOrOwner` 快捷扩展 |
| `UserMe` | `cn.qfys521.qqbot.model.user` | `@me` 接口专用的机器人自我属性：`id`、`username`、`avatar` |
| `Guild` | `cn.qfys521.qqbot.model.guild` | 大频道信息类：`id`、`name`、`memberCount`、`maxMembers` |
| `Channel` | `cn.qfys521.qqbot.model.guild` | 子频道分类描述：`id`、`guildId`、`name`、`type` |
| `Interaction` | `cn.qfys521.qqbot.model.interaction`| 界面按钮按动后的上行负载：`id`、`type`、`data: InteractionData` |
| `UploadMediaRequest` | `cn.qfys521.qqbot.model.message` | 媒体文件转存定义体：`fileType` (1图/2视/3音/4文) 与 `url` |
| `UploadMediaResponse`| `cn.qfys521.qqbot.model.message` | 回发取得发信所依赖的字符串令牌凭据 `fileInfo` |
| `WssUrlResponse` | `cn.qfys521.qqbot.model.gateway` | WSS 地址分配描述体：含 `url`、建议的 `shards` 数量及启动并发配额 |

---

## 5. 错误码与异常继承关系 (Exceptions & Error Codes)

SDK 在发生网络异常、业务鉴权拒绝或频率控管时将统一抛出下列派生自 [`QQBotException`](file:///E:/Code/qqbotkt/src/main/kotlin/cn/qfys521/qqbot/exception/QQBotException.kt) 的异常对象：

```
RuntimeException
 └── QQBotException (SDK 全局基类)
      ├── QQBotApiException (HTTP OpenAPI 接口业务拒绝，带 errCode, httpStatusCode 与 traceId)
      ├── QQBotAuthException (鉴权中心授权拒绝或 AppSecret 解析失败)
      └── QQBotGatewayException (WebSocket 连接、分片握手及协议报文校验异常)
```

### 常见开发错误码排障手册 (Troubleshooting)

| 平台错误码 (`errCode`) | HTTP Status | 常见原因解释 | SDK 自带防护策略 |
| :--- | :--- | :--- | :--- |
| `11243` / `11241` | `401 Unauthorized` | AccessToken 授权凭据失效或过期 | `QQBotHttpClient` 自动捕获到此码，立马触发向 `AccessTokenManager` 申请凭证刷取并隐式重试。 |
| `-` (无 API 返回) | `429 Too Many Requests` | 短时间内调用该会话/该类接口的请求频率大幅溢出 | 内置**指数退避重试**引擎自动排队退避延迟进行第 `1..maxRetries` 次补发请求。 |
| `40001` - `40005` | `400 Bad Request` | 发错报文 JSON 格式、参数缺失或 `msg_seq` 不自增 | 检查传入参数或直接查看异常日志打印的 `traceId` 前往平台查验。 |
| `11252` | `403 Forbidden` | 当前机器人权限/分片不符合该应用配置条件 | 检查开发者后台的 Intents 开关项以及机器人应用运行沙箱状态 (`sandbox=false`) 是否吻合。 |
