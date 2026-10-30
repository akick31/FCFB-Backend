package com.fcfb.arceus.enums.animation

import com.fcfb.arceus.enums.game.GameType

enum class FieldStyle {
    HOME_FIELD,
    PLAYOFF,
    NATIONAL_CHAMPIONSHIP,
    CONFERENCE_CHAMPIONSHIP,
    BOWL,
    ;

    companion object {
        fun of(gameType: GameType?): FieldStyle =
            when (gameType) {
                GameType.PLAYOFFS -> PLAYOFF
                GameType.NATIONAL_CHAMPIONSHIP -> NATIONAL_CHAMPIONSHIP
                GameType.CONFERENCE_CHAMPIONSHIP -> CONFERENCE_CHAMPIONSHIP
                GameType.BOWL -> BOWL
                else -> HOME_FIELD
            }
    }
}
