package com.fcfb.arceus.exception.game

class GameWeekJobNotFoundException(jobId: String) : Exception("No game week job found with id $jobId") {
    override fun toString(): String = "GameWeekJobNotFoundException: ${super.message}"
}
