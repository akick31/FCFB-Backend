package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.request.UserValidationRequest
import com.fcfb.arceus.dto.response.UserDTO
import com.fcfb.arceus.dto.response.UserValidationResponse
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.User
import com.fcfb.arceus.repositories.UserRepository
import com.fcfb.arceus.util.DTOConverter
import com.fcfb.arceus.util.EncryptionUtils
import com.fcfb.arceus.util.UserNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val encryptionUtils: EncryptionUtils,
    private val dtoConverter: DTOConverter,
) {
    fun updateUserWinsAndLosses(game: Game) {
        val homeUsers =
            try {
                getUsersByTeam(game.homeTeam)
            } catch (e: Exception) {
                emptyList()
            }
        val awayUsers =
            try {
                getUsersByTeam(game.awayTeam)
            } catch (e: Exception) {
                emptyList()
            }

        for (user in homeUsers + awayUsers) {
            val isHomeUser = user.team == game.homeTeam
            val isAwayUser = user.team == game.awayTeam
            if (!isHomeUser && !isAwayUser) {
                continue
            }
            val isWin = if (isHomeUser) game.homeScore > game.awayScore else game.awayScore > game.homeScore
            val gameType = game.gameType

            updateUserRecord(user, gameType ?: GameType.SCRIMMAGE, isWin)
        }
    }

    fun updateUserAverageResponseTime(
        userId: Long,
        responseTime: Double,
    ) = userRepository.updateAverageResponseTime(userId, responseTime)

    /**
     * Reset all users' delay of game instances to 0
     * Used when starting a new season
     */
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

    fun getUserDTOById(id: Long) = dtoConverter.convertToUserDTO(getUserById(id))

    fun getUserById(id: Long) = userRepository.getById(id) ?: throw UserNotFoundException("User not found with id $id")

    fun getUserDTOByDiscordId(discordId: String) =
        dtoConverter.convertToUserDTO(
            userRepository.getByDiscordId(discordId)
                ?: throw UserNotFoundException("User not found with Discord ID $discordId"),
        )

    fun getUserByTeam(team: String) =
        dtoConverter.convertToUserDTO(
            userRepository.getByTeam(team)
                ?: throw UserNotFoundException("User not found with team $team"),
        )

    fun getUsersByTeam(team: String): List<UserDTO> {
        val users =
            userRepository.getUsersByTeam(team).ifEmpty {
                throw UserNotFoundException("No users found coaching team $team")
            }
        return users.map { dtoConverter.convertToUserDTO(it) }
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
        return userData.map { dtoConverter.convertToUserDTO(it) }
    }

    fun getFreeAgents(): List<UserDTO> {
        val userData =
            userRepository.getFreeAgents().ifEmpty {
                throw UserNotFoundException("No free agents found")
            }
        return userData.map { dtoConverter.convertToUserDTO(it) }
    }

    fun getUserByCoachName(name: String) =
        userRepository.getByCoachName(name)
            ?: throw UserNotFoundException("User not found with coach name $name")

    fun getUserDTOByName(name: String) = dtoConverter.convertToUserDTO(getUserByCoachName(name))

    fun updateUserPassword(
        id: Long,
        newPassword: String,
    ): UserDTO {
        val user = getUserById(id)

        val passwordEncoder = BCryptPasswordEncoder()
        user.password = passwordEncoder.encode(newPassword)
        user.salt = passwordEncoder.encode(newPassword)
        user.resetToken = null
        user.resetTokenExpiration = null

        userRepository.save(user)
        return dtoConverter.convertToUserDTO(user)
    }

    fun updateEmail(
        id: Long,
        email: String,
    ): UserDTO {
        val user = getUserById(id)
        user.apply {
            this.email = email
        }
        saveUser(user)
        return dtoConverter.convertToUserDTO(user)
    }

    fun updateResetToken(email: String): User {
        val user = getUserByEmail(email)
        val resetToken = UUID.randomUUID().toString()
        user.apply {
            this.resetToken = resetToken
            this.resetTokenExpiration = LocalDateTime.now().plusHours(1).toString()
        }
        saveUser(user)
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
        }
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
}
