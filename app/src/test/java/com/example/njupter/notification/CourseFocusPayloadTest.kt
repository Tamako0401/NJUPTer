package com.example.njupter.notification

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class CourseFocusPayloadTest {
    @Test fun unsupportedAndExpiredDoNotCreateFocusNotifications() {
        assertNull(CourseFocusPayload.build(1, "课", "", "08:00-09:35", 60_000))
        assertNull(CourseFocusPayload.build(3, "课", "", "08:00-09:35", 0))
    }

    @Test fun os2KeepsCardWithoutIslandAndEscapesCourseData() {
        val name = "课程\"A\"\n实验"
        val p = JsonParser.parseString(CourseFocusPayload.build(2, name, "", "08:00-09:35", 60_001))
            .asJsonObject.getAsJsonObject("param_v2")
        assertFalse(p.has("param_island"))
        assertEquals(name, p.getAsJsonObject("baseInfo").get("title").asString)
        assertEquals("教室待定", p.getAsJsonObject("baseInfo").get("content").asString)
        assertEquals(2, p.get("timeout").asInt)
        assertFalse(p.get("filterWhenNoPermission").asBoolean)
    }

    @Test fun os3UsesOfficialTextFieldAndFiniteLifetime() {
        val p = JsonParser.parseString(CourseFocusPayload.build(3, "大学物理", "教2-304", "08:00-09:35", 600_000))
            .asJsonObject.getAsJsonObject("param_v2")
        val island = p.getAsJsonObject("param_island")
        assertEquals(600, island.get("islandTimeout").asInt)
        val text = island.getAsJsonObject("bigIslandArea")
            .getAsJsonObject("imageTextInfoLeft").getAsJsonObject("textInfo")
        assertEquals("08:00", text.get("title").asString)
        assertEquals(CourseFocusPayload.PIC, island.getAsJsonObject("smallIslandArea")
            .getAsJsonObject("picInfo").get("pic").asString)
        assertEquals(CourseFocusPayload.ACTION, p.getAsJsonObject("hintInfo")
            .getAsJsonObject("actionInfo").get("action").asString)
    }
}
