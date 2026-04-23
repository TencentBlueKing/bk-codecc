package com.codecc.preci.service.oauth

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.security.MessageDigest
import java.util.Base64

@DisplayName("PKCEHelper 测试")
class PKCEHelperTest {

    @Test
    @DisplayName("code_verifier 长度在 43-128 之间")
    fun testCodeVerifierLength() {
        val verifier = PKCEHelper.generateCodeVerifier()
        assertTrue(verifier.length in 43..128, "verifier length=${verifier.length}")
    }

    @Test
    @DisplayName("code_verifier 仅包含 base64url 合法字符")
    fun testCodeVerifierCharacters() {
        val verifier = PKCEHelper.generateCodeVerifier()
        assertTrue(verifier.matches(Regex("[A-Za-z0-9_-]+")), "verifier=$verifier")
    }

    @Test
    @DisplayName("每次生成的 code_verifier 不同")
    fun testCodeVerifierUniqueness() {
        val v1 = PKCEHelper.generateCodeVerifier()
        val v2 = PKCEHelper.generateCodeVerifier()
        assertNotEquals(v1, v2)
    }

    @Test
    @DisplayName("code_challenge = BASE64URL(SHA256(code_verifier))")
    fun testCodeChallengeCorrectness() {
        val verifier = PKCEHelper.generateCodeVerifier()
        val challenge = PKCEHelper.generateCodeChallenge(verifier)

        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray())
        val expected = Base64.getUrlEncoder().withoutPadding().encodeToString(digest)

        assertEquals(expected, challenge)
    }

    @Test
    @DisplayName("state 长度合理且仅含 base64url 字符")
    fun testGenerateState() {
        val state = PKCEHelper.generateState()
        assertTrue(state.length >= 16, "state too short: ${state.length}")
        assertTrue(state.matches(Regex("[A-Za-z0-9_-]+")), "state=$state")
    }

    @Test
    @DisplayName("每次生成的 state 不同")
    fun testStateUniqueness() {
        val s1 = PKCEHelper.generateState()
        val s2 = PKCEHelper.generateState()
        assertNotEquals(s1, s2)
    }
}
