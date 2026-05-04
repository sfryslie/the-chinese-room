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

```bash
cp .env.example .env
# Add your ANTHROPIC_API_KEY to .env

./run.sh        # Linux/macOS
./run.ps1       # Windows
```

The app starts on **http://localhost:8080**.

## Endpoints

| Endpoint | Method | Purpose |
|---|---|---|
| `/` | GET | Chat UI (WebSocket) |
| `/chat` | POST | Stateless REST — `{"message": "..."}` → `{"reply": "..."}` |
| `/ws` | WS | STOMP/SockJS — persistent session with conversation history |

## Tech Stack

- Spring Boot 4 · Spring AI 2.0.0-M5 · Kotlin
- Provider: Anthropic (configurable via `application.yml`)
- Default model: `claude-haiku-4-5`

## Configuration

```yaml
# application.yml
spring:
  ai:
    anthropic:
      chat:
        options:
          model: claude-haiku-4-5   # swap for claude-sonnet-4-6 etc.
```
