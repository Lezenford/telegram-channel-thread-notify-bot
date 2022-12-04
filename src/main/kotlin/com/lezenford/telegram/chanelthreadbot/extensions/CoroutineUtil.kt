package com.lezenford.telegram.chanelthreadbot.extensions

import java.lang.reflect.Method
import kotlin.reflect.jvm.kotlinFunction

val Method.isSuspend: Boolean
    get() = kotlinFunction?.isSuspend ?: false

internal fun <T> Array<T>.setLast(value: T) {
    this[lastIndex] = value
}