package org.commcare.androidTests

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.commcare.dalvik.R
import org.commcare.utils.InstrumentationUtility
import org.hamcrest.Matchers.endsWith
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class AppUpdateTest: BaseTest() {

    companion object {
        const val CCZ_NAME = "app_update.ccz"
        const val APP_NAME = "Update Test"
    }

    @Before
    fun setup() {
        installApp(APP_NAME, CCZ_NAME)
        InstrumentationUtility.login("user_with_no_data", "123")
    }

    @Test
    fun testAppUpdate() {
        InstrumentationUtility.enableDeveloperMode()
        // Enable app update item.
        InstrumentationUtility.openOptionsMenu()
        onView(withText("Settings"))
                .perform(click())
        onView(withText("Developer Options"))
                .perform(click())
        onView(withText("Show Update Options Item"))
                .perform(click())
        onView(withText("Enabled"))
                .perform(click())
        pressBack()

        // Make sure the update endpoint is set to "Latest starred version"
        onView(withText("Update Options"))
                .perform(click())
        onView(withText("Latest starred version"))
                .perform(click())
        pressBack()

        // check base form content
        onView(withText("Start"))
                .perform(click())
        onView(withText("Module Three"))
                .check(matches(isDisplayed()))
        onView(withText("Module One"))
                .perform(click())
        onView(withText("Example 1"))
                .perform(click())
        onView(withText("A text question"))
                .check(matches(isDisplayed()))
        closeSoftKeyboard()
        pressBack()
        onView(withText(R.string.do_not_save))
                .perform(click())
        pressBack()

        // make sure case list doesn't have status column
        onView(withText("Module Two"))
                .perform(click())
        onView(withText("Update Case"))
                .perform(click())
        onView(withText("Status"))
                .check(doesNotExist())
        pressBack()
        pressBack()
        pressBack()

        // download the app update
        InstrumentationUtility.openOptionsMenu()
        onView(withText("Update App"))
                .perform(click())
        onView(withText("Current version: 2"))
                .check(matches(isDisplayed()))
        onView(withText("Update to version 11 & log out"))
                .check(matches(isDisplayed()))
        onView(withText("Update to version 11 & log out"))
                .perform(click())

        // Login into the updated version
        InstrumentationUtility.login("user_with_no_data", "123")

        //TODO check that a sync is triggered automatically

        // Check updated data, including multimedia
        onView(withText("Start"))
                .perform(click())
        onView(withText("Module Three"))
                .check(doesNotExist())
        onView(withText("Module One"))
                .perform(click())
        onView(withText("Example 1"))
                .perform(click())
        onView(withText("Question with audio"))
                .check(matches(isDisplayed()))
        onView(withClassName(endsWith("AudioPlaybackButton")))
                .check(matches(isDisplayed()))
        closeSoftKeyboard()
        pressBack()
        onView(withText(R.string.do_not_save))
                .perform(click())
        pressBack()

        // make sure case list `Status` column was added
        onView(withText("Module Two"))
                .perform(click())
        onView(withText("Update Case"))
                .perform(click())
        onView(withText("Status"))
                .check(matches(isDisplayed()))
        pressBack()
        pressBack()
        pressBack()

        // make sure there are no new updates
        InstrumentationUtility.openOptionsMenu()
        onView(withText("Update App"))
                .perform(click())
        onView(withText("Current version: 11"))
                .check(matches(isDisplayed()))
        onView(withText("Recheck"))
                .perform(click())
        onView(withText("Current version: 11"))
                .check(matches(isDisplayed()))
        pressBack()

        // Change the update endpoint to "Latest version"
        InstrumentationUtility.openOptionsMenu()
        onView(withText("Settings"))
                .perform(click())
        onView(withText("Update Options"))
                .perform(click())
        onView(withText("Latest version"))
                .perform(click())
        pressBack()

        // Confirm that you can now see an update. And Update the app.
        InstrumentationUtility.openOptionsMenu()
        onView(withText("Update App"))
                .perform(click())
        onView(withText("Update to version 22 & log out"))
                .check(matches(isDisplayed()))
        onView(withText("Update to version 22 & log out"))
                .perform(click())

        // Login again
        InstrumentationUtility.login("user_with_no_data", "123")
        //TODO Check that sync is triggered automatically

        // Check updates in base form
        onView(withText("Start"))
                .perform(click())
        onView(withText("Module One, renamed"))
                .check(matches(isDisplayed()))
        pressBack()

        // The below tests will only work on a pre-android Q device.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            changeWifi(false)
            InstrumentationUtility.openOptionsMenu()
            onView(withText("Update App"))
                    .perform(click())
            onView(withText("No network connectivity"))
                    .perform(click())
            changeWifi(true)
        }
    }

    /**
     * Starting with Android Q, applications are not allowed to enable/disable Wi-Fi.
     */
    private fun changeWifi(enable: Boolean) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.isWifiEnabled = enable
    }

}