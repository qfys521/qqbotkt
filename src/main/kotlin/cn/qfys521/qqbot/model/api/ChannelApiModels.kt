package cn.qfys521.qqbot.model.api

import cn.qfys521.qqbot.model.message.MessageAttachment
import cn.qfys521.qqbot.model.message.Message
import cn.qfys521.qqbot.model.message.MessageMarkdown
import cn.qfys521.qqbot.model.user.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class GuildMember(
    val user: User? = null,
    val nick: String? = null,
    val roles: List<String> = emptyList(),
    @SerialName("joined_at") val joinedAt: String? = null,
    val deaf: Boolean? = null,
    val mute: Boolean? = null,
    val pending: Boolean? = null
)

@Serializable
data class GuildMemberWithGuildId(
    @SerialName("guild_id") val guildId: String? = null,
    val user: User? = null,
    val nick: String? = null,
    val roles: List<String> = emptyList(),
    @SerialName("joined_at") val joinedAt: String? = null,
    @SerialName("op_user_id") val opUserId: String? = null
)

@Serializable
data class GuildRole(
    val id: String = "",
    val name: String? = null,
    val color: Long? = null,
    val hoist: Long? = null,
    val number: Long? = null,
    @SerialName("member_limit") val memberLimit: Long? = null
)

@Serializable
data class GuildRolesResponse(
    @SerialName("guild_id") val guildId: String? = null,
    val roles: List<GuildRole> = emptyList(),
    @SerialName("role_num_limit") val roleNumLimit: String? = null
)

@Serializable
data class CreateGuildRoleRequest(
    val name: String? = null,
    val color: Long? = null,
    val hoist: Int? = null
)

@Serializable
data class UpdateGuildRoleRequest(
    val name: String? = null,
    val color: Long? = null,
    val hoist: Int? = null
)

@Serializable
data class GuildRoleResponse(
    @SerialName("guild_id") val guildId: String? = null,
    @SerialName("role_id") val roleId: String? = null,
    val role: GuildRole? = null
)

@Serializable
data class CreateGuildRoleResponse(
    @SerialName("role_id") val roleId: String? = null,
    val role: GuildRole? = null
)

@Serializable
data class RoleChannelRef(val id: String = "")

@Serializable
data class GuildRoleMemberRequest(val channel: RoleChannelRef? = null)

@Serializable
data class GuildRoleMembersResponse(
    val data: List<GuildMember> = emptyList(),
    val next: String? = null
)

@Serializable
data class RemoveGuildMemberRequest(
    @SerialName("add_blacklist") val addBlacklist: Boolean? = null,
    @SerialName("delete_history_msg_days") val deleteHistoryMsgDays: Int? = null
)

@Serializable
data class GuildMuteRequest(
    @SerialName("mute_end_timestamp") val muteEndTimestamp: String? = null,
    @SerialName("mute_seconds") val muteSeconds: String? = null
)

@Serializable
data class GuildMembersMuteRequest(
    @SerialName("mute_end_timestamp") val muteEndTimestamp: String? = null,
    @SerialName("mute_seconds") val muteSeconds: String? = null,
    @SerialName("user_ids") val userIds: List<String> = emptyList()
)

@Serializable
data class GuildMembersMuteResponse(@SerialName("user_ids") val userIds: List<String> = emptyList())

@Serializable
data class MessageSetting(
    @SerialName("disable_create_dm") val disableCreateDm: Boolean? = null,
    @SerialName("disable_push_msg") val disablePushMsg: Boolean? = null,
    @SerialName("channel_ids") val channelIds: List<String> = emptyList(),
    @SerialName("channel_push_max_num") val channelPushMaxNum: Int? = null
)

@Serializable
data class ChannelPermission(
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("role_id") val roleId: String? = null,
    val permissions: String? = null
)

@Serializable
data class UpdateChannelPermissionRequest(
    val add: String? = null,
    val remove: String? = null
)

@Serializable
data class MessageReaction(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("guild_id") val guildId: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    val target: ReactionTarget? = null,
    val emoji: ReactionEmoji? = null
)

@Serializable
data class ReactionTarget(
    val id: String? = null,
    val type: Int? = null
)

