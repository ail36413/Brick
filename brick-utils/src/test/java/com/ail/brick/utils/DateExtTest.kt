package com.ail.brick.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * DateExt 扩展函数的单元测试。
 */
class DateExtTest {

    @Test
    fun `formatDate produces correct format`() {
        // 使用已知时间戳验证格式化
        val formatted = System.currentTimeMillis().formatDate("yyyy")
        assertEquals("2026", formatted)
    }

    @Test
    fun `parseDate with valid input`() {
        val date = "2026-04-03".parseDate("yyyy-MM-dd")
        assertNotNull(date)
    }

    @Test
    fun `parseDate with invalid input returns null`() {
        val date = "not-a-date".parseDate("yyyy-MM-dd")
        assertNull(date)
    }

    @Test
    fun `isToday returns true for current time`() {
        assertTrue(System.currentTimeMillis().isToday())
    }

    @Test
    fun `isToday returns false for yesterday`() {
        assertFalse((System.currentTimeMillis() - 86_400_000L * 2).isToday())
    }

    @Test
    fun `isYesterday returns true for yesterday timestamp`() {
        assertTrue((System.currentTimeMillis() - 86_400_000L).isYesterday())
    }

    @Test
    fun `isYesterday returns false for today`() {
        assertFalse(System.currentTimeMillis().isYesterday())
    }

    @Test
    fun `isSameDay returns true for same day timestamps`() {
        val now = System.currentTimeMillis()
        assertTrue(now.isSameDay(now - 1000L))
    }

    @Test
    fun `isSameDay returns false for different days`() {
        val now = System.currentTimeMillis()
        assertFalse(now.isSameDay(now - 86_400_000L * 2))
    }

    @Test
    fun `toFriendlyTime shows correct labels`() {
        val now = System.currentTimeMillis()
        assertEquals("刚刚", (now - 30_000L).toFriendlyTime())
        assertTrue((now - 5 * 60_000L).toFriendlyTime().contains("分钟前"))
        assertTrue((now - 3 * 3_600_000L).toFriendlyTime().contains("小时前"))
        assertEquals("昨天", (now - 30 * 3_600_000L).toFriendlyTime())
    }
}
