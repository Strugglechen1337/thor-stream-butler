package de.thorstream.butler.feature.dashboard

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
