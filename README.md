# FlowChat

FlowChat 是一个偏娱乐、偏折腾、偏“小白也能玩”的 Android AI 聊天软件。

它不是官方客户端，也不是商业级产品，更不是那种严肃到让人不想打开的效率工具。它的定位更像一个可以自己接模型、自己捏 AI 角色、自己慢慢加玩法的 AI 聊天小玩具。

你可以把它当成：

- 一个能自己填 API Key、自己选择模型的手机 AI 聊天壳子。
- 一个给 AI 角色聊天、人格设定、头像昵称、系统提示词折腾用的小 playground。
- 一个把 AI 从“只会回消息”慢慢拉进生活场景里的实验项目。

## 项目特点

FlowChat 现在更想突出的不是“我有多少常规聊天功能”，而是这几个方向：

- **娱乐感**：头像、昵称、系统提示词、思考过程、模型切换，都围绕“和一个可塑造的 AI 角色聊天”来做。
- **小白友好**：尽量减少复杂配置。预设服务商只需要填 API Key，自定义配置也保留给想折腾的人。
- **模型自由**：不把用户锁死在某一个模型里，能接 OpenAI-compatible API，也能切换不同服务商和模型。
- **生活感工具**：AI 不只停留在对话框里。联网搜索、应用使用情况等工具会逐步变成 AI 可以主动调用的能力。
- **本地优先**：聊天记录、角色配置、API Key 都尽量放在本机，API Key 使用 Android Keystore 加密保存。
- **持续整活**：后续会继续加更有趣的功能，而不是只把它做成一个普通聊天 App。

## 玩法方向

FlowChat 适合拿来做这些事：

- 给不同模型套不同 AI 人设。
- 调角色语气、背景、系统提示词。
- 用 DeepSeek 一类模型看可展示的思考内容。
- 开联网搜索，让 AI 在需要时自己查资料再回复。
- 授权后让 AI 看看手机应用使用情况，做一点更贴近生活的互动。
- 在手机上快速试不同模型的回答风格。

这些玩法以后还会继续扩展，比如更丰富的角色卡、更自然的聊天界面、更有趣的本地生活工具、更多模型兼容和更多娱乐化互动。

## 当前状态

这个项目还在快速迭代中，很多地方会继续改。

目前它更适合：

- 想自己接 API 玩 AI 聊天的人。
- 想看 Kotlin / Jetpack Compose / AI API 接入示例的人。
- 想把 AI 角色聊天做得更有生活感、更有娱乐感的人。

如果你想要一个稳定、完整、开箱即用的商业软件，它现在还不是那个东西。

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- Hilt
- Room
- Ktor Client
- kotlinx.serialization
- Android Keystore
- Android UsageStatsManager

## 本地构建

先安装 Android Studio 和 Android SDK，然后在项目根目录创建 `local.properties`：

```properties
sdk.dir=你的 Android SDK 路径
```

Windows 下运行：

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

Debug APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 注意

- 项目不内置任何模型 API Key。
- 不同模型对思考内容、工具调用、联网上下文的兼容程度不同。
- 应用使用情况工具需要用户手动授予 Android Usage Access 权限。
- 这是娱乐和学习用途项目，第三方接口是否长期可用取决于对应服务商。

## 开源协议

MIT License
