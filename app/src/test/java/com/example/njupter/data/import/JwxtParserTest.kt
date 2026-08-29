package com.example.njupter.data.import

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class JwxtParserTest {
    private val parser = JwxtParser()

    @Test
    fun parseHtml_readsNewListTableAndFiltersPendingCourses() {
        val courses = parser.parseHtml(NEW_JWGLXT_HTML)

        assertEquals(3, courses.size)
        assertEquals(
            RemoteCourse(
                name = "大学物理（下）",
                teacher = "闫巍",
                classroom = "教3－520",
                dayOfWeek = 1,
                startSection = 1,
                endSection = 2,
                weeks = (2..18).filter { it % 2 == 0 }
            ),
            courses[0]
        )
        assertEquals("物理实验（下）", courses[1].name)
        assertEquals((1..17).filter { it % 2 == 1 }, courses[1].weeks)
        assertEquals(listOf(1, 3, 5, 8, 9, 10), courses[2].weeks)
    }

    @Test
    fun parseHtml_reportsAuthenticationPageInsteadOfReturningEmptyData() {
        assertThrows(IllegalArgumentException::class.java) {
            parser.parseHtml("<html><body><form id='login'>统一认证</form></body></html>")
        }
    }

    private companion object {
        val NEW_JWGLXT_HTML = """
            <table id="kblist_table">
              <tr><td>课表标题</td></tr>
              <tr class="tbody_head"><td>星期</td><td>节次</td><td>课表信息</td></tr>
              <tr><td rowspan="3"><span class="week">星期一</span></td></tr>
              <tr>
                <td><span class="festival">1-2</span></td>
                <td><div class="timetable_con text-left">
                  <span class="title"><font color="blue">大学物理（下）</font></span>
                  <p><font color="blue">周数：2-18周(双)</font><font color="blue">校区:仙林 上课地点：教3－520</font><font color="blue">教师 ：闫巍</font><font color="blue">学分：3</font><font color="blue">课程性质：必修</font></p>
                </div></td>
              </tr>
              <tr>
                <td><span class="festival">3-5</span></td>
                <td><div class="timetable_con text-left">
                  <span class="title"><font color="blue">物理实验 （下）</font></span>
                  <p><font color="blue">周数：1－17周（单）</font><font color="blue">校区:仙林 上课地点：未排地点</font><font color="blue">教师 ：王老师</font></p>
                </div></td>
              </tr>
              <tr><td rowspan="2"><span class="week">星期二</span></td></tr>
              <tr>
                <td><span class="festival">6-7</span></td>
                <td>
                  <div class="timetable_con text-left">
                    <span class="title"><font color="blue">信号与系统</font></span>
                    <p><font color="blue">周数：1,3,5,8-10周</font><font color="blue">校区:仙林 上课地点：教3-300</font><font color="blue">教师 ：孙老师</font></p>
                  </div>
                  <div class="timetable_con text-left">
                    <span class="title"><font color="red"><i>待筛选课程</i></font></span>
                    <p><font color="red">周数：1-18周</font><font color="red">上课地点：教1-101</font><font color="red">教师 ：待定</font></p>
                  </div>
                </td>
              </tr>
            </table>
        """.trimIndent()
    }
}
