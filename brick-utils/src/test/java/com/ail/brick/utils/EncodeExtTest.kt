package com.ail.brick.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * EncodeExt 编解码扩展函数的单元测试。
 *
 * 注意：Base64 测试依赖 android.util.Base64，需在 instrumented test 中运行。
 * 这里只测试纯 JVM 的 Hex 编解码。
 */
class EncodeExtTest {

    // ==================== Hex ====================

    @Test
    fun `toHexString produces correct output`() {
        assertEquals("0aff", byteArrayOf(0x0A, 0xFF.toByte()).toHexString())
    }

    @Test
    fun `toHexString of empty array`() {
        assertEquals("", byteArrayOf().toHexString())
    }

    @Test
    fun `hexToByteArray decodes correctly`() {
        val result = "0aff".hexToByteArray()
        assertEquals(2, result.size)
        assertEquals(0x0A.toByte(), result[0])
        assertEquals(0xFF.toByte(), result[1])
    }

    @Test
    fun `hexToByteArray of empty string`() {
        assertEquals(0, "".hexToByteArray().size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `hexToByteArray throws on odd length`() {
        "0af".hexToByteArray()
    }

    @Test
    fun `hex roundtrip`() {
        val original = "Hello Brick".toByteArray()
        val hex = original.toHexString()
        val decoded = hex.hexToByteArray()
        assertArrayEquals(original, decoded)
    }
}
