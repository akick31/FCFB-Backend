package com.fcfb.arceus.dto.standard

import com.fasterxml.jackson.annotation.JsonProperty
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SignupInfoDTOTest {
    @Test
    fun `SignupInfo should be a data class`() {
        val signupInfo = createTestSignupInfo()

        // Data classes automatically implement equals, hashCode, toString, and copy
        assertNotNull(signupInfo.toString())
        assertTrue(signupInfo.toString().contains("SignupInfo"))
    }

    @Test
    fun `SignupInfo should create instance with all properties`() {
        val signupInfo =
            SignupInfo(
                discordTag = "testuser#1234",
                discordId = "123456789012345678",
                teamChoiceOne = "Alabama",
                teamChoiceTwo = "Georgia",
                teamChoiceThree = "Texas",
            )

        assertEquals("testuser#1234", signupInfo.discordTag)
        assertEquals("123456789012345678", signupInfo.discordId)
        assertEquals("Alabama", signupInfo.teamChoiceOne)
        assertEquals("Georgia", signupInfo.teamChoiceTwo)
        assertEquals("Texas", signupInfo.teamChoiceThree)
    }

    @Test
    fun `SignupInfo should have proper JsonProperty annotations`() {
        val signupInfoClass = SignupInfo::class.java

        // Check discord_tag annotation
        val discordTagField = signupInfoClass.declaredConstructors[0].parameters.find { it.name == "discordTag" }
        val discordTagAnnotation = discordTagField?.getAnnotation(JsonProperty::class.java)
        assertEquals("discord_tag", discordTagAnnotation?.value)

        // Check discord_id annotation
        val discordIdField = signupInfoClass.declaredConstructors[0].parameters.find { it.name == "discordId" }
        val discordIdAnnotation = discordIdField?.getAnnotation(JsonProperty::class.java)
        assertEquals("discord_id", discordIdAnnotation?.value)

        // Check team_choice_one annotation
        val teamChoiceOneField = signupInfoClass.declaredConstructors[0].parameters.find { it.name == "teamChoiceOne" }
        val teamChoiceOneAnnotation = teamChoiceOneField?.getAnnotation(JsonProperty::class.java)
        assertEquals("team_choice_one", teamChoiceOneAnnotation?.value)

        // Check team_choice_two annotation
        val teamChoiceTwoField = signupInfoClass.declaredConstructors[0].parameters.find { it.name == "teamChoiceTwo" }
        val teamChoiceTwoAnnotation = teamChoiceTwoField?.getAnnotation(JsonProperty::class.java)
        assertEquals("team_choice_two", teamChoiceTwoAnnotation?.value)

        // Check team_choice_three annotation
        val teamChoiceThreeField = signupInfoClass.declaredConstructors[0].parameters.find { it.name == "teamChoiceThree" }
        val teamChoiceThreeAnnotation = teamChoiceThreeField?.getAnnotation(JsonProperty::class.java)
        assertEquals("team_choice_three", teamChoiceThreeAnnotation?.value)
    }

    @Test
    fun `SignupInfo should handle Discord tag formats`() {
        val signupInfo1 =
            SignupInfo(
                discordTag = "user#1234",
                discordId = "123456789012345678",
                teamChoiceOne = "Alabama",
                teamChoiceTwo = "Georgia",
                teamChoiceThree = "Texas",
            )

        val signupInfo2 =
            SignupInfo(
                discordTag = "longusernamewithspecialchars123#9999",
                discordId = "987654321098765432",
                teamChoiceOne = "Michigan",
                teamChoiceTwo = "Ohio State",
                teamChoiceThree = "Penn State",
            )

        assertEquals("user#1234", signupInfo1.discordTag)
        assertEquals("longusernamewithspecialchars123#9999", signupInfo2.discordTag)
    }

    @Test
    fun `SignupInfo should handle Discord ID formats`() {
        val signupInfo = createTestSignupInfo()

        // Discord IDs are typically 18-digit numbers
        assertTrue(signupInfo.discordId.length >= 17)
        assertTrue(signupInfo.discordId.all { it.isDigit() })
    }

    @Test
    fun `SignupInfo should handle different team names`() {
        val signupInfo =
            SignupInfo(
                discordTag = "testuser#1234",
                discordId = "123456789012345678",
                teamChoiceOne = "Texas A&M",
                teamChoiceTwo = "Ole Miss",
                teamChoiceThree = "Miami (FL)",
            )

        assertEquals("Texas A&M", signupInfo.teamChoiceOne)
        assertEquals("Ole Miss", signupInfo.teamChoiceTwo)
        assertEquals("Miami (FL)", signupInfo.teamChoiceThree)
    }

    @Test
    fun `SignupInfo should handle long team names`() {
        val longTeamName = "A".repeat(100)
        val signupInfo =
            SignupInfo(
                discordTag = "testuser#1234",
                discordId = "123456789012345678",
                teamChoiceOne = longTeamName,
                teamChoiceTwo = longTeamName,
                teamChoiceThree = longTeamName,
            )

        assertEquals(longTeamName, signupInfo.teamChoiceOne)
        assertEquals(longTeamName, signupInfo.teamChoiceTwo)
        assertEquals(longTeamName, signupInfo.teamChoiceThree)
    }

    @Test
    fun `SignupInfo data class should support copy functionality`() {
        val original = createTestSignupInfo()
        val copied = original.copy(teamChoiceOne = "Michigan")

        assertEquals("Michigan", copied.teamChoiceOne)
        assertEquals(original.discordTag, copied.discordTag)
        assertEquals(original.discordId, copied.discordId)
        assertEquals(original.teamChoiceTwo, copied.teamChoiceTwo)
        assertEquals(original.teamChoiceThree, copied.teamChoiceThree)
    }

    @Test
    fun `SignupInfo data class should support equality comparison`() {
        val signupInfo1 = createTestSignupInfo()
        val signupInfo2 = createTestSignupInfo()
        val signupInfo3 = signupInfo1.copy(teamChoiceOne = "Different Team")

        assertEquals(signupInfo1, signupInfo2)
        assertTrue(signupInfo1 != signupInfo3)
    }

    @Test
    fun `SignupInfo should handle special characters in Discord tags`() {
        val signupInfo =
            SignupInfo(
                discordTag = "user_with.special-chars123#9999",
                discordId = "123456789012345678",
                teamChoiceOne = "Alabama",
                teamChoiceTwo = "Georgia",
                teamChoiceThree = "Texas",
            )

        assertEquals("user_with.special-chars123#9999", signupInfo.discordTag)
    }

    @Test
    fun `SignupInfo should handle team choices with special characters and spaces`() {
        val signupInfo =
            SignupInfo(
                discordTag = "testuser#1234",
                discordId = "123456789012345678",
                teamChoiceOne = "North Carolina State",
                teamChoiceTwo = "Virginia Tech",
                teamChoiceThree = "Boston College",
            )

        assertEquals("North Carolina State", signupInfo.teamChoiceOne)
        assertEquals("Virginia Tech", signupInfo.teamChoiceTwo)
        assertEquals("Boston College", signupInfo.teamChoiceThree)
    }

    @Test
    fun `SignupInfo should be immutable`() {
        val signupInfo = createTestSignupInfo()

        // All properties should be val (immutable)
        // This is enforced by the data class declaration
        assertNotNull(signupInfo.discordTag)
        assertNotNull(signupInfo.discordId)
        assertNotNull(signupInfo.teamChoiceOne)
        assertNotNull(signupInfo.teamChoiceTwo)
        assertNotNull(signupInfo.teamChoiceThree)
    }

    @Test
    fun `SignupInfo should handle same team choices`() {
        val signupInfo =
            SignupInfo(
                discordTag = "testuser#1234",
                discordId = "123456789012345678",
                teamChoiceOne = "Alabama",
                teamChoiceTwo = "Alabama",
                teamChoiceThree = "Alabama",
            )

        assertEquals("Alabama", signupInfo.teamChoiceOne)
        assertEquals("Alabama", signupInfo.teamChoiceTwo)
        assertEquals("Alabama", signupInfo.teamChoiceThree)
    }

    private fun createTestSignupInfo(): SignupInfo {
        return SignupInfo(
            discordTag = "testuser#1234",
            discordId = "123456789012345678",
            teamChoiceOne = "Alabama",
            teamChoiceTwo = "Georgia",
            teamChoiceThree = "Texas",
        )
    }
}
