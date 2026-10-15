package com.fcfb.arceus.controllers.game

import com.fcfb.arceus.service.auth.SessionService
import com.fcfb.arceus.service.fcfb.game.StartGameWeekJobService
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class StartGameWeekJobTestConfiguration {
    @Bean
    @Primary
    fun jobServiceMock(): StartGameWeekJobService = mockk(relaxUnitFun = true)

    @Bean
    @Primary
    fun sessionServiceMock(): SessionService = mockk(relaxed = true)
}
