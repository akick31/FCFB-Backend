package com.fcfb.arceus.repositories.play

import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.model.play.Ranges
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface RangesRepository : CrudRepository<Ranges, Int> {
    @Query(
        value =
            "SELECT * FROM new_ranges WHERE play_type = ? AND offensive_playbook = ? " +
                "AND defensive_playbook = ? AND ? BETWEEN lower_range AND upper_range;",
        nativeQuery = true,
    )
    fun getNormalResult(
        playType: String?,
        offensivePlaybook: String?,
        defensivePlaybook: String?,
        difference: String,
    ): Ranges?

    @Query(
        value = "SELECT * FROM new_ranges WHERE play_type = ? AND ? BETWEEN lower_range AND upper_range;",
        nativeQuery = true,
    )
    fun getNonNormalResult(
        playType: String?,
        difference: String,
    ): Ranges?

    @Query(
        value =
            "SELECT * FROM new_ranges WHERE play_type = ? AND ? BETWEEN ball_location_lower AND ball_location_upper " +
                "AND ? BETWEEN lower_range AND upper_range;",
        nativeQuery = true,
    )
    fun getPuntResult(
        playType: String?,
        ballLocation: String,
        difference: String,
    ): Ranges?

    @Query(
        value = "SELECT * FROM new_ranges WHERE play_type = ? AND distance = ? AND ? BETWEEN lower_range AND upper_range;",
        nativeQuery = true,
    )
    fun getFieldGoalResult(
        playType: String?,
        distance: String,
        difference: String,
    ): Ranges?

    @Query(
        """
        SELECT play_time FROM ranges 
        WHERE play_type = :playType 
        AND result REGEXP '^[0-9]+$' 
        ORDER BY ABS(CAST(result AS SIGNED) - :yards) 
        LIMIT 1
        """,
        nativeQuery = true,
    )
    fun getPlayTime(
        playType: String,
        yards: Int,
    ): Int?

    fun findByPlayType(playType: String): List<Ranges>

    fun findByOffensivePlaybook(offensivePlaybook: String): List<Ranges>

    fun findByDefensivePlaybook(defensivePlaybook: String): List<Ranges>

    fun findByLowerRange(lowerRange: Int): List<Ranges>

    fun findByUpperRange(upperRange: Int): List<Ranges>

    fun findByResult(result: Scenario): List<Ranges>

    fun findByPlayTime(playTime: Int): List<Ranges>
}
