package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.domain.appearance.StadiumUsage
import com.fcfb.arceus.dto.request.EventStadiumRequest
import com.fcfb.arceus.dto.request.GameStadiumRequest
import com.fcfb.arceus.dto.request.StadiumCreateRequest
import com.fcfb.arceus.dto.request.StadiumDesignRequest
import com.fcfb.arceus.dto.request.StadiumUpdateRequest
import com.fcfb.arceus.dto.request.TeamStadiumRequest
import com.fcfb.arceus.dto.response.StadiumResponse
import com.fcfb.arceus.enums.animation.PostseasonCategory
import com.fcfb.arceus.exception.team.InvalidUniformException
import com.fcfb.arceus.model.appearance.StadiumDesign
import com.fcfb.arceus.model.appearance.Venue
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.repositories.appearance.BowlFieldRepository
import com.fcfb.arceus.repositories.appearance.PlayoffFieldRepository
import com.fcfb.arceus.repositories.appearance.VenueRepository
import com.fcfb.arceus.repositories.conference.ConferenceRepository
import com.fcfb.arceus.repositories.game.GameRepository
import com.fcfb.arceus.repositories.schedule.ScheduleRepository
import com.fcfb.arceus.repositories.team.TeamRepository
import com.fcfb.arceus.service.fcfb.conference.ConferenceService
import com.fcfb.arceus.service.fcfb.game.GameSplitTableSyncService
import com.fcfb.arceus.service.fcfb.team.TeamService
import com.fcfb.arceus.util.AuthContext
import com.fcfb.arceus.util.appearance.AppearanceThumbnailCache
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StadiumService(
    private val venueRepository: VenueRepository,
    private val teamRepository: TeamRepository,
    private val teamService: TeamService,
    private val bowlFieldRepository: BowlFieldRepository,
    private val playoffFieldRepository: PlayoffFieldRepository,
    private val conferenceRepository: ConferenceRepository,
    private val gameRepository: GameRepository,
    private val scheduleRepository: ScheduleRepository,
    private val gameSplitTableSyncService: GameSplitTableSyncService,
    private val teamEditPermissionService: TeamEditPermissionService,
    private val bowlFieldService: BowlFieldService,
    private val playoffFieldService: PlayoffFieldService,
    private val conferenceService: ConferenceService,
    private val thumbnailCache: AppearanceThumbnailCache,
) {
    fun getAll(includeInactive: Boolean): List<StadiumResponse> {
        val venues = if (includeInactive) venueRepository.findAllByOrderByNameAsc() else venueRepository.findAllByActiveTrueOrderByNameAsc()
        val usage = usage()
        return venues.map { usage.describe(it) }
    }

    fun get(name: String): StadiumResponse = usage().describe(venue(name))

    fun designOf(name: String?): StadiumDesign? =
        name?.trim()?.takeIf {
            it.isNotBlank()
        }?.let { venueRepository.findById(it).orElse(null)?.design }

    @Transactional
    fun create(request: StadiumCreateRequest): StadiumResponse {
        AuthContext().requireAdminOrCommissioner()
        val venue = newVenue(request.name)
        venue.city = request.city
        venue.state = request.state
        venue.capacity = request.capacity
        request.copyFrom?.let { venue(it).design.copyInto(venue.design) }
        venueRepository.save(venue)
        return get(venue.name)
    }

    @Transactional
    fun update(
        name: String,
        request: StadiumUpdateRequest,
    ): StadiumResponse {
        requireCanEdit(name)
        val target = request.name?.trim()?.takeIf { it.isNotBlank() && it != name }?.also { rename(name, it) } ?: name
        val venue = venue(target)
        request.city?.let { venue.city = it }
        request.state?.let { venue.state = it }
        request.capacity?.let { venue.capacity = it }
        request.design?.let { applyDesign(venue.design, it) }
        venueRepository.save(venue)
        evictThumbnails(target)
        return get(target)
    }

    @Transactional
    fun setActive(
        name: String,
        active: Boolean,
    ): StadiumResponse {
        AuthContext().requireAdminOrCommissioner()
        val venue = venue(name)
        venue.active = active
        venueRepository.save(venue)
        return get(name)
    }

    @Transactional
    fun assignTeam(request: TeamStadiumRequest): StadiumResponse {
        teamEditPermissionService.requireCanEdit(request.team)
        val name = request.venue.trim()
        if (request.buildNew) venueRepository.save(newVenue(name)) else venue(name)
        val team = teamService.getTeamByName(request.team)
        team.stadium = name
        teamRepository.save(team)
        thumbnailCache.evictTeam(request.team)
        return get(name)
    }

    @Transactional
    fun assignGame(request: GameStadiumRequest): StadiumResponse {
        AuthContext().requireAdminOrCommissioner()
        val name = venue(request.venue).name
        val game = gameRepository.getGameById(request.gameId) ?: throw InvalidUniformException("No game ${request.gameId}")
        game.venue = name
        gameRepository.save(game)
        gameSplitTableSyncService.sync(game)
        scheduleRepository.findByGameId(request.gameId)?.let {
            it.venue = name
            scheduleRepository.save(it)
        }
        return get(name)
    }

    @Transactional
    fun assignEvent(request: EventStadiumRequest): String? {
        AuthContext().requireAdminOrCommissioner()
        val name = request.venue?.trim()?.takeIf { it.isNotBlank() }?.let { venue(it).name }
        when (request.category) {
            PostseasonCategory.BOWL -> bowlFieldRepository.save(bowlFieldService.getField(request.key).also { it.venue = name })
            PostseasonCategory.PLAYOFF -> playoffFieldRepository.save(playoffFieldService.getField(request.key).also { it.venue = name })
            PostseasonCategory.CCG ->
                conferenceRepository.save(
                    conferenceService.requireExists(request.key).also { it.championshipVenue = name },
                )
        }
        return name
    }

    private fun usage(): StadiumUsage =
        StadiumUsage(
            teamRepository.findAll().toList(),
            bowlFieldRepository.findAll().toList(),
            conferenceRepository.findAll().toList(),
            playoffFieldRepository.findAll().toList(),
        )

    private fun evictThumbnails(venue: String) =
        teamRepository.findAllByStadium(
            venue,
        ).forEach { thumbnailCache.evictTeam(it.name.orEmpty()) }

    private fun requireCanEdit(name: String) = teamEditPermissionService.requireCoachOfAny(teamRepository.findAllByStadium(name))

    private fun venue(name: String): Venue =
        venueRepository.findById(name.trim()).orElseThrow { InvalidUniformException("No stadium named $name") }

    private fun newVenue(name: String): Venue {
        val trimmed = name.trim()
        if (trimmed.isBlank()) throw InvalidUniformException("A stadium needs a name")
        if (venueRepository.existsById(trimmed)) throw InvalidUniformException("A stadium named $trimmed already exists")
        return Venue(trimmed)
    }

    private fun rename(
        oldName: String,
        newName: String,
    ) {
        if (venueRepository.existsById(newName)) throw InvalidUniformException("A stadium named $newName already exists")
        venueRepository.renameVenue(oldName, newName)
        venueRepository.renameTeamStadiums(oldName, newName)
        venueRepository.renameGameVenues(oldName, newName)
        venueRepository.renamePresentationVenues(oldName, newName)
        venueRepository.renameScheduleVenues(oldName, newName)
        venueRepository.renameChampionshipVenues(oldName, newName)
        venueRepository.renameBowlLastVenues(oldName, newName)
        venueRepository.renameBowlVenues(oldName, newName)
        venueRepository.renameBowlFieldVenues(oldName, newName)
        venueRepository.renamePlayoffFieldVenues(oldName, newName)
    }

    fun draftDesign(
        venue: String?,
        request: StadiumDesignRequest?,
    ): StadiumDesign {
        val design = StadiumDesign()
        designOf(venue)?.copyInto(design)
        request?.let { applyDesign(design, it) }
        return design
    }

    fun teamsAt(venue: String): List<Team> = teamRepository.findAllByStadium(venue.trim())

    private fun applyDesign(
        design: StadiumDesign,
        request: StadiumDesignRequest,
    ) {
        request.stadiumBackdrop?.let { design.stadiumBackdrop = it }
        request.stadiumSize?.let { design.stadiumSize = it }
        design.rightStadiumBackdrop = request.rightStadiumBackdrop
        design.rightStadiumSize = request.rightStadiumSize
        request.jumbotronEnabled?.let { design.jumbotronEnabled = it }
        request.rightJumbotronEnabled?.let { design.rightJumbotronEnabled = it }
        request.jumbotronPylonMount?.let { design.jumbotronPylonMount = it }
        request.rightJumbotronPylonMount?.let { design.rightJumbotronPylonMount = it }
        request.jumbotronSize?.let { design.jumbotronSize = it }
        request.rightJumbotronSize?.let { design.rightJumbotronSize = it }
        request.wallEnabled?.let { design.wallEnabled = it }
        request.rightWallEnabled?.let { design.rightWallEnabled = it }
        request.jumbotronElevation?.let { design.jumbotronElevation = it }
        request.rightJumbotronElevation?.let { design.rightJumbotronElevation = it }
        request.skylineBackdrop?.let { design.skylineBackdrop = it }
        design.rightSkylineBackdrop = request.rightSkylineBackdrop
        request.jumbotronShape?.let { design.jumbotronShape = it }
        design.rightJumbotronShape = request.rightJumbotronShape
        request.jumbotronPylonsOutside?.let { design.jumbotronPylonsOutside = it }
        request.rightJumbotronPylonsOutside?.let { design.rightJumbotronPylonsOutside = it }
        request.jumbotronColor?.let { design.jumbotronColor = it }
        design.rightJumbotronColor = request.rightJumbotronColor
        request.topperColor?.let { design.topperColor = it }
        design.rightTopperColor = request.rightTopperColor
        request.facilityFullWidth?.let { design.facilityFullWidth = it }
        request.rightFacilityFullWidth?.let { design.rightFacilityFullWidth = it }
        request.jumbotronOverBuilding?.let { design.jumbotronOverBuilding = it }
        request.rightJumbotronOverBuilding?.let { design.rightJumbotronOverBuilding = it }
        request.scoreboardLogo?.let { design.scoreboardLogo = it }
        design.rightScoreboardLogo = request.rightScoreboardLogo
        request.jumbotronWidth?.let { design.jumbotronWidth = it }
        request.rightJumbotronWidth?.let { design.rightJumbotronWidth = it }
        request.jumbotronPylonStyle?.let { design.jumbotronPylonStyle = it }
        design.rightJumbotronPylonStyle = request.rightJumbotronPylonStyle
        request.backdropStyle?.let { design.backdropStyle = it }
        design.rightBackdropStyle = request.rightBackdropStyle
        request.jumbotronStyle?.let { design.jumbotronStyle = it }
        design.rightJumbotronStyle = request.rightJumbotronStyle
    }
}
