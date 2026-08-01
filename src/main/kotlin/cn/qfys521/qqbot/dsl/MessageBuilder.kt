package cn.qfys521.qqbot.dsl

import cn.qfys521.qqbot.model.message.*

/**
 * 构建发送消息请求体 (`SendMessageRequest`) 的专用顶层声明式语法糖。
 *
 * 示例 usage：
 * ```kotlin
 * val request = message {
 *     content = "大家好！"
 *     markdown("# 这是一个 Markdown")
 *     keyboard {
 *         row {
 *             urlButton("btn1", "腾讯官方文档", "https://bot.q.qq.com")
 *         }
 *     }
 * }
 * ```
 *
 * @param block 在 [SendMessageRequestBuilder] 作用域执行的构建属性赋值。
 * @return 拼合完成的 [SendMessageRequest] 网络请求报文结构。
 */
fun message(block: SendMessageRequestBuilder.() -> Unit): SendMessageRequest {
    val builder = SendMessageRequestBuilder().apply(block)
    return builder.build()
}

/**
 * 专供 `message { ... }` 作用域采用构建器模式生成消息包和多重高级卡片/键盘布局的包装类。
 */
class SendMessageRequestBuilder {
    /** 要展示发送给用户的普通纯文字（若已定义了 `markdown(...)`，本字串不会起效）。 */
    var content: String? = null

    /** 特定显式指定的消息类别编码：`0` = 普通纯文本, `2` = Markdown 文本, `7` = 富媒体文件消息。 */
    var msgType: Int? = null

    /** 源于外部事件并计划响应或者撤销回送的目标消息 ID。 */
    var msgId: String? = null

    /** 关联回应用户操作上报产生的交互 `id` 凭证。 */
    var eventId: String? = null

    /** 多次回话消息分配防被网关误拒吞除的唯一增量次序码 (自定默认设为 `1`)。 */
    var msgSeq: Int? = null

    /** 对待引用的某句对话执行气泡样式指向展示的结构。 */
    var messageReference: MessageReference? = null

    /** 附送发出的 Markdown 具体定义属性结构。 */
    var markdown: MessageMarkdown? = null

    /** 与随信息一同展现给对话界面用户的交互按钮键盘模型。 */
    var keyboard: Keyboard? = null

    /** 利用通过 `/files` 接口完成上传得到之文件信息建立展示的富媒体配置。 */
    var media: MediaInfo? = null

    /** 标记本消息是否属于对超时事件主动召回的会话唤醒消息。 */
    var isWakeup: Boolean? = null

    /** 专用来触发在对方窗口打字栏展示正在输入的提示说明。 */
    var inputNotify: InputNotify? = null

    /**
     * 在本次发送中使用 Markdown 原生内容字符串或者既定模板排版。
     *
     * @param content Markdown 原生文本内容，当不传且使用 `customTemplateId` 时可设为空字串或不传。
     * @param customTemplateId 在开放平台后台申请核批获取的专属 Markdown 模板编号。
     * @param templateId 官方公共开箱模板系统数字 ID。
     */
    fun markdown(content: String? = null, customTemplateId: String? = null, templateId: Int? = null) {
        this.msgType = 2
        this.markdown = MessageMarkdown(
            content = content,
            customTemplateId = customTemplateId,
            templateId = templateId
        )
    }

    /**
     * 使用极具表达力的声明式高阶 DSL 在消息末尾追加展示一个多行自适应式交互键盘 (`Keyboard`)。
     *
     * @param id 选填，键盘自定义编号 ID。
     * @param block 在 [KeyboardBuilder] 范围内构造多排按钮的逻辑表达式。
     */
    fun keyboard(id: String? = null, block: KeyboardBuilder.() -> Unit) {
        val builder = KeyboardBuilder().apply(block)
        this.keyboard = Keyboard(id = id, content = KeyboardContent(rows = builder.rows))
    }

    /**
     * 将本消息声明为富媒体文件信息 (`msg_type = 7`) 并关联之前调用 API 后取得的有效媒体令牌。
     *
     * @param fileInfo API 下发的有效 `file_info` 字串。
     */
    fun media(fileInfo: String) {
        this.msgType = 7
        this.media = MediaInfo(fileInfo = fileInfo)
    }

    /**
     * 将本次下达的消息以气泡引用的表现形式关联指向特定源消息。
     *
     * @param messageId 所欲引述指涉的源头具体消息 ID。
     * @param ignoreGetMessageError 是否在此消息原句无法由服务器查阅拉取时跳过报错以正常普通文字继续回复（默认为 `false`）。
     */
    fun reference(messageId: String, ignoreGetMessageError: Boolean = false) {
        this.messageReference = MessageReference(messageId = messageId, ignoreGetMessageError = ignoreGetMessageError)
    }

