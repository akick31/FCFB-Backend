package com.fcfb.arceus.enums.records

enum class RecordScope {
    LEAGUE,
    CONFERENCE,
    TEAM,
    ;

    companion object {
        val ALL: Set<RecordScope> = setOf(LEAGUE, CONFERENCE, TEAM)
        val TEAM_AND_CONFERENCE: Set<RecordScope> = setOf(TEAM, CONFERENCE)
    }
}
