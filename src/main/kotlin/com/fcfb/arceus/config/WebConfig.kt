package com.fcfb.arceus.config

import com.fcfb.arceus.controllers.ApiConstants.FULL_PATH
import com.fcfb.arceus.filters.JwtAuthenticationFilter
import com.fcfb.arceus.service.auth.SessionService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

private val ADMIN_ROLES = arrayOf("ADMIN", "CONFERENCE_COMMISSIONER")
private val PRIVILEGED_ROLES = arrayOf("ADMIN", "CONFERENCE_COMMISSIONER", "SERVICE")
private const val BOT_SERVICE_KEY_PLACEHOLDER = "CHANGE_ME_GENERATE_A_REAL_SECRET"

private val PRE_AUTH_POST_PATHS =
    arrayOf(
        "$FULL_PATH/auth/register",
        "$FULL_PATH/auth/login",
        "$FULL_PATH/auth/logout",
        "$FULL_PATH/auth/verification-email/resend",
        "$FULL_PATH/auth/forgot-password",
        "$FULL_PATH/auth/reset-password",
        "$FULL_PATH/internal/frontend-errors",
        "$FULL_PATH/user/validate",
    )

private val PRE_AUTH_GET_PATHS =
    arrayOf(
        "$FULL_PATH/auth/verify-email",
        "$FULL_PATH/discord/redirect",
        "$FULL_PATH/health",
    )

private const val ACTUATOR_HEALTH_PATH = "/actuator/health"
private const val ACTUATOR_WILDCARD_PATH = "/actuator/**"

private val PRIVILEGED_GAME_STATUS_GET_PATHS =
    arrayOf(
        "$FULL_PATH/game/request-message",
        "$FULL_PATH/game/platform",
        "$FULL_PATH/game/week/status/*",
        "$FULL_PATH/game/week/jobs",
    )

private val PUBLIC_READ_GET_PATHS =
    arrayOf(
        "$FULL_PATH/chart/score",
        "$FULL_PATH/chart/score/matchup",
        "$FULL_PATH/chart/win-probability",
        "$FULL_PATH/chart/win-probability/matchup",
        "$FULL_PATH/chart/elo",
        "$FULL_PATH/coach_transaction_log",
        "$FULL_PATH/conference-stats",
        "$FULL_PATH/game-stats",
        "$FULL_PATH/game-stats/elo-history",
        "$FULL_PATH/game-stats/by-season-week",
        "$FULL_PATH/game_writeup",
        "$FULL_PATH/game",
        "$FULL_PATH/game/*",
        "$FULL_PATH/league-stats",
        "$FULL_PATH/playbook-stats",
        "$FULL_PATH/records",
        "$FULL_PATH/season-stats",
        "$FULL_PATH/season-stats/leaderboard",
    )

private val ADMIN_ONLY_GET_PATHS = arrayOf("$FULL_PATH/new_signups")

private val ADMIN_ONLY_POST_PATHS =
    arrayOf(
        "$FULL_PATH/conference-stats/generate/all",
        "$FULL_PATH/game-stats/generate",
        "$FULL_PATH/game-stats/generate/all/more_recent_than",
        "$FULL_PATH/game-stats/generate/all",
        "$FULL_PATH/league-stats/generate/all",
        "$FULL_PATH/playbook-stats/generate/all",
        "$FULL_PATH/records/generate/all",
        "$FULL_PATH/season-stats/generate/all",
        "$FULL_PATH/season-stats/generate/team-season",
        "$FULL_PATH/scorebug/generate/all",
        "$FULL_PATH/vegas-odds/update-spreads",
        "$FULL_PATH/win-probability/calculate",
        "$FULL_PATH/win-probability/calculate/all",
        "$FULL_PATH/team",
        "$FULL_PATH/upload/postseason-logo",
        "$FULL_PATH/user/hash_emails",
    )

private val ADMIN_ONLY_PUT_PATHS =
    arrayOf(
        "$FULL_PATH/team",
        "$FULL_PATH/user/update",
        "$FULL_PATH/play",
    )

private val ADMIN_ONLY_DELETE_PATHS =
    arrayOf(
        "$FULL_PATH/team/*",
        "$FULL_PATH/user/*",
    )

private val PRIVILEGED_POST_PATHS =
    arrayOf(
        "$FULL_PATH/play/submit_defense",
        "$FULL_PATH/game",
        "$FULL_PATH/game/overtime",
        "$FULL_PATH/game/week",
        "$FULL_PATH/game/week/retry/*",
        "$FULL_PATH/game/end",
        "$FULL_PATH/game/*/end",
        "$FULL_PATH/game/end-all",
        "$FULL_PATH/game/chew",
        "$FULL_PATH/game/*/chew",
        "$FULL_PATH/game/chew-all",
        "$FULL_PATH/game/restart",
        "$FULL_PATH/team/hire",
        "$FULL_PATH/team/hire/interim",
        "$FULL_PATH/team/fire",
        "$FULL_PATH/request_message_log",
        "$FULL_PATH/schedule",
        "$FULL_PATH/schedule/bulk",
        "$FULL_PATH/schedule/generate-conference",
        "$FULL_PATH/schedule/generate-all-conferences/*",
        "$FULL_PATH/schedule/conference-rules",
        "$FULL_PATH/season",
        "$FULL_PATH/season/*",
    )

