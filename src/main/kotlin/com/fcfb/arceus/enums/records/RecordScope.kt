package com.fcfb.arceus.enums.records

/**
 * The competitive scope a record is measured within.
 */
enum class RecordScope {
    /**
     * Best/worst across the entire league (scope value is null).
     */
    LEAGUE,

    /**
     * Best/worst among the teams of a single conference (scope value is the conference).
     */
    CONFERENCE,

    /**
     * Best/worst for a single team (scope value is the team name).
     */
    TEAM,
}
