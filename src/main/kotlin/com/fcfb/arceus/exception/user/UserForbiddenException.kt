package com.fcfb.arceus.exception.user

class UserForbiddenException : Exception("User is not permitted to modify this account") {
    override fun toString(): String = "UserForbiddenException: ${super.message}"
}
