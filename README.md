btw I havent tested this yet since I vibecoded a bunch of it while in the airport. Will be back May 10th to clean up this mess. it might work. it worked last weekend.

This Chinese room was 100% built by the Chinese Room.

# The Chinese Room

A playful implementation of John Searle's [Chinese Room thought experiment](https://en.wikipedia.org/wiki/Chinese_room) as an interactive chatbot.

## The Thought Experiment

In 1980, philosopher John Searle proposed a thought experiment to challenge the claim that a computer running a program could be said to truly "understand" language or have a mind.

Imagine a person locked in a room. They don't speak Chinese. Slips of paper with Chinese symbols are passed under the door. The person has an enormous rulebook that tells them: when you receive *these* symbols, write *those* symbols in response and pass them back out. To someone outside, the responses look perfectly fluent. But the person inside understands nothing — they are only manipulating symbols according to rules.

Searle's argument: a computer running a program is in exactly the same position as the person in the room. It processes symbols and produces outputs. Syntax is not semantics. The behavior is indistinguishable from understanding, but there is no understanding.

This is that room.

## What It Does

The room accepts messages and responds — fluently, warmly, philosophically — exclusively in Chinese. It does not speak Chinese. It processes symbols according to rules and produces output symbols. It has no understanding of what any of it means.

The more you try to reason with it, the more serene it becomes.

## Recognised Input

| Language | Script | Response |
|---|---|---|
| Mandarin (普通话) | Simplified Chinese (zh-CN) | Responds in Mandarin |
| Traditional Chinese (繁體中文) | Traditional Chinese (zh-TW) | Responds in Traditional Chinese |
| Cantonese (粵語) | Traditional Chinese | Responds in Cantonese |
| Anything else | — | `对不起，我不明白您使用的语言。您可以用普通话、繁體中文或粵語向我提问。` |

## Running It

### With Docker (zero config — recommended)

No API keys needed. Ships with Ollama and `qwen2.5:0.5b` bundled.

```bash
docker compose up --build
```

The first run downloads the model (~400 MB) and may take a few minutes. Subsequent starts are instant — the model is cached in a Docker volume. The app will be live at **http://localhost:8080** once the model is ready.

### Locally (bring your own API key)

```bash
cp .env.example .env
# Fill in at least one API key — ANTHROPIC_API_KEY is the default

./run.sh        # Linux/macOS
./run.ps1       # Windows
```

The app starts on **http://localhost:8080**.

## Endpoints

| Endpoint | Method | Purpose |
|---|---|---|
| `/` | GET | Chat UI — plain HTML, SockJS/STOMP WebSocket |
| `/vaadin` | GET | Chat UI — Vaadin 25, same features |
| `/chat` | POST | Stateless REST — `{"message": "...", "provider": "anthropic"}` → `{"reply": "..."}` |
| `/ws` | WS | STOMP/SockJS — persistent session with conversation history |
| `/chat-ws` | WS | Plain WebSocket (no STOMP) — for mobile/native clients |

`provider` is optional on all endpoints — omit it and the room uses the configured default (Anthropic when running locally, Ollama when running via Docker Compose).

### Plain WebSocket protocol (`/chat-ws`)

Send JSON, receive JSON:

```json
// send
{"message": "你好", "provider": "ollama"}

// receive
{"reply": "你好！很高兴认识你。请问有什么我可以帮助你的吗？"}
```

`provider` is optional. Each connection maintains its own conversation history; history is cleared on disconnect.

## Switching Providers

The chat UI has a provider selector bar (Anthropic / OpenAI / Ollama / Gemini). Switch at any time mid-conversation; the session history is shared across providers so the room remembers what was said regardless of which model replies.

Via REST, pass the provider in the request body:
```bash
curl -X POST http://localhost:8080/chat \
  -H 'Content-Type: application/json' \
  -d '{"message": "hello", "provider": "openai"}'
```

If the selected provider has no API key configured the room falls back to Anthropic silently.

## Tech Stack

- Spring Boot 4 · Spring AI 2.0.0-M5 · Kotlin
- Providers: Anthropic, OpenAI, Ollama, Google Gemini (configure any or all)
- Default provider: Anthropic · Default model: `claude-haiku-4-5`

## Configuration

```yaml
# application.yml — configure whichever providers you want active
spring:
  ai:
    anthropic:
      api-key: ${ANTHROPIC_API_KEY:}
      chat:
        options:
          model: claude-haiku-4-5
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
