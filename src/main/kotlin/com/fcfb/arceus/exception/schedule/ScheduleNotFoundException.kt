package com.fcfb.arceus.exception.schedule

class ScheduleNotFoundException(message: String) : Exception(message) {
    override fun toString(): String = "ScheduleNotFoundException: ${super.message}"
}
