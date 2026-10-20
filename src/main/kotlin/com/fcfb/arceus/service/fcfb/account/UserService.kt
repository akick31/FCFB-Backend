package com.fcfb.arceus.service.fcfb.account

import com.fcfb.arceus.dto.request.UpdateUserRequest
import com.fcfb.arceus.dto.request.UserValidationRequest
import com.fcfb.arceus.dto.response.ApiKeyResponse
import com.fcfb.arceus.dto.response.UserDTO
import com.fcfb.arceus.dto.response.UserValidationResponse
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.user.UserRole
import com.fcfb.arceus.exception.user.DiscordAlreadyLinkedException
import com.fcfb.arceus.exception.user.UserForbiddenException
import com.fcfb.arceus.exception.user.UserNotFoundException
import com.fcfb.arceus.exception.user.UserUnauthorizedException
import com.fcfb.arceus.model.account.User
import com.fcfb.arceus.model.account.UserCredential
import com.fcfb.arceus.model.account.UserPreferences
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.repositories.account.UserCredentialRepository
import com.fcfb.arceus.repositories.account.UserPreferencesRepository
import com.fcfb.arceus.repositories.account.UserRepository
import com.fcfb.arceus.service.fcfb.coach.CoachStintService
import com.fcfb.arceus.service.log.UsernameHistoryService
import com.fcfb.arceus.util.AuthContext
import com.fcfb.arceus.util.EncryptionUtils
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.Base64
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val encryptionUtils: EncryptionUtils,
    private val userDtoService: UserDtoService,
    private val passwordEncoder: PasswordEncoder,
    private val usernameHistoryService: UsernameHistoryService,
    private val coachStintService: CoachStintService,
    private val userCredentialRepository: UserCredentialRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    private fun syncCredential(user: User) {
        userCredentialRepository.save(
            UserCredential(
                userId = user.id,
                email = user.email,
                hashedEmail = user.hashedEmail,
                password = user.password,
                salt = user.salt,
                apiKeyHash = user.apiKeyHash,
                resetToken = user.resetToken,
                resetTokenExpiration = user.resetTokenExpiration,
            ),
        )
    }

    private fun syncPreferences(user: User) {
        userPreferencesRepository.save(
            UserPreferences(
                userId = user.id,
                delayOfGameWarningOptOut = user.delayOfGameWarningOptOut,
                scoutingReportHiddenColumns = user.scoutingReportHiddenColumns,
            ),
        )
    }

    fun updateUserWinsAndLosses(game: Game) {
        val gameType = game.gameType ?: GameType.SCRIMMAGE
        val timestamp = coachStintService.parseGameTimestamp(game.timestamp) ?: return
        creditCoaches(game.homeCoachDiscordIds, game.homeTeam, game.homeScore > game.awayScore, gameType, timestamp)
        creditCoaches(game.awayCoachDiscordIds, game.awayTeam, game.awayScore > game.homeScore, gameType, timestamp)
    }

    private fun creditCoaches(
        coachDiscordIds: List<String>?,
        team: String,
        isWin: Boolean,
        gameType: GameType,
        timestamp: LocalDateTime,
    ) {
        for (discordId in coachDiscordIds ?: emptyList()) {
            val user = userRepository.getByDiscordId(discordId) ?: continue
            val stints = coachStintService.getStintsForCoach(user.username, user)
            if (!coachStintService.countsForCoach(stints, team, timestamp)) continue
            updateUserRecord(userDtoService.convertToUserDTO(user), gameType, isWin)
        }
    }

    fun updateUserAverageResponseTime(
        userId: Long,
        responseTime: Double,
    ) = userRepository.updateAverageResponseTime(userId, responseTime)

    fun resetAllDelayOfGameInstances() = userRepository.resetAllDelayOfGameInstances()

    private fun updateUserRecord(
        user: UserDTO,
        gameType: GameType,
        isWin: Boolean,
    ) {
        if (isWin) {
            user.wins += 1
            when (gameType) {
                GameType.CONFERENCE_GAME -> user.conferenceWins += 1
                GameType.CONFERENCE_CHAMPIONSHIP -> user.conferenceChampionshipWins += 1
                GameType.BOWL -> user.bowlWins += 1
                GameType.PLAYOFFS -> {
                    user.bowlWins += 1
                    user.playoffWins += 1
                }
                GameType.NATIONAL_CHAMPIONSHIP -> {
                    user.bowlWins += 1
                    user.playoffWins += 1
                    user.nationalChampionshipWins += 1
                }
                else -> {}
            }
        } else {
            user.losses += 1
            when (gameType) {
                GameType.CONFERENCE_GAME -> user.conferenceLosses += 1
                GameType.CONFERENCE_CHAMPIONSHIP -> user.conferenceChampionshipLosses += 1
                GameType.BOWL -> user.bowlLosses += 1
                GameType.PLAYOFFS -> {
                    user.bowlLosses += 1
                    user.playoffLosses += 1
                }
                GameType.NATIONAL_CHAMPIONSHIP -> {
                    user.bowlLosses += 1
                    user.playoffLosses += 1
                    user.nationalChampionshipLosses += 1
                }
                else -> {}
            }
        }
        updateUser(user)
    }

    fun getUserDTOById(id: Long) = userDtoService.convertToUserDTO(getUserById(id))

    fun getUserById(id: Long) = userRepository.getById(id) ?: throw UserNotFoundException("User not found with id $id")

    fun getUserDTOByDiscordId(discordId: String) =
        userDtoService.convertToUserDTO(
            userRepository.getByDiscordId(discordId)
                ?: throw UserNotFoundException("User not found with Discord ID $discordId"),
        )

    fun findUserByDiscordId(discordId: String): User? = userRepository.getByDiscordId(discordId)

    fun getUserByTeam(team: String) =
        userDtoService.convertToUserDTO(
            userRepository.getByTeam(team)
                ?: throw UserNotFoundException("User not found with team $team"),
        )

    fun getUsersByTeam(team: String): List<UserDTO> {
        val users =
            userRepository.getUsersByTeam(team).ifEmpty {
                throw UserNotFoundException("No users found coaching team $team")
            }
        return users.map { userDtoService.convertToUserDTO(it) }
    }

    fun getUserByUsernameOrEmail(usernameOrEmail: String) =
        try {
            getUserByEmail(usernameOrEmail)
        } catch (e: Exception) {
            getUserByUsername(usernameOrEmail)
        }

    private fun getUserByUsername(username: String) =
        userRepository.getByUsername(username)
            ?: throw UserNotFoundException("User not found with username $username")

    private fun getUserByEmail(email: String) =
        userRepository.getUserByEmail(encryptionUtils.hash(email))
            ?: throw UserNotFoundException("User not found with email $email")

    fun getAllUsers(): List<UserDTO> {
        val userData =
            userRepository.findAll().filterNotNull().ifEmpty {
                throw UserNotFoundException("No users found")
            }
        return userData.map { userDtoService.convertToUserDTO(it) }
    }

    fun getFreeAgents(): List<UserDTO> {
        val userData =
            userRepository.getFreeAgents().ifEmpty {
                throw UserNotFoundException("No free agents found")
            }
        return userData.map { userDtoService.convertToUserDTO(it) }
    }

    fun getUserByCoachName(name: String) =
        userRepository.getByCoachName(name)
            ?: throw UserNotFoundException("User not found with coach name $name")

    fun getUserDTOByName(name: String) = userDtoService.convertToUserDTO(getUserByCoachName(name))

    fun getUserDTOByUsernameOrCoachName(name: String): UserDTO {
        val user =
            userRepository.getByUsername(name) ?: userRepository.getByCoachName(name)
                ?: throw UserNotFoundException("User not found with username or coach name $name")
        return userDtoService.convertToUserDTO(user)
    }

    fun updateUserPassword(
        id: Long,
        newPassword: String,
    ): UserDTO {
        val user = getUserById(id)

        val encoder = BCryptPasswordEncoder()
        user.password = encoder.encode(newPassword)
        user.salt = encoder.encode(newPassword)
        user.resetToken = null
        user.resetTokenExpiration = null

        userRepository.save(user)
        syncCredential(user)
        return userDtoService.convertToUserDTO(user)
    }

    fun changePassword(
        id: Long,
        currentPassword: String?,
        newPassword: String,
    ): UserDTO {
        requireSelfOrAdmin(id)
        val isAdminOrCommissioner = AuthContext().isAdminOrCommissioner()
        val user = getUserById(id)
        if (!isAdminOrCommissioner) {
            if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.password)) {
                throw UserUnauthorizedException()
            }
        }
        return updateUserPassword(id, newPassword)
    }

    fun updateUsername(
        id: Long,
        username: String,
    ): UserDTO {
        requireSelfOrAdmin(id)
        val user = getUserById(id)
        if (user.username != username) {
            usernameHistoryService.recordUsernameChange(id, user.username)
        }
        user.apply {
            this.username = username
        }
        saveUser(user)
        return userDtoService.convertToUserDTO(user)
    }

    fun addHistoricalUsername(
        id: Long,
        username: String,
    ) {
        requireSelfOrAdmin(id)
        usernameHistoryService.recordUsernameChange(id, username)
    }

    fun getUsernameHistory(id: Long): List<String> = usernameHistoryService.getHistoricalUsernames(id)

    fun updateEmail(
        id: Long,
        email: String,
    ): UserDTO {
        requireSelfOrAdmin(id)
        val user = getUserById(id)
        user.apply {
            this.email = email
        }
        saveUser(user)
        syncCredential(user)
        return userDtoService.convertToUserDTO(user)
    }

    fun updateScoutingReportHiddenColumns(
        id: Long,
        hiddenColumns: List<String>,
    ): UserDTO {
        requireSelfOrAdmin(id)
        val user = getUserById(id)
        user.apply {
            this.scoutingReportHiddenColumns = hiddenColumns.toMutableList()
        }
        saveUser(user)
        syncPreferences(user)
        return userDtoService.convertToUserDTO(user)
    }

    fun updateDelayOfGameWarningOptOut(
        id: Long,
        optOut: Boolean,
    ): UserDTO {
        requireSelfOrAdmin(id)
        val user = getUserById(id)
        user.apply {
            this.delayOfGameWarningOptOut = optOut
        }
        saveUser(user)
        syncPreferences(user)
        return userDtoService.convertToUserDTO(user)
    }

    private fun requireSelfOrAdmin(id: Long) {
        if (AuthContext().currentUserId() != id && !AuthContext().isAdminOrCommissioner()) {
            throw UserForbiddenException()
        }
    }

    private fun requireCurrentUserId(): Long = AuthContext().currentUserId() ?: throw UserForbiddenException()

    fun updateResetToken(email: String): User {
        val user = getUserByEmail(email)
        val resetToken = UUID.randomUUID().toString()
        user.apply {
            this.resetToken = resetToken
            this.resetTokenExpiration = LocalDateTime.now().plusHours(1).toString()
        }
        saveUser(user)
        syncCredential(user)
        return user
    }

    fun validateUser(userValidationRequest: UserValidationRequest): UserValidationResponse {
        val discordIdExists = userRepository.existsByDiscordId(userValidationRequest.discordId)
        val discordTagExists = userRepository.existsByDiscordTag(userValidationRequest.discordTag)
        val usernameExists = userRepository.existsByUsername(userValidationRequest.username)
        val emailExists = userRepository.existsByEmail(userValidationRequest.email)

        return UserValidationResponse(
            discordIdExists,
            discordTagExists,
            usernameExists,
            emailExists,
        )
    }

    fun hashEmails() {
        val users = userRepository.findAll().filterNotNull()
        users.forEach {
            it.hashedEmail = encryptionUtils.hash(encryptionUtils.decrypt(it.email))
            userRepository.save(it)
            syncCredential(it)
        }
    }

    fun linkDiscord(
        userId: Long,
        discordId: String,
        discordTag: String,
    ): UserDTO {
        val existing = userRepository.getByDiscordId(discordId)
        if (existing != null && existing.id != userId) {
            throw DiscordAlreadyLinkedException()
        }

        val user = getUserById(userId)
        user.discordId = discordId
        user.discordTag = discordTag
        userRepository.save(user)
        return userDtoService.convertToUserDTO(user)
    }

    fun updateUserAsRequester(request: UpdateUserRequest): UserDTO {
        val user = request.toUserDTO()
        requireSelfOrAdmin(user.id)
        if (!AuthContext().isAdmin()) {
            val commissionerManageableRoles = setOf(UserRole.USER, UserRole.CONFERENCE_COMMISSIONER)
            val existingUser = getUserDTOById(user.id)
            if (existingUser.role !in commissionerManageableRoles || user.role !in commissionerManageableRoles) {
                throw UserForbiddenException()
            }
        }
        return updateUser(user)
    }

    fun updateUser(user: UserDTO): UserDTO {
        val existingUser = getUserDTOById(user.id)

        existingUser.apply {
            username = user.username
            coachName = user.coachName
            discordTag = user.discordTag
            discordId = user.discordId
            position = user.position
            role = user.role
            team = user.team
            wins = user.wins
            losses = user.losses
            winPercentage = if (user.wins + user.losses > 0) user.wins.toDouble() / (user.wins + user.losses) else 0.0
            conferenceWins = user.conferenceWins
            conferenceLosses = user.conferenceLosses
            conferenceChampionshipWins = user.conferenceChampionshipWins
            conferenceChampionshipLosses = user.conferenceChampionshipLosses
            bowlWins = user.bowlWins
            bowlLosses = user.bowlLosses
            playoffWins = user.playoffWins
            playoffLosses = user.playoffLosses
            nationalChampionshipWins = user.nationalChampionshipWins
            nationalChampionshipLosses = user.nationalChampionshipLosses
            offensivePlaybook = user.offensivePlaybook
            defensivePlaybook = user.defensivePlaybook
        }

        return saveUserDTOToUser(user.id, existingUser)
    }

    private fun saveUserDTOToUser(
        id: Long,
        user: UserDTO,
    ): UserDTO {
        val existingUser = getUserById(id)

        existingUser.apply {
            username = user.username
            coachName = user.coachName
            discordTag = user.discordTag
            discordId = user.discordId
            position = user.position
            role = user.role
            team = user.team
            wins = user.wins
            losses = user.losses
            winPercentage = user.winPercentage
            conferenceWins = user.conferenceWins
            conferenceLosses = user.conferenceLosses
            conferenceChampionshipWins = user.conferenceChampionshipWins
            conferenceChampionshipLosses = user.conferenceChampionshipLosses
            bowlWins = user.bowlWins
            bowlLosses = user.bowlLosses
            playoffWins = user.playoffWins
            playoffLosses = user.playoffLosses
            nationalChampionshipWins = user.nationalChampionshipWins
            nationalChampionshipLosses = user.nationalChampionshipLosses
            offensivePlaybook = user.offensivePlaybook
            defensivePlaybook = user.defensivePlaybook
        }

        userRepository.save(existingUser)
        return user
    }

    fun getUserByDiscordId(id: String) =
        userRepository.getByDiscordId(id) ?: throw UserNotFoundException("User not found with Discord ID $id")

    fun getUserByResetToken(token: String) = userRepository.getUserByResetToken(token)

    fun saveUser(user: User): User = userRepository.save(user)

    fun deleteUser(id: Long): HttpStatus {
        userRepository.getById(id)

        userRepository.deleteById(id)
        return HttpStatus.OK
    }

    fun generateApiKey(userId: Long): String {
        val user = getUserById(userId)
        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        val apiKey = "fcfb_pat_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
        user.apiKeyHash = encryptionUtils.hash(apiKey)
        userRepository.save(user)
        syncCredential(user)
        return apiKey
    }

    fun generateApiKeyForUser(userId: Long): ApiKeyResponse = ApiKeyResponse(generateApiKey(userId))

    fun generateApiKeyForCurrentUser(): ApiKeyResponse = ApiKeyResponse(generateApiKey(requireCurrentUserId()))

    fun revokeApiKey(userId: Long) {
        val user = getUserById(userId)
        user.apiKeyHash = null
        userRepository.save(user)
        syncCredential(user)
    }

    fun revokeApiKeyForCurrentUser() = revokeApiKey(requireCurrentUserId())
}
