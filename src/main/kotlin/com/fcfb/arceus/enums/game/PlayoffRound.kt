package com.fcfb.arceus.enums.game

enum class PlayoffRound(val label: String) {
    FIRST_ROUND("First Round"),
    SECOND_ROUND("Second Round"),
    QUARTERFINAL("Quarterfinal"),
    SEMIFINAL("Semifinal"),
    NATIONAL_CHAMPIONSHIP("National Championship"),
    ;

    companion object {
        fun fromLabel(label: String?): PlayoffRound? = entries.firstOrNull { it.label.equals(label?.trim(), ignoreCase = true) }
    }
}
