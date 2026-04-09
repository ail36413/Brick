package com.ail.brick.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * StringExt 扩展函数的单元测试。
 *
 * 注意：这些测试验证纯 Kotlin 逻辑，不依赖 Android 框架。
 * Android 相关的扩展（如 Context 相关）需要 instrumented test。
 */
class StringExtTest {

    // ==================== isPhoneNumber ====================

    @Test
    fun `valid phone numbers`() {
        assertTrue("13812345678".isPhoneNumber())
        assertTrue("15912345678".isPhoneNumber())
        assertTrue("18612345678".isPhoneNumber())
        assertTrue("17012345678".isPhoneNumber())
    }

    @Test
    fun `invalid phone numbers`() {
        assertFalse("".isPhoneNumber())
        assertFalse("1381234567".isPhoneNumber())     // 10 digits
        assertFalse("138123456789".isPhoneNumber())   // 12 digits
        assertFalse("12345678901".isPhoneNumber())    // starts with 12
        assertFalse("abc12345678".isPhoneNumber())    // contains letters
        assertFalse("038 1234 5678".isPhoneNumber())  // starts with 0
    }

    // ==================== isEmail ====================

    @Test
    fun `valid emails`() {
        assertTrue("test@example.com".isEmail())
        assertTrue("user.name@domain.co.uk".isEmail())
        assertTrue("user+tag@example.org".isEmail())
    }

    @Test
    fun `invalid emails`() {
        assertFalse("".isEmail())
        assertFalse("not-an-email".isEmail())
        assertFalse("@example.com".isEmail())
        assertFalse("user@".isEmail())
    }

    // ==================== maskPhone ====================

    @Test
    fun `maskPhone hides middle digits`() {
        assertEquals("138****5678", "13812345678".maskPhone())
    }

    @Test
    fun `maskPhone returns original for short strings`() {
        assertEquals("123", "123".maskPhone())
        assertEquals("1234567", "1234567".maskPhone())
    }

    // ==================== md5 ====================

    @Test
    fun `md5 produces correct hash`() {
        // Well-known MD5 of "hello"
        assertEquals("5d41402abc4b2a76b9719d911017c592", "hello".md5())
    }

    @Test
    fun `md5 of empty string`() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", "".md5())
    }

    // ==================== sha256 ====================

    @Test
    fun `sha256 produces correct hash`() {
        // Well-known SHA-256 of "hello"
        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            "hello".sha256()
        )
    }

    @Test
    fun `sha256 of empty string`() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            "".sha256()
        )
    }

    // ==================== isUrl ====================

    @Test
    fun `valid urls`() {
        assertTrue("https://example.com".isUrl())
        assertTrue("http://example.com/path".isUrl())
        assertTrue("https://sub.domain.com/path?q=1&a=2".isUrl())
    }

    @Test
    fun `invalid urls`() {
        assertFalse("".isUrl())
        assertFalse("not-a-url".isUrl())
        assertFalse("ftp://example.com".isUrl())
        assertFalse("example.com".isUrl())
    }

    // ==================== orDefault ====================

    @Test
    fun `orDefault returns value when not blank`() {
        assertEquals("hello", "hello".orDefault("default"))
    }

    @Test
    fun `orDefault returns default when null or blank`() {
        assertEquals("default", (null as String?).orDefault("default"))
        assertEquals("default", "".orDefault("default"))
        assertEquals("default", "   ".orDefault("default"))
    }

    // ==================== ellipsize ====================

    @Test
    fun `ellipsize truncates long strings`() {
        assertEquals("Hello…", "Hello World".ellipsize(5))
        assertEquals("Hi", "Hi".ellipsize(5))
    }
}
