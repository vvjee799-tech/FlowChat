# FlowChat

FlowChat 是一个 Android AI 聊天软件。

这个项目不是商业级产品，也不是任何官方客户端。它更像是一个面向小白、娱乐向、可继续改造的 AI 聊天小玩具：你可以自己填 API Key，接入 OpenAI-compatible 模型服务，保存本地会话，自定义 AI 头像、昵称和系统提示词，也可以让 AI 在你允许的范围内调用一些工具，慢慢变成更贴近生活的聊天伙伴。

## 现在能做什么

- 接入自定义 OpenAI-compatible 服务商。
- 提供 ChatGPT、Claude、DeepSeek、Gemini 等常见服务商预设配置，用户只需要补 API Key。
- 保存模型 API Key，并使用 Android Keystore 加密存储。
- 本地保存会话和消息历史。
- 支持流式回复。
- 支持部分 DeepSeek 模型的思考内容展示。
- 支持模型自主工具调用，而不是只靠 prompt 模拟工具。
- 支持 Tavily 联网搜索工具，模型可在需要时自主调用。
- 支持应用使用情况工具，模型可在用户授权后查看 App 使用时长和最近前台活动。
- 工具调用时会在聊天界面显示调用状态，例如正在调用、已完成、调用失败。
- 支持服务商模型列表下拉获取。
- 支持会话级系统提示词。
- 支持自定义 AI 头像和昵称。
- 支持浅色 / 深色外观。
- 支持长按消息复制和选取文字。

## 自主工具调用

FlowChat 已经接入 OpenAI-compatible 的 tool/function calling 流程：

1. App 把可用工具声明发送给模型。
2. 模型自己判断是否需要调用工具。
3. 模型返回 `tool_calls`。
4. App 执行对应工具。
5. App 把工具结果作为 `role=tool` 消息发回模型。
6. 模型基于工具结果生成最终回复。

目前内置工具包括：

- `web_search`：联网搜索，底层使用 Tavily。
- `get_app_usage_summary`：查看今天、昨天或最近 7 天的应用使用情况。
- `get_recent_app_activity`：查看最近 1 到 24 小时的前台 App 活动。

应用使用情况工具只读取 Android 系统提供的前台使用统计，例如某个 App 用了多久、最近什么时候打开。它不能读取微信聊天内容、视频内容、输入框内容、账号内容或应用内部页面文字。

## 权限说明

联网搜索需要配置 Tavily API Key。

应用使用情况工具需要用户手动开启 Android 的“使用情况访问权限”。FlowChat 会在设置页提供入口，点击后跳转到系统授权页面。

不开启该权限时，模型仍然可以聊天和使用其它工具；如果模型调用应用使用情况工具，App 会返回明确的权限未开启提示。

## 项目定位

FlowChat 的定位很简单：

- 给小白用户一个能自己填 API Key、自己玩模型的 Android 客户端。
- 给想学习 Kotlin / Jetpack Compose / AI API 接入的人一个可拆开的例子。
- 给喜欢折腾 AI 角色聊天、娱乐玩法、生活助理方向的人一个起点。

后续会继续更新更有趣的功能，例如更丰富的角色设置、更自然的聊天界面、更多模型兼容、更多本地生活工具、更多娱乐化玩法等。

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- Hilt
- Room
- DataStore
- Ktor Client
- kotlinx.serialization
- Android Keystore
- Android UsageStatsManager

## 本地构建

你需要先安装 Android Studio 和 Android SDK。

然后在项目根目录创建 `local.properties`：

```properties
sdk.dir=你的 Android SDK 路径
```

Windows 下可以运行：

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

Debug APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 使用说明

1. 安装 APK。
2. 进入模型提供商配置。
3. 选择预设服务商或自定义配置。
4. 填写 Base URL、模型名和 API Key。
5. 回到聊天页新建会话并发送消息。
6. 如需联网搜索，填写 Tavily API Key 并打开输入框下方的联网搜索按钮。
7. 如需让 AI 查看应用使用情况，在设置页开启“应用使用情况”权限。

## 注意

- 项目不会内置任何模型 API Key。
- API Key 保存在本机，卸载应用后会随应用数据删除。
- 不同模型对工具调用、思考内容、联网搜索上下文等能力的兼容程度不同。
- 应用使用情况权限属于 Android 特殊权限，需要用户手动在系统设置中开启。
- 这是娱乐和学习用途项目，不保证所有第三方接口长期可用。

## 开源协议

MIT License
