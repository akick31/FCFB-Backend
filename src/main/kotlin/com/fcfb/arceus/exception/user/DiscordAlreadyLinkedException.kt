package com.fcfb.arceus.exception.user

class DiscordAlreadyLinkedException : Exception("This Discord account is already linked to another user") {
    override fun toString(): String = "DiscordAlreadyLinkedException: ${super.message}"
}
