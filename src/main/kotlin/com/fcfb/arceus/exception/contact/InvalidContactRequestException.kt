package com.fcfb.arceus.exception.contact

class InvalidContactRequestException(message: String) : Exception(message) {
    override fun toString(): String = "InvalidContactRequestException: ${super.message}"
}
