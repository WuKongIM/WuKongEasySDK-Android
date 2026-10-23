package com.githubim.easysdk.example.stream

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class StreamTimelineTest {
    private fun event(id: String, type: String, offset: Int, payload: String) = JsonParser.parseString(
        """{"id":"$id","type":"stream.$type","data":{"channel_id":"room","channel_type":2,"client_msg_no":"key","event_key":"main","text_offset":$offset,"payload":$payload}}""").asJsonObject

    @Test fun `UTF8 deltas merge overlaps and ignore retries`() {
        val timeline = StreamTimeline("room")
        timeline.apply(event("1", "delta", 0, """{"delta":"你好🙂"}"""))
        timeline.apply(event("1", "delta", 0, """{"delta":"你好🙂"}"""))
        timeline.apply(event("2", "delta", 6, """{"delta":"🙂世界"}"""))
        assertEquals("你好🙂世界", timeline.rows.single().text)
    }

    @Test fun `recovery snapshot and buffered deltas do not duplicate text`() {
        val timeline = StreamTimeline("room")
        timeline.restore(JsonParser.parseString("""{"messages":[{"client_msg_no":"key","message_seq":3,"setting":2,"event_meta":{"events":[{"event_key":"main","status":"open","snapshot":{"kind":"text","text":"你好🙂"}}]}}]}""").asJsonObject)
        timeline.apply(event("1", "delta", 0, """{"delta":"你好🙂"}"""))
        timeline.apply(event("2", "delta", 10, """{"delta":"继续"}"""))
        assertEquals("你好🙂继续", timeline.rows.single().text)
    }

    @Test fun `terminal cancel survives finish and late delta`() {
        val timeline = StreamTimeline("room")
        timeline.apply(event("1", "cancel", 0, """{"snapshot":{"kind":"text","text":"部分"}}"""))
        timeline.apply(event("2", "finish", 0, """{"snapshot":{"kind":"text","text":"部分"}}"""))
        timeline.apply(event("3", "delta", 6, """{"delta":"迟到"}"""))
        assertEquals("部分", timeline.rows.single().text)
        assertEquals("已取消", timeline.rows.single().status)
    }

    @Test fun `gap waits for snapshot and other channels stay invisible`() {
        val timeline = StreamTimeline("room")
        timeline.apply(event("1", "delta", 10, """{"delta":"缺失"}"""))
        assertEquals("", timeline.rows.single().text)
        assertEquals("增量中断，等待快照", timeline.rows.single().status)
        timeline.apply(event("2", "finish", 0, """{"snapshot":{"kind":"text","text":"完整内容"}}"""))
        assertEquals("完整内容", timeline.rows.single().text)
        val foreign = event("3", "delta", 0, """{"delta":"other"}""")
        foreign["data"].asJsonObject.addProperty("channel_id", "other-room")
        timeline.apply(foreign)
        assertEquals(1, timeline.rows.size)
    }
}
