package com.fcfb.arceus.exception.signup

class InvalidNewSignupException(message: String) : Exception(message) {
    override fun toString(): String = "InvalidNewSignupException: ${super.message}"
}
