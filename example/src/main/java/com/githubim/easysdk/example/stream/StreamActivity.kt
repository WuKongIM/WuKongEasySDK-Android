package com.githubim.easysdk.example.stream

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider

/** Native streaming chat example; online text is rendered exclusively from SDK events. */
class StreamActivity : AppCompatActivity() {
    private lateinit var controller: StreamController
    private lateinit var api: EditText
    private lateinit var question: EditText
    private lateinit var reply: EditText
    private lateinit var chunk: EditText
    private lateinit var interval: EditText
    private lateinit var modelUrl: EditText
    private lateinit var modelName: EditText
    private lateinit var modelKey: EditText
    private lateinit var modelMode: CheckBox
    private lateinit var status: TextView
    private lateinit var generator: TextView
    private lateinit var error: TextView
    private lateinit var logs: TextView
    private lateinit var messages: LinearLayout
    private lateinit var chatScroll: ScrollView
    private lateinit var settings: ScrollView
    private lateinit var connect: Button
    private lateinit var receiver: Button
    private lateinit var reset: Button
    private lateinit var generate: Button
    private lateinit var send: Button
    private lateinit var cancel: Button
    private lateinit var fail: Button
    private val ink = Color.rgb(38, 42, 48)
    private val muted = Color.rgb(103, 109, 118)
    private val amber = Color.rgb(255, 218, 99)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        controller = ViewModelProvider(this)[StreamController::class.java]
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(10)); setBackgroundColor(Color.rgb(250, 250, 248))
        }
        setContentView(root)
        val heading = horizontal(root)
        label(heading, "流式聊天", 24, true, weight = 1f)
        label(heading, "ANDROID · EasySDK", 11, false)
        val statuses = horizontal(root)
        status = label(statuses, "未连接", 13, true, weight = 1f).apply { tag = "stream-status" }
        generator = label(statuses, "就绪", 12, false).apply { tag = "stream-generator" }
        api = field(root, "HTTP API 地址", "stream-api", intent.getStringExtra("api_url") ?: "http://10.0.2.2:5001")
        val connection = horizontal(root)
        connect = button(connection, "创建会话并连接", "stream-connect") { controller.connect(api.text.toString()) }
        receiver = button(connection, "断开接收端", "stream-receiver") { controller.toggleReceiver() }
        reset = button(connection, "新会话", "stream-reset") { controller.reset() }
        error = label(root, "", 12, false).apply { setTextColor(Color.rgb(180, 52, 43)); visibility = View.GONE; tag = "stream-error" }
        button(root, "演示设置  ·  模拟回复 / 真实模型", "stream-settings-toggle") {
            val show = settings.visibility != View.VISIBLE
            settings.visibility = if (show) View.VISIBLE else View.GONE
            chatScroll.visibility = if (show) View.GONE else View.VISIBLE
        }
        settings = ScrollView(this).apply { visibility = View.GONE; isFillViewport = false }
        root.addView(settings, LinearLayout.LayoutParams(-1, 0, 1f))
        val options = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        settings.addView(options)
        label(options, "模拟生成", 16, true)
        reply = field(options, "模拟内容", "stream-reply", StreamController.DEFAULT_REPLY, multiline = true).apply { minLines = 3; maxLines = 5 }
        val tuning = horizontal(options)
        chunk = field(tuning, "每段字数（1–64）", "stream-chunk", "4", numeric = true, weight = 1f)
        interval = field(tuning, "间隔毫秒（40–2000）", "stream-interval", "180", numeric = true, weight = 1f)
        val actions = horizontal(options)
        generate = button(actions, "单独生成回复", "stream-generate") {
            controller.generate(reply.text.toString(), chunkValue(), intervalValue())
        }
        fail = button(actions, "模拟失败", "stream-fail") { controller.stop(true) }
        label(options, "真实聊天模型", 16, true)
        modelMode = CheckBox(this).apply { text = "使用兼容模型的 SSE 回复"; tag = "stream-model-mode"; setTextColor(ink) }
        options.addView(modelMode)
        modelUrl = field(options, "模型 URL（基础地址或 /chat/completions）", "stream-model-url", "")
        modelName = field(options, "模型名称", "stream-model-name", "")
        modelKey = field(options, "API Key（仅内存）", "stream-model-key", "").apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            isSaveEnabled = false
            if (android.os.Build.VERSION.SDK_INT >= 26) importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }
        label(options, "在线：SDK 实时事件\n离线 / 重连：完整历史快照\n账号和模型 Key 仅在当前演示会话内存中保留。", 12, false)
        logs = label(options, "", 11, false).apply { tag = "stream-log"; typeface = Typeface.MONOSPACE }
        chatScroll = ScrollView(this).apply { isFillViewport = true }
        root.addView(chatScroll, LinearLayout.LayoutParams(-1, 0, 1f))
        messages = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; tag = "stream-transcript"; setPadding(0, dp(12), 0, dp(12)) }
        chatScroll.addView(messages)
        question = field(root, "输入消息，观察回复逐段到达…", "stream-question", "", multiline = true).apply { maxLines = 4 }
        val composer = horizontal(root)
        send = button(composer, "发送消息", "stream-send", amber) {
            val text = question.text.toString().trim()
            if (text.isNotEmpty() && controller.canSend) {
                controller.send(text, reply.text.toString(), chunkValue(), intervalValue(), modelConfig())
                question.setText("")
            }
        }
        cancel = button(composer, "取消生成", "stream-cancel") { controller.stop() }
        if (savedInstanceState != null) {
            api.setText(savedInstanceState.getString("api", api.text.toString()))
            reply.setText(savedInstanceState.getString("reply", reply.text.toString()))
            chunk.setText(savedInstanceState.getString("chunk", "4")); interval.setText(savedInstanceState.getString("interval", "180"))
            modelUrl.setText(savedInstanceState.getString("model-url", "")); modelName.setText(savedInstanceState.getString("model-name", ""))
            modelMode.isChecked = savedInstanceState.getBoolean("model-mode")
        }
        controller.onChange = { render() }
        render()
    }

    private fun modelConfig(): ModelConfig? = if (!modelMode.isChecked) null else ModelConfig(
        modelUrl.text.toString().trim(), modelKey.text.toString().trim(), modelName.text.toString().trim())
    private fun chunkValue() = chunk.text.toString().toIntOrNull()?.coerceIn(1, 64) ?: 4
    private fun intervalValue() = interval.text.toString().toLongOrNull()?.coerceIn(40, 2000) ?: 180L

    private fun render() {
        status.text = controller.status; generator.text = controller.generator
        connect.text = if (controller.hasSession) "重试连接" else "创建会话并连接"
        connect.isEnabled = !controller.busy && !controller.generating && controller.status != "在线"
        receiver.text = if (controller.userOnline) "断开接收端" else "重连接收端"
        receiver.isEnabled = controller.hasSession && !controller.busy
        reset.isEnabled = !controller.busy && !controller.generating
        api.isEnabled = !controller.hasSession && !controller.busy
        send.isEnabled = controller.canSend
        generate.isEnabled = controller.writerOnline && !controller.generating && !controller.busy
        cancel.isEnabled = controller.generating; fail.isEnabled = controller.generating
        error.text = controller.error; error.visibility = if (controller.error.isEmpty()) View.GONE else View.VISIBLE
        logs.text = controller.logText
        messages.removeAllViews()
        val rows = controller.timeline?.rows.orEmpty()
        if (rows.isEmpty()) label(messages, "从一句问候开始\n\n创建演示会话，发送问题。助手的回答会通过真实 WuKongIM 逐段抵达。", 16, false).apply { gravity = Gravity.CENTER; setPadding(dp(20), dp(60), dp(20), 0) }
        val lastAssistant = rows.lastOrNull { !it.user }
        for (row in rows) {
            val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = if (row.user) Gravity.END else Gravity.START }
            messages.addView(column, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) })
            label(column, if (row.user) "你" else "WuKong 助手", 11, false)
            val body = TextView(this).apply {
                text = row.text.ifEmpty { if (row.finished) "（无文本）" else "正在思考…" }
                textSize = 16f; setTextColor(ink); setTextIsSelectable(true); maxWidth = resources.displayMetrics.widthPixels - dp(68)
                setPadding(dp(14), dp(12), dp(14), dp(12)); background = rounded(if (row.user) amber else Color.rgb(235, 237, 240))
                if (row === lastAssistant) tag = "stream-last-text"
            }
            column.addView(body, LinearLayout.LayoutParams(-2, -2).apply {
                gravity = if (row.user) Gravity.END else Gravity.START
            })
            label(column, row.status, 11, false).apply { if (row === lastAssistant) tag = "stream-last-status" }
        }
        chatScroll.post { chatScroll.fullScroll(View.FOCUS_DOWN) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        for ((key, edit) in listOf("api" to api, "reply" to reply, "chunk" to chunk, "interval" to interval,
            "model-url" to modelUrl, "model-name" to modelName)) outState.putString(key, edit.text.toString())
        outState.putBoolean("model-mode", modelMode.isChecked)
        super.onSaveInstanceState(outState)
    }
    override fun onDestroy() { controller.onChange = null; modelKey.setText(""); super.onDestroy() }

    private fun dp(value: Int) = (resources.displayMetrics.density * value).toInt()
    private fun rounded(color: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(12).toFloat() }
    private fun horizontal(parent: LinearLayout): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; parent.addView(this, LinearLayout.LayoutParams(-1, -2))
    }
    private fun label(parent: LinearLayout, value: String, size: Int, bold: Boolean, weight: Float = 0f): TextView = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(if (bold) ink else muted)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(5), 0, dp(5)); parent.addView(this, LinearLayout.LayoutParams(if (weight > 0) 0 else -2, -2, weight))
    }
    private fun field(parent: LinearLayout, hintText: String, key: String, value: String, multiline: Boolean = false, numeric: Boolean = false, weight: Float = 0f): EditText = EditText(this).apply {
        hint = hintText; tag = key; contentDescription = hintText; textSize = 14f; setTextColor(ink); setHintTextColor(muted)
        inputType = when { numeric -> InputType.TYPE_CLASS_NUMBER; multiline -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE; else -> InputType.TYPE_CLASS_TEXT }
        if (!multiline) setSingleLine(true)
        setPadding(dp(12), dp(10), dp(12), dp(10)); background = rounded(Color.WHITE); setText(value)
        isSaveEnabled = false
        parent.addView(this, LinearLayout.LayoutParams(if (weight > 0) 0 else -1, -2, weight).apply { topMargin = dp(5); bottomMargin = dp(5); rightMargin = dp(4) })
    }
    private fun button(parent: LinearLayout, value: String, key: String, color: Int = Color.rgb(234, 235, 232), action: () -> Unit): Button = Button(this).apply {
        text = value; tag = key; textSize = 12f; isAllCaps = false; setTextColor(ink); minWidth = 0
        val enabled = arrayOf(intArrayOf(android.R.attr.state_enabled), intArrayOf())
        backgroundTintList = android.content.res.ColorStateList(enabled, intArrayOf(color, Color.rgb(240, 240, 237)))
        setTextColor(android.content.res.ColorStateList(enabled, intArrayOf(ink, Color.rgb(160, 165, 171))))
        setOnClickListener { action() }
        parent.addView(this, LinearLayout.LayoutParams(if (parent.orientation == LinearLayout.HORIZONTAL) 0 else -1, -2,
            if (parent.orientation == LinearLayout.HORIZONTAL) 1f else 0f).apply { rightMargin = dp(3) })
    }
}
