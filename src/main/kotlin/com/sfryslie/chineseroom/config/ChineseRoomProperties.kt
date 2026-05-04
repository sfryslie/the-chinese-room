package com.sfryslie.chineseroom.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "chinese-room")
data class ChineseRoomProperties(
    val model: String = "claude-haiku-4-5",
    val provider: String = "anthropic"
)
