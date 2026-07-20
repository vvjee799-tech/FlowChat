# FlowChat

FlowChat 是一个偏娱乐、偏角色体验的 Android AI 聊天软件。

它不是官方客户端，也不是商业级产品，更不是那种严肃到让人不想打开的效率工具。它的定位更像一个可以自己接模型、自己捏 AI 角色、自己慢慢加玩法的 AI 聊天小玩具。

你可以把它当成：

- 一个能自己填 API Key、自己选择模型的手机 AI 聊天壳子。
- 一个给 AI 角色聊天、人格设定、头像昵称、系统提示词定制用的小 playground。
- 一个把 AI 从“只会回消息”慢慢拉进生活场景里的实验项目。

## 项目特点

FlowChat 现在更想突出的不是“我有多少常规聊天功能”，而是这几个方向：

- **娱乐感**：头像、昵称、系统提示词、思考过程、模型切换，都围绕“和一个可塑造的 AI 角色聊天”来做。
- **配置轻量**：尽量减少复杂配置。预设服务商只需要填 API Key，自定义配置也保留给进阶使用。
- **模型自由**：不把用户锁死在某一个模型里，能接 OpenAI-compatible API，也能切换不同服务商和模型。
- **生活感工具**：AI 不只停留在对话框里。它可以联网搜索、查看应用使用情况，也能在你明确要求时打开应用、理解当前页面并连续完成简单操作。
- **浮窗陪伴**：切到其他 App 后仍能查看回复、继续输入或停止输出，不必反复回到 FlowChat。
- **轻量记忆与文件**：可以让 AI 从本机记忆中找回相关对话，也可以读取常用文档继续聊；记忆可随时关闭、逐条删除或全部清空。
- **Power 模式**：把设备能力集中到一个入口，在系统授权范围内按对话需要调用，避免堆满零散开关。
- **本地优先**：聊天记录、角色配置、API Key 都尽量放在本机，API Key 使用 Android Keystore 加密保存。
- **持续整活**：后续会继续加更有趣的功能，而不是只把它做成一个普通聊天 App。

## 玩法方向

FlowChat 适合拿来做这些事：

- 给不同模型套不同 AI 人设。
- 调角色语气、背景、系统提示词。
- 用 DeepSeek 一类模型看可展示的思考内容。
- 开联网搜索，让 AI 在需要时自己查资料再回复。
- 授权后让 AI 看看手机应用使用情况，做一点更贴近生活的互动。
- 直接对 AI 说“打开微信”或“启动 DeepSeek”，让它通过工具调用打开对应应用。
- 配合 Shizuku 与系统无障碍服务，让 AI 在普通页面中识别可见控件、点击、滑动、填写文字或返回上一页；即使当前模型不能看图片，也能使用结构化页面信息完成简单任务。
- 让浮窗助手停留在其他 App 上方，边看任务进度边继续聊天。
- 在手机上快速试不同模型的回答风格。
- 把 TXT、PDF、DOCX、XLSX、PPTX、OpenDocument 等常用文件附在消息里，让 AI 接着内容聊天。

这些玩法以后还会继续扩展，比如更丰富的角色卡、更自然的聊天界面、更有趣的本地生活工具、更多模型兼容和更多娱乐化互动。

## 当前状态

这个项目还在快速迭代中，很多地方会继续改。

目前它更适合：

- 想自己接 API 玩 AI 聊天的人。
- 想看 Kotlin / Jetpack Compose / AI API 接入示例的人。
- 想把 AI 角色聊天做得更有生活感、更有娱乐感的人。

如果你想要一个稳定、完整、开箱即用的商业软件，它现在还不是那个东西。

## 下载

前往 [GitHub Releases](https://github.com/vvjee799-tech/FlowChat/releases/latest) 下载正式版 APK。当前版本为 `FlowChat v1.4.0`。

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
- Shizuku
- Android AccessibilityService

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

## 联网搜索

仓库中的 `search-proxy/` 是轻量 Cloudflare Worker。正式使用时建议由 App 请求自己的 Worker，再由 Worker 使用服务端保存的 Tavily Key 搜索，避免把公共搜索密钥打进 APK。用户自己填写的 Tavily Key 仍可作为可选备用通道。

部署前在 `search-proxy/` 中设置 Worker secret：

```powershell
npx wrangler secret put TAVILY_API_KEY
npx wrangler deploy
```

构建 App 时传入部署后的地址：

```powershell
.\gradlew.bat :app:assembleRelease -PflowchatSearchProxyUrl=https://你的-worker.workers.dev
```

未配置 Worker 地址时，App 不会获得仓库作者的搜索额度；只有用户自行保存 Tavily Key 后才会走备用通道。

## 注意

- 项目不内置任何模型 API Key。
- 项目也不内置 Tavily Key；搜索代理中的密钥必须使用 Cloudflare secret 配置。
- 单个附件最大 20 MB；支持常见文本、PDF、Office 与 OpenDocument 文档，暂不支持图片理解。
- 不同模型对思考内容、工具调用、联网上下文的兼容程度不同。
- 应用使用情况工具需要用户手动授予 Android Usage Access 权限。
- 应用内操作仅在开启“设备助手”并授予对应权限后提供；页面文字会被当作不可信数据，密码、验证码、支付页面以及 FlowChat 自身界面会阻止自动操作。
- 当前 DeepSeek 等纯文本模型通过结构化 UI 控件列表理解页面；截图视觉理解接口会在支持多模态模型后继续接入。
- 这是娱乐和学习用途项目，第三方接口是否长期可用取决于对应服务商。

## 致谢

设备自动化的 Tools/Agent 分层、Shizuku 与 UI 树混合执行思路参考了 MIT 开源项目 [肉包 Roubao](https://github.com/Turbo1123/roubao)。FlowChat 保留自己的聊天、多模型和权限交互结构，并针对纯文本模型增加了结构化页面观察与敏感页面拦截。相关许可见 `NOTICE`。

## 开源协议

MIT License
