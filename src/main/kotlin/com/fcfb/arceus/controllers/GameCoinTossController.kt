package com.fcfb.arceus.controllers

import com.fcfb.arceus.enums.gameflow.CoinTossCall
import com.fcfb.arceus.enums.gameflow.CoinTossChoice
import com.fcfb.arceus.enums.gameflow.OvertimeCoinTossChoice
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.service.fcfb.game.GameCoinTossService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/game-coin-toss")
@CrossOrigin(origins = ["*"])
class GameCoinTossController(
    private val gameCoinTossService: GameCoinTossService,
) {
    @Operation(summary = "Run coin toss")
    @PutMapping("/call")
    fun runCoinToss(
        @RequestParam("gameId") gameId: Int,
        @RequestParam("coinTossCall") coinTossCall: CoinTossCall,
    ): Game = gameCoinTossService.runCoinToss(gameId, coinTossCall)

    @Operation(summary = "Record coin toss choice")
    @PutMapping("/choice")
    fun makeCoinTossChoice(
        @RequestParam("gameId") gameId: Int,
        @RequestParam("coinTossChoice") coinTossChoice: CoinTossChoice,
    ): Game = gameCoinTossService.makeCoinTossChoice(gameId, coinTossChoice)

    @Operation(summary = "Record overtime coin toss choice")
    @PutMapping("/overtime-choice")
    fun makeOvertimeCoinTossChoice(
        @RequestParam("gameId") gameId: Int,
        @RequestParam("coinTossChoice") coinTossChoice: OvertimeCoinTossChoice,
    ): Game = gameCoinTossService.makeOvertimeCoinTossChoice(gameId, coinTossChoice)
}
