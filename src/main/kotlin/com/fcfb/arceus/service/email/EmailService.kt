package com.fcfb.arceus.service.email

import com.fcfb.arceus.util.EncryptionUtils
import com.fcfb.arceus.util.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.util.Base64

@Service
class EmailService(
    private val encryptionUtils: EncryptionUtils,
    private val mailSender: JavaMailSender,
    @Value("\${website.url}")
    private val websiteUrl: String,
    @Value("\${spring.mail.username}")
    private val fromEmail: String,
) {
    private val logoDataUri: String by lazy {
        val bytes = ClassPathResource("images/email/fcfb-logo.png").inputStream.readBytes()
        "data:image/png;base64,${Base64.getEncoder().encodeToString(bytes)}"
    }

    /**
     * Send a verification email
     */
    fun sendVerificationEmail(
        email: String,
        userId: Long,
        verificationToken: String,
    ) {
        val subject = "Welcome to Fake College Football! Please verify your email and join Discord."
        val link = "$websiteUrl/verify?id=$userId&token=$verificationToken"
        val html = loadTemplate("verification.html", mapOf("link" to link, "logo" to logoDataUri))

        sendEmail(email, subject, html)
    }

    fun sendPasswordResetEmail(
        email: String,
        userId: Long,
        resetToken: String,
    ) {
        val subject = "Reset Your FCFB Password"
        val link = "$websiteUrl/reset-password?userId=$userId&token=$resetToken"
        val html = loadTemplate("password-reset.html", mapOf("link" to link, "logo" to logoDataUri))

        sendEmail(email, subject, html)
    }

    fun sendPasswordResetConfirmation(
        email: String,
        username: String,
    ) {
        val subject = "Your FCFB Password Has Been Reset"
        val html =
            loadTemplate(
                "password-reset-confirmation.html",
                mapOf("username" to username, "logo" to logoDataUri),
            )

        sendEmail(email, subject, html)
    }

    /**
     * Load an email HTML template and substitute {{placeholder}} values
     */
    private fun loadTemplate(
        name: String,
        placeholders: Map<String, String>,
    ): String {
        var text =
            ClassPathResource("templates/email/$name")
                .inputStream
                .bufferedReader()
                .readText()
        placeholders.forEach { (key, value) -> text = text.replace("{{$key}}", value) }
        return text
    }

    /**
     * Send an email
     */
    private fun sendEmail(
        to: String,
        subject: String,
        html: String,
    ) {
        try {
            val message = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, false, "UTF-8")
            helper.setFrom(fromEmail, "Fake College Football")
            helper.setTo(encryptionUtils.decrypt(to))
            helper.setSubject(subject)
            helper.setText(html, true)
            mailSender.send(message)
            Logger.debug("Email sent to $to")
        } catch (e: Exception) {
            Logger.error("{}", e)
        }
    }
}
