package com.lezenford.telegram.chanelthreadbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
@ConfigurationPropertiesScan
class ChanelThreadBotApplication

fun main(args: Array<String>) {
    runApplication<ChanelThreadBotApplication>(*args)
}
