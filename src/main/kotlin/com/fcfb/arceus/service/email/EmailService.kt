package com.fcfb.arceus.service.email

import com.fcfb.arceus.util.EncryptionUtils
import com.fcfb.arceus.util.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val encryptionUtils: EncryptionUtils,
    private val mailSender: JavaMailSender,
    @Value("\${website.url}")
    private val websiteUrl: String,
    @Value("\${spring.mail.username}")
    private val fromEmail: String,
) {
    /**
     * Send a verification email
     */
    fun sendVerificationEmail(
        email: String,
        userId: Long,
        verificationToken: String,
    ) {
        val subject = "Welcome to Fake College Football! Please verify your email and join Discord."
        val emailBody =
            """
            Dear User,
            
            Thank you for registering with Fake College Football! To complete your registration and gain full access to the game, please verify your email address by clicking on the following link:
            
            $websiteUrl/verify?id=$userId&token=$verificationToken
            
            After verifying your email, we invite you to join our Discord community to get your team and play the game. You can join our Discord server using the following invite link:
            
            discord.gg/fcfb
            
            We're excited to have you join our community! If you have any questions or need assistance, feel free to reach out to our support team on Discord.
            
            Best regards,
            The Fake College Football Team
            """.trimIndent()

        sendEmail(email, subject, emailBody)
    }

    fun sendPasswordResetEmail(
        email: String,
        userId: Long,
        resetToken: String,
    ) {
        val subject = "Reset Your FCFB Password"
        val emailBody =
            """
            Dear User,
            
            You have requested to reset your Fake College Football password. To reset your password, please click on the following link:
            
            $websiteUrl/reset-password?userId=$userId&token=$resetToken
            
            If you did not request to reset your password, please ignore this email. This link will expire in 1 hour.
            
            Best regards,
            The Fake College Football Team
            """.trimIndent()

        sendEmail(email, subject, emailBody)
    }

    /**
     * Send an email
     */
    private fun sendEmail(
        to: String,
        subject: String,
        text: String,
    ) {
        try {
            val message = SimpleMailMessage()
            message.setFrom(fromEmail)
            message.setTo(encryptionUtils.decrypt(to))
            message.setSubject(subject)
            message.setText(text)
            mailSender.send(message)
            Logger.debug("Email sent to $to")
        } catch (e: Exception) {
            Logger.error("{}", e)
        }
    }
}
