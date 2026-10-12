package com.fcfb.arceus.exception.signup

class NewSignupNotFoundException(id: Long) : Exception("New signup not found with id $id") {
    override fun toString(): String = "NewSignupNotFoundException: ${super.message}"
}
