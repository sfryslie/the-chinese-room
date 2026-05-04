package com.sfryslie.chineseroom

import com.sfryslie.chineseroom.config.ChineseRoomProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(ChineseRoomProperties::class)
class ChineseRoomApplication

fun main(args: Array<String>) {
    runApplication<ChineseRoomApplication>(*args)
}
