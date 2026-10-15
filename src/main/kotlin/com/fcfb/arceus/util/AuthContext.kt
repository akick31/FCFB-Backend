package com.fcfb.arceus.util

import com.fcfb.arceus.exception.user.UserForbiddenException
import org.springframework.security.core.context.SecurityContextHolder

class AuthContext {
    fun currentUserId(): Long? = SecurityContextHolder.getContext().authentication?.name?.toLongOrNull()

    fun currentPrincipal(): String = SecurityContextHolder.getContext().authentication?.name ?: "unauthenticated"

    fun isAdminOrCommissioner(): Boolean {
        val authorities = SecurityContextHolder.getContext().authentication?.authorities ?: return false
        return authorities.any { it.authority == "ROLE_ADMIN" || it.authority == "ROLE_CONFERENCE_COMMISSIONER" }
    }

    fun requireAdminOrCommissioner() {
        if (!isAdminOrCommissioner()) throw UserForbiddenException()
    }

    fun isAdmin(): Boolean {
        val authorities = SecurityContextHolder.getContext().authentication?.authorities ?: return false
        return authorities.any { it.authority == "ROLE_ADMIN" }
    }
}
