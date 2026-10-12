package com.fcfb.arceus.exception.discord

class DiscordUserNotFoundException : Exception("Discord user not found") {
    override fun toString(): String = "DiscordUserNotFoundException: ${super.message}"
}
