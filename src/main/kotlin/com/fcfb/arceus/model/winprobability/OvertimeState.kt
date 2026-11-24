package com.fcfb.arceus.model.winprobability

import com.fcfb.arceus.enums.team.TeamSide

data class OvertimeState(
    val offense: TeamSide,
    val period: Int,
    val offenseHasFirstPossession: Boolean,
    val offenseMargin: Int,
    val conversionPending: Boolean,
    val down: Int,
    val distance: Int,
    val yardsToGoal: Int,
)
