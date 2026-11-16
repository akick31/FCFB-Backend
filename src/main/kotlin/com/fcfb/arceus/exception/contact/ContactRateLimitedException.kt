package com.fcfb.arceus.exception.contact

class ContactRateLimitedException : Exception("Too many messages sent. Please wait a few minutes and try again.") {
    override fun toString(): String = "ContactRateLimitedException: ${super.message}"
}
