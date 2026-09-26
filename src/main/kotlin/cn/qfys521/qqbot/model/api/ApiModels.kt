package cn.qfys521.qqbot.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShareLinkRequest(@SerialName("callback_data") val callbackData: String? = null)
@Serializable
data class ShareLinkResponse(val data: ShareLinkData = ShareLinkData())
@Serializable
data class ShareLinkData(val url: String = "")

@Serializable
data class GroupMember(
    @SerialName("member_openid") val memberOpenId: String = "",
    val username: String = "",
    @SerialName("member_role") val memberRole: String = "member",
    val bot: Boolean = false,
    @SerialName("joined_at") val joinedAt: String? = null,
    @SerialName("union_openid") val unionOpenId: String? = null
)
@Serializable
data class GroupMemberPage(
    val members: List<GroupMember> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String = ""
)
@Serializable
data class GroupMemberDetail(
    @SerialName("member_openid") val memberOpenId: String = "",
    val username: String = "",
    @SerialName("member_role") val memberRole: String = "member",
    val bot: Boolean = false,
    @SerialName("joined_at") val joinedAt: String? = null,
    @SerialName("union_openid") val unionOpenId: String? = null
)

@Serializable
data class BlacklistUser(
    @SerialName("union_openid") val unionOpenId: String? = null,
    @SerialName("member_openid") val memberOpenId: String = "",
    val username: String = "",
    @SerialName("banned_at") val bannedAt: String? = null,
    val bot: Boolean = false
)
@Serializable
data class BlacklistPage(
    val users: List<BlacklistUser> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String = ""
)
@Serializable
data class BlacklistOperationRequest(val op: String, @SerialName("member_openids") val memberOpenIds: List<String>)
@Serializable
data class BlacklistOperationResponse(@SerialName("fail_openids") val failOpenIds: List<String> = emptyList())

@Serializable
data class MuteMemberRequest(
    val op: String,
    @SerialName("member_openid") val memberOpenId: String,
    @SerialName("mute_expire_at") val muteExpireAt: String? = null
)
@Serializable
data class RestrictChatSettingRequest(val members: List<MuteMemberRequest> = emptyList())
@Serializable
data class RestrictChatSetting(
    @SerialName("global_rule") val globalRule: GlobalMuteRule = GlobalMuteRule(),
    val members: List<MemberMuteState> = emptyList()
)
@Serializable
data class GlobalMuteRule(
    val mode: String = "none",
    @SerialName("schedule_rules") val scheduleRules: List<MuteScheduleRule> = emptyList(),
    @SerialName("recurring_rules") val recurringRules: List<MuteRecurringRule> = emptyList()
)
@Serializable
data class MuteScheduleRule(
    @SerialName("task_id") val taskId: String = "",
    @SerialName("start_at") val startAt: String = "",
    @SerialName("end_at") val endAt: String = "",
    val enabled: Boolean = true
)
@Serializable
data class MuteRecurringRule(
    @SerialName("task_id") val taskId: String = "",
    val weekdays: List<Int> = emptyList(),
    @SerialName("start_time") val startTime: String = "",
    @SerialName("end_time") val endTime: String = "",
    val enabled: Boolean = true
)
@Serializable
data class MemberMuteState(
    @SerialName("member_openid") val memberOpenId: String = "",
    @SerialName("mute_expire_at") val muteExpireAt: String = "",
    val username: String = "",
    @SerialName("union_openid") val unionOpenId: String? = null
)

@Serializable
data class JoinRequestPage(
    val list: List<JoinRequest> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String = ""
)
@Serializable
data class JoinRequest(
    @SerialName("join_request_id") val joinRequestId: String = "",
    @SerialName("risk_tips") val riskTips: String = "",
    @SerialName("union_openid") val unionOpenId: String? = null,
    @SerialName("member_openid") val memberOpenId: String = "",
    val username: String = "",
    @SerialName("apply_at") val applyAt: String? = null,
    @SerialName("apply_source") val applySource: String? = null,
    @SerialName("invited_by") val invitedBy: String? = null,
    val bot: Boolean = false,
    @SerialName("verify_info") val verifyInfo: VerifyInfo? = null
)
@Serializable
data class VerifyInfo(
    val method: String? = null,
    @SerialName("verify_message") val verifyMessage: String? = null,
    @SerialName("review_qa_list") val reviewQaList: List<ReviewQuestion> = emptyList()
)
@Serializable
data class ReviewQuestion(val question: String = "", val answer: String = "")
@Serializable
data class ApproveJoinRequest(
    val op: String,
    @SerialName("join_request_id") val joinRequestId: String? = null,
    @SerialName("reject_reason") val rejectReason: String? = null,
    @SerialName("add_to_member_blacklist") val addToMemberBlacklist: Boolean? = null
)
@Serializable
data class BatchRemoveMembersRequest(
    @SerialName("member_openids") val memberOpenIds: List<String>,
    @SerialName("add_to_member_blacklist") val addToMemberBlacklist: Boolean = false
)
@Serializable
data class BatchRemoveMembersResponse(
    @SerialName("remove_members_result") val removeMembersResult: String = "",
    @SerialName("add_to_member_blacklist_fail_openids") val blacklistFailOpenIds: List<String> = emptyList()
)

