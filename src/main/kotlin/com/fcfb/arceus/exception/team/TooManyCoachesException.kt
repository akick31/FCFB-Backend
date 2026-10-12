package com.fcfb.arceus.exception.team

class TooManyCoachesException : Exception("Attempting to add too many coaches to a team") {
    override fun toString(): String = "TooManyCoachesException: ${super.message}"
}
