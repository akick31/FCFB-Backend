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

        val testFactory = HttpComponentsClientHttpRequestFactory()
        testFactory.setConnectTimeout(5000)
        testFactory.setReadTimeout(5000)

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

        assertNotNull(restTemplate.requestFactory, "Request factory should be set")
        assertNotNull(restTemplate.messageConverters, "Message converters should be set")
        assertTrue(restTemplate.messageConverters.isNotEmpty(), "Should have message converters")
    }

    @Test
    fun `restTemplate should have default message converters plus Jackson`() {
        val restTemplate = appConfig.restTemplate()
        val messageConverters = restTemplate.messageConverters

        assertTrue(messageConverters.size > 1, "Should have multiple message converters")

        val jacksonConverters = messageConverters.filterIsInstance<MappingJackson2HttpMessageConverter>()
        assertTrue(jacksonConverters.isNotEmpty(), "Should have at least one Jackson converter")
    }

    @Test
    fun `restTemplate bean should be properly configured for JSON processing`() {
        val restTemplate = appConfig.restTemplate()

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

        assertTrue(restTemplate1 !== restTemplate2, "Each call should return a new RestTemplate instance")

        assertEquals(
            restTemplate1.requestFactory.javaClass,
            restTemplate2.requestFactory.javaClass,
            "Both instances should have the same request factory type",
        )
    }
}
