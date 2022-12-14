package com.lezenford.telegram.chanelthreadbot.extensions

import org.hibernate.Hibernate

fun <T> T.init(): T = also { Hibernate.initialize(it) }