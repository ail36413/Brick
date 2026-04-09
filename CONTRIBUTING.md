# 贡献指南

感谢你对 Brick 项目感兴趣！以下是参与贡献的基本流程和规范。

## 开发环境

- **Android Studio** Hedgehog (2023.1) 或更高版本
- **JDK 17**
- **Kotlin 2.0.21**
- **Gradle 8.x**（项目自带 Wrapper，无需手动安装）

## 项目结构

```
Brick/
├── app/            Demo 示例应用
├── brick-net/      网络模块（HTTP + WebSocket）
├── brick-utils/    工具扩展模块
├── brick-ui/       UI 组件模块
├── brick-image/    图片加载模块
├── brick-arch/     架构基座模块
├── brick-store/    键值存储模块（MMKV）
├── brick-log/      日志系统模块（Timber）
├── brick-data/     数据库模块（Room）
├── brick-permission/ 权限管理模块
├── brick-startup/  启动优化模块
└── gradle/         版本目录 + 发布脚本
```

## 提交流程

### 1. Fork & Clone

```bash
git clone https://github.com/<your-username>/Brick.git
cd Brick
```

### 2. 创建分支

```bash
git checkout -b feature/your-feature-name
# 或
git checkout -b fix/your-bug-description
```

### 3. 开发与测试

```bash
# 编译
./gradlew assembleDebug

# Lint 检查
./gradlew lint

# 运行测试
./gradlew test

# 运行关键仪器测试（涉及权限 / UI / 导航时至少执行）
./gradlew :brick-permission:connectedDebugAndroidTest \
          :brick-ui:connectedDebugAndroidTest \
          :brick-arch:connectedDebugAndroidTest
```

说明：
- 一般改动至少应通过 `assembleDebug`、`lint`、`test`
- 如果改动涉及 `brick-permission`、`brick-ui`、`brick-arch` 的系统交互或界面行为，应额外运行对应 `connectedDebugAndroidTest`
- 提交 PR 前请确保与改动相关的 CI 任务能够通过

### 4. 提交代码

遵循以下 commit message 格式：

```
<type>(<scope>): <subject>

<body>
```

**type 类型**：
- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档
- `refactor`: 重构（不改变功能）
- `test`: 测试
- `chore`: 构建/CI/工具

**scope**：`net`、`utils`、`ui`、`image`、`arch`、`store`、`log`、`data`、`permission`、`startup`、`app`

**示例**：
```
feat(net): add configurable sensitive header masking

NetworkConfig now accepts a sensitiveHeaders parameter that controls
which HTTP headers are masked in network logs.
```

### 5. 提交 Pull Request

- PR 标题简洁（< 70 字符）
- 描述中说明**改了什么**和**为什么改**
- 如有 UI 变更，附截图
- 确保 CI 全部通过（包含常规构建、单元测试、Lint，以及相关模块的仪器测试）

## 代码规范

### Kotlin 编码风格
- 遵循 [Kotlin 官方编码规范](https://kotlinlang.org/docs/coding-conventions.html)
- 使用 4 空格缩进
- 公开 API 必须添加 KDoc 注释
- 使用 `@param` / `@return` 说明参数和返回值

### 命名规范
- 类名：`PascalCase`
- 函数/属性：`camelCase`
- 常量：`UPPER_SNAKE_CASE`
- 扩展函数文件：`XxxExt.kt`

### 架构约定
- 每个模块保持独立，模块间不互相依赖（`app` 除外）
- 新功能优先考虑扩展函数而非继承
- 使用 Hilt 进行依赖注入（仅 `brick-net` 和 `brick-arch`）
- 内部实现类使用 `internal` 修饰

## 问题反馈

- 提交 Issue 时请包含：
  - Brick 版本号
  - Android 版本 / 设备信息
  - 最小复现步骤
  - 期望行为 vs 实际行为
  - 相关日志（如有）

## License

贡献的代码将同样遵循 [Apache License 2.0](LICENSE)。
