package com.example.njupter.notification

import com.google.gson.JsonObject

/** Xiaomi public template v2; OS3 adds an island to the same focus card. */
internal object CourseFocusPayload {
    const val PIC = "miui.focus.pic_course"
    const val ACTION = "miui.focus.action_timetable"

    fun build(protocol: Int, name: String, classroom: String, timeText: String, lifetimeMillis: Long): String? {
        if (protocol < 2 || lifetimeMillis <= 0) return null
        val start = timeText.substringBefore('-').trim()
        val location = classroom.ifBlank { "教室待定" }
        val params = obj {
            addProperty("protocol", 1)
            // Confirm this business identifier with Xiaomi during scenario review.
            addProperty("business", "course_reminder")
            addProperty("enableFloat", false)
            addProperty("islandFirstFloat", true)
            addProperty("updatable", false)
            addProperty("filterWhenNoPermission", false)
            addProperty("timeout", ((lifetimeMillis + 59_999) / 60_000).coerceAtLeast(1))
            addProperty("ticker", "$start 上课 · $location")
            addProperty("tickerPic", PIC)
            addProperty("aodTitle", "$start $name · $location")
            addProperty("aodPic", PIC)
            add("baseInfo", obj {
                addProperty("type", 2)
                addProperty("title", name)
                addProperty("content", location)
                addProperty("subContent", timeText)
                addProperty("showDivider", true)
            })
            add("hintInfo", obj {
                addProperty("type", 2)
                addProperty("content", "上课时间")
                addProperty("title", start)
                add("actionInfo", obj { addProperty("action", ACTION) })
            })
            if (protocol >= 3) add("param_island", obj {
                addProperty("islandProperty", 1)
                addProperty("islandTimeout", ((lifetimeMillis + 999) / 1000).coerceAtLeast(1))
                add("bigIslandArea", obj {
                    add("imageTextInfoLeft", obj {
                        addProperty("type", 1)
                        add("picInfo", picture())
                        add("textInfo", obj {
                            addProperty("title", start)
                            addProperty("content", "上课")
                        })
                    })
                    add("textInfo", obj { addProperty("title", location) })
                })
                add("smallIslandArea", obj { add("picInfo", picture()) })
            })
        }
        return obj { add("param_v2", params) }.toString()
    }

    private fun picture() = obj { addProperty("type", 1); addProperty("pic", PIC) }
    private inline fun obj(block: JsonObject.() -> Unit) = JsonObject().apply(block)
}
