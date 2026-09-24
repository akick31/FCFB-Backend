package com.fcfb.arceus.config

import com.fcfb.arceus.service.fcfb.animation.LogoLoader
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import javax.annotation.PostConstruct

/** [LogoLoader] is an object on the render path rather than a bean, so the configured images directory is pushed to it at startup. */
@Configuration
open class LogoPathInitializer(
    @Value("\${images.path:./images}") private val imagesPath: String,
) {
    @PostConstruct
    fun applyImagesPath() {
        LogoLoader.imagesPath = imagesPath
    }
}
