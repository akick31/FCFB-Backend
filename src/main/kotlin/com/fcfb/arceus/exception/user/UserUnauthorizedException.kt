package com.fcfb.arceus.exception.user

class UserUnauthorizedException : Exception("User is unauthorized") {
    override fun toString(): String = "UserUnauthorizedException: ${super.message}"
}
