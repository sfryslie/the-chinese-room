package com.sfryslie.chineseroom.view

import com.sfryslie.chineseroom.config.ChineseRoomProperties
import com.sfryslie.chineseroom.service.中文屋服务
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.ComponentEventListener
import com.vaadin.flow.component.Key
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.page.Push
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route

@Route("vaadin")
@PageTitle("The Chinese Room — Vaadin")
@Push
class ChatView(
    private val service: 中文屋服务,
    private val properties: ChineseRoomProperties
) : VerticalLayout() {

    private val messagesDiv = Div()
    private val input = TextField()
    private val sendButton = Button("Send")
    private var selectedProvider = properties.provider
    private val providerButtons = linkedMapOf(
        "anthropic" to Button("Anthropic"),
        "openai"    to Button("OpenAI"),
        "ollama"    to Button("Ollama"),
        "gemini"    to Button("Gemini")
    )
    private var sessionId = ""
    private var welcomeRemoved = false

    init {
        setSizeFull()
        isPadding = false
        isSpacing = false
        style
            .set("font-family", "Georgia, serif")
            .set("background", "#fafaf8")
            .set("color", "#1a1a1a")
            .set("height", "100vh")
            .set("display", "flex")
            .set("flex-direction", "column")

        add(buildHeader(), buildProviderBar(), buildMessages(), buildInputArea())
        setProviderActive(selectedProvider)
    }

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        sessionId = attachEvent.ui.session.session.id
    }

    private fun buildHeader(): HorizontalLayout {
        val title = H1("The Chinese Room")
        title.style
            .set("font-size", "1.3rem")
            .set("font-weight", "normal")
            .set("letter-spacing", "0.02em")
            .set("margin", "0")

        val subtitle = Span("Searle (1980) — faithfully implemented")
        subtitle.style
            .set("font-size", "0.72rem")
            .set("color", "#999")
            .set("font-family", "monospace")
            .set("display", "block")
            .set("margin-top", "2px")

        val titleBlock = VerticalLayout(title, subtitle)
        titleBlock.isPadding = false
        titleBlock.isSpacing = false

        val header = HorizontalLayout(titleBlock)
        header.setWidthFull()
        header.isPadding = false
        header.style
            .set("padding", "14px 24px")
            .set("border-bottom", "1px solid #e0e0d8")
            .set("background", "white")
            .set("flex-shrink", "0")
        header.defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
        return header
    }

    private fun buildProviderBar(): HorizontalLayout {
        val label = Span("Provider:")
        label.style
            .set("font-size", "0.7rem")
            .set("font-family", "monospace")
            .set("color", "#aaa")
            .set("text-transform", "uppercase")
            .set("letter-spacing", "0.08em")
            .set("margin-right", "4px")

        providerButtons.forEach { (provider, btn) ->
            btn.addClickListener { setProviderActive(provider) }
            styleProviderButton(btn, false)
        }

        val bar = HorizontalLayout(label, *providerButtons.values.toTypedArray())
        bar.setWidthFull()
        bar.isPadding = false
        bar.style
            .set("padding", "8px 24px")
            .set("border-bottom", "1px solid #e0e0d8")
            .set("background", "#fafaf8")
            .set("flex-shrink", "0")
            .set("gap", "8px")
        bar.defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
        return bar
    }

    private fun buildMessages(): Div {
        val welcome = Div()
        welcome.addClassName("welcome-msg")
        welcome.element.setProperty(
            "innerHTML",
            "<p>Type anything. The room will respond.</p>" +
            "<p>The room does not speak Mandarin.</p>" +
            "<p>The room is not an AI. <strong style=\"color:#888\">It is a room.</strong></p>"
        )
        welcome.style
            .set("text-align", "center")
            .set("color", "#bbb")
            .set("font-size", "0.82rem")
            .set("font-family", "monospace")
            .set("padding", "48px 20px")
            .set("line-height", "2")

        messagesDiv.add(welcome)
        messagesDiv.style
            .set("flex", "1")
            .set("overflow-y", "auto")
            .set("padding", "24px")
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("gap", "14px")
            .set("width", "100%")
        return messagesDiv
    }

    private fun buildInputArea(): HorizontalLayout {
        input.placeholder = "Type a message..."
        input.isEnabled = true
        input.style.set("font-family", "monospace")
        input.element.setAttribute("autocomplete", "off")
        input.addKeyDownListener(Key.ENTER, ComponentEventListener { send() })

        sendButton.style
            .set("background", "#1a1a1a")
            .set("color", "white")
            .set("border-radius", "8px")
            .set("font-family", "monospace")
            .set("letter-spacing", "0.04em")
            .set("min-width", "80px")
        sendButton.addClickListener { send() }

        val area = HorizontalLayout(input, sendButton)
        area.setWidthFull()
        area.isPadding = false
        area.style
            .set("padding", "14px 24px")
            .set("border-top", "1px solid #e0e0d8")
            .set("background", "white")
            .set("flex-shrink", "0")
            .set("gap", "10px")
        area.expand(input)
        area.defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
        return area
    }

    private fun styleProviderButton(btn: Button, active: Boolean) {
        btn.style
            .set("padding", "4px 12px")
            .set("border-radius", "20px")
            .set("font-family", "monospace")
            .set("font-size", "0.75rem")
            .set("cursor", "pointer")
            .set("min-width", "0")
        if (active) {
            btn.style
                .set("background", "#1a1a1a")
                .set("border", "1px solid #1a1a1a")
                .set("color", "white")
        } else {
            btn.style
                .set("background", "white")
                .set("border", "1px solid #ddd")
                .set("color", "#666")
        }
    }

    private fun setProviderActive(provider: String) {
        selectedProvider = provider
        providerButtons.forEach { (p, btn) -> styleProviderButton(btn, p == provider) }
    }

    private fun send() {
        val text = input.value.trim()
        if (text.isEmpty() || !sendButton.isEnabled) return
        input.value = ""
        input.isEnabled = false
        sendButton.isEnabled = false

        val capturedId = sessionId
        val capturedProvider = selectedProvider

        if (!welcomeRemoved) {
            messagesDiv.children
                .filter { it.element.classList.contains("welcome-msg") }
                .findFirst().orElse(null)
                ?.removeFromParent()
            welcomeRemoved = true
        }

        addBubble("user", text)
        val thinking = addThinking()

        val ui = ui.orElse(null) ?: return
        Thread {
            val reply = try {
                service.对话(capturedId, text, capturedProvider)
            } catch (e: Exception) {
                "（房间发生了错误）"
            }
            ui.access {
                thinking.removeFromParent()
                addBubble("room", reply)
                input.isEnabled = true
                sendButton.isEnabled = true
                input.focus()
            }
        }.start()
    }

    private fun addBubble(side: String, text: String): Div {
        val label = Span(if (side == "user") "You" else "The Room")
        label.style
            .set("font-size", "0.68rem")
            .set("font-family", "monospace")
            .set("color", "#bbb")
            .set("text-transform", "uppercase")
            .set("letter-spacing", "0.08em")

        val bubble = Div()
        bubble.text = text
        bubble.style
            .set("padding", "10px 14px")
            .set("border-radius", "14px")
            .set("line-height", "1.55")
            .set("word-break", "break-word")

        if (side == "user") {
            bubble.style
                .set("background", "#ebebeb")
                .set("border-bottom-right-radius", "4px")
                .set("font-family", "monospace")
                .set("font-size", "0.88rem")
                .set("color", "#333")
        } else {
            bubble.style
                .set("background", "#e8f0fe")
                .set("border-bottom-left-radius", "4px")
                .set("font-size", "1.15rem")
                .set("color", "#1a1a2e")
                .set("letter-spacing", "0.03em")
        }

        val group = Div(label, bubble)
        group.style
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("gap", "3px")
            .set("max-width", "72%")
        if (side == "user") {
            group.style.set("align-self", "flex-end").set("align-items", "flex-end")
        } else {
            group.style.set("align-self", "flex-start").set("align-items", "flex-start")
        }

        messagesDiv.add(group)
        messagesDiv.element.executeJs("this.scrollTop = this.scrollHeight")
        return group
    }

    private fun addThinking(): Div {
        val label = Span("The Room")
        label.style
            .set("font-size", "0.68rem")
            .set("font-family", "monospace")
            .set("color", "#bbb")
            .set("text-transform", "uppercase")
            .set("letter-spacing", "0.08em")

        val bubble = Div()
        bubble.text = "正在处理符号…"
        bubble.style
            .set("padding", "10px 14px")
            .set("border-radius", "14px")
            .set("border-bottom-left-radius", "4px")
            .set("font-style", "italic")
            .set("color", "#aaa")
            .set("font-size", "0.82rem")
            .set("font-family", "monospace")
            .set("background", "#f5f5f5")

        val group = Div(label, bubble)
        group.style
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("gap", "3px")
            .set("max-width", "72%")
            .set("align-self", "flex-start")
            .set("align-items", "flex-start")

        messagesDiv.add(group)
        messagesDiv.element.executeJs("this.scrollTop = this.scrollHeight")
        return group
    }
}
