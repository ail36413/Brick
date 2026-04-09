# brick-store

基于腾讯 [MMKV](https://github.com/Tencent/MMKV) 的高性能键值存储模块，提供与 `SpDelegate` 一致的属性委托 API，但性能远超 SharedPreferences。

## 特性

- **高性能**：基于 mmap 内存映射，读写速度是 SharedPreferences 的 10-100 倍
- **属性委托**：Kotlin 属性语法读写，零学习成本
- **加密存储**：支持 AES-CFB 加密，保护敏感数据
- **多实例隔离**：不同业务使用不同 mmapId，数据互不干扰
- **类型丰富**：String / Int / Long / Float / Double / Boolean / ByteArray / Set\<String\> / Parcelable
- **SP 迁移**：一键从 SharedPreferences 迁移到 MMKV

## 引入

```kotlin
val brickVersion = "1.0.0"
implementation("com.github.ail36413.Brick:brick-store:$brickVersion")
```

请将版本号替换为 JitPack Release 页面的最新版本。

## 基本用法

### 1. 初始化

在 `Application.onCreate()` 中初始化：

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        BrickStore.init(this)
    }
}
```

### 2. 定义存储

```kotlin
object UserStore : MmkvDelegate() {
    var token by string("token", "")
    var userId by long("user_id", 0L)
    var isLoggedIn by boolean("is_logged_in", false)
    var score by float("score", 0f)
    var nickname by string("nickname", "游客")
    var tags by stringSet("tags")
}
```

### 3. 读写数据

```kotlin
// 写入
UserStore.token = "eyJhbGciOiJIUzI1NiJ9..."
UserStore.isLoggedIn = true

// 读取
val token = UserStore.token
val isLoggedIn = UserStore.isLoggedIn
```

## 加密存储

对于敏感数据（如密码、密钥），使用加密实例：

```kotlin
object SecureStore : MmkvDelegate(cryptKey = "my_16byte_key!!") {
    var password by string("password", "")
    var secretKey by bytes("secret_key")
}
```

> ⚠️ **安全提醒**：`cryptKey` 不应硬编码在源码中。生产环境建议从 [Android Keystore](https://developer.android.com/training/articles/keystore) 或密钥派生函数 (PBKDF2) 获取密钥，避免反编译后密钥泄露。

## 多实例隔离

不同业务模块使用不同的存储文件：

```kotlin
// 用户数据
object UserStore : MmkvDelegate(mmapId = "user_store") {
    var name by string("name", "")
}

// 应用配置
object ConfigStore : MmkvDelegate(mmapId = "config_store") {
    var theme by int("theme", 0)
    var language by string("language", "zh")
}
```

## Parcelable 支持

```kotlin
@Parcelize
data class Address(val city: String, val street: String) : Parcelable

object UserStore : MmkvDelegate() {
    var address by parcelable("address", Address::class.java)
}

// 写入
UserStore.address = Address("北京", "朝阳路")

// 读取
val addr = UserStore.address  // Address(city=北京, street=朝阳路)
```

## 从 SharedPreferences 迁移

如果你之前使用 `SpDelegate`，可以一键迁移数据到 MMKV：

```kotlin
// 在 Application.onCreate() 中
BrickStore.init(this)

// 迁移 "app_prefs" SP 文件到默认 MMKV 实例
SpMigration.migrate(this, "app_prefs")

// 迁移到指定实例（加密）
SpMigration.migrate(
    context = this,
    spName = "secure_prefs",
    mmapId = "secure_store",
    cryptKey = "my_secret_key"
)
```

## 其他操作

```kotlin
// 检查键是否存在
UserStore.contains("token")  // true

// 删除单个键
UserStore.remove("token")

// 批量删除
UserStore.remove(arrayOf("token", "userId"))

// 获取所有键
val keys = UserStore.allKeys()  // ["token", "userId", "isLoggedIn"]

// 清空所有数据
UserStore.clear()

// 获取存储文件大小
val size = UserStore.totalSize()  // 字节
```

## FAQ

### Q: MMKV 和 SharedPreferences 能共存吗？

可以。迁移只是拷贝数据到 MMKV，原 SP 文件不会被删除。迁移完成后可以安全移除 SP 相关代码。

### Q: 多进程环境下怎么办？

MMKV 支持多进程，需要在创建实例时指定 `MMKV.MULTI_PROCESS_MODE`。`MmkvDelegate` 目前使用单进程模式，如需多进程请直接使用 MMKV API。

### Q: cryptKey 的长度有限制吗？

MMKV 内部使用 AES-CFB 128 位加密，建议使用 16 字节密钥。过长的密钥会被截断。

## 线程模型与边界约束

### 线程安全

- **MMKV 底层线程安全**：所有读写操作均通过 mmap + 文件锁实现，可在任意线程调用。
- `MmkvDelegate` 的属性委托（`getValue` / `setValue`）可安全地在多线程中使用。
- `BrickStore.init()` 必须在 `Application.onCreate()` 中调用，且**仅需调用一次**。

### 生命周期约束

| 约束 | 说明 |
|------|------|
| 初始化时序 | `BrickStore.init(context)` 必须在任何 `MmkvDelegate` 属性访问之前调用 |
| 未初始化访问 | 访问未初始化的 `MmkvDelegate` 会导致 `IllegalStateException` |
| SP 迁移时序 | `SpMigration.migrate()` 应在 `init()` 之后、首次读取之前执行 |
| 加密实例 | 加密与非加密实例使用不同文件，**数据不互通** |

### 失败场景

| 场景 | 行为 |
|------|------|
| 未调用 `init()` 就访问属性 | `IllegalStateException` |
| `cryptKey` 变更 | 原数据无法解密，等效于**数据丢失** |
| 磁盘空间不足 | MMKV 内部返回默认值，不抛异常 |
| 多进程读写（单进程模式） | 可能出现数据不一致，需使用 `MULTI_PROCESS_MODE` |
