package com.ail.brick.utils

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files

/**
 * FileExt 文件扩展函数的单元测试（纯 JVM）。
 */
class FileExtTest {

    private lateinit var tempDir: File

    @Before
    fun setup() {
        tempDir = Files.createTempDirectory("brick_file_ext_test_").toFile()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    // ==================== friendlySize ====================

    @Test
    fun `friendlySize bytes`() {
        val f = File(tempDir, "tiny.txt").apply { writeText("hi") }
        assertEquals("2 B", f.friendlySize())
    }

    @Test
    fun `friendlySize kilobytes`() {
        val f = File(tempDir, "kb.bin").apply { writeBytes(ByteArray(2048)) }
        assertEquals("2.0 KB", f.friendlySize())
    }

    @Test
    fun `friendlySize megabytes`() {
        val f = File(tempDir, "mb.bin").apply { writeBytes(ByteArray(1024 * 1024 * 3)) }
        assertEquals("3.0 MB", f.friendlySize())
    }

    // ==================== extensionName ====================

    @Test
    fun `extensionName returns lowercase`() {
        assertEquals("jpg", File("photo.JPG").extensionName)
    }

    @Test
    fun `extensionName no extension`() {
        assertEquals("", File("Makefile").extensionName)
    }

    // ==================== safeDeleteRecursively ====================

    @Test
    fun `safeDeleteRecursively deletes directory tree`() {
        val dir = File(tempDir, "a/b/c").apply { mkdirs() }
        File(dir, "file.txt").writeText("hello")
        assertTrue(File(tempDir, "a").safeDeleteRecursively())
        assertFalse(File(tempDir, "a").exists())
    }

    @Test
    fun `safeDeleteRecursively deletes single file`() {
        val f = File(tempDir, "single.txt").apply { writeText("x") }
        assertTrue(f.safeDeleteRecursively())
        assertFalse(f.exists())
    }

    // ==================== ensureParentDir ====================

    @Test
    fun `ensureParentDir creates parent directories`() {
        val f = File(tempDir, "deep/nested/dir/file.txt")
        f.ensureParentDir()
        assertTrue(f.parentFile!!.exists())
    }

    @Test
    fun `ensureParentDir returns self for chaining`() {
        val f = File(tempDir, "chain/file.txt")
        assertSame(f, f.ensureParentDir())
    }

    // ==================== md5 / sha256 ====================

    @Test
    fun `md5 returns correct hash`() {
        val f = File(tempDir, "hash.txt").apply { writeText("hello") }
        // Known MD5 of "hello"
        assertEquals("5d41402abc4b2a76b9719d911017c592", f.md5())
    }

    @Test
    fun `sha256 returns correct hash`() {
        val f = File(tempDir, "hash.txt").apply { writeText("hello") }
        // Known SHA-256 of "hello"
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", f.sha256())
    }

    // ==================== totalSize ====================

    @Test
    fun `totalSize of file`() {
        val f = File(tempDir, "size.txt").apply { writeText("12345") }
        assertEquals(5L, f.totalSize())
    }

    @Test
    fun `totalSize of directory sums all files`() {
        File(tempDir, "a.txt").writeText("123")     // 3
        File(tempDir, "sub").mkdir()
        File(tempDir, "sub/b.txt").writeText("4567") // 4
        assertEquals(7L, tempDir.totalSize())
    }

    // ==================== writeToFile ====================

    @Test
    fun `writeToFile creates file from InputStream`() {
        val target = File(tempDir, "output/data.bin")
        ByteArrayInputStream("stream content".toByteArray()).writeToFile(target)
        assertTrue(target.exists())
        assertEquals("stream content", target.readText())
    }

    // ==================== readTextOrNull ====================

    @Test
    fun `readTextOrNull returns content for existing file`() {
        val f = File(tempDir, "read.txt").apply { writeText("content") }
        assertEquals("content", f.readTextOrNull())
    }

    @Test
    fun `readTextOrNull returns null for non-existent file`() {
        assertNull(File(tempDir, "missing.txt").readTextOrNull())
    }
}
