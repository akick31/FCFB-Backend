package com.fcfb.arceus.config

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
        factory.setReadTimeout(5000)
        val restTemplate = RestTemplate()
        restTemplate.requestFactory = factory
        restTemplate.messageConverters.add(MappingJackson2HttpMessageConverter())
        return restTemplate
    }
}
