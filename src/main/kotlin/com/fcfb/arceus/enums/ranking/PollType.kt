package com.fcfb.arceus.enums.ranking

enum class PollType(val description: String) {
    COACHES_POLL("Coaches Poll"),
    PLAYOFF_COMMITTEE("Playoff Committee"),
    ;

    companion object {
        fun fromString(value: String): PollType? =
            values().find { it.name.equals(value, ignoreCase = true) || it.description.equals(value, ignoreCase = true) }
    }
}
