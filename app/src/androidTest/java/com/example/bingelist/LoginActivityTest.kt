package com.example.bingelist

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bingelist.ui.account.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginActivityTest {

    @Before
    fun setUp() {
        FirebaseAuth.getInstance().signOut()
    }

    @Test
    fun verifyLoginViews_areDisplayed() {
        ActivityScenario.launch(LoginActivity::class.java)

        onView(withId(R.id.etLoginEmail)).check(matches(isDisplayed()))
        onView(withId(R.id.etLoginPassword)).check(matches(isDisplayed()))
        onView(withId(R.id.btnLogin)).check(matches(isDisplayed()))
    }
    @Test
    fun typeCredentials_displaysInInputFields() {
        ActivityScenario.launch(LoginActivity::class.java)

        // Set email and verify
        onView(withId(R.id.etLoginEmail))
            .perform(replaceText("test@bingelist.com"), closeSoftKeyboard())
        onView(withId(R.id.etLoginEmail))
            .check(matches(withText("test@bingelist.com")))

        // Set password and verify
        onView(withId(R.id.etLoginPassword))
            .perform(replaceText("password123"), closeSoftKeyboard())
        onView(withId(R.id.etLoginPassword))
            .check(matches(withText("password123")))
    }
    @Test
    fun clickGoToRegister_opensRegisterActivity() {
        ActivityScenario.launch(LoginActivity::class.java)

        // Click "Sign Up" / Register link
        onView(withId(R.id.tvGoToRegister)).perform(click())

        // Verify RegisterActivity view is displayed
        onView(withId(R.id.btnRegister)).check(matches(isDisplayed()))
    }
}