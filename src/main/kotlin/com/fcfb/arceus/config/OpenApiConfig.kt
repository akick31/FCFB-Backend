package com.fcfb.arceus.config

import com.fcfb.arceus.controllers.ApiConstants.FULL_PATH
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.util.AntPathMatcher

private val pathMatcher = AntPathMatcher()

private val restrictedPathsByMethod =
    mapOf(
        HttpMethod.GET to (ADMIN_ONLY_GET_PATHS + PRIVILEGED_GAME_STATUS_GET_PATHS),
        HttpMethod.POST to (ADMIN_ONLY_POST_PATHS + PRIVILEGED_POST_PATHS),
        HttpMethod.PUT to (ADMIN_ONLY_PUT_PATHS + PRIVILEGED_PUT_PATHS),
        HttpMethod.DELETE to (ADMIN_ONLY_DELETE_PATHS + PRIVILEGED_DELETE_PATHS),
    )

private fun isRestricted(
    method: PathItem.HttpMethod,
    path: String,
): Boolean {
    val patterns = restrictedPathsByMethod[HttpMethod.valueOf(method.name)] ?: return false
    return patterns.any { pathMatcher.match(it, path) }
}

private fun filterOperations(
    openApi: OpenAPI,
    keepRestricted: Boolean,
) {
    val paths = openApi.paths ?: return
    val emptyPaths = mutableListOf<String>()
    for ((path, pathItem) in paths) {
        pathItem.readOperationsMap().keys.forEach { method ->
            if (isRestricted(method, path) != keepRestricted) {
                pathItem.operation(method, null)
            }
        }
        if (pathItem.readOperations().isEmpty()) {
            emptyPaths.add(path)
        }
    }
    emptyPaths.forEach { paths.remove(it) }
}

@Configuration
open class OpenApiConfig {
    @Bean
    open fun fcfbOpenApi(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("FCFB API")
                .version("v1")
                .description("Public and admin API for Fake College Football."),
        )

    @Bean
    open fun publicApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("public")
            .pathsToMatch("$FULL_PATH/**")
            .addOpenApiCustomiser { openApi -> filterOperations(openApi, keepRestricted = false) }
            .build()

    @Bean
    open fun adminApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("admin")
            .pathsToMatch("$FULL_PATH/**")
            .addOpenApiCustomiser { openApi -> filterOperations(openApi, keepRestricted = true) }
            .build()
}
