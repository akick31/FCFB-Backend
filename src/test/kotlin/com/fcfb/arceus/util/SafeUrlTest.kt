package com.fcfb.arceus.util

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SafeUrlTest {
    private fun rejected(url: String) = assertThrows(InvalidUniformException::class.java) { SafeUrl.requireAllowed(url) }

    @Test
    fun rejectsNonHttpSchemes() {
        rejected("file:///etc/passwd")
        rejected("ftp://example.com/font.ttf")
        rejected("gopher://example.com/")
        rejected("jar:file:///x!/y")
    }

    @Test
    fun rejectsLoopbackAndPrivateHosts() {
        rejected("http://127.0.0.1/")
        rejected("http://localhost/")
        rejected("http://[::1]/")
        rejected("http://10.0.0.1/")
        rejected("http://192.168.1.1/")
        rejected("http://172.16.0.1/")
        rejected("http://169.254.169.254/latest/meta-data/")
    }

    @Test
    fun rejectsMalformedUrls() {
        rejected("not a url")
        rejected("")
    }
}
