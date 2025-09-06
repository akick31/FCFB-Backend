package com.fcfb.arceus.service.specification

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.service.fcfb.TeamService
import com.fcfb.arceus.service.specification.GameSpecificationService.GameCategory.ONGOING
import com.fcfb.arceus.service.specification.GameSpecificationService.GameCategory.PAST
import com.fcfb.arceus.service.specification.GameSpecificationService.GameCategory.PAST_SCRIMMAGE
import com.fcfb.arceus.service.specification.GameSpecificationService.GameCategory.SCRIMMAGE
import com.fcfb.arceus.service.specification.GameSpecificationService.GameFilter.BOWL
import com.fcfb.arceus.service.specification.GameSpecificationService.GameFilter.CONFERENCE_CHAMPIONSHIP
import com.fcfb.arceus.service.specification.GameSpecificationService.GameFilter.CONFERENCE_GAME
import com.fcfb.arceus.service.specification.GameSpecificationService.GameFilter.IN_PROGRESS
import com.fcfb.arceus.service.specification.GameSpecificationService.GameFilter.NATIONAL_CHAMPIONSHIP
import com.fcfb.arceus.service.specification.GameSpecificationService.GameFilter.OPENING_KICKOFF
import com.fcfb.arceus.service.specification.GameSpecificationService.GameFilter.OUT_OF_CONFERENCE
import com.fcfb.arceus.service.specification.GameSpecificationService.GameFilter.OVERTIME
import com.fcfb.arceus.service.specification.GameSpecificationService.GameFilter.PLAYOFFS
import com.fcfb.arceus.service.specification.GameSpecificationService.GameFilter.PREGAME
import com.fcfb.arceus.service.specification.GameSpecificationService.GameFilter.RANKED_GAME
import com.fcfb.arceus.service.specification.GameSpecificationService.GameSort.CLOSEST_TO_END
import com.fcfb.arceus.service.specification.GameSpecificationService.GameSort.MOST_TIME_REMAINING
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
    enum class GameFilter {
        RANKED_GAME,
        CONFERENCE_GAME,
        OUT_OF_CONFERENCE,
        CONFERENCE_CHAMPIONSHIP,
        BOWL,
        PLAYOFFS,
        NATIONAL_CHAMPIONSHIP,
        PREGAME,
        OPENING_KICKOFF,
        IN_PROGRESS,
        OVERTIME,
    }

    enum class GameCategory {
        ONGOING,
        PAST,
        SCRIMMAGE,
        PAST_SCRIMMAGE,
    }

    enum class GameSort {
        CLOSEST_TO_END,
        MOST_TIME_REMAINING,
    }

    /**
     * Create the spec for a game
     * @param filters
     * @param conference
     * @param season
     * @param week
     */
    fun createSpecification(
        filters: List<GameFilter>,
        category: GameCategory?,
        conference: String?,
        season: Int?,
        week: Int?,
    ): Specification<Game> {
        return Specification { root: Root<Game>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
            val predicates = mutableListOf<Predicate>()

            // Handle category filter
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

            // Handle other filters
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

            // Conference filter
            conference?.let {
                val conferenceTeams = teamService.getTeamsInConference(it)?.map { team -> team.name }
                predicates.add(
                    cb.or(
                        root.get<String>("homeTeam").`in`(conferenceTeams),
                        root.get<String>("awayTeam").`in`(conferenceTeams),
                    ),
                )
            }

            // Season/week filters
            season?.let { predicates.add(cb.equal(root.get<Int>("season"), it)) }
            week?.let { predicates.add(cb.equal(root.get<Int>("week"), it)) }

            cb.and(*predicates.toTypedArray())
        }
    }

    /**
     * Sort games by how far they are from being done
     * @param sort
     */
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
        }
    }
}
