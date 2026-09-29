package com.fcfb.arceus.config

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate

@Configuration
open class AppConfig {
    @Bean
    open fun restTemplate(): RestTemplate {
        val factory = HttpComponentsClientHttpRequestFactory()
        factory.setConnectTimeout(5000)
        factory.setReadTimeout(20000)
        val restTemplate = RestTemplate()
        restTemplate.requestFactory = factory
        restTemplate.messageConverters.add(0, isoDateJsonConverter())
        return restTemplate
    }

    private fun isoDateJsonConverter(): MappingJackson2HttpMessageConverter {
        val objectMapper =
            jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        return MappingJackson2HttpMessageConverter(objectMapper)
    }
}
