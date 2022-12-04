package com.lezenford.telegram.chanelthreadbot.extensions

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethodBoolean
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethodMessage
import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import java.io.Serializable

private val typeFactory = jacksonObjectMapper().typeFactory

fun <T : Serializable> BotApiMethod<T>.responseClass(): JavaType {
    return when (this) {
        is GetUpdates -> typeFactory.constructParametricType(List::class.java, Update::class.java)
        is BotApiMethodMessage -> typeFactory.constructType(Message::class.java)
        is BotApiMethodBoolean -> typeFactory.constructType(Boolean::class.java)
        else -> throw IllegalArgumentException("Unsupported class ${this::class}")
    }
}