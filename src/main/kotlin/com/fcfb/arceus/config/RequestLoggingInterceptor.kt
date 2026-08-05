package com.fcfb.arceus.config

import com.fcfb.arceus.util.Logger
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private const val START_TIME_ATTR = "com.fcfb.arceus.requestStartTime"
private const val ANONYMOUS_PRINCIPAL = "anonymousUser"
private val EXCLUDED_ROLES = setOf("ROLE_SERVICE", "ROLE_WEBSITE")
private val SENSITIVE_PARAM_KEYS = listOf("token", "code", "secret", "password", "key", "number")

@Component
class RequestLoggingInterceptor : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis())
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        if (handler !is HandlerMethod) return

        val auth = SecurityContextHolder.getContext().authentication
        val role = auth?.authorities?.firstOrNull()?.authority ?: "-"
        if (role in EXCLUDED_ROLES) return

        val start = request.getAttribute(START_TIME_ATTR) as? Long
        val durationMs = if (start != null) System.currentTimeMillis() - start else -1

        val principal = auth?.name?.takeIf { it != ANONYMOUS_PRINCIPAL } ?: "anonymous"

        val query = request.queryString?.let { "?" + redactQuery(it) } ?: ""
        val endpoint = "${handler.beanType.simpleName}.${handler.method.name}"

        Logger.info(
            "API call: {} {}{} -> {} ({}ms) by user={} role={} endpoint={}",
            request.method,
            request.requestURI,
            query,
            response.status,
            durationMs,
            principal,
            role,
            endpoint,
        )
    }

    private fun redactQuery(queryString: String): String =
        queryString.split("&").joinToString("&") { pair ->
            val idx = pair.indexOf('=')
            if (idx < 0) return@joinToString pair
            val key = pair.substring(0, idx)
            if (SENSITIVE_PARAM_KEYS.any { key.lowercase().contains(it) }) "$key=***" else pair
        }
}
