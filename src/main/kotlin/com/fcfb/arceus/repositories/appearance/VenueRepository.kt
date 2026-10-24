package com.fcfb.arceus.repositories.appearance

import com.fcfb.arceus.model.appearance.Venue
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface VenueRepository : CrudRepository<Venue, String> {
    fun findAllByOrderByNameAsc(): List<Venue>

    fun findAllByActiveTrueOrderByNameAsc(): List<Venue>

    @Modifying
    @Query(value = "UPDATE venue SET name = ?2 WHERE name = ?1", nativeQuery = true)
    fun renameVenue(
        oldName: String,
        newName: String,
    )

    @Modifying
    @Query(value = "UPDATE team SET stadium = ?2 WHERE stadium = ?1", nativeQuery = true)
    fun renameTeamStadiums(
        oldName: String,
        newName: String,
    )

    @Modifying
    @Query(value = "UPDATE game SET venue = ?2 WHERE venue = ?1", nativeQuery = true)
    fun renameGameVenues(
        oldName: String,
        newName: String,
    )

    @Modifying
    @Query(value = "UPDATE game_presentation SET venue = ?2 WHERE venue = ?1", nativeQuery = true)
    fun renamePresentationVenues(
        oldName: String,
        newName: String,
    )

    @Modifying
    @Query(value = "UPDATE schedule SET venue = ?2 WHERE venue = ?1", nativeQuery = true)
    fun renameScheduleVenues(
        oldName: String,
        newName: String,
    )

    @Modifying
    @Query(value = "UPDATE conference SET championship_venue = ?2 WHERE championship_venue = ?1", nativeQuery = true)
    fun renameChampionshipVenues(
        oldName: String,
        newName: String,
    )

    @Modifying
    @Query(value = "UPDATE bowl SET last_venue = ?2 WHERE last_venue = ?1", nativeQuery = true)
    fun renameBowlLastVenues(
        oldName: String,
        newName: String,
    )

    @Modifying
    @Query(value = "UPDATE bowl_venue SET venue = ?2 WHERE venue = ?1", nativeQuery = true)
    fun renameBowlVenues(
        oldName: String,
        newName: String,
    )

    @Modifying
    @Query(value = "UPDATE bowl_field SET venue = ?2 WHERE venue = ?1", nativeQuery = true)
    fun renameBowlFieldVenues(
        oldName: String,
        newName: String,
    )

    @Modifying
    @Query(value = "UPDATE playoff_field SET venue = ?2 WHERE venue = ?1", nativeQuery = true)
    fun renamePlayoffFieldVenues(
        oldName: String,
        newName: String,
    )
}
