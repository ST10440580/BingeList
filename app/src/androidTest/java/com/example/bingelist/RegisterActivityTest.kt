package com.example.bingelist

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bingelist.ui.account.RegisterActivity
import com.google.firebase.auth.FirebaseAuth
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegisterActivityTest {

    @Before
    fun setUp() {
        FirebaseAuth.getInstance().signOut()
    }

    @Test
    fun verifyRegisterViews_areDisplayed() {
        ActivityScenario.launch(RegisterActivity::class.java)

        onView(withId(R.id.etEmail)).check(matches(isDisplayed()))
        onView(withId(R.id.etPassword)).check(matches(isDisplayed()))
        onView(withId(R.id.btnRegister)).check(matches(isDisplayed()))
        onView(withId(R.id.tvGoToLogin)).check(matches(isDisplayed()))
    }

    @Test
    fun enterStudentRegistrationDetails_populatesInputFields() {
        ActivityScenario.launch(RegisterActivity::class.java)

        val studentTestEmail = "st10441936@bingelist.ac.za"
        val studentSecurePass = "SibongilePass2026"

        onView(withId(R.id.etEmail))
            .perform(replaceText(studentTestEmail), closeSoftKeyboard())
        onView(withId(R.id.etEmail))
            .check(matches(withText(studentTestEmail)))

        onView(withId(R.id.etPassword))
            .perform(replaceText(studentSecurePass), closeSoftKeyboard())
        onView(withId(R.id.etPassword))
            .check(matches(withText(studentSecurePass)))
    }

    @Test
    fun clickGoToLogin_navigatesToLoginActivity() {
        ActivityScenario.launch(RegisterActivity::class.java)

        onView(withId(R.id.tvGoToLogin)).perform(click())

        onView(withId(R.id.btnLogin)).check(matches(isDisplayed()))
    }
}