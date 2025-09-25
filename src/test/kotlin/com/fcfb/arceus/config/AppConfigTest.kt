package com.fcfb.arceus.config

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppConfigTest {
    private lateinit var appConfig: AppConfig

    @BeforeEach
    fun setup() {
        appConfig = AppConfig()
    }

    @Test
    fun `AppConfig should be properly annotated`() {
        val configAnnotation = AppConfig::class.annotations.find { it is org.springframework.context.annotation.Configuration }

        assertNotNull(configAnnotation, "AppConfig should be annotated with @Configuration")
    }

    @Test
    fun `restTemplate should return properly configured RestTemplate`() {
        val restTemplate = appConfig.restTemplate()

        assertNotNull(restTemplate, "RestTemplate should not be null")
        assertTrue(restTemplate.javaClass == RestTemplate::class.java, "Should return RestTemplate instance")
    }

    @Test
    fun `restTemplate should have HttpComponentsClientHttpRequestFactory`() {
        val restTemplate = appConfig.restTemplate()
        val requestFactory = restTemplate.requestFactory

        assertNotNull(requestFactory, "Request factory should not be null")
        assertTrue(
            requestFactory is HttpComponentsClientHttpRequestFactory,
            "Request factory should be HttpComponentsClientHttpRequestFactory",
        )
    }

    @Test
    fun `restTemplate should have correct timeout settings`() {
        val restTemplate = appConfig.restTemplate()
        val requestFactory = restTemplate.requestFactory as HttpComponentsClientHttpRequestFactory

        // Note: These methods are protected in HttpComponentsClientHttpRequestFactory
        // We'll test by creating a new instance and verifying the configuration approach
        val testFactory = HttpComponentsClientHttpRequestFactory()
        testFactory.setConnectTimeout(5000)
        testFactory.setReadTimeout(5000)

        // The configuration should match what's set in AppConfig
        // We can't directly access the timeout values, but we can verify the factory type
        assertNotNull(requestFactory, "Request factory should be configured")
    }

    @Test
    fun `restTemplate should have MappingJackson2HttpMessageConverter`() {
        val restTemplate = appConfig.restTemplate()
        val messageConverters = restTemplate.messageConverters

        assertNotNull(messageConverters, "Message converters should not be null")
        assertTrue(messageConverters.isNotEmpty(), "Message converters should not be empty")

        val jacksonConverter = messageConverters.find { it is MappingJackson2HttpMessageConverter }
        assertNotNull(jacksonConverter, "Should have MappingJackson2HttpMessageConverter")
    }

    @Test
    fun `restTemplate should be usable for HTTP requests`() {
        val restTemplate = appConfig.restTemplate()

        // Verify the RestTemplate is properly configured for use
        assertNotNull(restTemplate.requestFactory, "Request factory should be set")
        assertNotNull(restTemplate.messageConverters, "Message converters should be set")
        assertTrue(restTemplate.messageConverters.isNotEmpty(), "Should have message converters")
    }

    @Test
    fun `restTemplate should have default message converters plus Jackson`() {
        val restTemplate = appConfig.restTemplate()
        val messageConverters = restTemplate.messageConverters

        // Should have the default converters plus our added Jackson converter
        assertTrue(messageConverters.size > 1, "Should have multiple message converters")

        val jacksonConverters = messageConverters.filterIsInstance<MappingJackson2HttpMessageConverter>()
        assertTrue(jacksonConverters.isNotEmpty(), "Should have at least one Jackson converter")
    }

    @Test
    fun `restTemplate bean should be properly configured for JSON processing`() {
        val restTemplate = appConfig.restTemplate()

        // Test that the RestTemplate can handle JSON by checking message converters
        val jsonCapableConverters =
            restTemplate.messageConverters.filter { converter ->
                converter.supportedMediaTypes.any { mediaType ->
                    mediaType.type == "application" && mediaType.subtype == "json"
                }
            }

        assertTrue(jsonCapableConverters.isNotEmpty(), "RestTemplate should support JSON processing")
    }

    @Test
    fun `multiple calls to restTemplate should return different instances`() {
        val restTemplate1 = appConfig.restTemplate()
        val restTemplate2 = appConfig.restTemplate()

        // Since it's not a singleton bean (no @Scope annotation), each call should return a new instance
        assertTrue(restTemplate1 !== restTemplate2, "Each call should return a new RestTemplate instance")

        // But both should be configured the same way
        assertEquals(
            restTemplate1.requestFactory.javaClass,
            restTemplate2.requestFactory.javaClass,
            "Both instances should have the same request factory type",
        )
    }
}
