package com.fcfb.arceus.exception.contact

class ContactDeliveryException : Exception("Your message couldn't be sent right now. Please try again later.") {
    override fun toString(): String = "ContactDeliveryException: ${super.message}"
}
