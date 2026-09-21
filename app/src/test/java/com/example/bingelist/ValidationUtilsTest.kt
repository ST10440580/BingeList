package com.example.bingelist

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationUtilsTest {

    // Helper functions tested locally
    private object ValidationUtils {
        fun isValidEmail(email: String?): Boolean {
            if (email.isNullOrBlank()) return false
            val trimmed = email.trim()
            val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
            return trimmed.matches(emailRegex.toRegex())
        }

        fun isValidPassword(password: String?): Boolean {
            if (password.isNullOrBlank()) return false
            return password.length >= 6
        }
    }

    @Test
    fun email_validFormat_returnsTrue() {
        val studentEmail = "st10441936@bingelist.ac.za"
        val generalEmail = "john.doe@example.com"
        assertTrue(ValidationUtils.isValidEmail(studentEmail))
        assertTrue(ValidationUtils.isValidEmail(generalEmail))
    }

    @Test
    fun email_missingAtSymbol_returnsFalse() {
        val email = "st10441936bingelist.ac.za"
        assertFalse(ValidationUtils.isValidEmail(email))
    }

    @Test
    fun email_missingDomainExtension_returnsFalse() {
        val email = "sibongile@example"
        assertFalse(ValidationUtils.isValidEmail(email))
    }

    @Test
    fun email_emptyString_returnsFalse() {
        assertFalse(ValidationUtils.isValidEmail(""))
    }

    @Test
    fun email_nullInput_returnsFalse() {
        assertFalse(ValidationUtils.isValidEmail(null))
    }

    @Test
    fun password_shorterThanSixChars_returnsFalse() {
        assertFalse(ValidationUtils.isValidPassword("12345"))
    }

    @Test
    fun password_sixOrMoreChars_returnsTrue() {
        assertTrue(ValidationUtils.isValidPassword("123456"))
        assertTrue(ValidationUtils.isValidPassword("SibongilePass2026!"))
    }

    @Test
    fun password_nullOrEmpty_returnsFalse() {
        assertFalse(ValidationUtils.isValidPassword(""))
        assertFalse(ValidationUtils.isValidPassword(null))
    }
}