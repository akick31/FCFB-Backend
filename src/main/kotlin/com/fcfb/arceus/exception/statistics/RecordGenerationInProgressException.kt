package com.fcfb.arceus.exception.statistics

class RecordGenerationInProgressException : Exception("Record generation is already running; try again once it completes") {
    override fun toString(): String = "RecordGenerationInProgressException: ${super.message}"
}
