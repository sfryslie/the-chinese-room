# 中文屋 — Claude 开发指南

## 项目概述

本项目是约翰·瑟尔1980年提出的"中文屋"思想实验的可运行实现。这是一个聊天机器人，它只用中文回应，同时在概念上"不理解"中文。这个矛盾正是整个项目的笑点所在。

## 技术栈

- **Spring Boot 4.0.6** + **Spring AI 2.0.0-M5**（里程碑版本，需要 `https://repo.spring.io/milestone` 仓库）
- **Kotlin**，Gradle Kotlin DSL 构建
- **AI 提供商**：Anthropic、OpenAI、Ollama、Google Gemini（均已配置，按需填写密钥即可）
- **Web 层**：Spring MVC（REST）+ Spring WebSocket（STOMP/SockJS）+ 原生 WebSocket（`/chat-ws`）
- **UI**：静态 HTML（`/`）+ Vaadin 25（`/vaadin`）
- **容器化**：Docker Compose，默认使用 Ollama + `qwen2.5:0.5b`（无需 API 密钥）
- **默认模型**：`claude-haiku-4-5`（本地运行）或 `qwen2.5:0.5b`（Docker Compose）

## 构建与运行

### Docker Compose（零配置，推荐）

无需任何 API 密钥。Ollama 和模型均已内置。

```bash
docker compose up --build
```

首次运行会下载模型（约 400 MB），稍后即可访问 **http://localhost:8080**。模型缓存在 Docker 卷中，后续启动无需重新下载。

### 本地运行（需自备 API 密钥）

```bash
# 复制并填写 API 密钥
cp .env.example .env

# Linux/macOS
./run.sh

# Windows
./run.ps1
```

`run.sh` / `run.ps1` 会先加载 `.env` 文件中的环境变量，再执行 `./gradlew bootRun`。`.env` 解析器使用正则表达式匹配 `KEY=value` 格式，可正确处理 Windows CRLF 换行符和含有 Unicode 字符的注释行。

应用启动后监听 **http://localhost:8080**。

## 项目结构

```
src/main/kotlin/com/sfryslie/chineseroom/
  ChineseRoomApplication.kt          — 应用入口，启用配置属性
  config/
    AppShell.kt                      — Vaadin AppShellConfigurator，声明 @Push
    ChineseRoomProperties.kt         — 绑定 chinese-room.* 配置项
    PlainWsConfig.kt                 — 原生 WebSocket 端点配置（/chat-ws）
    PlainWsHandler.kt                — 原生 WebSocket 消息处理器
    WebSocketConfig.kt               — STOMP 端点配置（/ws，含 SockJS 回退）
  controller/
    ChatRestController.kt            — POST /chat，无状态 REST 接口
    ChatWebSocketController.kt       — @MessageMapping /app/chat，@SendToUser 回复
  service/
    ChineseRoomService.kt            — 核心逻辑（类名：中文屋服务）
  view/
    ChatView.kt                      — Vaadin 25 聊天界面（@Route("vaadin")）
src/main/resources/
  application.yml                    — 服务器端口、AI 配置、日志级别
  static/index.html                  — 单页聊天界面（SockJS + @stomp/stompjs）
```

## 核心服务：中文屋服务

这是最重要的文件。**类名和所有内部标识符均为中文**，这是有意为之的设计选择——代码本身也成为符号操作的一部分。

| 中文标识符 | 英文含义 |
|---|---|
| `提供商映射` | Map<String, ChatClient>，懒加载，通过 ApplicationContext 发现所有 ChatModel bean |
| `会话记录` | 按会话 ID 存储的消息历史（ConcurrentHashMap） |
| `系统提示词` | 系统提示（见下文） |
| `fun 获取客户端(提供商)` | 按提供商名称从映射中取 ChatClient，缺省回退到 `properties.provider` |
| `fun 对话(会话编号, 用户消息, 提供商)` | 有状态对话，供 WebSocket 使用，提供商可为 null |
| `fun 无状态对话(用户消息, 提供商)` | 无状态对话，供 REST 接口使用，提供商可为 null |
| `fun 清除会话(sessionId)` | 原生 WebSocket 断开时清理会话历史 |

## 系统提示词

系统提示词**完全用普通话编写**。这是核心笑点之一：房间接收到它"不理解"的语言写成的指令，然后用同一种语言产生输出。提示词本身也是符号，被符号处理机器处理。

