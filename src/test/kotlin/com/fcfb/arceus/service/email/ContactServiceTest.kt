package com.fcfb.arceus.service.email

import com.fcfb.arceus.dto.request.ContactRequest
import com.fcfb.arceus.util.ContactDeliveryException
import com.fcfb.arceus.util.ContactRateLimitedException
import com.fcfb.arceus.util.InMemoryRateLimiter
import com.fcfb.arceus.util.InvalidContactRequestException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.mail.MailSendException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import javax.servlet.http.HttpServletRequest

private const val CONTACT_EMAIL = "contact@fakecollegefootball.com"

class ContactServiceTest {
    private val mailSender: JavaMailSender = mockk(relaxed = true)
    private lateinit var contactService: ContactService

    @BeforeEach
    fun setup() {
        contactService = ContactService(mailSender, InMemoryRateLimiter(), CONTACT_EMAIL)
    }

    @Test
    fun `should send to and from the contact address with reply-to set to the sender`() {
        val sent = slot<SimpleMailMessage>()
        every { mailSender.send(capture(sent)) } returns Unit

        contactService.sendContactMessage(validRequest(), requestFrom("1.1.1.1"))

        assertEquals(CONTACT_EMAIL, sent.captured.to?.single())
        assertEquals(CONTACT_EMAIL, sent.captured.from)
        assertEquals("coach@example.com", sent.captured.replyTo)
        assertEquals("[FCFB] Bug report", sent.captured.subject)
    }

    @Test
    fun `should silently drop messages that fill the honeypot`() {
        contactService.sendContactMessage(validRequest().copy(company = "Spam Inc"), requestFrom("1.1.1.1"))

        verify(exactly = 0) { mailSender.send(any<SimpleMailMessage>()) }
    }

    @Test
    fun `should reject an invalid email`() {
        assertThrows<InvalidContactRequestException> {
            contactService.sendContactMessage(validRequest().copy(email = "not-an-email"), requestFrom("1.1.1.1"))
        }
    }

    @Test
    fun `should rate limit per client ip after three messages`() {
        repeat(3) { contactService.sendContactMessage(validRequest(), requestFrom("2.2.2.2")) }

        assertThrows<ContactRateLimitedException> {
            contactService.sendContactMessage(validRequest(), requestFrom("2.2.2.2"))
        }
        contactService.sendContactMessage(validRequest(), requestFrom("3.3.3.3"))
    }

    @Test
    fun `should fall back to the remote address when no proxy header is present`() {
        repeat(3) { contactService.sendContactMessage(validRequest(), requestFrom(null, remoteAddr = "4.4.4.4")) }

        assertThrows<ContactRateLimitedException> {
            contactService.sendContactMessage(validRequest(), requestFrom(null, remoteAddr = "4.4.4.4"))
        }
    }

    @Test
    fun `should hide mail server failures behind a friendly error`() {
        every { mailSender.send(any<SimpleMailMessage>()) } throws MailSendException("535 auth failed")

        val error =
            assertThrows<ContactDeliveryException> {
                contactService.sendContactMessage(validRequest(), requestFrom("5.5.5.5"))
            }
        assertEquals("Your message couldn't be sent right now. Please try again later.", error.message)
    }

    private fun validRequest() =
        ContactRequest(
            name = "Coach",
            email = "coach@example.com",
            subject = "Bug report",
            message = "The scoreboard is stuck.",
        )

    private fun requestFrom(
        realIp: String?,
        remoteAddr: String = "10.0.0.1",
    ): HttpServletRequest =
        mockk {
            every { getHeader("X-Real-IP") } returns realIp
            every { this@mockk.remoteAddr } returns remoteAddr
        }
}