    /**
     * 内部组装拼合函数，根据赋值项自动推定 [msgType] 并且构造成最终的 [SendMessageRequest] 对象。
     */
    internal fun build(): SendMessageRequest {
        val resolvedMsgType = msgType ?: when {
            markdown != null -> 2
            media != null -> 7
            else -> 0
        }
        return SendMessageRequest(
            content = content,
            msgType = resolvedMsgType,
            markdown = markdown,
            keyboard = keyboard,
            msgId = msgId,
            eventId = eventId,
            msgSeq = msgSeq,
            media = media,
            messageReference = messageReference,
            isWakeup = isWakeup,
            inputNotify = inputNotify
        )
    }
}

/**
 * 构建交互按键键盘 ([Keyboard]) 行排列的特定 DSL 构建器域。
 */
class KeyboardBuilder {
    internal val rows = mutableListOf<Row>()

    /**
     * 声明键盘布局中新增新的一排横行布局，且其内部自适应安置该行包含的所有有效按钮。
     *
     * @param block 面向此行 [RowBuilder] 添加各种不同样式类型按键的声明块。
     */
    fun row(block: RowBuilder.() -> Unit) {
        val rowBuilder = RowBuilder().apply(block)
        if (rowBuilder.buttons.isNotEmpty()) {
            rows.add(Row(buttons = rowBuilder.buttons))
        }
    }
}

/**
 * 键盘同一行布局内添加和排版各个不同功能触达按键 ([Button]) 的 DSL 构建作用域。
 */
class RowBuilder {
    internal val buttons = mutableListOf<Button>()

    /**
     * 插入一个点击后可以直接引导用户直接通过外部默认浏览器或者内置小程序拉起的 URL 跳转类型按钮 (`type=0`)。
     *
     * @param id 按钮在该套件内独有的唯一识别标志。
     * @param label 按键上所渲染印出展示给客户看的主文字文本。
     * @param url 客户按下后需要前往导向的目的地合法 URL 地址。
     * @param style 视觉呈现风格样式编号：`0`=灰底框, `1`=蓝底蓝字, `2`=白底色, `3`=蓝底白字醒目标识。
     * @param visitedLabel 若用户点击操作完毕之后将转化的状态标记说明。
     */
    fun urlButton(
        id: String,
        label: String,
        url: String,
        style: Int = 0,
        visitedLabel: String? = null
    ) {
        buttons.add(
            Button(
                id = id,
                renderData = RenderData(label = label, visitedLabel = visitedLabel, style = style),
                action = Action(type = 0, data = url)
            )
        )
    }

    /**
     * 插入一个点击后直接向服务器触发由客户端后台接收的互动回调事件 (`INTERACTION_CREATE`, `type=1`) 按钮。
     *
     * @param id 按钮识别序号。
     * @param label 正常展示的主按钮文字。
     * @param callbackData 要通过 interaction 事件里的 `data` 参数向机器人上报发回的定制业务字串或标记变量。
     * @param data 与 [callbackData] 等效的方法参数重载或默认引用。
     * @param style 按钮显示样式（如 3 为蓝底实心显眼按钮）。
     * @param visitedLabel 按过此键之后变成的完成说明提示字。
     */
    fun callbackButton(
        id: String,
        label: String,
        callbackData: String = "",
        data: String = callbackData,
        style: Int = 0,
        visitedLabel: String? = null
    ) {
        val resolvedData = if (data.isNotBlank()) data else callbackData
        buttons.add(
            Button(
                id = id,
                renderData = RenderData(label = label, visitedLabel = visitedLabel, style = style),
                action = Action(type = 1, data = resolvedData)
            )
        )
    }

    /**
     * 插入一个当客户触及按压后直接自动把预设命令字符或者问候文本帮用户发送/粘帖至其输入提示处的指令操作型按钮 (`type=2`)。
     *
     * @param id 按钮自身全局识别码。
     * @param label 给到访用户的说明提示按键字。
     * @param commandText 用户按后被引导填充或者自动发信的具体业务命令行文本内容。
     * @param command 与 [commandText] 等效的方法参数重载或默认引用。
     * @param style 样式色彩标记（默认 `0` 灰底边框）。
     * @param visitedLabel 按动完成后的说明。
     */
    fun commandButton(
        id: String,
        label: String,
        commandText: String = "",
        command: String = commandText,
        style: Int = 0,
        visitedLabel: String? = null
    ) {
        val resolvedCommand = if (command.isNotBlank()) command else commandText
        buttons.add(
            Button(
                id = id,
                renderData = RenderData(label = label, visitedLabel = visitedLabel, style = style),
                action = Action(type = 2, data = resolvedCommand)
            )
        )
    }

    /**
     * 支持用户手动配置或者自行定制任意复合权限和高阶特性的底层通用按钮加入工具函数。
     *
     * @param button 已经实例化完善规范的 [Button] 模型单体。
     */
    fun customButton(button: Button) {
        buttons.add(button)
    }
}
