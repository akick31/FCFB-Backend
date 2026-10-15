package com.fcfb.arceus.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.functions
import com.lemonappdev.konsist.api.ext.list.withAnnotationOf
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RestController

class ArchitectureTest {
    @Test
    fun `classes in service_fcfb are named Service and annotated as Spring beans`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .withPackage("com.fcfb.arceus.service.fcfb..")
            .assertTrue {
                it.hasNameEndingWith("Service") &&
                    (it.hasAnnotationOf(Service::class) || it.hasAnnotationOf(Component::class))
            }
    }

    @Test
    fun `domain package has no Spring framework references`() {
        Konsist
            .scopeFromProduction()
            .files
            .withPackage("com.fcfb.arceus.domain..")
            .assertFalse {
                it.imports.any { import -> import.name.startsWith("org.springframework") }
            }
    }

    @Test
    fun `RestController functions delegate to a service with an expression body`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .withAnnotationOf(RestController::class)
            .functions()
            .assertTrue { it.hasExpressionBody }
    }
}
