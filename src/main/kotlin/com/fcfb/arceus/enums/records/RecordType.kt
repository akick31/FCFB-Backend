package com.fcfb.arceus.enums.records

enum class RecordType(val isLowest: Boolean) {
    SINGLE_GAME(false),

    SINGLE_GAME_LOWEST(true),

    SINGLE_SEASON(false),

    SINGLE_SEASON_LOWEST(true),

    GENERAL(false),

    GENERAL_LOWEST(true),

    SINGLE_POSTSEASON_GAME(false),

    SINGLE_POSTSEASON_GAME_LOWEST(true),

    SINGLE_POSTSEASON(false),

    SINGLE_POSTSEASON_LOWEST(true),
}
