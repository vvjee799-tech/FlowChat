# FlowChat

FlowChat 是一个 Android AI 聊天软件，主打“普通小白也能折腾自己的 AI 聊天应用”。

这个项目不是商业级产品，也不是某个官方客户端。它更像是一个娱乐向、可继续改造的小玩具：能接入 OpenAI-compatible 接口，能保存本地会话，能自定义 AI 头像、昵称、系统提示词，也会继续加入一些更有意思的玩法。

## 现在能做什么

- 接入自定义 OpenAI-compatible 服务商。
- 保存模型 API Key，使用 Android Keystore 加密存储。
- 本地保存会话和消息历史。
- 支持流式回复。
- 支持 DeepSeek 部分模型的深度思考展示。
- 支持 Tavily 联网搜索上下文注入。
- 支持服务商模型列表下拉获取。
- 支持会话级系统提示词。
- 支持自定义 AI 头像和昵称。
- 支持浅色/深色外观。
- 支持长按消息复制和选取文字。

## 项目定位

FlowChat 的定位很简单：

- 给小白用户一个能自己填 API Key、自己玩模型的 Android 客户端。
- 给想学习 Kotlin / Jetpack Compose / AI API 接入的人一个可拆开的例子。
- 给喜欢折腾 AI 角色聊天、娱乐玩法的人一个起点。

后续会继续更新一些更有趣的功能，例如更丰富的角色设置、更好看的聊天界面、更多模型兼容、更多娱乐化玩法等。

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- Hilt
- Room
- DataStore
- Ktor Client
- kotlinx.serialization

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
2. 进入服务商配置。
3. 填写服务商名称、Base URL、模型名和 API Key。
4. 回到聊天页发送消息。

如果要使用联网搜索，需要在服务商配置页填写 Tavily API Key。

## 注意

- 项目不会内置任何模型 API Key。
- API Key 保存在本机，卸载应用后会随应用数据删除。
- 不同模型对“深度思考”“联网搜索上下文”等能力的兼容程度不同。
- 这是娱乐和学习用途项目，不保证所有第三方接口长期可用。

## 开源协议

MIT License
