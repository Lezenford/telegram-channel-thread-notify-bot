package com.lezenford.telegram.chanelthreadbot.service

import com.lezenford.telegram.chanelthreadbot.telegram.menu.Menu
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Update

@Service
class MenuService(menus: List<Menu>) {
    private val menus: Map<String, Menu> = menus.associateBy { it.id }

    suspend fun executeCallbackQuery(update: Update): BotApiMethod<*> {
        val menuId = update.callbackQuery.data.split(":").firstOrNull()
            ?: throw IllegalArgumentException("Incorrect callback data: ${update.callbackQuery.data}")
        val menu = menus[menuId] ?: throw IllegalArgumentException("Unknown menu: $menuId")
        return menu.receiveRequest(update.callbackQuery)
            ?: AnswerCallbackQuery(update.callbackQuery.id)
    }
}
