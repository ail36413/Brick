# brick-ui

[![](https://jitpack.io/v/ail36413/Brick.svg)](https://jitpack.io/#ail36413/Brick)

Android 通用 UI 组件库，提供常用自定义 View、RecyclerView 适配器和对话框工具。

## 引入

```kotlin
val brickVersion = "1.0.0"
implementation("com.github.ail36413.Brick:brick-ui:$brickVersion")
```

请将版本号替换为 JitPack Release 页面的最新版本。

## 组件一览

| 组件 | 类别 | 说明 |
|------|------|------|
| `StateLayout` | 布局 | 多状态布局（CONTENT / LOADING / EMPTY / ERROR），延迟 inflate，支持状态回调 |
| `TitleBar` | 布局 | 通用标题栏（返回按钮 + 居中标题 + 右侧文字/图标按钮），支持沉浸式状态栏 |
| `RoundLayout` | 布局 | 圆角裁剪容器（四角独立半径 + 描边），基于 `clipPath` |
| `FlowLayout` | 布局 | 自动换行流式布局，支持最大行数限制和行内 gravity 对齐 |
| `BadgeView` | 控件 | 角标红点 View（隐藏 / 红点 / 数字三种模式） |
| `SimpleAdapter` | 适配器 | 单类型 RecyclerView 泛型适配器（ViewBinding + DiffUtil + 空视图） |
| `MultiTypeAdapter` | 适配器 | 多类型 RecyclerView 适配器（支持 DiffUtil + DSL 构建） |
| `DividerDecoration` | 装饰 | 可配置分割线（颜色 / 像素 / 边距 / 支持 Linear & Grid / 跳过首尾） |
| `BrickDialog` | 对话框 | 快捷对话框工具（confirm / alert / input / list / bottomList / custom） |
| `LoadingDialog` | 对话框 | 全局加载弹窗（WeakReference 防泄漏 + 生命周期感知 + 自定义布局 + 取消回调） |
| `BrickAnim` | 动画 | View 动画扩展函数（淡入淡出 / 滑入滑出 / 缩放 / 抖动 / 组合动画） |
| `BrickItemAnimator` | 动画 | RecyclerView Item 入场动画工具（5 种动效 + 逐个延迟） |

## 使用示例

### StateLayout

```xml
<com.ail.brick.ui.statelayout.StateLayout
    android:id="@+id/stateLayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    <!-- 内容布局（第一个子 View 会作为 CONTENT） -->
    <RecyclerView ... />
</com.ail.brick.ui.statelayout.StateLayout>
```

```kotlin
stateLayout.showLoading()                   // 展示加载中
stateLayout.showContent()                   // 展示内容
stateLayout.showEmpty()                     // 展示空页面
stateLayout.showError { retryLoad() }       // 展示错误页并设置重试按钮回调

// 状态变更监听
stateLayout.setOnStateChangeListener { oldState, newState ->
    Log.d("StateLayout", "$oldState -> $newState")
}
```

> 状态布局使用 **延迟 inflate**（`ViewStub`），未展示的状态不占内存。
> 切换状态时自动发送无障碍播报（如 "加载中" / "内容已加载"），并支持横竖屏切换后自动恢复当前状态。

### SimpleAdapter

```kotlin
val adapter = SimpleAdapter<ItemBinding, Item>(
    inflate = ItemBinding::inflate,
    diffCallback = object : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem
    }
) { binding, item, _ ->
    binding.tvName.text = item.name
    binding.ivAvatar.loadCircle(item.avatar)
}
recyclerView.adapter = adapter
adapter.submitList(items)

// 空视图支持
adapter.setEmptyView(emptyView)

// 点击事件 — 使用 bindingAdapterPosition 保证位置准确
adapter.setOnItemClickListener { item, position ->
    navigateToDetail(item.id)
}
adapter.setOnItemLongClickListener { item, position ->
    showDeleteDialog(item)
}
```

### MultiTypeAdapter

```kotlin
// 传统用法
val adapter = MultiTypeAdapter(
    itemDiff = { old, new -> (old as? HasId)?.id == (new as? HasId)?.id },
    contentDiff = { old, new -> old == new }
)

adapter.register(Header::class, { parent ->
    HeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
}) { binding, item, _ ->
    (binding as HeaderBinding).tvTitle.text = (item as Header).title
}

// DSL 用法（推荐）
val adapter = multiTypeAdapter {
    itemDiff { old, new -> (old as? HasId)?.id == (new as? HasId)?.id }
    contentDiff { old, new -> old == new }

    register<Header, HeaderBinding>(HeaderBinding::inflate) { binding, item, _ ->
        binding.tvTitle.text = item.title
    }
    register<Content, ContentBinding>(ContentBinding::inflate) { binding, item, _ ->
        binding.tvBody.text = item.body
    }
}

recyclerView.adapter = adapter
adapter.submitList(newItems)
```

### TitleBar

```xml
<com.ail.brick.ui.titlebar.TitleBar
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:titleBar_title="首页"
    app:titleBar_showBack="true"
    app:titleBar_rightText="编辑"
    app:titleBar_rightIcon="@drawable/ic_more"
    app:titleBar_immersive="true"
    app:titleBar_titleColor="@color/black"
    app:titleBar_bgColor="@color/white" />
```

```kotlin
// 设置右侧图标按钮
titleBar.setRightIcon(R.drawable.ic_more) { showMenu() }

// 沉浸式状态栏适配
titleBar.applyImmersivePadding()
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `titleBar_title` | string | — | 标题文字 |
| `titleBar_showBack` | boolean | true | 是否显示返回按钮 |
| `titleBar_leftIcon` | reference | — | 自定义左侧图标资源 |
| `titleBar_rightText` | string | — | 右侧文字 |
| `titleBar_rightIcon` | reference | — | 右侧图标资源 |
| `titleBar_titleColor` | color | `colorOnSurface` | 标题颜色（自动跟随 Material 主题） |
| `titleBar_bgColor` | color | `colorSurface` | 背景颜色（自动跟随 Material 主题） |
| `titleBar_immersive` | boolean | false | 是否适配沉浸式状态栏 |

### BadgeView

```xml
<com.ail.brick.ui.widget.BadgeView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:badge_count="5"
    app:badge_bgColor="#FF0000"
    app:badge_textColor="#FFFFFF" />
```

```kotlin
badgeView.count = 0    // 显示红点
badgeView.count = -1   // 隐藏
badgeView.count = 99   // 显示 "99"
badgeView.count = 100  // 显示 "99+"
```

> BadgeView 默认使用 Material 主题色（`colorError` / `colorOnError`），支持无障碍播报与状态保存。

### BrickDialog

```kotlin
// 确认对话框
BrickDialog.confirm(this, "提示", "确定删除？") { deleteItem() }

// 输入对话框
BrickDialog.input(this, "修改昵称", hint = "请输入新昵称") { text ->
    updateNickname(text)
}

// 列表选择
BrickDialog.list(this, "选择城市", listOf("北京", "上海", "广州")) { index ->
    selectedCity = cities[index]
}

// 底部列表
BrickDialog.bottomList(this, "拍照方式", listOf("拍照", "从相册选择")) { index ->
    handleAction(index)
}

// 自定义布局对话框（布局资源）
BrickDialog.custom(this, title = "设置", layoutRes = R.layout.dialog_settings,
    onConfirm = { saveSettings() }) { view ->
    view.findViewById<Switch>(R.id.switchDarkMode).isChecked = isDarkMode
}

// 自定义布局对话框（View 实例）
BrickDialog.custom(this, title = "提示", view = myCustomView,
    onConfirm = { handleConfirm() })
```

### LoadingDialog

```kotlin
// 基本用法
LoadingDialog.show(this, "加载中...")
// ... 异步操作完成
LoadingDialog.dismiss()

// 可取消 + 取消回调
LoadingDialog.show(this, "请稍候…", cancelable = true) {
    cancelRequest()  // 用户按返回键时回调
}

// 自定义布局
LoadingDialog.show(this, R.layout.dialog_custom_loading)

// 自定义 View
val progressView = MyProgressView(this)
LoadingDialog.showWithView(this, progressView)
```

> 内部使用 `WeakReference<Dialog>` 防止 Activity 泄漏。
> 当 Context 为 `LifecycleOwner`（如 `AppCompatActivity`）时，Activity 销毁后自动 dismiss，无需手动管理。

### RoundLayout

```xml
<!-- 圆角容器 -->
<com.ail.brick.ui.widget.RoundLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:round_radius="12dp">
    <ImageView ... />
</com.ail.brick.ui.widget.RoundLayout>

<!-- 圆角 + 描边 -->
<com.ail.brick.ui.widget.RoundLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:round_radius="12dp"
    app:round_strokeColor="#FF5722"
    app:round_strokeWidth="2dp">
    <TextView ... />
</com.ail.brick.ui.widget.RoundLayout>
```

```kotlin
// 代码设置描边（单位为 px）
roundLayout.setStroke(Color.RED, 2 * resources.displayMetrics.density)
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `round_radius` | dimension | 0 | 统一圆角半径 |
| `round_topLeftRadius` | dimension | round_radius | 左上角半径 |
| `round_topRightRadius` | dimension | round_radius | 右上角半径 |
| `round_bottomLeftRadius` | dimension | round_radius | 左下角半径 |
| `round_bottomRightRadius` | dimension | round_radius | 右下角半径 |
| `round_strokeColor` | color | 透明 | 描边颜色 |
| `round_strokeWidth` | dimension | 0 | 描边宽度 |

### FlowLayout

```xml
<!-- 流式标签布局 -->
<com.ail.brick.ui.widget.FlowLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:flow_horizontalSpacing="8dp"
    app:flow_verticalSpacing="8dp"
    app:flow_maxLines="3"
    app:flow_gravity="center">
    <!-- 自动换行的子 View -->
</com.ail.brick.ui.widget.FlowLayout>
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `flow_horizontalSpacing` | dimension | 8dp | 水平间距 |
| `flow_verticalSpacing` | dimension | 8dp | 垂直间距 |
| `flow_maxLines` | integer | 0（不限制） | 最大行数 |
| `flow_gravity` | enum | start | 行内对齐（start/center/end） |

### DividerDecoration

```kotlin
// LinearLayoutManager 分割线
recyclerView.addItemDecoration(
    DividerDecoration(
        color = Color.LTGRAY,
        height = 1,
        paddingStart = 16,
        paddingEnd = 16
    )
)

// GridLayoutManager 同样支持（自动识别，四边绘制网格线）
gridRecyclerView.addItemDecoration(
    DividerDecoration(color = Color.LTGRAY, height = 1)
)
```

## View 动画

### BrickAnim — 常用 View 动画扩展

```kotlin
import com.ail.brick.ui.anim.*

// 淡入淡出
view.fadeIn()                                      // alpha 0→1
view.fadeOut()                                     // alpha 1→0, 结束后 GONE

// 滑入滑出
view.slideInFromBottom()                           // 从底部滑入
view.slideInFromLeft()                             // 从左侧滑入
view.slideOutToTop()                               // 向上滑出

// 缩放
view.scaleIn()                                     // 0→1 弹入（OvershootInterpolator）
view.pulse()                                       // 脉冲效果（收藏/点赞）

// 抖动/弹跳
view.shake()                                       // 水平抖动（表单校验）
view.bounce()                                      // 垂直弹跳
view.rotate(360f)                                  // 旋转

// 组合动画
view.fadeSlideIn()                                 // 淡入 + 上滑
view.fadeSlideOut()                                // 淡出 + 下滑

// 带回调
view.fadeIn(duration = 500L) { doAfterAnimation() }
```

### BrickItemAnimator — RecyclerView Item 入场动画

```kotlin
// 在 onBindViewHolder 中调用
override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    bind(holder, getItem(position))
    BrickItemAnimator.animateItem(holder.itemView, position)
}

// 5 种动画类型
BrickItemAnimator.animateItem(itemView, pos, AnimType.FADE_SLIDE_UP)
BrickItemAnimator.animateItem(itemView, pos, AnimType.FADE_SLIDE_LEFT)
BrickItemAnimator.animateItem(itemView, pos, AnimType.SCALE_IN)

// 在 onViewRecycled 中重置（防止复用问题）
override fun onViewRecycled(holder: ViewHolder) {
    BrickItemAnimator.resetItem(holder.itemView)
}
```

## StateLayout 动画

StateLayout 状态切换默认带有 200ms 淡入动画，可自定义或关闭：

```xml
<!-- XML 配置 -->
<com.ail.brick.ui.statelayout.StateLayout
    app:enableAnimation="true"
    app:animationDuration="300" />
```

```kotlin
// 代码配置
stateLayout.enableAnimation = false       // 关闭动画
stateLayout.animationDuration = 300L      // 自定义时长
```

## 最佳实践

1. **StateLayout**：对于网络请求场景，建议在请求开始时 `showLoading()`，成功后 `showContent()`，空数据 `showEmpty()`，异常 `showError { retry() }`。使用 `setOnStateChangeListener` 追踪页面状态变化用于埋点分析
2. **SimpleAdapter DiffUtil**：始终提供 `diffCallback` 参数并比较唯一标识（如 `id`），以获得最佳 RecyclerView 动画效果
3. **SimpleAdapter 空视图**：通过 `setEmptyView()` 设置空数据占位视图，列表为空时自动切换显示
4. **MultiTypeAdapter DSL**：优先使用 `multiTypeAdapter { }` DSL 构建，类型安全且代码更简洁
5. **LoadingDialog**：调用 `show()` 后务必在合适时机调用 `dismiss()`（包括异常分支），可设置 `onCancel` 回调处理用户主动取消
6. **BrickDialog**：对话框回调中避免持有 Activity 引用做耗时操作，必要时转到 ViewModel
7. **TitleBar 沉浸式**：使用 `titleBar_immersive="true"` 或调用 `applyImmersivePadding()` 适配全屏模式
8. **RoundLayout 描边**：使用 `round_strokeColor` 和 `round_strokeWidth` 属性添加边框效果，无需额外嵌套 shape drawable

## 常见问题

详见 [FAQ.md](../FAQ.md#brick-ui)。

## 线程模型与边界约束

### 线程安全

- **所有 View 组件（StateLayout / TitleBar / RoundLayout / FlowLayout / BadgeView）** 必须在**主线程**操作。
- `SimpleAdapter.submitList()` 和 `MultiTypeAdapter.submitList()` 底层使用 `AsyncListDiffer`，**可在主线程安全调用**，Diff 计算在后台线程完成。
- `LoadingDialog.show()` / `dismiss()` 必须在**主线程**调用。
- `BrickAnim` 动画扩展函数必须在**主线程**调用。

### 生命周期约束

| 组件 | 约束 |
|------|------|
| `LoadingDialog` | 内部使用 `WeakReference<Dialog>` + `LifecycleObserver` 双重防护；当传入 `LifecycleOwner` 时 Activity 销毁自动 dismiss，无需手动管理 |
| `BrickDialog` | 基于 `AlertDialog`，遵循标准 Activity 生命周期。不要在 `onDestroy()` 后调用 |
| `SimpleAdapter` 点击监听 | `setOnItemClickListener` 内部引用闭包，如闭包捕获 Activity 引用需注意泄漏（建议通过 ViewModel 中转） |
| `StateLayout` | 可在任何生命周期阶段切换状态，使用 ViewStub 延迟 inflate 节省初始内存 |

### 失败场景

| 场景 | 行为 |
|------|------|
| `submitList` 传入相同列表引用 | DiffUtil 不触发更新，需传入新列表实例 |
| 子线程调用 View 方法 | `CalledFromWrongThreadException`（Android 标准限制） |
| `showError()` 不设置重试回调 | 重试按钮隐藏或不可点击 |
| RecyclerView Item 动画复用问题 | 在 `onViewRecycled()` 中调用 `BrickItemAnimator.resetItem()` 重置 |

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
