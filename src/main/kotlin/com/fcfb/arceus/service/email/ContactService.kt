package com.fcfb.arceus.service.email

import com.fcfb.arceus.dto.request.ContactRequest
import com.fcfb.arceus.util.ContactDeliveryException
import com.fcfb.arceus.util.ContactRateLimitedException
import com.fcfb.arceus.util.InMemoryRateLimiter
import com.fcfb.arceus.util.InvalidContactRequestException
import com.fcfb.arceus.util.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.MailException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import javax.servlet.http.HttpServletRequest

private const val MAX_MESSAGES_PER_WINDOW = 3
private const val WINDOW_SECONDS = 600L
private const val MAX_NAME_LENGTH = 100
private const val MAX_EMAIL_LENGTH = 200
private const val MAX_SUBJECT_LENGTH = 200
private const val MAX_MESSAGE_LENGTH = 5000
private const val CLIENT_IP_HEADER = "X-Real-IP"
private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

@Service
class ContactService(
    private val mailSender: JavaMailSender,
    private val rateLimiter: InMemoryRateLimiter,
    @Value("\${contact.email}")
    private val contactEmail: String,
    @Value("\${mail.from}")
    private val fromEmail: String,
) {
    fun sendContactMessage(
        request: ContactRequest,
        servletRequest: HttpServletRequest,
    ) {
        if (!request.company.isNullOrBlank()) {
            Logger.info("Dropped contact message that filled the honeypot field")
            return
        }
        val contact = normalize(request)
        validate(contact)
        enforceRateLimit(clientIp(servletRequest))
        deliver(contact)
    }

    private fun normalize(request: ContactRequest) =
        request.copy(
            name = request.name.trim().take(MAX_NAME_LENGTH),
            email = request.email.trim().take(MAX_EMAIL_LENGTH),
            subject = request.subject.trim().take(MAX_SUBJECT_LENGTH),
            message = request.message.trim().take(MAX_MESSAGE_LENGTH),
        )

    private fun validate(contact: ContactRequest) {
        if (contact.name.isBlank()) throw InvalidContactRequestException("Name is required")
        if (!EMAIL_REGEX.matches(contact.email)) throw InvalidContactRequestException("A valid email is required")
        if (contact.subject.isBlank()) throw InvalidContactRequestException("Subject is required")
        if (contact.message.isBlank()) throw InvalidContactRequestException("Message is required")
    }

    private fun clientIp(servletRequest: HttpServletRequest) =
        servletRequest.getHeader(CLIENT_IP_HEADER)?.takeIf { it.isNotBlank() } ?: servletRequest.remoteAddr

    private fun enforceRateLimit(clientIp: String) {
        if (!rateLimiter.allow("contact:$clientIp", MAX_MESSAGES_PER_WINDOW, WINDOW_SECONDS)) {
            throw ContactRateLimitedException()
        }
    }

    private fun deliver(contact: ContactRequest) {
        try {
            mailSender.send(buildMessage(contact))
            Logger.info("Contact message sent from ${contact.email}")
        } catch (e: MailException) {
            Logger.error("Failed to send contact message", e)
            throw ContactDeliveryException()
        }
    }

    private fun buildMessage(contact: ContactRequest) =
        SimpleMailMessage().apply {
            setTo(contactEmail)
            setFrom(fromEmail)
            replyTo = contact.email
            subject = "[FCFB] ${contact.subject}"
            text = "From: ${contact.name} <${contact.email}>\n\n${contact.message}"
        }
}
