package com.fcfb.arceus.util

import org.springframework.security.core.context.SecurityContextHolder

object AuthContext {
    fun currentUserId(): Long? = SecurityContextHolder.getContext().authentication?.name?.toLongOrNull()

    fun isAdmin(): Boolean {
        val authorities = SecurityContextHolder.getContext().authentication?.authorities ?: return false
        return authorities.any { it.authority == "ROLE_ADMIN" || it.authority == "ROLE_CONFERENCE_COMMISSIONER" }
    }
}
