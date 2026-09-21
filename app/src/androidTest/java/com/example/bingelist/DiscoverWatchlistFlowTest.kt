package com.example.bingelist

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bingelist.ui.discover.DiscoverActivity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiscoverWatchlistFlowTest {

    @Test
    fun verifyBottomNavigationItems_areDisplayed() {
        ActivityScenario.launch(DiscoverActivity::class.java)

        onView(withId(R.id.bottomNavBar)).check(matches(isDisplayed()))
        onView(withId(R.id.navDiscover)).check(matches(isDisplayed()))
        onView(withId(R.id.navWatchList)).check(matches(isDisplayed()))
        onView(withId(R.id.navFavorites)).check(matches(isDisplayed()))
        onView(withId(R.id.navSettings)).check(matches(isDisplayed()))
    }

    @Test
    fun navigateToWatchlist_displaysWatchlistScreen() {
        ActivityScenario.launch(DiscoverActivity::class.java)

        // Click the Watchlist item in the bottom navigation
        onView(withId(R.id.navWatchList)).perform(click())

        // Verify elements inside Watchlist screen are displayed
        onView(withId(R.id.watchlistTitle)).check(matches(isDisplayed()))
    }
}