@Serializable
data class ReactionEmoji(
    // The docs call this a string, while event examples also send numeric IDs.
    val id: JsonElement? = null,
    val type: Int? = null,
    val name: String? = null
)

@Serializable
data class MessageReactionUsersResponse(
    val users: List<User> = emptyList(),
    val cookie: String? = null,
    @SerialName("is_end") val isEnd: Boolean? = null
)

@Serializable
data class MessageAudited(
    @SerialName("audit_id") val auditId: String? = null,
    @SerialName("message_id") val messageId: String? = null,
    @SerialName("guild_id") val guildId: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("audit_time") val auditTime: String? = null,
    @SerialName("create_time") val createTime: String? = null,
    @SerialName("seq_in_channel") val seqInChannel: String? = null
)

@Serializable
data class MessageDelete(
    val message: Message? = null,
    @SerialName("op_user") val opUser: User? = null
)

@Serializable
data class CreateDirectMessageRequest(
    @SerialName("recipient_id") val recipientId: String = "",
    @SerialName("source_guild_id") val sourceGuildId: String = ""
)

@Serializable
data class DirectMessageSession(
    @SerialName("guild_id") val guildId: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("create_time") val createTime: String? = null
)

@Serializable
data class ChannelMessageSendRequest(
    val content: String? = null,
    val embed: ChannelMessageEmbed? = null,
    val ark: ChannelMessageArk? = null,
    @SerialName("message_reference") val messageReference: MessageReference? = null,
    val image: String? = null,
    @SerialName("msg_id") val messageId: String? = null,
    @SerialName("event_id") val eventId: String? = null,
    val markdown: MessageMarkdown? = null
)

/** Multipart `file_image` part for direct image upload through [cn.qfys521.qqbot.http.QQBotApi.sendChannelMessage]. */
data class ChannelMessageImageFile(
    val fileName: String,
    val bytes: ByteArray,
    val contentType: String = "application/octet-stream"
)

@Serializable
data class ChannelMessageEmbed(
    val title: String? = null,
    val prompt: String? = null,
    val thumbnail: ChannelMessageEmbedThumbnail? = null,
    val fields: List<ChannelMessageEmbedField> = emptyList()
)

@Serializable
data class ChannelMessageEmbedThumbnail(val url: String? = null)

@Serializable
data class ChannelMessageEmbedField(val name: String? = null)

@Serializable
data class ChannelMessageArk(
    @SerialName("template_id") val templateId: Int? = null,
    val kv: List<ChannelMessageArkKv> = emptyList()
)

@Serializable
data class ChannelMessageArkKv(
    val key: String? = null,
    val value: String? = null,
    val obj: List<ChannelMessageArkObj> = emptyList()
)

@Serializable
data class ChannelMessageArkObj(@SerialName("obj_kv") val objKv: List<ChannelMessageArkObjKv> = emptyList())

@Serializable
data class ChannelMessageArkObjKv(val key: String? = null, val value: String? = null)

@Serializable
data class ChannelMessage(
    val id: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("guild_id") val guildId: String? = null,
    val content: String? = null,
    val timestamp: String? = null,
    @SerialName("edited_timestamp") val editedTimestamp: String? = null,
    val tts: Boolean? = null,
    @SerialName("mention_everyone") val mentionEveryone: Boolean? = null,
    val author: User? = null,
    val attachments: List<MessageAttachment> = emptyList(),
    val embeds: List<ChannelMessageEmbed> = emptyList(),
    val mentions: List<User> = emptyList(),
    val member: GuildMember? = null,
    val ark: ChannelMessageArk? = null,
    val seq: Long? = null,
    @SerialName("seq_in_channel") val seqInChannel: String? = null,
    @SerialName("message_reference") val messageReference: MessageReference? = null,
    val pinned: Boolean? = null,
    val type: Int? = null,
    val flags: Int? = null
)

@Serializable
data class MessageReference(
    @SerialName("message_id") val messageId: String? = null,
    @SerialName("ignore_get_message_error") val ignoreGetMessageError: Boolean? = null
)

@Serializable
data class PinsMessage(
    @SerialName("guild_id") val guildId: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("message_ids") val messageIds: List<String> = emptyList()
)

