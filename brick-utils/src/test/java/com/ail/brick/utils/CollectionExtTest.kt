package com.ail.brick.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * CollectionExt 扩展函数的单元测试。
 */
class CollectionExtTest {

    // ==================== ifNotEmpty ====================

    @Test
    fun `ifNotEmpty fires for non-empty list`() {
        var fired = false
        listOf(1, 2, 3).ifNotEmpty { fired = true }
        assertTrue(fired)
    }

    @Test
    fun `ifNotEmpty does not fire for empty list`() {
        var fired = false
        emptyList<Int>().ifNotEmpty { fired = true }
        assertFalse(fired)
    }

    @Test
    fun `ifNotEmpty does not fire for null`() {
        var fired = false
        val list: List<Int>? = null
        list.ifNotEmpty { fired = true }
        assertFalse(fired)
    }

    // ==================== safeJoinToString ====================

    @Test
    fun `safeJoinToString joins elements`() {
        assertEquals("1, 2, 3", listOf(1, 2, 3).safeJoinToString())
    }

    @Test
    fun `safeJoinToString with custom separator`() {
        assertEquals("a|b|c", listOf("a", "b", "c").safeJoinToString(separator = "|"))
    }

    @Test
    fun `safeJoinToString with transform`() {
        assertEquals("2, 4, 6", listOf(1, 2, 3).safeJoinToString { (it * 2).toString() })
    }

    @Test
    fun `safeJoinToString returns empty for null`() {
        val list: List<Int>? = null
        assertEquals("", list.safeJoinToString())
    }

    @Test
    fun `safeJoinToString returns empty for empty list`() {
        assertEquals("", emptyList<Int>().safeJoinToString())
    }
}
