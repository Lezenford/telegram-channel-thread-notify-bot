package com.lezenford.telegram.chanelthreadbot.extensions

const val PARSE_MODE = "MarkdownV2"

const val USER_ID_LINK_PREFIX = "tg://user?id="
const val USER_LOGIN_LINK_PREFIX = "tg://resolve?domain="

fun String.escape(): String = this.map { if (it.code in 1..126) "\\$it" else it }.joinToString("")

fun org.telegram.telegrambots.meta.api.objects.User.fullName() =
    listOf(firstName, lastName).mapNotNull { it }.joinToString(" ")

fun org.telegram.telegrambots.meta.api.objects.User.toLink(): String =
    userName?.let { "[${fullName().escape()}]($USER_LOGIN_LINK_PREFIX$it)" }
        ?: "[${fullName().escape()}]($USER_ID_LINK_PREFIX$id)"

fun com.lezenford.telegram.chanelthreadbot.model.entity.User.toLink(): String =
    username?.let { "[${fullName.escape()}]($USER_LOGIN_LINK_PREFIX$it)" }
        ?: "[${fullName.escape()}]($USER_ID_LINK_PREFIX$id)"