private val PRIVILEGED_PUT_PATHS =
    arrayOf(
        "$FULL_PATH/play/submit_offense",
        "$FULL_PATH/play/rollback",
        "$FULL_PATH/game/*/coin-toss",
        "$FULL_PATH/game/*/coin-toss-choice",
        "$FULL_PATH/game/*/overtime-coin-toss-choice",
        "$FULL_PATH/game/*/request-message",
        "$FULL_PATH/game/*/last-message-timestamp",
        "$FULL_PATH/game/*/sub",
        "$FULL_PATH/game/*/close-game-pinged",
        "$FULL_PATH/game/*/upset-alert-pinged",
        "$FULL_PATH/game",
        "$FULL_PATH/schedule/*",
        "$FULL_PATH/schedule/move",
        "$FULL_PATH/season/*/lock-schedule",
        "$FULL_PATH/season/*/unlock-schedule",
    )

private val PRIVILEGED_DELETE_PATHS =
    arrayOf(
        "$FULL_PATH/game",
        "$FULL_PATH/schedule/*",
        "$FULL_PATH/schedule/season/*",
    )

@Configuration
@EnableWebSecurity
open class WebConfig(
    private val sessionService: SessionService,
    @Value("\${bot.service.key}") private val botServiceKey: String,
) : WebSecurityConfigurerAdapter() {
    init {
        check(botServiceKey.isNotBlank() && botServiceKey != BOT_SERVICE_KEY_PLACEHOLDER) {
            "bot.service.key must be set to a real secret (BOT_SERVICE_KEY env var) before startup"
        }
    }

    @Bean
    open fun noopUserDetailsService(): UserDetailsService = InMemoryUserDetailsManager()

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http
            .csrf().disable()
            .addFilterBefore(
                JwtAuthenticationFilter(sessionService, botServiceKey),
                UsernamePasswordAuthenticationFilter::class.java,
            )
            .exceptionHandling()
            .authenticationEntryPoint { _, response, _ ->
                response.contentType = MediaType.APPLICATION_JSON_VALUE
                response.status = 401
                response.writer.write("""{"error":"Authentication required"}""")
            }
            .accessDeniedHandler { _, response, _ ->
                response.contentType = MediaType.APPLICATION_JSON_VALUE
                response.status = 403
                response.writer.write("""{"error":"Access denied"}""")
            }
            .and()
            .authorizeRequests()
            .antMatchers(HttpMethod.OPTIONS).permitAll()
            .antMatchers(HttpMethod.POST, *PRE_AUTH_POST_PATHS).permitAll()
            .antMatchers(HttpMethod.GET, *PRE_AUTH_GET_PATHS).permitAll()
            .antMatchers(HttpMethod.GET, ACTUATOR_HEALTH_PATH).permitAll()
            .antMatchers(ACTUATOR_WILDCARD_PATH).hasAnyRole(*ADMIN_ROLES)
            .antMatchers(HttpMethod.GET, *PRIVILEGED_GAME_STATUS_GET_PATHS).hasAnyRole(*PRIVILEGED_ROLES)
            .antMatchers(HttpMethod.GET, *PUBLIC_READ_GET_PATHS).permitAll()
            .antMatchers(HttpMethod.GET, "$FULL_PATH/play/**").permitAll()
            .antMatchers(HttpMethod.GET, "$FULL_PATH/schedule/**").permitAll()
            .antMatchers(HttpMethod.GET, "$FULL_PATH/scorebug/**").permitAll()
            .antMatchers(HttpMethod.GET, "$FULL_PATH/season/**").permitAll()
            .antMatchers(HttpMethod.GET, "$FULL_PATH/offseason/**").permitAll()
            .antMatchers(HttpMethod.GET, "$FULL_PATH/team/**").permitAll()
            .antMatchers(HttpMethod.GET, "$FULL_PATH/vegas-odds/**").permitAll()
            .antMatchers(HttpMethod.GET, "$FULL_PATH/win-probability/**").permitAll()
            .antMatchers(HttpMethod.GET, *ADMIN_ONLY_GET_PATHS).hasAnyRole(*ADMIN_ROLES)
            .antMatchers(HttpMethod.POST, *ADMIN_ONLY_POST_PATHS).hasAnyRole(*ADMIN_ROLES)
            .antMatchers(HttpMethod.PUT, *ADMIN_ONLY_PUT_PATHS).hasAnyRole(*ADMIN_ROLES)
            .antMatchers(HttpMethod.DELETE, *ADMIN_ONLY_DELETE_PATHS).hasAnyRole(*ADMIN_ROLES)
            .antMatchers(HttpMethod.POST, *PRIVILEGED_POST_PATHS).hasAnyRole(*PRIVILEGED_ROLES)
            .antMatchers(HttpMethod.PUT, *PRIVILEGED_PUT_PATHS).hasAnyRole(*PRIVILEGED_ROLES)
            .antMatchers(HttpMethod.DELETE, *PRIVILEGED_DELETE_PATHS).hasAnyRole(*PRIVILEGED_ROLES)
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
