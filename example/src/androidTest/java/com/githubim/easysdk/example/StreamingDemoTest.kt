package com.githubim.easysdk.example

import android.content.Intent
import android.graphics.Bitmap
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Opt-in native UI acceptance against a real, temporary WuKongIM cluster. */
@RunWith(AndroidJUnit4::class)
class StreamingDemoTest {
    @Test fun realServerStreamingConversation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        val api = requireNotNull(args.getString("streamApiUrl")) { "Pass -Pandroid.testInstrumentationRunnerArguments.streamApiUrl" }
        val model = requireNotNull(args.getString("streamModelUrl")) { "Pass streamModelUrl for the local SSE fixture" }
        val context = instrumentation.targetContext
        val artifacts = File(context.getExternalFilesDir(null), "stream-acceptance").apply { mkdirs() }
        val checks = JSONArray()
        val report = JSONObject().put("passed", false).put("checks", checks)
        val intent = Intent().setClassName(context, "com.githubim.easysdk.example.stream.StreamActivity")
            .putExtra("api_url", api).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val scenario = ActivityScenario.launch<android.app.Activity>(intent)
        fun text(tag: String): String {
            var result = ""
            scenario.onActivity { result = it.window.decorView.findViewWithTag<TextView>(tag)?.text?.toString() ?: "" }
            return result
        }
        fun transcript(): String {
            var value = ""
            fun collect(view: android.view.View): String = when (view) {
                is TextView -> view.text.toString()
                is android.view.ViewGroup -> (0 until view.childCount).joinToString("\n") { collect(view.getChildAt(it)) }
                else -> ""
            }
            scenario.onActivity { value = collect(it.window.decorView.findViewWithTag<android.view.View>("stream-transcript")) }
            return value
        }
        fun field(tag: String, value: String) { scenario.onActivity { it.window.decorView.findViewWithTag<EditText>(tag).setText(value) } }
        fun tap(tag: String) { scenario.onActivity { val view = it.window.decorView.findViewWithTag<android.view.View>(tag); assertTrue("Button $tag is disabled", view.isEnabled); view.performClick() } }
        fun until(label: String, predicate: () -> Boolean) {
            val deadline = System.currentTimeMillis() + 30_000
            while (System.currentTimeMillis() < deadline) { if (predicate()) return; Thread.sleep(50) }
            fail("Timed out: $label")
        }
        fun screenshot(name: String) {
            instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
                File(artifacts, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
            }
        }
        fun metrics(): JSONObject = java.net.URL("$model/metrics").openStream().bufferedReader().use { JSONObject(it.readText()) }
        val answer = "你好🙂，这是经过真实 WuKongIM 的流式消息。重连后也能恢复完整内容。"
        try {
            tap("stream-connect")
            until("both authenticated peers and history ready") { text("stream-status") == "在线" }
            val initialReads = metrics().getInt("history_reads")
            field("stream-reply", answer); field("stream-interval", "100"); field("stream-chunk", "2")
            field("stream-question", "什么是流式消息？"); tap("stream-send")
            until("visible live delta before terminal snapshot") { (text("stream-last-text").isNotEmpty() && text("stream-last-text") != "正在思考…") && text("stream-last-status") == "生成中" }
            screenshot("partial.png")
            until("completed SDK answer") { text("stream-last-status") == "完成" }
            assertEquals(answer, text("stream-last-text"))
            assertTrue(transcript().contains("什么是流式消息？"))
            assertTrue(text("stream-log").contains("stream.delta"))
            checks.put("actual SDK SEND, EVENT and progressive UTF-8 reply")
            assertEquals(initialReads, metrics().getInt("history_reads"))
            checks.put("no online history polling")
            screenshot("completed.png")
            for ((action, terminal) in listOf("stream-cancel" to "已取消", "stream-fail" to "失败")) {
                tap("stream-generate")
                until("partial reply before $terminal") { text("stream-last-status") == "生成中" && (text("stream-last-text").isNotEmpty() && text("stream-last-text") != "正在思考…") }
                tap(action)
                until(terminal) { text("stream-last-status") == terminal && text("stream-generator") != "生成中" }
                assertTrue((text("stream-last-text").isNotEmpty() && text("stream-last-text") != "正在思考…"))
                checks.put("$terminal preserves partial snapshot")
            }
            tap("stream-receiver")
            until("receiver offline") { text("stream-status") == "接收端离线" }
            val offline = "离线期间生成🙂，历史快照恢复完整答案。"
            field("stream-reply", offline); field("stream-interval", "40")
            tap("stream-generate")
            until("offline producer complete") { text("stream-generator") == "完成" }
            assertNotEquals(offline, text("stream-last-text"))
            tap("stream-receiver")
            until("history recovered") { text("stream-status") == "在线" && text("stream-last-text") == offline }
            assertEquals("完成", text("stream-last-status"))
            checks.put("offline generation and messagesync recovery")
            scenario.recreate()
            until("activity recreated with same session") { text("stream-status") == "在线" && text("stream-last-text") == offline }
            checks.put("activity recreation recovers the session")
            field("stream-reply", answer.repeat(3)); field("stream-interval", "100")
            tap("stream-generate")
            until("midstream before reconnect") { text("stream-last-status") == "生成中" && (text("stream-last-text").isNotEmpty() && text("stream-last-text") != "正在思考…") }
            tap("stream-receiver"); Thread.sleep(250); tap("stream-receiver")
            until("reconnect during generation") { text("stream-status") == "在线" && text("stream-last-status") == "完成" }
            assertEquals(answer.repeat(3), text("stream-last-text"))
            checks.put("snapshot and buffered live deltas merge without repetition")
            field("stream-model-url", "$model/v1"); field("stream-model-name", "fixture-chat")
            field("stream-model-key", "fixture-private-key"); tap("stream-model-mode")
            field("stream-question", "测试真实模型🙂"); tap("stream-send")
            until("model SSE completion") { text("stream-last-text") == "模型回复🙂，分段到达。" && text("stream-last-status") == "完成" && text("stream-generator") != "生成中" }
            checks.put("local OpenAI-compatible SSE model through actual IM events")
            field("stream-model-name", "fixture-error"); field("stream-question", "模型失败"); tap("stream-send")
            until("model SSE error saved") { text("stream-last-status") == "失败" && text("stream-generator") != "生成中" }
            assertEquals("保留部分内容🙂", text("stream-last-text"))
            field("stream-model-name", "fixture-truncated"); field("stream-question", "模型断流"); tap("stream-send")
            until("truncated SSE saved as failure") { text("stream-last-text") == "断流前内容" && text("stream-last-status") == "失败" && text("stream-generator") != "生成中" }
            field("stream-model-name", "fixture-slow"); field("stream-question", "取消模型"); tap("stream-send")
            until("slow SSE delta") { text("stream-last-status") == "生成中" && (text("stream-last-text").isNotEmpty() && text("stream-last-text") != "正在思考…") }
            tap("stream-cancel")
            until("model cancelled") { text("stream-last-status") == "已取消" && text("stream-generator") != "生成中" }
            checks.put("SSE error, unexpected EOF and upstream cancellation")
            assertFalse(text("stream-log").contains("fixture-private-key"))
            assertFalse(transcript().contains("fixture-private-key"))
            checks.put("model key excluded from transcript and logs")
            field("stream-model-key", "")
            screenshot("recovered.png")
            assertEquals(0, metrics().getInt("eventsync_reads"))
            report.put("passed", true)
        } finally {
            if (!report.getBoolean("passed")) screenshot("failure.png")
            File(artifacts, "report.json").writeText(report.toString(2))
            scenario.close()
        }
    }
}
