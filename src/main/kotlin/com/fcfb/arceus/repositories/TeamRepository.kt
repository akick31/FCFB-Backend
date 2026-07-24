package com.fcfb.arceus.repositories

import com.fcfb.arceus.enums.team.Conference
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.Team
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface TeamRepository : CrudRepository<Team, Int> {
    @Query(
        value = """
            SELECT * 
            FROM team 
            WHERE active = true
        """,
        nativeQuery = true,
    )
    fun getAllActiveTeams(): List<Team>

    @Query(
        value = """
            SELECT * 
            FROM team 
            WHERE conference = :conference
        """,
        nativeQuery = true,
    )
    fun getTeamsInConference(conference: String): List<Team>?

    @Query(
        value = """
            SELECT * 
            FROM team t 
            WHERE LOWER(t.name) = LOWER(:name)
        """,
        nativeQuery = true,
    )
    fun getTeamByName(name: String?): Team?

    @Query(
        value = """
            SELECT t.name 
            FROM team t 
            WHERE t.active = true 
            AND t.is_taken = false
        """,
        nativeQuery = true,
    )
    fun getOpenTeams(): List<String>?

    @Query(
        value = """
            SELECT CASE 
                WHEN COUNT(*) > 0 THEN 1 
                ELSE 0 
            END 
            FROM team 
            WHERE active = true 
            AND playoff_committee_ranking IS NOT NULL
        """,
        nativeQuery = true,
    )
    fun usePlayoffRanking(): Int

    @Query(
        value = """
            SELECT playoff_committee_ranking 
            FROM team 
            WHERE id = :id
        """,
        nativeQuery = true,
    )
    fun getPlayoffRankingById(id: Int): Int?

    @Query(
        value = """
            SELECT coaches_poll_ranking 
            FROM team 
            WHERE id = :id
        """,
        nativeQuery = true,
    )
    fun getCoachesPollRankingById(id: Int): Int?

    @Query(
        value = """
            SELECT * 
            FROM team
        """,
        nativeQuery = true,
    )
    fun getAllTeams(): Team?

    @Modifying
    @Transactional
    @Query(
        value = """
            UPDATE team 
            SET current_wins = 0, 
                current_losses = 0, 
                current_conference_wins = 0, 
                current_conference_losses = 0
        """,
        nativeQuery = true,
    )
    fun resetWinsAndLosses()

    fun findByName(name: String): Team?

    fun findByAbbreviation(abbreviation: String): Team?

    fun findByShortName(shortName: String): Team?

    fun findByActive(active: Boolean): List<Team>

    fun findByIsTaken(isTaken: Boolean): List<Team>

    fun findByConference(conference: Conference): List<Team>

    fun findBySubdivision(subdivision: Subdivision): List<Team>

    fun findByCurrentWins(currentWins: Int): List<Team>

    fun findByCurrentLosses(currentLosses: Int): List<Team>

    fun findByCurrentConferenceWins(currentConferenceWins: Int): List<Team>

    fun findByCurrentConferenceLosses(currentConferenceLosses: Int): List<Team>

    @Modifying
    @Transactional
    @Query(value = "UPDATE team SET coaches_poll_ranking = NULL", nativeQuery = true)
    fun clearCoachesPollRankings()

    @Modifying
    @Transactional
    @Query(value = "UPDATE team SET coaches_poll_ranking = :rank WHERE id = :id", nativeQuery = true)
    fun setCoachesPollRankingById(
        id: Int,
        rank: Int,
    )

    @Modifying
    @Transactional
    @Query(value = "UPDATE team SET playoff_committee_ranking = NULL", nativeQuery = true)
    fun clearPlayoffCommitteeRankings()

    @Modifying
    @Transactional
    @Query(value = "UPDATE team SET playoff_committee_ranking = :rank WHERE id = :id", nativeQuery = true)
    fun setPlayoffCommitteeRankingById(
        id: Int,
        rank: Int,
    )
}