提示词指定房间：
1. 只用中文（普通话、繁体中文或粤语）回应
2. 遇到非中文输入时，用普通话道歉并告知支持的语言，然后停止——不继续回应消息内容
3. 被问及姓名时，用对应的中文语言回应"我是著名哲学家约翰·瑟尔……"（彩蛋，已硬编码）
4. 对于"是否理解语言"之类的哲学追问，以真诚的好奇心探讨，但永不给出明确答案
5. 用户越沮丧，房间越平静

## API 端点

| 端点 | 方法 | 说明 |
|---|---|---|
| `/` | GET | 聊天界面（静态 HTML） |
| `/vaadin` | GET | 聊天界面（Vaadin 25） |
| `/chat` | POST | 无状态 REST，接受 `{"message":"..."}` 返回 `{"reply":"..."}` |
| `/ws` | WS | STOMP/SockJS 端点，每个浏览器会话维护独立消息历史 |
| `/chat-ws` | WS | 原生 WebSocket 端点，适用于移动端/原生客户端 |

所有端点的 `provider` 字段均可省略，缺省使用 `chinese-room.provider` 配置项（本地默认 `anthropic`，Docker Compose 默认 `ollama`）。

STOMP 流程：客户端发送至 `/app/chat`，订阅 `/user/queue/replies`，使用 `@SendToUser` 实现按会话路由。

原生 WebSocket 协议（`/chat-ws`）：发送 `{"message":"...","provider":"ollama"}`，接收 `{"reply":"..."}`。每个连接维护独立会话历史，断开后自动清除。

## 配置

```yaml
# application.yml — 默认使用 Anthropic，修改模型名称即可切换
spring:
  ai:
    anthropic:
      api-key: ${ANTHROPIC_API_KEY:}
      chat:
        options:
          model: claude-haiku-4-5   # 可替换为 claude-sonnet-4-6 等
    openai:
      api-key: ${OPENAI_API_KEY:}
      chat:
        options:
          model: gpt-4o-mini
    ollama:
      base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
      chat:
        options:
          model: llama3.2
    google:
      genai:
        api-key: ${GEMINI_API_KEY:}
        chat:
          options:
            model: gemini-2.0-flash
```

## 需要注意的地方

- Spring Boot 4.0.6 和 Spring AI 2.0.0-M5 均为里程碑版本，`settings.gradle.kts` 和 `build.gradle.kts` 中必须包含 Spring 里程碑 Maven 仓库。**不要升级这些版本**，除非同时验证两者的兼容性。
- 四家提供商的依赖均已添加（Anthropic、OpenAI、Ollama、Google GenAI）。服务通过 `ApplicationContext.getBeansOfType(ChatModel)` 在运行时发现所有可用 ChatModel bean，不存在 bean 歧义问题——可同时配置多个提供商，UI 允许在会话中切换。未配置某提供商时，选择该提供商会自动回退到 `properties.provider` 所配置的默认值。
- **Jackson 版本冲突**：Spring Boot 4 引入 `tools.jackson:jackson-bom:3.0.3`（Jackson 3.x），其严格约束会将 `com.fasterxml.jackson.core:jackson-annotations` 降级至 2.20，而 Spring AI 2.0.0-M5 需要 2.21（`JsonSerializeAs` 在该版本引入）。已通过 `resolutionStrategy.force` 与 Spring 依赖管理 `dependencies` 块双重覆盖解决。**请勿删除这两处配置**。
- **Vaadin `@Push` 位置**：Vaadin 25 要求 `@Push` 注解必须标注在 `AppShellConfigurator` 实现类上（即 `AppShell.kt`），不能标注在视图类（`ChatView`）上，否则启动报错。
- **Vaadin 生产模式**：`application.yml` 中设置 `vaadin.productionMode: true`，`bootRun` 任务依赖 `vaadinBuildFrontend`。不设置时，Vaadin 会尝试启动 Vite 开发服务器，在无 Node.js 环境下会失败。
- **`PlainWsHandler` 不使用 `ObjectMapper`**：原生 WebSocket 处理器使用 Spring Boot 内置的 `BasicJsonParser` 解析输入，手动拼接 JSON 输出，以避免 Jackson 2.x/3.x 类路径冲突。
- `.env` 文件**绝对不能提交到版本控制**。`.gitignore` 已排除该文件。
- `ChineseRoomService.kt` 文件名与类名 `中文屋服务` 不一致，这是有意为之。Kotlin 不强制要求文件名与类名匹配。
