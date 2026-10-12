package com.fcfb.arceus.exception.user

class UserNotFoundException(message: String) : Exception(message) {
    override fun toString(): String = "UserNotFoundException: ${super.message}"
}
