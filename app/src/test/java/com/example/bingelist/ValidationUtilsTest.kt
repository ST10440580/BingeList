package com.example.bingelist

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationUtilsTest {

    // Helper functions tested locally
    private fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        return trimmed.matches(emailRegex.toRegex())
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    @Test
    fun email_validFormat_returnsTrue() {
        val email = "john.doe@example.com"
        assertTrue(isValidEmail(email))
    }

    @Test
    fun email_missingAtSymbol_returnsFalse() {
        val email = "johndoeexample.com"
        assertFalse(isValidEmail(email))
    }

    @Test
    fun email_missingDomainExtension_returnsFalse() {
        val email = "john@example"
        assertFalse(isValidEmail(email))
    }

    @Test
    fun email_emptyString_returnsFalse() {
        assertFalse(isValidEmail(""))
    }

    @Test
    fun password_shorterThanSixChars_returnsFalse() {
        assertFalse(isValidPassword("12345"))
    }

    @Test
    fun password_sixOrMoreChars_returnsTrue() {
        assertTrue(isValidPassword("123456"))
        assertTrue(isValidPassword("SecurePassword!"))
    }
}