@Serializable
data class Schedule(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    @SerialName("start_timestamp") val startTimestamp: String? = null,
    @SerialName("end_timestamp") val endTimestamp: String? = null,
    val creator: GuildMember? = null,
    @SerialName("jump_channel_id") val jumpChannelId: String? = null,
    @SerialName("remind_type") val remindType: String? = null
)

@Serializable
data class ScheduleRequest(val schedule: Schedule)

@Serializable
data class Announces(
    @SerialName("guild_id") val guildId: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("message_id") val messageId: String? = null,
    @SerialName("announces_type") val announcesType: Int? = null,
    @SerialName("recommend_channels") val recommendChannels: List<RecommendChannel> = emptyList()
)

@Serializable
data class RecommendChannel(
    @SerialName("channel_id") val channelId: String = "",
    val introduce: String? = null
)

@Serializable
data class CreateAnnouncesRequest(
    @SerialName("message_id") val messageId: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("announces_type") val announcesType: Int? = null,
    @SerialName("recommend_channels") val recommendChannels: List<RecommendChannel>? = null
)

@Serializable
data class ForumThread(
    @SerialName("guild_id") val guildId: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("author_id") val authorId: String? = null,
    @SerialName("thread_info") val threadInfo: ForumThreadInfo? = null
)

@Serializable
data class ForumThreadInfo(
    @SerialName("thread_id") val threadId: String? = null,
    val title: JsonElement? = null,
    val content: JsonElement? = null,
    @SerialName("date_time") val dateTime: String? = null
)

@Serializable
data class ForumPost(
    @SerialName("guild_id") val guildId: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("author_id") val authorId: String? = null,
    @SerialName("post_info") val postInfo: ForumPostInfo? = null
)

@Serializable
data class ForumPostInfo(
    @SerialName("thread_id") val threadId: String? = null,
    @SerialName("post_id") val postId: String? = null,
    val content: JsonElement? = null,
    @SerialName("date_time") val dateTime: String? = null
)

@Serializable
data class ForumReply(
    @SerialName("guild_id") val guildId: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("author_id") val authorId: String? = null,
    @SerialName("reply_info") val replyInfo: ForumReplyInfo? = null
)

@Serializable
data class ForumReplyInfo(
    @SerialName("thread_id") val threadId: String? = null,
    @SerialName("post_id") val postId: String? = null,
    @SerialName("reply_id") val replyId: String? = null,
    val content: JsonElement? = null,
    @SerialName("date_time") val dateTime: String? = null
)

@Serializable
data class ForumAuditResult(
    @SerialName("guild_id") val guildId: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("author_id") val authorId: String? = null,
    @SerialName("thread_id") val threadId: String? = null,
    @SerialName("post_id") val postId: String? = null,
    @SerialName("reply_id") val replyId: String? = null,
    val type: Int? = null,
    val result: Int? = null,
    @SerialName("err_msg") val errorMessage: String? = null
)

@Serializable
data class ForumRichObject(
    val type: Int? = null,
    @SerialName("text_info") val textInfo: ForumTextInfo? = null,
    @SerialName("at_info") val atInfo: ForumAtInfo? = null,
    @SerialName("url_info") val urlInfo: ForumUrlInfo? = null,
    @SerialName("emoji_info") val emojiInfo: ForumEmojiInfo? = null,
    @SerialName("channel_info") val channelInfo: ForumChannelInfo? = null
)

@Serializable
data class ForumTextInfo(val text: String? = null)

@Serializable
data class ForumAtInfo(
    val type: Int? = null,
    @SerialName("user_info") val userInfo: ForumAtUserInfo? = null,
    @SerialName("role_info") val roleInfo: ForumAtRoleInfo? = null,
    @SerialName("guild_info") val guildInfo: ForumAtGuildInfo? = null
)

@Serializable
data class ForumAtUserInfo(val id: String? = null, val nick: String? = null)

@Serializable
data class ForumAtRoleInfo(
    @SerialName("role_id") val roleId: JsonElement? = null,
    val name: String? = null,
    val color: Long? = null
)

@Serializable
data class ForumAtGuildInfo(
    @SerialName("guild_id") val guildId: String? = null,
    @SerialName("guild_name") val guildName: String? = null
)

