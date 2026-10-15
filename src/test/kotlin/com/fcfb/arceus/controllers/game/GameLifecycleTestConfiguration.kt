package com.fcfb.arceus.controllers.game

import com.fcfb.arceus.service.fcfb.game.GameCreationService
import com.fcfb.arceus.service.fcfb.game.GameFinalizationService
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class GameLifecycleTestConfiguration {
    @Bean
    @Primary
    fun creationServiceMock(): GameCreationService = mockk()

    @Bean
    @Primary
    fun finalizationServiceMock(): GameFinalizationService = mockk()
}
