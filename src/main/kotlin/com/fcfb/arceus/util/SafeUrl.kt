package com.fcfb.arceus.util

import com.fcfb.arceus.exception.team.InvalidUniformException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

object SafeUrl {
    fun isAllowedUrl(url: URL): Boolean {
        val protocol = url.protocol?.lowercase()
        if (protocol != "http" && protocol != "https") return false
        return isPublicHost(url.host)
    }

    fun requireAllowed(raw: String): URL {
        val url =
            try {
                URL(raw)
            } catch (e: Exception) {
                throw InvalidUniformException("That URL is not valid")
            }
        if (!isAllowedUrl(url)) throw InvalidUniformException("Only public http(s) links are allowed")
        return url
    }

    fun openStream(
        raw: String,
        connectTimeoutMs: Int,
        readTimeoutMs: Int,
    ): InputStream {
        var current = requireAllowed(raw)
        var redirects = 0
        while (true) {
            val connection = current.openConnection() as HttpURLConnection
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.instanceFollowRedirects = false
            val status = connection.responseCode
            if (status < 300 || status >= 400) return connection.inputStream
            val location = connection.getHeaderField("Location")
            connection.disconnect()
            if (location == null || redirects++ >= MAX_REDIRECTS) throw InvalidUniformException("Too many redirects")
            val next = URL(current, location)
            if (!isAllowedUrl(next)) throw InvalidUniformException("Only public http(s) links are allowed")
            current = next
        }
    }

    private fun isPublicHost(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        return try {
            InetAddress.getAllByName(host).all { isPublic(it) }
        } catch (e: Exception) {
            false
        }
    }

    private fun isPublic(address: InetAddress): Boolean =
        !address.isAnyLocalAddress &&
            !address.isLoopbackAddress &&
            !address.isLinkLocalAddress &&
            !address.isSiteLocalAddress &&
            !address.isMulticastAddress &&
            !isUniqueLocalV6(address)

    private fun isUniqueLocalV6(address: InetAddress): Boolean {
        val bytes = address.address
        return bytes.size == 16 && (bytes[0].toInt() and 0xFE) == 0xFC
    }

    private const val MAX_REDIRECTS = 3
}
