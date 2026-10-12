package com.fcfb.arceus.controllers

import com.fcfb.arceus.service.fcfb.game.GameCoinTossService
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class GameCoinTossTestConfiguration {
    @Bean
    @Primary
    fun coinTossServiceMock(): GameCoinTossService = mockk()
}
