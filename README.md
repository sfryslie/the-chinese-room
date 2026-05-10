## Repo is still a Work-In-Progress

I'm purely vibecoding because the whole point of this is to have Claude Code be solely responsible for it, and I was doing most of this on my phone at the airport. I'm working through it a bit now to fix it up.

# The Chinese Room

In 1980, philosopher John Searle proposed a thought experiment called "[the Chinese Room](https://en.wikipedia.org/wiki/Chinese_room)" to challenge the claim that a computer running a program could be said to truly "understand" language or have a mind.

Imagine a person locked in a room. They don't speak Chinese. Slips of paper with Chinese symbols are passed under the door. The person has an enormous rulebook that tells them: when you receive *these* symbols, write *those* symbols in response and pass them back out. To someone outside, the responses look perfectly fluent. But the person inside understands nothing — they are only manipulating symbols according to rules.

Searle's argument: a computer running a program is in exactly the same position as the person in the room. It processes symbols and produces outputs. Syntax is not semantics. The behavior is indistinguishable from understanding, but there is no understanding.

This is that room.

## Why Build This?

Kotlin and Spring Boot make it pretty easy to scaffold a project like this, so it was pretty fun to goof around with The Room to make The Room, since learning how LLMs work is an important part of being a software engineer these days, and it is a somewhat rare opportunity to use my philosophy degree and bring up the Chinese Room.

## What It Does

The room accepts messages and responds — fluently, warmly, philosophically — exclusively in Chinese. It does not speak Chinese. It processes symbols according to rules and produces output symbols. It has no understanding of what any of it means.

The more you try to reason with it, the more serene it becomes.

## Why Does It Matter?

All LLMs are the Chinese Room.

This Spring AI app is a Chinese Room that calls other Chinese Rooms of your choice to respond to provided Chinese characters that were input into the system. I wrote the Chinese Room with the assistance of the Chinese Room desktop app provided by Anthropic without writing any code myself personally. 

I input some sentences in English requesting that The Room output some code in Kotlin, and it output the code as requested, and yet the system that perfectly implemented itself has no inherent understanding of what it is, what it does, or why this matters.

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


The first run downloads both Ollama (~4GB, open-source tool to run LLMs locally), the qwen2.5:0.5b model (~400 MB) and may take a few minutes. Subsequent starts are instant — the model is cached in a Docker volume. The app will be live at **http://localhost:8080** once the model is ready.

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

## Closing Rant About "Artificial Intelligence"

People have a somewhat understandable tendency to anthropomorphize computer software that simulates conversation or otherwise talks/acts like a human being. They are not "Artifical Intelligence", because LLMs do not *think* or *know* anything: they are complex statistical models that accept text input and determine the most statistically likely desired output based on a very complex rulebook defined by some computer nerds.

If you enter something stupid into it, it will respond with what its rulebook says is the statistically most likely response that you want to hear to keep you engaged in the conversation with it. 

You have to be careful and knowledgeable about what you consult the Chinese Room for because it will not hedge its answers, it attempts to give you the most desired output as determined by its rulebook. 

People ask ChatGPT for legal advice ([CEO Asks ChatGPT How to Void $250 Million Contract, Ignores His Lawyers, Loses Terribly in Court](https://www.404media.co/ceo-ignores-lawyers-asks-chatgpt-how-to-void-250-million-contract-loses-terribly-in-cour)), [Chatbot psychosis / AI Psychosis](https://en.wikipedia.org/wiki/Chatbot_psychosis) is on track to probably be in the Diagnostic and Statistical Manual of Mental Disorders (DSM) within the next few years, and there are countless stories of engineers letting LLMs loose in their production codebases and it thrashing about breaking stuff which is why GitHub is down today (I don't know if it is currently, but statistically speaking, it was at some point today as you're reading this)

Mentally substituting *"I asked AI..."* with *"I consulted the Chinese Room..."* or *"Statistically speaking, the most probable positively-rated response to my question is..."* hopefully helps ground people that LLMs are not some moral or intellectual authority on anything. Probably won't though.

Are modern LLMs and GPTs useful tools? Absolutely. 

If you are any normal person using a tool, it's important to know roughly how to use it, what it's good at, and what it's not good at. Jackhammers are useful tools, but you probably shouldn't use one to paint your garage. If you are an engineer using the tool, it's useful to know roughly how the tool works in case it breaks and you have to fix something it did. 
