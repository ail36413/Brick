# brick-permission

基于协程的 Android 运行时权限请求库，无需覆写 `onRequestPermissionsResult`，一行代码搞定权限申请。

## 特性

- **协程挂起**：`suspend fun request()` 直接拿到结果，无需回调地狱
- **自动过滤**：已授予的权限自动跳过，只请求未授予的
- **三态结果**：区分 granted / denied / permanentlyDenied
- **Rationale 支持**：可在请求前展示理由对话框
- **扩展函数**：Activity/Fragment 均有便捷扩展
- **零侵入**：基于隐藏 Fragment，无需继承特定基类

## 引入

```kotlin
val brickVersion = "1.0.0"
implementation("com.github.ail36413.Brick:brick-permission:$brickVersion")
```

请将版本号替换为 JitPack Release 页面的最新版本。

## 基本用法

### 协程方式（推荐）

```kotlin
lifecycleScope.launch {
    val result = BrickPermission.request(
        this@MainActivity,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    when {
        result.isAllGranted -> {
            // 全部权限已授予
            openCamera()
        }
        result.hasPermanentlyDenied -> {
            // 有权限被永久拒绝，引导到设置页
            showDialog("需要在设置中手动开启权限") {
                BrickPermission.openAppSettings(this@MainActivity)
            }
        }
        else -> {
            // 普通拒绝
            showToast("需要相机和录音权限")
        }
    }
}
```

### 扩展函数方式

更简洁的回调风格：

```kotlin
// 请求 + 回调
requestPermissions(Manifest.permission.CAMERA) { result ->
    if (result.isAllGranted) openCamera()
}

// 成功/失败分离
requirePermissions(
    Manifest.permission.CAMERA,
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    onGranted = { takePicture() },
    onDenied = { result ->
        if (result.hasPermanentlyDenied) {
            showSettingsGuide()
        }
    }
)
```

### 在 Fragment 中使用

```kotlin
class CameraFragment : Fragment() {
    private fun checkCamera() {
        requirePermissions(
            Manifest.permission.CAMERA,
            onGranted = { startPreview() },
            onDenied = { showToast("需要相机权限") }
        )
    }
}
```

## 带理由的请求

当系统认为需要展示理由时（用户之前拒绝过），会先触发 rationale 回调：

```kotlin
lifecycleScope.launch {
    val result = BrickPermission.requestWithRationale(
        activity = this@MainActivity,
        permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
        rationale = { permissions, proceed, cancel ->
            AlertDialog.Builder(this@MainActivity)
                .setTitle("需要定位权限")
                .setMessage("为了显示附近的商家，需要获取您的位置信息")
                .setPositiveButton("允许") { _, _ -> proceed() }
                .setNegativeButton("拒绝") { _, _ -> cancel() }
                .setOnCancelListener { cancel() }
                .show()
        }
    )

    result?.let {
        if (it.isAllGranted) loadNearbyShops()
    }
}
```

## 权限检查

```kotlin
// 检查单个权限
if (context.hasPermission(Manifest.permission.CAMERA)) {
    // 已有权限，直接使用
}

// 检查多个权限
if (context.hasPermissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) {
    // 全部已授予
}

// 静态方法
BrickPermission.isGranted(context, Manifest.permission.CAMERA)
BrickPermission.isAllGranted(context, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
```

## 跳转应用设置页

当权限被永久拒绝时，引导用户手动开启：

```kotlin
if (result.hasPermanentlyDenied) {
    AlertDialog.Builder(this)
        .setTitle("权限被拒绝")
        .setMessage("请在设置中手动开启 ${result.permanentlyDenied.joinToString()} 权限")
        .setPositiveButton("去设置") { _, _ ->
            BrickPermission.openAppSettings(this)
        }
        .setNegativeButton("取消", null)
        .show()
}
```

## PermissionResult 说明

| 属性 | 类型 | 说明 |
|------|------|------|
| `granted` | `List<String>` | 已授予的权限 |
| `denied` | `List<String>` | 被拒绝的权限（可再次请求） |
| `permanentlyDenied` | `List<String>` | 被永久拒绝的权限（需跳转设置） |
| `isAllGranted` | `Boolean` | 是否全部已授予 |
| `hasPermanentlyDenied` | `Boolean` | 是否有被永久拒绝的权限 |

## 并发安全

多个协程同时调用 `BrickPermission.request()` 时，内部使用单个 `Mutex` 全局串行化请求，确保同一时刻只有一个权限弹窗。后续请求会挂起等待前一个完成后再弹出，不会出现多个弹窗叠加的异常行为。

## 线程模型与边界约束

### 线程安全

| 方法 | 线程要求 | 说明 |
|------|---------|------|
| `BrickPermission.request()` | **主线程协程** | 内部启动 Fragment，必须在主线程 |
| `BrickPermission.isGranted()` | 任意线程 | 纯 Context 查询 |
| `requestPermissions {}` 扩展 | **主线程协程** | 需要 `lifecycleScope` 或 `viewLifecycleOwner.lifecycleScope` |
| `hasPermission()` / `hasPermissions()` | 任意线程 | Context 扩展 |

### 生命周期约束

- **必须在 Activity STARTED 之后调用**。在 `onCreate()` 中调用可能因 Fragment 事务尚未提交而失败。
- 配合 `lifecycleScope.launch` 使用时，Activity 销毁会自动取消协程。
- 隐藏 Fragment 会在 Activity 销毁时自动清理，无需手动管理。
- 配置变更导致旧 Activity 销毁时，当前挂起请求会被取消；如需继续请求，应在新的宿主中重新发起。

### 失败场景

| 场景 | 行为 |
|------|------|
| Activity 已销毁时调用 | 协程被取消（`CancellationException`），不会弹窗 |
| 用户快速旋转屏幕 | Mutex + 隐藏 Fragment 确保请求不丢失，但旧 Activity 的协程会取消 |
| 权限字符串拼写错误 | 系统默认拒绝，返回 denied 列表 |
| 所有权限已授权时调用 | 自动跳过弹窗，直接返回全部 granted |

## FAQ

### Q: Fragment 中调用会泄漏吗？

不会。内部使用隐藏 Fragment 观测宿主 Activity 生命周期，Activity 销毁后自动清理。配合 `lifecycleScope` 可确保协程自动取消。

### Q: 为什么 permanently denied 在部分设备上不准？

Android 未提供"永久拒绝"的公开 API，`brick-permission` 通过 `shouldShowRequestPermissionRationale()` 推断。部分国产 ROM 对此返回值有魔改，可能导致判断不准确。
