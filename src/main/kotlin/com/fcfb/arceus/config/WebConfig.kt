package com.fcfb.arceus.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableWebSecurity
open class WebConfig : WebSecurityConfigurerAdapter() {
    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http
            .csrf().disable()
            .authorizeRequests()
            .antMatchers(HttpMethod.OPTIONS).permitAll() // Allow pre-flight requests
            .antMatchers("/**").permitAll() // Allow all paths for now
            .anyRequest().authenticated()
            .and()
            .sessionManagement()
            .invalidSessionUrl("/invalidSession")
            .maximumSessions(1)
            .maxSessionsPreventsLogin(false)
            .expiredUrl("/sessionExpired")
    }
}

@Configuration
open class MvcConfig(
    @Value("\${images.path:./images}") private val imagesPath: String,
) : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/images/**")
            .addResourceLocations("file:$imagesPath/")
    }
}
