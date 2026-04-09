# brick-data

基于 [Room](https://developer.android.com/training/data-storage/room) 的数据库封装模块，提供通用 Dao 基类、DSL 构建器、迁移助手和结果包装。

## 特性

- **BaseDao 泛型基类**：内置 insert / update / delete / upsert 等通用 CRUD 操作
- **DSL 构建器**：`BrickDatabase.build<T>()` 一行代码创建数据库
- **迁移助手**：`migration(1, 2) { ... }` 简洁的 DSL 创建 Migration
- **结果包装**：`DbResult<T>` 统一 Loading / Success / Failure 三态
- **类型转换器**：内置 Date↔Long、List\<String\>↔String 常用转换
- **协程友好**：所有操作基于 suspend 函数和 Flow

## 引入

```kotlin
val brickVersion = "1.0.0"
implementation("com.github.ail36413.Brick:brick-data:$brickVersion")
```

请将版本号替换为 JitPack Release 页面的最新版本。

> **注意**：使用 Room 需要配置 KSP 注解处理器：
> ```kotlin
> // app/build.gradle.kts
> plugins {
>     id("com.google.devtools.ksp")
> }
> dependencies {
>     ksp("androidx.room:room-compiler:2.6.1")
> }
> ```

## 基本用法

### 1. 定义实体

```kotlin
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val email: String,
    val createdAt: Date = Date()
)
```

### 2. 定义 Dao

继承 `BaseDao<T>` 即可获得完整的 CRUD 能力，只需补充自定义查询：

```kotlin
@Dao
abstract class UserDao : BaseDao<UserEntity>() {

    @Query("SELECT * FROM users WHERE id = :id")
    abstract suspend fun getById(id: Long): UserEntity?

    @Query("SELECT * FROM users ORDER BY name ASC")
    abstract fun observeAll(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE name LIKE '%' || :keyword || '%'")
    abstract suspend fun search(keyword: String): List<UserEntity>

    @Query("SELECT COUNT(*) FROM users")
    abstract suspend fun count(): Int

    @Query("DELETE FROM users")
    abstract suspend fun deleteAll()
}
```

`BaseDao` 已内置的方法：

| 方法 | 说明 |
|------|------|
| `insert(entity)` | 插入（冲突替换） |
| `insertAll(entities)` | 批量插入 |
| `insertOrIgnore(entity)` | 插入（冲突忽略） |
| `update(entity)` | 更新 |
| `updateAll(entities)` | 批量更新 |
| `delete(entity)` | 删除 |
| `deleteAll(entities)` | 批量删除 |
| `upsert(entity)` | 插入或更新 |
| `upsertAll(entities)` | 批量插入或更新 |

### 3. 定义数据库

```kotlin
@Database(entities = [UserEntity::class], version = 1)
@TypeConverters(BrickConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
```

### 4. 构建数据库

```kotlin
// 基本构建
val db = BrickDatabase.build<AppDatabase>(context, "app.db")

// 带配置的构建
val db = BrickDatabase.build<AppDatabase>(context, "app.db") {
    addMigrations(MIGRATION_1_2, MIGRATION_2_3)
    fallbackToDestructiveMigration()
    setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
}

// 内存数据库（测试用）
val testDb = BrickDatabase.buildInMemory<AppDatabase>(context) {
    allowMainThreadQueries()
}
```

## 数据库迁移

使用 `migration()` DSL 简化迁移代码：

```kotlin
val MIGRATION_1_2 = migration(1, 2) {
    execSQL("ALTER TABLE users ADD COLUMN age INTEGER NOT NULL DEFAULT 0")
}

val MIGRATION_2_3 = migration(2, 3) {
    execSQL("CREATE TABLE IF NOT EXISTS `orders` (`id` INTEGER PRIMARY KEY NOT NULL, `user_id` INTEGER NOT NULL)")
    execSQL("CREATE INDEX IF NOT EXISTS `index_orders_user_id` ON `orders` (`user_id`)")
}

val db = BrickDatabase.build<AppDatabase>(context, "app.db") {
    addMigrations(MIGRATION_1_2, MIGRATION_2_3)
}
```

## 结果包装

使用 `DbResult<T>` 统一处理数据库操作结果：

### 配合 Flow

```kotlin
class UserRepository(private val userDao: UserDao) {

    fun observeUsers(): Flow<DbResult<List<UserEntity>>> {
        return userDao.observeAll().asDbResult()
    }
}

// 在 ViewModel 中
userRepository.observeUsers().collect { result ->
    result
        .onLoading { showLoading() }
        .onSuccess { users -> showUsers(users) }
        .onFailure { error -> showError(error.message) }
}
```

### 配合 suspend 函数

```kotlin
val result = dbResultOf { userDao.getById(1) }
result
    .onSuccess { user -> showUser(user) }
    .onFailure { error -> showError(error) }

// 或获取数据
val user = result.getOrNull()
val userName = result.map { it.name }.getOrDefault("未知")
```

### 转换为 LiveData

```kotlin
val usersLiveData: LiveData<DbResult<List<UserEntity>>> =
    userDao.observeAll().asDbResult().asDbResultLiveData()
```

## 类型转换器

内置 `BrickConverters`，注册后全局生效：

```kotlin
@Database(entities = [UserEntity::class], version = 1)
@TypeConverters(BrickConverters::class)
abstract class AppDatabase : RoomDatabase() { ... }
```

支持的转换：

| 类型 | 存储格式 | 说明 |
|------|---------|------|
| `Date` | `Long` | 毫秒时间戳 |
| `List<String>` | `String` | JSON 数组 |
| `Map<String, String>` | `String` | JSON 对象 |

## 数据库回调

### 预填充数据

```kotlin
val db = Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
    .addCallback(onCreateCallback {
        execSQL("INSERT INTO config (key, value) VALUES ('app_version', '1.0')")
        execSQL("INSERT INTO config (key, value) VALUES ('initialized', 'true')")
    })
    .build()
```

### 打开回调

```kotlin
val db = Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
    .addCallback(onOpenCallback {
        execSQL("PRAGMA optimize")
    })
    .build()
```

## FAQ

### Q: BaseDao 的 upsert 和 Room 2.5+ 的 @Upsert 有什么区别？

`BaseDao.upsert()` 使用 `@Insert(onConflict = REPLACE)` 实现，语义为"存在则替换整行"。Room 2.5+ 的 `@Upsert` 注解在冲突时只更新非主键列，不会触发 DELETE + INSERT。如果使用 Room 2.5+，建议优先使用 `@Upsert` 注解。

### Q: BrickConverters 是否需要手动注册？

是的。在 `@Database` 注解的类上添加 `@TypeConverters(BrickConverters::class)` 即可全局生效。

### Q: 如何处理复杂对象类型？

对于 `BrickConverters` 未覆盖的类型，请自行编写 `@TypeConverter` 方法，并注册到 `@TypeConverters` 中。

## 线程模型与边界约束

### 线程安全

| 操作 | 线程要求 | 说明 |
|------|---------|------|
| `BrickDatabase.build()` | 任意线程 | 返回线程安全的 Database 实例 |
| `@Query suspend fun` | **不可主线程** | Room 自动切到内部 IO 线程执行 |
| `@Query fun ... : Flow<T>` | 任意线程 collect | Room Flow 在内部调度器发射，collect 端可在主线程 |
| `@Insert` / `@Update` / `@Delete` | **不可主线程** | 除非开启 `allowMainThreadQueries()` |
| `DbResult.onSuccess {}` 等回调 | **跟随 collect 端** | 在 `collect` 所在线程执行 |

### 生命周期约束

- **Database 实例应为单例**（通常由 Hilt / DI 管理），避免多实例导致连接池竞争。
- `Flow<DbResult<T>>` 建议在 `viewModelScope` 或 `lifecycleScope` 中 collect，确保 Activity/Fragment 销毁时自动取消。
- `asDbResultLiveData()` 返回的 LiveData 已自动感知 Lifecycle，无需手动清理。

### 失败场景

| 场景 | 行为 |
|------|------|
| 数据库版本升级无迁移 | 默认抛 `IllegalStateException`。如配置 `fallbackToDestructiveMigration()`，会清空数据重建 |
| SQL 语法错误 | `DbResult.Failure(SQLiteException)` |
| 磁盘空间不足 | `DbResult.Failure(SQLiteFullException)` |
| 主线程执行 suspend Dao 方法 | Room 内部切到 IO 线程，**不会阻塞主线程** |
| `BrickConverters` 遇到异常 JSON | `fromStringMap()` 返回空 Map，不抛异常 |
