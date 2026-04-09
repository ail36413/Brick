# brick-image

[![](https://jitpack.io/v/ail36413/Brick.svg)](https://jitpack.io/#ail36413/Brick)

基于 Coil 封装的 Android 图片加载库，提供简洁的 DSL API、常用变换和预加载支持。

## 引入

```kotlin
val brickVersion = "1.0.0"
implementation("com.github.ail36413.Brick:brick-image:$brickVersion")
```

请将版本号替换为 JitPack Release 页面的最新版本。

> Coil 以 `api` 方式传递，无需额外声明。

## 初始化（可选）

> **零配置即可使用**：所有加载 API 无需调用 `init()` 即可直接工作，库会使用 Coil 默认 ImageLoader。
> 如需自定义缓存、GIF 支持或全局占位图，再调用 `init()`。

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // 可选：自定义缓存 / GIF / 占位图
        BrickImage.init(this) {
            memoryCacheSize(0.3)                   // 内存缓存占最大堆比例 (0.05~0.5)
            diskCacheSize(256L * 1024 * 1024)      // 磁盘缓存 256MB
            enableGif(true)                        // 启用 GIF 支持
            placeholder(R.drawable.placeholder)
            error(R.drawable.error)
        }
    }
}
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `memoryCachePercent` | Double | 0.25 | 内存缓存占最大堆比例，自动限制在 0.05~0.5 |
| `diskCacheSize` | Long | 100MB | 磁盘缓存大小(字节)，≤0 不限制 |
| `crossfadeEnabled` | Boolean | true | 是否开启渐入动画 |
| `crossfadeDuration` | Int | 200 | 渐入动画时长（ms） |
| `gifEnabled` | Boolean | true | 是否支持 GIF 解码 |
| `placeholderRes` | Int | 0 | 全局占位图资源 ID |
| `errorRes` | Int | 0 | 全局错误图资源 ID |

## 图片加载

```kotlin
// 基础加载
imageView.loadImage("https://example.com/photo.jpg")

// 圆形（头像）
imageView.loadCircle(user.avatarUrl)

// 圆角
imageView.loadRounded(url, radiusPx = 12f)
```

## DSL 配置

```kotlin
imageView.loadImage(url) {
    placeholder(R.drawable.loading)
    error(R.drawable.fail)
    circle()                     // 圆形裁剪（与 roundedCorners 互斥）
    roundedCorners(12f)          // 圆角裁剪
    crossfade(300)               // 渐显动画时长（ms）
    override(200, 200)           // 指定尺寸
    transform(                   // 自定义变换
        GrayscaleTransformation(),
        BorderTransformation(2f, Color.WHITE)
    )
}
```

## 预加载

```kotlin
lifecycleScope.launch {
    // 单张预加载 — 返回是否成功，异常已内部捕获
    val success = ImagePreloader.preload(context, url)

    // 批量预加载 — 并行执行，单张失败不影响其他
    ImagePreloader.preloadAll(context, urls)

    // 从缓存获取 Drawable，失败返回 null
    val drawable = ImagePreloader.get(context, url)
}
```

## 内置变换

| 变换 | 构造参数 | 说明 |
|------|---------|------|
| `GrayscaleTransformation` | — | 灰度效果 |
| `ColorFilterTransformation` | `color: Int` | 颜色滤镜叠加 |
| `BorderTransformation` | `borderWidth: Float`(>0), `borderColor: Int` | 圆形/圆角图片加边框 |
| `BlurTransformation` | `radius: Int`(1~25), `sampling: Int`(≥1) | 高斯模糊（StackBlur） |

```kotlin
// 灰度 + 红色滤镜
imageView.loadImage(url) {
    transform(
        GrayscaleTransformation(),
        ColorFilterTransformation(0x33FF0000)
    )
}

// 模糊效果（默认 radius=15, sampling=4）
imageView.loadImage(url) {
    transform(BlurTransformation())
}

// 更强模糊，较少采样
imageView.loadImage(url) {
    transform(BlurTransformation(25, 2))
}
```

## 加载取消

`loadImage()` 返回 Coil 的 `Disposable`，可手动取消加载：

```kotlin
val disposable = imageView.loadImage(url)

// 手动取消
disposable?.dispose()
```

> Coil 已在 View detach 或发起新请求时自动取消，仅在需要主动控制时使用。

## 加载状态监听

```kotlin
imageView.loadImage(url) {
    listener(
        onStart = { showProgress() },
        onSuccess = { result -> hideProgress() },
        onError = { result -> showRetry() }
    )
}
```

## 缓存管理

```kotlin
// 清除内存缓存
BrickImage.clearMemoryCache(context)

// 清除磁盘缓存
BrickImage.clearDiskCache(context)
```

## GIF 支持

`brick-image` 已内置 `coil-gif` 依赖，GIF 图片可直接加载：

```kotlin
imageView.loadImage("https://example.com/animation.gif")
```

## 最佳实践

1. **零配置优先**：无需调用 `BrickImage.init()` 即可直接使用所有加载 API，Coil 默认配置即可满足大部分场景
2. **按需初始化**：仅当需要自定义缓存大小、GIF 支持或全局占位图时，在 `Application.onCreate()` 中调用 `init()`
3. **内存缓存比例**：默认 0.25（最大堆的 25%），低内存设备可降低到 0.15；图片密集型应用可提升到 0.35
4. **磁盘缓存大小**：根据应用场景调整，社交/电商类建议 256MB+，工具类 50MB 即可
5. **列表场景**：配合 `RecyclerView` 使用时，Coil 自动取消离屏 item 的加载，无需手动管理
6. **预加载**：对于确定会展示的下一页图片（如 ViewPager），使用 `ImagePreloader.preloadAll()` 提升体验

## 常见问题

详见 [FAQ.md](../FAQ.md#brick-image)。

## License

```
Copyright 2024 ail36413

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
