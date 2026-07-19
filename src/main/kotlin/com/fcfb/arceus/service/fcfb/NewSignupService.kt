package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.response.NewSignupDTO
import com.fcfb.arceus.enums.user.UserRole
import com.fcfb.arceus.model.NewSignup
import com.fcfb.arceus.model.User
import com.fcfb.arceus.repositories.NewSignupRepository
import com.fcfb.arceus.util.DTOConverter
import com.fcfb.arceus.util.EncryptionUtils
import com.fcfb.arceus.util.Logger
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class NewSignupService(
    private val dtoConverter: DTOConverter,
    private val encryptionUtils: EncryptionUtils,
    private val userService: UserService,
    private val newSignupRepository: NewSignupRepository,
) {
    fun createNewSignup(newSignup: NewSignup): NewSignup {
        val passwordEncoder = BCryptPasswordEncoder()
        val salt = passwordEncoder.encode(newSignup.password)
        val verificationToken = UUID.randomUUID().toString()

        val newSignup =
            NewSignup(
                newSignup.username,
                newSignup.coachName,
                newSignup.discordTag,
                newSignup.discordId,
                encryptionUtils.encrypt(newSignup.email),
                encryptionUtils.hash(newSignup.email),
                passwordEncoder.encode(newSignup.password),
                newSignup.position,
                salt,
                newSignup.teamChoiceOne,
                newSignup.teamChoiceTwo,
                newSignup.teamChoiceThree,
                newSignup.offensivePlaybook,
                newSignup.defensivePlaybook,
                false,
                verificationToken,
                LocalDateTime.now().plusHours(24),
            )

        saveNewSignup(newSignup)
        return newSignup
    }

    fun approveNewSignup(newSignup: NewSignup): Boolean {
        try {
            newSignup.apply {
                approved = true
            }
            saveNewSignup(newSignup)
            userService.saveUser(
                User(
                    username = newSignup.username,
                    coachName = newSignup.coachName,
                    discordTag = newSignup.discordTag,
                    discordId = newSignup.discordId,
                    email = newSignup.email,
                    hashedEmail = newSignup.hashedEmail,
                    password = newSignup.password,
                    position = newSignup.position,
                    role = UserRole.USER,
                    salt = newSignup.salt,
                    team = null,
                    delayOfGameInstances = 0,
                    wins = 0,
                    losses = 0,
                    winPercentage = 0.0,
                    conferenceWins = 0,
                    conferenceLosses = 0,
                    conferenceChampionshipWins = 0,
                    conferenceChampionshipLosses = 0,
                    bowlWins = 0,
                    bowlLosses = 0,
                    playoffWins = 0,
                    playoffLosses = 0,
                    nationalChampionshipWins = 0,
                    nationalChampionshipLosses = 0,
                    offensivePlaybook = newSignup.offensivePlaybook,
                    defensivePlaybook = newSignup.defensivePlaybook,
                    averageResponseTime = 0.0,
                    delayOfGameWarningOptOut = false,
                    resetToken = null,
                    resetTokenExpiration = null,
                ),
            )
            return true
        } catch (e: Exception) {
            Logger.error("Error approving new signup ${newSignup.id}: ${e.message}", e)
            return false
        }
    }

    fun getNewSignupById(id: Long) = newSignupRepository.getById(id)

    fun getNewSignupByDiscordId(discordId: String) = newSignupRepository.getByDiscordId(discordId)

    fun getByVerificationToken(token: String) = newSignupRepository.getByVerificationToken(token)

    fun getNewSignups(): List<NewSignupDTO> {
        val userData = newSignupRepository.getNewSignups()
        return userData.map { dtoConverter.convertToNewSignupDTO(it) }
    }

    fun saveNewSignup(newSignup: NewSignup) = newSignupRepository.save(newSignup)

    fun deleteNewSignup(newSignup: NewSignup) = newSignupRepository.delete(newSignup)
}
