package com.fcfb.arceus.config

import com.fcfb.arceus.rendering.animation.assets.LogoLoader
import com.fcfb.arceus.util.hostedImagesUrl
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import javax.annotation.PostConstruct

@Configuration
open class LogoPathInitializer(
    @Value("\${images.path:./images}") private val imagesPath: String,
    @Value("\${domain.url}") private val domainUrl: String,
) {
    @PostConstruct
    fun applyImagesPath() {
        LogoLoader.imagesPath = imagesPath
        LogoLoader.hostedImagesUrl = hostedImagesUrl(domainUrl)
    }
}
