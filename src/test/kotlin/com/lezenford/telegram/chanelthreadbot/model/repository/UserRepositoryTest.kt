package com.lezenford.telegram.chanelthreadbot.model.repository

import com.lezenford.telegram.chanelthreadbot.BaseTest
import com.lezenford.telegram.chanelthreadbot.model.entity.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class UserRepositoryTest : BaseTest() {

    @Test
    fun `save user with telegram id`() {
        val user = User(id = 1234, username = "test")
        userRepository.save(user)
        assertThat(userRepository.findAll()).anyMatch { it.id == user.id }
    }

    @Test
    fun `modify user and save`() {
        val user = userRepository.save(User(id = 1234, username = "test"))
        user.username = "test2"
        userRepository.save(user)
        assertThat(userRepository.findAll()).anyMatch { it.username == user.username }
    }
}