@Serializable
data class JoinApprovalStrategyPage(
    val strategies: List<JoinApprovalStrategy> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String = ""
)
@Serializable
data class JoinApprovalStrategy(
    @SerialName("strategy_id") val strategyId: String = "",
    @SerialName("group_openids") val groupOpenIds: List<String> = emptyList(),
    @SerialName("group_ids") val groupIds: List<String> = emptyList(),
    @SerialName("whitelist_user_count") val whitelistUserCount: Int = 0,
    @SerialName("is_enable") val isEnable: String = "on",
    @SerialName("expire_at") val expireAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val remark: String? = null
)
@Serializable
data class CreateJoinApprovalStrategyRequest(
    @SerialName("group_openids") val groupOpenIds: List<String>? = null,
    @SerialName("group_ids") val groupIds: List<String>? = null,
    @SerialName("is_enable") val isEnable: String? = null,
    @SerialName("expire_at") val expireAt: String? = null,
    val remark: String? = null
)
@Serializable
data class GroupAction(
    val op: String,
    @SerialName("group_openids") val groupOpenIds: List<String>? = null,
    @SerialName("group_ids") val groupIds: List<String>? = null
)
@Serializable
data class UpdateJoinApprovalStrategyRequest(
    @SerialName("is_enable") val isEnable: String? = null,
    @SerialName("expire_at") val expireAt: String? = null,
    @SerialName("group_action") val groupAction: GroupAction? = null,
    val remark: String? = null
)
@Serializable
data class CreateJoinApprovalStrategyResponse(
    @SerialName("strategy_id") val strategyId: String = "",
    @SerialName("is_enable") val isEnable: String = "on",
    @SerialName("expire_at") val expireAt: String? = null
)
@Serializable
data class UpdateJoinApprovalStrategyResponse(
    @SerialName("is_enable") val isEnable: String = "on",
    @SerialName("expire_at") val expireAt: String? = null
)
@Serializable
data class WhitelistUsersRequest(val op: String, @SerialName("whitelist_users") val whitelistUsers: List<String>)
@Serializable
data class WhitelistUsersResponse(
    @SerialName("strategy_id") val strategyId: String = "",
    @SerialName("whitelist_user_count") val whitelistUserCount: Int = 0,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class Menu(val items: List<MenuItem> = emptyList())
@Serializable
data class MenuItem(
    val name: String? = null,
    val type: String? = null,
    @SerialName("sub_menu_items") val subMenuItems: List<SubMenuItem> = emptyList(),
    @SerialName("send_message") val sendMessage: String? = null,
    val link: String? = null,
    val switch: MenuSwitch? = null
)
@Serializable
data class SubMenuItem(
    val name: String? = null,
    val type: String? = null,
    @SerialName("send_message") val sendMessage: String? = null,
    val link: String? = null
)
@Serializable
data class MenuSwitch(@SerialName("switch_id") val switchId: String? = null, val default: Boolean = false)
@Serializable
data class MenuRequest(val menu: Menu? = null)
@Serializable
data class MenuResponse(val menu: Menu? = null, val version: Int = 0)
@Serializable
data class MenuVersion(val version: Int = 0)

@Serializable
data class Panel(
    val items: List<PanelItem> = emptyList(),
    val remark: String? = null,
    val version: Int? = null
)
@Serializable
data class PanelItem(
    val name: String? = null,
    val desc: String? = null,
    val type: String? = null,
    @SerialName("only_admin") val onlyAdmin: Boolean = false,
    val link: String? = null
)
@Serializable
data class CreatePanelRequest(
    val scope: String,
    @SerialName("target_type") val targetType: String? = null,
    @SerialName("user_openids") val userOpenIds: List<String> = emptyList(),
    @SerialName("group_openids") val groupOpenIds: List<String> = emptyList(),
    val panel: Panel
)
@Serializable
data class PanelRecord(
    @SerialName("panel_id") val panelId: String = "",
    val scope: String = "",
    @SerialName("target_type") val targetType: String = "",
    @SerialName("user_openids") val userOpenIds: List<String> = emptyList(),
    @SerialName("group_openids") val groupOpenIds: List<String> = emptyList(),
    val panel: Panel = Panel(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val version: Int = 0
)
@Serializable
data class PanelPage(
    val records: List<PanelRecord> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String = "",
    @SerialName("is_end") val isEnd: Boolean = false
)
@Serializable
data class PanelId(@SerialName("panel_id") val panelId: String = "")
@Serializable
data class UpdatePanelRequest(val panel: Panel)
@Serializable
data class PanelTargetRequest(
    val op: String,
    @SerialName("user_openids") val userOpenIds: List<String> = emptyList(),
    @SerialName("group_openids") val groupOpenIds: List<String> = emptyList()
)

@Serializable
data class UploadPrepareRequest(
    @SerialName("file_type") val fileType: Int,
    @SerialName("file_size") val fileSize: String,
    @SerialName("file_name") val fileName: String,
    val md5: String,
    val sha1: String,
    @SerialName("md5_10m") val md5_10m: String
)
@Serializable
data class UploadPart(
    val index: Int = 0,
    @SerialName("presigned_url") val presignedUrl: String = "",
    @SerialName("block_size") val blockSize: String = ""
)
@Serializable
data class UploadConfig(
    val concurrency: Int = 1,
    @SerialName("retry_timeout") val retryTimeout: Int = 300,
    @SerialName("retry_delay") val retryDelay: Int = 1
)
@Serializable
data class UploadPrepareResponse(
    @SerialName("upload_id") val uploadId: String = "",
    @SerialName("block_size") val blockSize: String = "",
    val parts: List<UploadPart> = emptyList(),
    @SerialName("upload_config") val uploadConfig: UploadConfig = UploadConfig()
)
@Serializable
data class UploadPartFinishRequest(
    @SerialName("upload_id") val uploadId: String,
    @SerialName("part_index") val partIndex: Int,
    @SerialName("block_size") val blockSize: String,
    val md5: String
)
