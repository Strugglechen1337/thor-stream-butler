package de.thorstream.butler.feature.dashboard

import android.content.pm.ActivityInfo
import android.view.KeyEvent
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.pressKey
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import de.thorstream.butler.MainActivity
import org.junit.Rule
import org.junit.Test

class DashboardControllerNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun dpadMovesFocusBetweenDashboardTiles() {
        // The adaptive grid has one column in portrait and multiple columns in
        // landscape. Pin the geometry so DPAD_RIGHT has the same meaning on
        // local devices and differently configured CI emulator profiles.
        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithTag("dashboard-tile-1").fetchSemanticsNode()
            }.isSuccess
        }

        composeRule.onNodeWithTag("dashboard-tile-0").assertIsFocused()
        onView(isRoot()).perform(pressKey(KeyEvent.KEYCODE_DPAD_RIGHT))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dashboard-tile-1").assertIsFocused()
    }
}