@Serializable
data class ForumUrlInfo(val url: String? = null, @SerialName("display_text") val displayText: String? = null)

@Serializable
data class ForumEmojiInfo(
    val id: JsonElement? = null,
    val type: String? = null,
    val name: String? = null,
    val url: String? = null
)

@Serializable
data class ForumChannelInfo(
    @SerialName("channel_id") val channelId: JsonElement? = null,
    @SerialName("channel_name") val channelName: String? = null
)

@Serializable
data class ForumRichText(val paragraphs: List<ForumParagraph> = emptyList())

@Serializable
data class ForumParagraph(
    val elems: List<ForumRichTextElement> = emptyList(),
    val props: ForumParagraphProps? = null
)

@Serializable
data class ForumRichTextElement(
    val text: ForumTextElement? = null,
    val image: ForumImageElement? = null,
    val video: ForumVideoElement? = null,
    val url: ForumUrlElement? = null,
    val type: Int? = null
)

@Serializable
data class ForumTextElement(val text: String? = null, val props: ForumTextProps? = null)

@Serializable
data class ForumTextProps(
    @SerialName("font_bold") val fontBold: Boolean? = null,
    val italic: Boolean? = null,
    val underline: Boolean? = null
)

@Serializable
data class ForumImageElement(
    @SerialName("third_url") val thirdUrl: String? = null,
    @SerialName("width_percent") val widthPercent: Double? = null
)

@Serializable
data class ForumPlatformImage(
    val url: String? = null,
    val width: Long? = null,
    val height: Long? = null,
    @SerialName("image_id") val imageId: String? = null
)

@Serializable
data class ForumVideoElement(@SerialName("third_url") val thirdUrl: String? = null)

@Serializable
data class ForumPlatformVideo(
    val url: String? = null,
    val width: Long? = null,
    val height: Long? = null,
    @SerialName("video_id") val videoId: String? = null,
    val duration: Long? = null,
    val cover: ForumPlatformImage? = null
)

@Serializable
data class ForumUrlElement(val url: String? = null, val desc: String? = null)

@Serializable
data class ForumParagraphProps(val alignment: Int? = null)

@Serializable
data class ForumThreadResponse(val thread: ForumThread? = null)

@Serializable
data class ForumThreadsResponse(
    val threads: List<ForumThread> = emptyList(),
    @SerialName("is_finish") val isFinish: Int? = null
)

@Serializable
data class CreateForumThreadRequest(
    val title: String = "",
    val content: String = "",
    val format: Int? = null
)

@Serializable
data class CreateForumThreadResponse(
    @SerialName("task_id") val taskId: String? = null,
    @SerialName("create_time") val createTime: String? = null
)

@Serializable
data class AudioControlRequest(
    @SerialName("audio_url") val audioUrl: String? = null,
    val text: String? = null,
    val status: Int
)

@Serializable
data class AudioAction(
    @SerialName("guild_id") val guildId: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("audio_url") val audioUrl: String? = null,
    val text: String? = null
)

@Serializable
data class OnlineNumbersResponse(@SerialName("online_nums") val onlineNumbers: Int? = null)

@Serializable
data class AudioOrLiveChannelMemberEvent(
    @SerialName("guild_id") val guildId: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("channel_type") val channelType: Int? = null,
    @SerialName("user_id") val userId: String? = null
)

@Serializable
data class ApiPermission(
    val path: String? = null,
    val method: String? = null,
    val desc: String? = null,
    @SerialName("auth_status") val authStatus: Int? = null
)

@Serializable
data class ApiPermissionsResponse(val apis: List<ApiPermission> = emptyList())

@Serializable
data class ApiPermissionDemandIdentify(
    val path: String = "",
    val method: String = ""
)

@Serializable
data class ApiPermissionDemandRequest(
    @SerialName("channel_id") val channelId: String = "",
    @SerialName("api_identify") val apiIdentify: ApiPermissionDemandIdentify,
    val desc: String = ""
)

@Serializable
data class ApiPermissionDemand(
    @SerialName("guild_id") val guildId: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("api_identify") val apiIdentify: ApiPermissionDemandIdentify? = null,
    val title: String? = null,
    val desc: String? = null
)
