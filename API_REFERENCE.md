# QQBotKt API Reference

> 本参考手册整理当前 SDK 已实现的 OpenAPI 和 `server-inter` HTTP 接口、WebSocket 网关事件、DSL 语法与数据模型，并参考官方文档 `tools/qq_bot_docs/develop/api-v2/`。

---

## 目录 (Table of Contents)
1. [OpenAPI v2 接口速查 (QQBotApi)](#1-openapi-v2-接口速查-qqbotapi)
   - [1.1 机器人账号及频道基本信息](#11-机器人账号及频道基本信息)
   - [1.2 单聊私信会话 (C2C Messages & Media)](#12-单聊私信会话-c2c-messages--media)
   - [1.3 QQ 群聊会话 (Group Messages & Media)](#13-qq-群聊会话-group-messages--media)
   - [1.4 频道与子频道管理 (Guild & Channel API)](#14-频道与子频道管理-guild--channel-api)
   - [1.5 群管理与扩展接口 (Management API)](#15-群管理与扩展接口-management-api)
   - [1.6 交互回调应答 (Interaction Response)](#16-交互回调应答-interaction-response)
   - [1.7 WebSocket 网关寻址 (Gateway URL)](#17-websocket-网关寻址-gateway-url)
2. [WebSocket 网关事件体系 (BotEvent & DSL 侦听器)](#2-websocket-网关事件体系-botevent--dsl-侦听器)
3. [高阶声明式 DSL 规范 (Builder DSL)](#3-高阶声明式-dsl-规范-builder-dsl)
4. [核心数据模型词典 (Model Dictionary)](#4-核心数据模型词典-model-dictionary)
5. [错误码与异常继承关系 (Exceptions & Error Codes)](#5-错误码与异常继承关系-exceptions--error-codes)

---

## 1. OpenAPI v2 接口速查 (QQBotApi)

在 `QQBotKt` 中，你可以通过 `bot.api` 对象调用 SDK 已封装的开放平台 HTTPS 接口。

### 1.1 机器人账号及频道基本信息
| SDK 方法名 | 对应 HTTP 方法与相对路径 | 业务功能 | 返回结果类型 |
| :--- | :--- | :--- | :--- |
| `getMe()` | `GET /users/@me` | 查验认证账号机器人的头像、ID 与名称 | `UserMe` |
| `getMyGuilds(before, after, limit)` | `GET /users/@me/guilds` | 分页拉取当前账号已加入的频道 | `List<GuildItem>` |

`before` 和 `after` 用于按频道 ID 分页；设置 `before` 时先反序再分页，同时设置时 `after` 无效。`limit` 默认 100、最大 100。频道条目中的 `ownerId` 对应 `owner_id`，`isOwner` 对应 `owner`，表示当前查询账号是否为频道创建者。

---

### 1.2 单聊私信会话 (C2C Messages & Media)
| SDK 方法名 | 对应 HTTP 方法与相对路径 | 业务功能 | 参数说明 | 返回结果类型 |
| :--- | :--- | :--- | :--- | :--- |
| `sendC2CMessage(userOpenId, request)` | `POST /v2/users/{id}/messages` | 针对特定用户的私聊发信与被动回信 | `userOpenId`: 用户 OpenID<br>`request`: 发信结构体 | `MessageResult` |
| `sendC2CMessage(userOpenId, content, ...)` | `POST /v2/users/{id}/messages` | 重载便携版：发送普通纯文本字串 | `content`: 文字内容<br>`msgId`: 被引用消息 ID | `MessageResult` |
| `sendC2CStreamMessage(userOpenId, request: StreamMessageRequest)` | `POST /v2/users/{id}/stream_messages` | 分片发送单聊流式消息 | `userOpenId`: 用户 OpenID<br>`request`: 流式状态、分片序号与内容等 | `MessageResult` |
| `sendC2CStreamMessage(userOpenId, request: SendMessageRequest)` | `POST /v2/users/{id}/stream_messages` | 兼容旧调用的已弃用重载；请改用 `StreamMessageRequest` | `request`: 普通消息结构体 | `MessageResult` |
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
| `getGroupBotState(groupOpenId)` | `GET /v2/groups/{id}/bot_state` | 查验自己在群内所属的管理状态和特权 | `groupOpenId`: 群聊 ID | `GroupBotState` |
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

#### 频道成员、权限及身份组接口

| SDK 方法 | HTTP 路径 | 结果 |
| :--- | :--- | :--- |
| `getGuildMembers(guildId, after, limit)` / `getGuildMember(guildId, userId)` / `removeGuildMember(...)` | `GET/DELETE /guilds/{guild_id}/members...` | `List<GuildMember>` / `GuildMember` / `Unit` |
| `getGuildRoleMembers(guildId, roleId, startIndex, limit)` | `GET /guilds/{guild_id}/roles/{role_id}/members` | `GuildRoleMembersResponse` |
| `getGuildRoles(...)` / `createGuildRole(...)` / `updateGuildRole(...)` / `deleteGuildRole(...)` | `GET/POST/PATCH/DELETE /guilds/{guild_id}/roles...` | 身份组列表或操作结果 |
| `addGuildMemberRole(...)` / `removeGuildMemberRole(...)` | `PUT/DELETE /guilds/{guild_id}/members/{user_id}/roles/{role_id}` | `Unit` |
| `setGuildMute(...)` / `muteGuildMembers(...)` / `muteGuildMember(...)` | `PATCH /guilds/{guild_id}/mute`、`PATCH /guilds/{guild_id}/members/{user_id}/mute` | `Unit` 或 `GuildMembersMuteResponse` |
| `getGuildMessageSetting(guildId)` | `GET /guilds/{guild_id}/message/setting` | `MessageSetting` |
| `getChannelMemberPermissions(...)` / `getChannelRolePermissions(...)` / `updateChannelMemberPermissions(...)` / `updateChannelRolePermissions(...)` | `GET/PUT /channels/{channel_id}/members|roles/{id}/permissions` | 权限结果或 `Unit` |

#### 频道消息、私信及内容接口

| SDK 方法 | HTTP 路径 |
| :--- | :--- |
| `sendChannelMessage(channelId, request)` / `deleteChannelMessage(...)` | `POST/DELETE /channels/{channel_id}/messages...` |
| `sendChannelMessage(channelId, request, fileImage)` | `POST /channels/{channel_id}/messages`，`multipart/form-data` 字段 `file_image` |
| `createDirectMessage(...)` / `sendDirectMessage(...)` / `deleteDirectMessage(...)` | `POST /users/@me/dms`、`POST/DELETE /dms/{guild_id}/messages...` |
| `putMessageReaction(...)` / `deleteMessageReaction(...)` / `getMessageReactionUsers(...)` | `PUT/DELETE/GET /channels/{channel_id}/messages/{message_id}/reactions/{type}/{id}` |
| `getChannelPins(...)` / `addChannelPin(...)` / `removeChannelPin(...)` | `GET /channels/{channel_id}/pins`、`PUT/DELETE /channels/{channel_id}/pins/{message_id}`；查询与添加返回更新后的 `PinsMessage` |
| `getChannelSchedules(...)` / `getChannelSchedule(...)` / `createChannelSchedule(...)` / `updateChannelSchedule(...)` / `deleteChannelSchedule(...)` | `GET/POST/PATCH/DELETE /channels/{channel_id}/schedules...` |
| `createGuildAnnouncement(...)` / `deleteGuildAnnouncement(...)` | `POST/DELETE /guilds/{guild_id}/announces...` |
| `getForumThreads(...)` / `getForumThread(...)` / `createForumThread(...)` / `deleteForumThread(...)` | `GET/PUT/DELETE /channels/{channel_id}/threads...` |
| `controlChannelAudio(...)` / `putChannelMic(...)` / `deleteChannelMic(...)` / `getChannelOnlineNumbers(...)` | `/channels/{channel_id}/audio`, `/mic`, `/online_nums` |
| `getGuildApiPermissions(...)` / `requestGuildApiPermission(...)` | `GET/POST /guilds/{guild_id}/api_permission...` |

频道成员、身份组、音频及部分频道内容接口受机器人类型、权限或私域开通状态限制；具体限制以对应官方接口文档为准。

---

### 1.5 群管理与扩展接口 (Management API)

| SDK 方法名 | 对应 HTTP 方法与相对路径 | 说明 |
| :--- | :--- | :--- |
| `generateShareLink(request)` | `POST /v2/generate_url_link` | 生成机器人分享链接 |
| `getGroupMembers(...)` / `getGroupMember(...)` | `GET /v2/groups/{group_openid}/members...` | 分页查询群成员 |
| `getGroupBlacklist(...)` / `updateGroupBlacklist(...)` | `GET/POST /v2/groups/{group_openid}/member_blacklist` | 查询及维护群黑名单 |
| `getRestrictChatSetting(...)` / `updateRestrictChatSetting(...)` | `GET/POST /v2/groups/{group_openid}/restrict_chat_setting` | 查询及维护禁言状态 |
| `getJoinRequests(...)` | `GET /v2/groups/{group_openid}/join_request_list` | 分页查询入群申请 |
| `approveJoinRequest(groupOpenId, memberOpenId, request)` | `POST /v2/groups/{group_openid}/approval_join_request/{member_openid}` | 审批指定成员的入群申请 |
| `batchRemoveMembers(...)` | `POST /v2/groups/{group_openid}/batch_remove_members` | 批量移除群成员 |
| `getJoinApprovalStrategies(...)` / `createJoinApprovalStrategy(...)` / `updateJoinApprovalStrategy(...)` / `deleteJoinApprovalStrategy(...)` / `executeJoinApprovalStrategy(...)` | `GET/POST/PATCH/DELETE/POST /v2/groups/join_approval_strategy...` | 管理并执行自动审批策略 |
| `updateJoinApprovalWhitelist(strategyId, request)` | `POST /v2/groups/join_approval_strategy/{strategy_id}/whitelist_users` | 更新策略白名单 |
| `getMenu()` / `updateMenu(...)` | `GET/PUT /v2/menu` | 管理全局自定义菜单 |
| `getPanels(...)` / `createPanel(...)` / `getPanel(...)` / `updatePanel(...)` / `updatePanelTargets(...)` / `deletePanel(...)` | `GET/POST/GET/PUT/PUT/DELETE /v2/panels...` | 管理指令面板及投放范围 |
| `getGroupInfo(groupOpenId)` / `getGroupBotState(groupOpenId)` | `GET /v2/groups/{group_openid}/info` / `GET /v2/groups/{group_openid}/bot_state` | 查询群资料及机器人状态 |
| `prepare*Upload(...)` / `finish*Upload(...)` | `/v2/{groups,users}/{id}/upload_*` | 分片上传准备与完成回调 |

成员管理、审批策略、菜单和面板接口受平台灰度、机器人管理员身份或白名单权限限制；分片上传返回的预签名 URL 应由调用方直接 PUT 文件分片，不能附带 Bot Authorization。

---

### 1.6 交互回调应答 (Interaction Response)
| SDK 方法名 | 对应 HTTP 方法与相对路径 | 业务功能 | 参数说明 |
| :--- | :--- | :--- | :--- |
| `putInteractionResponse(interactionId, request)` | `PUT /interactions/{id}` | 应答 type 11 消息按钮或 type 12 快捷菜单交互 | `request`: `InteractionResponseRequest(code=0)`；每个 interaction 只能应答一次，超时后失效 |

---

### 1.7 WebSocket 网关寻址 (Gateway URL)
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
| `GROUP_MESSAGE_CREATE` | `onGroupMessage { event -> }` | `GroupMessageEvent` | 群聊消息事件 |
| `GROUP_AT_MESSAGE_CREATE` | `onGroupAtMessage { event -> }` | `GroupAtMessageEvent` | **群组被 @ 艾特发话**。内置 `event.reply(...)` |
| `C2C_MESSAGE_CREATE` | `onC2CMessage { event -> }` | `C2CMessageEvent` | **单聊私聊消息**。内置 `event.reply(...)` |
| `AT_MESSAGE_CREATE` | `onGuildAtMessage { event -> }` | `GuildAtMessageEvent` | 频道中特定子频道向机器人发出的 @ 消息 |
| `MESSAGE_CREATE` | `onGuildMessage { event -> }` | `GuildMessageEvent` | 监听所处频道的全体发言流水（需专属特权）|
| `DIRECT_MESSAGE_CREATE` | `onDirectMessage { event -> }` | `DirectMessageEvent`| 频道内部私聊消息沟通 |
| `INTERACTION_CREATE` | `onInteractionCreate { event -> }`| `InteractionCreateEvent`| 用户与消息按钮、快捷菜单及其他交互功能的操作事件；仅 type 11/12 需要应答 |
| `GUILD_CREATE` | `onGuildCreate { event -> }` | `GuildCreateEvent` | 机器人刚加入某全新频道时推送通知 |
| `GUILD_UPDATE` | `onGuildUpdate { event -> }` | `GuildUpdateEvent` | 大频道信息更新和属性变化监控 |
| `GUILD_DELETE` | `onGuildDelete { event -> }` | `GuildDeleteEvent` | 机器人退出或者频道废除告警 |
| `CHANNEL_CREATE` | `onChannelCreate { event -> }` | `ChannelCreateEvent` | 有子频道建立 |
| `CHANNEL_UPDATE` | `onChannelUpdate { event -> }` | `ChannelUpdateEvent` | 有子频道名称或排序修改 |
| `CHANNEL_DELETE` | `onChannelDelete { event -> }` | `ChannelDeleteEvent` | 有子频道关闭注销 |
| `GUILD_MEMBER_ADD/UPDATE/REMOVE` | `onGuildMemberAdd/Update/Remove { event -> }` | `GuildMemberAdd/Update/RemoveEvent` | 频道成员进出与资料变化 |
| `MESSAGE_REACTION_ADD/REMOVE` | `onMessageReactionAdd/Remove { event -> }` | `MessageReactionAdd/RemoveEvent` | 消息表情回应变化 |
| `MESSAGE_AUDIT_PASS/REJECT` | `onMessageAuditPass/Reject { event -> }` | `MessageAuditPass/RejectEvent` | 主动消息审核结果 |
| `MESSAGE_DELETE/PUBLIC_MESSAGE_DELETE/DIRECT_MESSAGE_DELETE` | `onMessageDelete/PublicMessageDelete/DirectMessageDelete { event -> }` | `MessageDeleteEvent` / `PublicMessageDeleteEvent` / `DirectMessageDeleteEvent` | 消息撤回 |
| `FORUM_THREAD_CREATE/UPDATE/DELETE` | `onForumThreadCreate/Update/Delete { event -> }` | `ForumThreadCreate/Update/DeleteEvent` | 论坛主题变化 |
| `FORUM_POST_CREATE/DELETE` | `onForumPostCreate/Delete { event -> }` | `ForumPostCreate/DeleteEvent` | 论坛帖子变化 |
| `FORUM_REPLY_CREATE/DELETE` | `onForumReplyCreate/Delete { event -> }` | `ForumReplyCreate/DeleteEvent` | 论坛回复变化 |
| `FORUM_PUBLISH_AUDIT_RESULT` | `onForumPublishAuditResult { event -> }` | `ForumPublishAuditResultEvent` | 论坛内容审核结果 |
| `GROUP_JOIN_REQUEST` | `onGroupJoinRequest { event -> }` | `GroupJoinRequestEvent` | 用户申请入群及验证信息 |
| `GROUP_MEMBER_ADD/REMOVE` | `onGroupMemberAdd/Remove { event -> }` | `GroupMemberAdd/RemoveEvent` | 群成员变更 |
| `GROUP_ADD_ROBOT/GROUP_DEL_ROBOT` | `onGroupAddRobot/DelRobot { event -> }` | `GroupAddRobot/GroupDelRobotEvent` | 机器人加入或退出群聊 |
| `FRIEND_ADD/DEL` | `onFriendAdd/Del { event -> }` | `FriendAdd/DelEvent` | 用户添加或删除好友 |
| `GROUP_MSG_RECEIVE/REJECT` | `onGroupMsgReceive/Reject { event -> }` | `GroupMsgReceive/RejectEvent` | 群主动消息接收开关变化 |
| `C2C_MSG_RECEIVE/REJECT` | `onC2CMsgReceive/Reject { event -> }` | `C2CMsgReceive/RejectEvent` | 单聊主动消息接收开关变化 |
| `AUDIO_START/AUDIO_FINISH/AUDIO_ON_MIC/AUDIO_OFF_MIC` | `onAudioStart/Finish/OnMic/OffMic { event -> }` | `AudioStartEvent` / `AudioFinishEvent` / `AudioOnMicEvent` / `AudioOffMicEvent` | 音频播放与上麦状态变化；事件类保留原始 JSON 载荷 |
| `AUDIO_OR_LIVE_CHANNEL_MEMBER_ENTER/EXIT` | `onAudioOrLiveChannelMemberEnter/Exit { event -> }` | `AudioOrLiveChannelMemberEnter/ExitEvent` | 音视频或直播子频道成员进出 |
| `(自定义/泛化)` | `onGenericEvent { event -> }` | `GenericEvent` | 匹配其它还未细化的未定义事件载荷字样 |
| `(全局兜底)` | `onAnyEvent { event -> }` | `BotEvent` | 每一个识别并派发的事件触发时，首先调用该处侦听函数 |

平台新增且 SDK 尚未细分的事件仍会通过 `GenericEvent` 保留原始 `data`。

`INTERACTION_CREATE` 中只有 type 11（消息按钮）和 type 12（快捷菜单）需要调用 `event.respond()`；其他类型无需应答。同一 interaction 只能应答一次，超时后失效。

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

SDK 在发生网络异常、业务鉴权拒绝或频率控管时将统一抛出下列派生自 [`QQBotException`](src/main/kotlin/cn/qfys521/qqbot/exception/QQBotException.kt) 的异常对象：

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
| `11241` | 以实际 HTTP 响应为准 | 请求参数中缺少 token | SDK 不对该错误码单独刷新 token；检查请求鉴权配置。 |
| `11242` | 以实际 HTTP 响应为准 | Token 校验系统错误 | SDK 最多重试一次，且受 `maxRetries` 限制。 |
| `11243` | 以实际 HTTP 响应为准 | token 校验未通过，通常是填入的 token 错误 | 检查 Token 配置；此错误码不会触发自动刷新。 |
| `-` (无 API 返回) | `429 Too Many Requests` | 短时间内调用该会话/该类接口的请求频率大幅溢出 | 内置**指数退避重试**引擎自动排队退避延迟进行第 `1..maxRetries` 次补发请求。 |
| `11252` | 以实际 HTTP 响应为准 | 检查应用权限失败，官方文档标为系统错误，通常重试一次有效 | SDK 对该错误码最多重试一次。 |
| `11263` | 以实际 HTTP 响应为准 | 检查频道权限失败，系统错误 | SDK 最多重试一次，且受 `maxRetries` 限制。 |
| `11281` | 以实际 HTTP 响应为准 | 检查管理员权限失败，系统错误 | SDK 最多重试一次，且受 `maxRetries` 限制。 |
| `11253` | 以实际 HTTP 响应为准 | 应用未获得调用该接口的权限 | 向平台申请该接口权限；SDK 不按该错误码单独重试。 |

未带非零业务错误码的 HTTP 401 会触发一次 Token 刷新；带有明确业务错误码的响应会保留该错误，不刷新。`QQBotApiException.errorData` 保留响应体中的 `data`，可用于读取消息审核 `audit_id` 等字段。HTTP 201/202 且带业务错误结构时，异常的 `isAsyncOperation` 为 `true`，表示异步操作已受理，结果需按响应数据继续处理。
