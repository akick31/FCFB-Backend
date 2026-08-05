package com.fcfb.arceus.util

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class LoggerTest {
    private lateinit var originalOut: PrintStream
    private lateinit var originalErr: PrintStream
    private lateinit var outContent: ByteArrayOutputStream
    private lateinit var errContent: ByteArrayOutputStream

    @BeforeEach
    fun setUp() {
        originalOut = System.out
        originalErr = System.err
        outContent = ByteArrayOutputStream()
        errContent = ByteArrayOutputStream()
        System.setOut(PrintStream(outContent))
        System.setErr(PrintStream(errContent))
    }

    @AfterEach
    fun tearDown() {
        System.setOut(originalOut)
        System.setErr(originalErr)
    }

    @Test
    fun `test debug logging`() {
        val message = "Debug test message"
        Logger.debug(message)
    }

    @Test
    fun `test debug logging with arguments`() {
        val message = "Debug test with args: {} and {}"
        val arg1 = "value1"
        val arg2 = 42

        Logger.debug(message, arg1, arg2)
    }

    @Test
    fun `test info logging`() {
        val message = "Info test message"
        Logger.info(message)
    }

    @Test
    fun `test info logging with arguments`() {
        val message = "Info test with args: {} and {}"
        val arg1 = "value1"
        val arg2 = 42

        Logger.info(message, arg1, arg2)
    }

    @Test
    fun `test warn logging`() {
        val message = "GameWarning test message"
        Logger.warn(message)
    }

    @Test
    fun `test warn logging with arguments`() {
        val message = "GameWarning test with args: {} and {}"
        val arg1 = "value1"
        val arg2 = 42

        Logger.warn(message, arg1, arg2)
    }

    @Test
    fun `test error logging`() {
        val message = "Error test message"
        Logger.error(message)
    }

    @Test
    fun `test error logging with arguments`() {
        val message = "Error test with args: {} and {}"
        val arg1 = "value1"
        val arg2 = 42

        Logger.error(message, arg1, arg2)
    }

    @Test
    fun `test logging with null arguments`() {
        val message = "Test with null args: {} and {}"
        val arg1: String? = null
        val arg2: Int? = null

        Logger.debug(message, arg1, arg2)
        Logger.info(message, arg1, arg2)
        Logger.warn(message, arg1, arg2)
        Logger.error(message, arg1, arg2)
    }

    @Test
    fun `test logging with empty string`() {
        val message = ""

        Logger.debug(message)
        Logger.info(message)
        Logger.warn(message)
        Logger.error(message)
    }

    @Test
    fun `test logging with special characters`() {
        val message = "Special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?"

        Logger.debug(message)
        Logger.info(message)
        Logger.warn(message)
        Logger.error(message)
    }

    @Test
    fun `test logging with unicode characters`() {
        val message = "Unicode: 你好世界 🌍 🚀 🎉"

        Logger.debug(message)
        Logger.info(message)
        Logger.warn(message)
        Logger.error(message)
    }

    @Test
    fun `test logging with very long message`() {
        val message = "A".repeat(10000)

        Logger.debug(message)
        Logger.info(message)
        Logger.warn(message)
        Logger.error(message)
    }

    @Test
    fun `test logging with multiple arguments`() {
        val message = "Multiple args: {}, {}, {}, {}, {}"
        val args = arrayOf("arg1", 42, true, null, 3.14)

        Logger.debug(message, *args)
        Logger.info(message, *args)
        Logger.warn(message, *args)
        Logger.error(message, *args)
    }

    @Test
    fun `test logging with complex objects`() {
        data class TestObject(val id: Int, val name: String)

        val message = "Complex object: {}"
        val obj = TestObject(123, "test")

        Logger.debug(message, obj)
        Logger.info(message, obj)
        Logger.warn(message, obj)
        Logger.error(message, obj)
    }

    @Test
    fun `test logging with exception`() {
        val message = "Exception occurred: {}"
        val exception = RuntimeException("Test exception")

        Logger.debug(message, exception)
        Logger.info(message, exception)
        Logger.warn(message, exception)
        Logger.error(message, exception)
    }

    @Test
    fun `test logging with different argument types`() {
        val message = "Different types: {}, {}, {}, {}, {}"
        val stringArg = "string"
        val intArg = 42
        val doubleArg = 3.14
        val booleanArg = true
        val nullArg: String? = null

        Logger.debug(message, stringArg, intArg, doubleArg, booleanArg, nullArg)
        Logger.info(message, stringArg, intArg, doubleArg, booleanArg, nullArg)
        Logger.warn(message, stringArg, intArg, doubleArg, booleanArg, nullArg)
        Logger.error(message, stringArg, intArg, doubleArg, booleanArg, nullArg)
    }

    @Test
    fun `test logging with format placeholders`() {
        val message = "Formatted: %s, %d, %f"
        val stringArg = "test"
        val intArg = 42
        val doubleArg = 3.14

        Logger.debug(message, stringArg, intArg, doubleArg)
        Logger.info(message, stringArg, intArg, doubleArg)
        Logger.warn(message, stringArg, intArg, doubleArg)
        Logger.error(message, stringArg, intArg, doubleArg)
    }

    @Test
    fun `test logging with no arguments`() {
        val message = "Simple message without placeholders"

        Logger.debug(message)
        Logger.info(message)
        Logger.warn(message)
        Logger.error(message)
    }

    @Test
    fun `test logging with more placeholders than arguments`() {
        val message = "More placeholders: {}, {}, {}"
        val arg1 = "arg1"
        val arg2 = "arg2"

        Logger.debug(message, arg1, arg2)
        Logger.info(message, arg1, arg2)
        Logger.warn(message, arg1, arg2)
        Logger.error(message, arg1, arg2)
    }

    @Test
    fun `test logging with more arguments than placeholders`() {
        val message = "Fewer placeholders: {}"
        val arg1 = "arg1"
        val arg2 = "arg2"
        val arg3 = "arg3"

        Logger.debug(message, arg1, arg2, arg3)
        Logger.info(message, arg1, arg2, arg3)
        Logger.warn(message, arg1, arg2, arg3)
        Logger.error(message, arg1, arg2, arg3)
    }
}
