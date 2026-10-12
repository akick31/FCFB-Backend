package com.fcfb.arceus.service.specification

import com.fcfb.arceus.enums.game.GameCategory
import com.fcfb.arceus.enums.game.GameCategory.ONGOING
import com.fcfb.arceus.enums.game.GameCategory.PAST
import com.fcfb.arceus.enums.game.GameCategory.PAST_SCRIMMAGE
import com.fcfb.arceus.enums.game.GameCategory.SCRIMMAGE
import com.fcfb.arceus.enums.game.GameFilter
import com.fcfb.arceus.enums.game.GameFilter.BOWL
import com.fcfb.arceus.enums.game.GameFilter.CONFERENCE_CHAMPIONSHIP
import com.fcfb.arceus.enums.game.GameFilter.CONFERENCE_GAME
import com.fcfb.arceus.enums.game.GameFilter.IN_PROGRESS
import com.fcfb.arceus.enums.game.GameFilter.NATIONAL_CHAMPIONSHIP
import com.fcfb.arceus.enums.game.GameFilter.OPENING_KICKOFF
import com.fcfb.arceus.enums.game.GameFilter.OUT_OF_CONFERENCE
import com.fcfb.arceus.enums.game.GameFilter.OVERTIME
import com.fcfb.arceus.enums.game.GameFilter.PLAYOFFS
import com.fcfb.arceus.enums.game.GameFilter.PREGAME
import com.fcfb.arceus.enums.game.GameFilter.RANKED_GAME
import com.fcfb.arceus.enums.game.GameMode
import com.fcfb.arceus.enums.game.GameSort
import com.fcfb.arceus.enums.game.GameSort.CLOSEST_TO_END
import com.fcfb.arceus.enums.game.GameSort.MOST_TIME_REMAINING
import com.fcfb.arceus.enums.game.GameSort.NEWEST
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.service.fcfb.team.TeamService
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

@Service
class GameSpecificationService(
    private val teamService: TeamService,
    private val gameRepository: GameRepository,
) {
    fun createSpecification(
        filters: List<GameFilter>,
        category: GameCategory?,
        conference: String?,
        season: Int?,
        week: Int?,
        gameMode: GameMode?,
    ): Specification<Game> {
        return Specification { root: Root<Game>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
            val predicates = mutableListOf<Predicate>()

            category?.let {
                when (it) {
                    ONGOING -> {
                        predicates.add(cb.notEqual(root.get<GameStatus>("gameStatus"), GameStatus.FINAL))
                        predicates.add(cb.notEqual(root.get<GameType>("gameType"), GameType.SCRIMMAGE))
                    }
                    PAST -> {
                        predicates.add(cb.equal(root.get<GameStatus>("gameStatus"), GameStatus.FINAL))
                        predicates.add(cb.notEqual(root.get<GameType>("gameType"), GameType.SCRIMMAGE))
                    }
                    SCRIMMAGE -> {
                        predicates.add(cb.notEqual(root.get<GameStatus>("gameStatus"), GameStatus.FINAL))
                        predicates.add(cb.equal(root.get<GameType>("gameType"), GameType.SCRIMMAGE))
                    }
                    PAST_SCRIMMAGE -> {
                        predicates.add(cb.equal(root.get<GameStatus>("gameStatus"), GameStatus.FINAL))
                        predicates.add(cb.equal(root.get<GameType>("gameType"), GameType.SCRIMMAGE))
                    }
                }
            }

            filters.forEach { filter ->
                when (filter) {
                    RANKED_GAME -> {
                        val rankedGames = gameRepository.getRankedGames().map { game -> game.gameId }
                        if (rankedGames.isNotEmpty()) {
                            predicates.add(root.get<Int>("gameId").`in`(rankedGames))
                        }
                    }
                    CONFERENCE_GAME -> {
                        predicates.add(cb.equal(root.get<GameType>("gameType"), GameType.CONFERENCE_GAME))
                    }
                    OUT_OF_CONFERENCE -> {
                        predicates.add(cb.equal(root.get<GameType>("gameType"), GameType.OUT_OF_CONFERENCE))
                    }
                    CONFERENCE_CHAMPIONSHIP -> {
                        predicates.add(cb.equal(root.get<GameType>("gameType"), GameType.CONFERENCE_CHAMPIONSHIP))
                    }
                    BOWL -> {
                        predicates.add(cb.equal(root.get<GameType>("gameType"), GameType.BOWL))
                    }
                    PLAYOFFS -> {
                        predicates.add(cb.equal(root.get<GameType>("gameType"), GameType.PLAYOFFS))
                    }
                    NATIONAL_CHAMPIONSHIP -> {
                        predicates.add(cb.equal(root.get<GameType>("gameType"), GameType.NATIONAL_CHAMPIONSHIP))
                    }
                    PREGAME -> {
                        predicates.add(cb.equal(root.get<GameStatus>("gameStatus"), GameStatus.PREGAME))
                    }
                    OPENING_KICKOFF -> {
                        predicates.add(cb.equal(root.get<GameStatus>("gameStatus"), GameStatus.OPENING_KICKOFF))
                    }
                    IN_PROGRESS -> {
                        predicates.add(cb.equal(root.get<GameStatus>("gameStatus"), GameStatus.IN_PROGRESS))
                    }
                    OVERTIME -> {
                        predicates.add(cb.equal(root.get<GameStatus>("gameStatus"), GameStatus.OVERTIME))
                    }
                }
            }

            conference?.let {
                val conferenceTeams = teamService.getTeamsInConference(it)?.map { team -> team.name }
                predicates.add(
                    cb.or(
                        root.get<String>("homeTeam").`in`(conferenceTeams),
                        root.get<String>("awayTeam").`in`(conferenceTeams),
                    ),
                )
            }

            season?.let { predicates.add(cb.equal(root.get<Int>("season"), it)) }
            week?.let { predicates.add(cb.equal(root.get<Int>("week"), it)) }

            gameMode?.let { predicates.add(cb.equal(root.get<GameMode>("gameMode"), it)) }

            cb.and(*predicates.toTypedArray())
        }
    }

    fun createSort(sort: GameSort): List<org.springframework.data.domain.Sort.Order> {
        return when (sort) {
            CLOSEST_TO_END ->
                listOf(
                    org.springframework.data.domain.Sort.Order.desc("quarter"),
                    org.springframework.data.domain.Sort.Order.asc("clock"),
                )
            MOST_TIME_REMAINING ->
                listOf(
                    org.springframework.data.domain.Sort.Order.asc("quarter"),
                    org.springframework.data.domain.Sort.Order.desc("clock"),
                )
            NEWEST ->
                listOf(
                    org.springframework.data.domain.Sort.Order.desc("season"),
                    org.springframework.data.domain.Sort.Order.desc("week"),
                    org.springframework.data.domain.Sort.Order.desc("lastMessageTimestamp"),
                    org.springframework.data.domain.Sort.Order.desc("gameId"),
                )
        }
    }
}
