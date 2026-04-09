package com.ail.brick.utils

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * 获取文件大小的人类可读描述
 *
 * ```kotlin
 * File("photo.jpg").friendlySize()  // "2.3 MB"
 * ```
 *
 * @return 格式化后的大小字符串（B / KB / MB / GB）
 */
fun File.friendlySize(): String {
    val size = length().toDouble()
    return when {
        size < 1024 -> "%.0f B".format(size)
        size < 1024 * 1024 -> "%.1f KB".format(size / 1024)
        size < 1024 * 1024 * 1024 -> "%.1f MB".format(size / (1024 * 1024))
        else -> "%.2f GB".format(size / (1024 * 1024 * 1024))
    }
}

/**
 * 获取文件扩展名（小写，不含点号）
 *
 * ```kotlin
 * File("photo.JPG").extensionName  // "jpg"
 * ```
 */
val File.extensionName: String get() = extension.lowercase()

/**
 * 安全删除文件或目录（递归删除子文件）
 *
 * @return 删除是否成功
 */
fun File.safeDeleteRecursively(): Boolean {
    return if (isDirectory) {
        (listFiles()?.all { it.safeDeleteRecursively() } ?: true) && delete()
    } else {
        delete()
    }
}

/**
 * 确保文件的父目录存在，不存在则自动创建
 *
 * ```kotlin
 * File("/data/cache/sub/test.txt").ensureParentDir()
 * ```
 *
 * @return 返回自身，方便链式调用
 */
fun File.ensureParentDir(): File {
    parentFile?.let { if (!it.exists()) it.mkdirs() }
    return this
}

/**
 * 计算文件的 MD5 摘要（小写32位十六进制）
 *
 * 使用 8KB 缓冲区流式读取，适用于大文件。
 *
 * **注意**：MD5 不适用于安全场景，仅建议用于校验和等非加密用途。
 *
 * @return MD5 摘要字符串
 */
fun File.md5(): String {
    val digest = MessageDigest.getInstance("MD5")
    inputStream().use { stream ->
        val buffer = ByteArray(8192)
        var read: Int
        while (stream.read(buffer).also { read = it } != -1) {
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

/**
 * 计算文件的 SHA-256 摘要（小写64位十六进制）
 *
 * 使用 8KB 缓冲区流式读取，适用于大文件。推荐用于文件完整性校验场景（比 MD5 更安全）。
 *
 * ```kotlin
 * val hash = File("data.bin").sha256()
 * ```
 *
 * @return SHA-256 摘要字符串
 */
fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { stream ->
        val buffer = ByteArray(8192)
        var read: Int
        while (stream.read(buffer).also { read = it } != -1) {
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

/**
 * 计算文件或目录的总大小（字节）
 *
 * 对于文件返回文件大小；对于目录递归计算所有子文件大小总和。
 * 常用于缓存大小计算和存储管理。
 *
 * ```kotlin
 * val cacheSize = context.cacheDir.let { File(it.path).totalSize() }
 * ```
 *
 * @return 总字节数
 */
fun File.totalSize(): Long {
    return if (isDirectory) {
        listFiles()?.sumOf { it.totalSize() } ?: 0L
    } else {
        length()
    }
}

/**
 * 将 InputStream 内容写入文件（自动创建父目录）
 *
 * ```kotlin
 * inputStream.writeToFile(File("/data/cache/output.bin"))
 * ```
 */
fun InputStream.writeToFile(file: File) {
    file.ensureParentDir()
    file.outputStream().use { out ->
        copyTo(out)
    }
}

/**
 * 安全读取文件文本内容
 *
 * @param charset 字符编码，默认 UTF-8
 * @return 文件内容，读取失败返回 null
 */
fun File.readTextOrNull(charset: java.nio.charset.Charset = Charsets.UTF_8): String? {
    return try {
        readText(charset)
    } catch (_: Exception) {
        null
    }
}
