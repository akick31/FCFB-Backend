package com.fcfb.arceus.exception.team

class InvalidUniformException(message: String) : Exception(message) {
    override fun toString(): String = "InvalidUniformException: ${super.message}"
}
