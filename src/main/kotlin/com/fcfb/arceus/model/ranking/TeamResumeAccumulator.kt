package com.fcfb.arceus.model.ranking

internal data class TeamResumeAccumulator(
    var conferenceWins: Int = 0,
    var conferenceLosses: Int = 0,
    var q1Wins: Int = 0,
    var q1Losses: Int = 0,
    var q2Wins: Int = 0,
    var q2Losses: Int = 0,
    var thWins: Int = 0,
    var thLosses: Int = 0,
    var q4Wins: Int = 0,
    var q4Losses: Int = 0,
    var t25Wins: Int = 0,
    var t25Losses: Int = 0,
    var t50Wins: Int = 0,
    var t50Losses: Int = 0,
    var t100Wins: Int = 0,
    var t100Losses: Int = 0,
    var opponentRankSum: Int = 0,
    var opponentRankCount: Int = 0,
    var opponentCompositeSum: Double = 0.0,
    var opponentCompositeCount: Int = 0,
)
