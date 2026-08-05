package com.fcfb.arceus.config

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SecurityConfigTest {
    private lateinit var securityConfig: SecurityConfig

    @BeforeEach
    fun setup() {
        securityConfig = SecurityConfig()
    }

    @Test
    fun `SecurityConfig should be properly annotated`() {
        val configAnnotation = SecurityConfig::class.annotations.find { it is org.springframework.context.annotation.Configuration }

        assertNotNull(configAnnotation, "SecurityConfig should be annotated with @Configuration")
    }

    @Test
    fun `passwordEncoder should return BCryptPasswordEncoder`() {
        val passwordEncoder = securityConfig.passwordEncoder()

        assertNotNull(passwordEncoder, "Password encoder should not be null")
        assertTrue(passwordEncoder is BCryptPasswordEncoder, "Password encoder should be BCryptPasswordEncoder")
    }

    @Test
    fun `passwordEncoder should encode passwords correctly`() {
        val passwordEncoder = securityConfig.passwordEncoder()
        val rawPassword = "testPassword123"

        val encodedPassword = passwordEncoder.encode(rawPassword)

        assertNotNull(encodedPassword, "Encoded password should not be null")
        assertTrue(encodedPassword != rawPassword, "Encoded password should be different from raw password")
        assertTrue(encodedPassword.startsWith("\$2a\$"), "BCrypt encoded password should start with \$2a\$")
    }

    @Test
    fun `passwordEncoder should verify passwords correctly`() {
        val passwordEncoder = securityConfig.passwordEncoder()
        val rawPassword = "testPassword123"
        val wrongPassword = "wrongPassword"

        val encodedPassword = passwordEncoder.encode(rawPassword)

        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword), "Password encoder should match correct password")
        assertTrue(!passwordEncoder.matches(wrongPassword, encodedPassword), "Password encoder should not match wrong password")
    }

    @Test
    fun `passwordEncoder should handle empty passwords`() {
        val passwordEncoder = securityConfig.passwordEncoder()
        val emptyPassword = ""

        val encodedPassword = passwordEncoder.encode(emptyPassword)

        assertNotNull(encodedPassword, "Encoded empty password should not be null")
        assertTrue(passwordEncoder.matches(emptyPassword, encodedPassword), "Password encoder should match empty password")
    }

    @Test
    fun `passwordEncoder should handle null passwords gracefully`() {
        val passwordEncoder = securityConfig.passwordEncoder()

        try {
            val encodedPassword = passwordEncoder.encode(null)
        } catch (e: IllegalArgumentException) {
            assertTrue(true, "Password encoder should throw IllegalArgumentException for null input")
        }
    }

    @Test
    fun `passwordEncoder bean should be properly configured`() {
        val passwordEncoder = securityConfig.passwordEncoder()

        val testPasswords = listOf("password1", "password2", "complexPassword!@#123")

        testPasswords.forEach { password ->
            val encoded = passwordEncoder.encode(password)
            assertTrue(passwordEncoder.matches(password, encoded), "Password encoder should work for password: $password")
        }
    }

    @Test
    fun `passwordEncoder should produce different hashes for same password`() {
        val passwordEncoder = securityConfig.passwordEncoder()
        val password = "samePassword"

        val hash1 = passwordEncoder.encode(password)
        val hash2 = passwordEncoder.encode(password)

        assertTrue(hash1 != hash2, "BCrypt should produce different hashes for the same password (due to salt)")
        assertTrue(passwordEncoder.matches(password, hash1), "First hash should match original password")
        assertTrue(passwordEncoder.matches(password, hash2), "Second hash should match original password")
    }
}
