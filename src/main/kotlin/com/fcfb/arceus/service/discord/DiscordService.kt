package com.fcfb.arceus.service.discord

import com.fcfb.arceus.dto.request.GameFinalizationRollbackRequest
import com.fcfb.arceus.dto.standard.SignupInfo
import com.fcfb.arceus.exception.discord.DiscordUserNotFoundException
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.util.Logger
import com.fcfb.arceus.util.ServerUtils
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.User
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.toList
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class DiscordService(
    private val restTemplate: RestTemplate,
    private val serverUtils: ServerUtils,
    @Value("\${discord.bot.url}")
    private val discordBotUrl: String,
    @Value("\${discord.guild.id}")
    private val guildId: String,
    @Value("\${discord.bot.token}")
    private val botToken: String,
    @Value("\${bot.service.key}")
    private val botServiceKey: String,
) {
    suspend fun createGameThread(game: Game): List<String>? {
        val discordBotUrl = "$discordBotUrl/start_game"
        val requestEntity = HttpEntity(game, botRequestHeaders())

        return try {
            val response =
                serverUtils.retryWithExponentialBackoff {
                    restTemplate.postForEntity(discordBotUrl, requestEntity, String::class.java)
                }
            response.body?.split(",")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.error("There was an error starting the game thread for ${game.gameId}: ${e.message}")
            null
        }
    }

    fun notifyDelayOfGame(
        game: Game,
        isDelayofGameOut: Boolean,
    ) {
        val discordBotUrl = "$discordBotUrl/delay_of_game?isDelayOfGameOut=$isDelayofGameOut"
        val requestEntity = HttpEntity(game, botRequestHeaders())
        try {
            restTemplate.postForEntity(discordBotUrl, requestEntity, String::class.java)
        } catch (e: Exception) {
            Logger.error("There was an error notifying the delay of game for ${game.gameId}: " + e.message)
        }
    }

    fun notifyGameModeChange(game: Game) {
        val discordBotUrl = "$discordBotUrl/game_mode"
        val requestEntity = HttpEntity(game, botRequestHeaders())
        try {
            restTemplate.postForEntity(discordBotUrl, requestEntity, String::class.java)
        } catch (e: Exception) {
            Logger.error("There was an error announcing the game mode change for ${game.gameId}: " + e.message)
        }
    }

    fun notifyWarning(
        game: Game,
        instance: Int,
    ): Boolean {
        val discordBotUrl = "$discordBotUrl/delay_of_game_warning?instance=$instance"
        val requestEntity = HttpEntity(game, botRequestHeaders())
        return try {
            restTemplate.postForEntity(discordBotUrl, requestEntity, String::class.java)
            true
        } catch (e: Exception) {
            Logger.error("There was an error notifying the delay of game  warning for ${game.gameId}: " + e.message)
            false
        }
    }

    fun sendRegistrationNotice(signupInfo: SignupInfo) {
        val discordBotUrl = "$discordBotUrl/new_signup"
        val requestEntity = HttpEntity(signupInfo, botRequestHeaders())
        try {
            restTemplate.postForEntity(discordBotUrl, requestEntity, String::class.java)
        } catch (e: Exception) {
            Logger.error("There was an error notifying the commissioners of a new signup: " + e.message)
        }
    }

    fun notifyFinalizationRollback(game: Game) {
        val request = GameFinalizationRollbackRequest(game.gameId, game.homeTeam, game.awayTeam)
        try {
            restTemplate.postForEntity(
                "$discordBotUrl/game_finalization_rollback",
                HttpEntity(request, botRequestHeaders()),
                String::class.java,
            )
        } catch (e: Exception) {
            Logger.error("Unable to report finalization rollback for game ${game.gameId}", e)
        }
    }

    private fun botRequestHeaders(): HttpHeaders =
        HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("X-Service-Key", botServiceKey)
        }

    suspend fun getUserByDiscordTag(tag: String): User {
        try {
            val client = Kord(botToken)
            val guild = client.getGuild(Snowflake(guildId))
            val members = guild.members.toList()
            for (member in members) {
                if (member.username == tag) {
                    val user = client.getUser(Snowflake(member.id.value)) ?: throw DiscordUserNotFoundException()
                    return user
                }
            }
            throw DiscordUserNotFoundException()
        } catch (e: Exception) {
            Logger.error("{}", e)
            throw DiscordUserNotFoundException()
        }
    }
}
