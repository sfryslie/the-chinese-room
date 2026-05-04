# 中文屋 — Claude 开发指南

## 项目概述

本项目是约翰·瑟尔1980年提出的"中文屋"思想实验的可运行实现。这是一个聊天机器人，它只用中文回应，同时在概念上"不理解"中文。这个矛盾正是整个项目的笑点所在。

## 技术栈

- **Spring Boot 4.0.6** + **Spring AI 2.0.0-M5**（里程碑版本，需要 `https://repo.spring.io/milestone` 仓库）
- **Kotlin**，Gradle Kotlin DSL 构建
- **AI 提供商**：Anthropic、OpenAI、Ollama、Google Gemini（均已配置，按需填写密钥即可）
- **Web 层**：Spring MVC（REST）+ Spring WebSocket（STOMP/SockJS）
- **默认模型**：`claude-haiku-4-5`（可在 `application.yml` 中配置）

## 构建与运行

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
    ChineseRoomProperties.kt         — 绑定 chinese-room.* 配置项
    WebSocketConfig.kt               — STOMP 端点配置（/ws，含 SockJS 回退）
  controller/
    ChatRestController.kt            — POST /chat，无状态 REST 接口
    ChatWebSocketController.kt       — @MessageMapping /app/chat，@SendToUser 回复
  service/
    ChineseRoomService.kt            — 核心逻辑（类名：中文屋服务）
src/main/resources/
  application.yml                    — 服务器端口、AI 配置、日志级别
  static/index.html                  — 单页聊天界面（SockJS + @stomp/stompjs）
```

## 核心服务：中文屋服务

这是最重要的文件。**类名和所有内部标识符均为中文**，这是有意为之的设计选择——代码本身也成为符号操作的一部分。

| 中文标识符 | 英文含义 |
|---|---|
| `聊天客户端` | ChatClient 实例 |
| `会话记录` | 按会话 ID 存储的消息历史（ConcurrentHashMap） |
| `系统提示词` | 系统提示（见下文） |
| `fun 对话(会话编号, 用户消息)` | 有状态对话，供 WebSocket 使用 |
| `fun 无状态对话(用户消息)` | 无状态对话，供 REST 接口使用 |
| `fun 断开连接(事件)` | WebSocket 断开时清理会话历史 |

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
| `/` | GET | 聊天界面（静态资源） |
| `/chat` | POST | 无状态 REST，接受 `{"message":"..."}` 返回 `{"reply":"..."}` |
| `/ws` | WS | STOMP/SockJS 端点，每个浏览器会话维护独立消息历史 |

WebSocket 流程：客户端发送至 `/app/chat`，订阅 `/user/queue/replies`，使用 `@SendToUser` 实现按会话路由。

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
- 四家提供商的依赖均已添加（Anthropic、OpenAI、Ollama、Google GenAI）。Spring AI 的自动配置是有条件的——只有设置了有效 API 密钥的提供商才会创建 ChatModel bean。**同时设置多个提供商的密钥会导致 Spring 报告 bean 歧义错误**，请一次只激活一个提供商。
- `.env` 文件**绝对不能提交到版本控制**。`.gitignore` 已排除该文件。
- `ChineseRoomService.kt` 文件名与类名 `中文屋服务` 不一致，这是有意为之。Kotlin 不强制要求文件名与类名匹配。
