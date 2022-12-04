package com.lezenford.telegram.chanelthreadbot.model.repository

import com.lezenford.telegram.chanelthreadbot.model.entity.UserHistory
import org.springframework.data.jpa.repository.JpaRepository

interface UserHistoryRepository : JpaRepository<UserHistory, Int> {
    fun findByUserId(userid: Long): UserHistory?
}