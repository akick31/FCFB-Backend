package com.fcfb.arceus.service.fcfb

import com.fasterxml.jackson.databind.ObjectMapper
import com.fcfb.arceus.dto.response.UserDelayOfGameResponse
import com.fcfb.arceus.repositories.PlayRepository
import org.springframework.stereotype.Service

@Service
class DelayOfGameReportService(
    private val playRepository: PlayRepository,
    private val userService: UserService,
    private val objectMapper: ObjectMapper,
) {
    fun getUserDelayOfGameInstances(
        season: Int,
        week: Int?,
    ): List<UserDelayOfGameResponse> {
        val countsByDiscordId = mutableMapOf<String, Int>()
        val teamsByDiscordId = mutableMapOf<String, String?>()

        val rows =
            if (week == null) {
                playRepository.getDelayOfGameCoachCountsBySeason(season)
            } else {
                playRepository.getDelayOfGameCoachCountsByWeek(season, week)
            }

        rows.forEach { row ->
            val discordIds = parseDiscordIds(row[0])
            val team = row[1] as String?
            val count = (row[2] as Number).toInt()
            discordIds.forEach { discordId ->
                countsByDiscordId[discordId] = (countsByDiscordId[discordId] ?: 0) + count
                teamsByDiscordId.putIfAbsent(discordId, team)
            }
        }

        return countsByDiscordId.mapNotNull { (discordId, count) ->
            val user = userService.findUserByDiscordId(discordId) ?: return@mapNotNull null
            UserDelayOfGameResponse(
                username = user.username,
                discordTag = user.discordTag,
                discordId = discordId,
                team = user.team ?: teamsByDiscordId[discordId],
                delayOfGameInstances = count,
            )
        }.sortedByDescending { it.delayOfGameInstances }
    }

    private fun parseDiscordIds(value: Any?): List<String> {
        val json = value as? String ?: return emptyList()
        return objectMapper.readValue(json, Array<String>::class.java).toList()
    }
}
