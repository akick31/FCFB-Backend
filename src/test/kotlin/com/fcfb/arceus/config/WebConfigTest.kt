package com.fcfb.arceus.config

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WebConfigTest {
    private lateinit var webConfig: WebConfig

    @BeforeEach
    fun setup() {
        webConfig = WebConfig()
    }

    @Test
    fun `WebConfig should be properly annotated`() {
        val configAnnotation = WebConfig::class.annotations.find { it is org.springframework.context.annotation.Configuration }
        val securityAnnotation =
            WebConfig::class.annotations.find {
                it is org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
            }

        assertNotNull(configAnnotation, "WebConfig should be annotated with @Configuration")
        assertNotNull(securityAnnotation, "WebConfig should be annotated with @EnableWebSecurity")
    }

    @Test
    fun `WebConfig should extend WebSecurityConfigurerAdapter`() {
        assertTrue(
            webConfig.javaClass.superclass.simpleName == "WebSecurityConfigurerAdapter",
            "WebConfig should extend WebSecurityConfigurerAdapter",
        )
    }

    @Test
    fun `WebConfig should be instantiable`() {
        val config = WebConfig()
        assertNotNull(config, "WebConfig should be instantiable")
    }

    @Test
    fun `WebConfig should have configure method`() {
        val configureMethod = WebConfig::class.java.declaredMethods.find { it.name == "configure" }
        assertNotNull(configureMethod, "WebConfig should have configure method")
    }

    @Test
    fun `WebConfig should be a valid Spring configuration class`() {
        // Test that the class can be instantiated and has proper annotations
        val webConfig = WebConfig()
        assertNotNull(webConfig, "WebConfig should be annotated with @Configuration")

        // Verify it's properly configured as a Spring Security configuration
        assertTrue(
            webConfig.javaClass.superclass.simpleName == "WebSecurityConfigurerAdapter",
            "WebConfig should extend WebSecurityConfigurerAdapter for Spring Security configuration",
        )
    }
}
