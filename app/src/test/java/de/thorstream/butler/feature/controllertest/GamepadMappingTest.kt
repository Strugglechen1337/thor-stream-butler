package de.thorstream.butler.feature.controllertest

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GamepadMappingTest {

    @Test
    fun `maps all standard gamepad buttons`() {
        assertEquals(GamepadButton.A, GamepadMapping.buttonForKeyCode(KeyEvent.KEYCODE_BUTTON_A))
        assertEquals(GamepadButton.B, GamepadMapping.buttonForKeyCode(KeyEvent.KEYCODE_BUTTON_B))
        assertEquals(GamepadButton.X, GamepadMapping.buttonForKeyCode(KeyEvent.KEYCODE_BUTTON_X))
        assertEquals(GamepadButton.Y, GamepadMapping.buttonForKeyCode(KeyEvent.KEYCODE_BUTTON_Y))
        assertEquals(GamepadButton.L1, GamepadMapping.buttonForKeyCode(KeyEvent.KEYCODE_BUTTON_L1))
        assertEquals(GamepadButton.R1, GamepadMapping.buttonForKeyCode(KeyEvent.KEYCODE_BUTTON_R1))
        assertEquals(GamepadButton.L2, GamepadMapping.buttonForKeyCode(KeyEvent.KEYCODE_BUTTON_L2))
        assertEquals(GamepadButton.R2, GamepadMapping.buttonForKeyCode(KeyEvent.KEYCODE_BUTTON_R2))
        assertEquals(GamepadButton.SELECT, GamepadMapping.buttonForKeyCode(KeyEvent.KEYCODE_BUTTON_SELECT))
        assertEquals(GamepadButton.START, GamepadMapping.buttonForKeyCode(KeyEvent.KEYCODE_BUTTON_START))
        assertEquals(GamepadButton.MODE, GamepadMapping.buttonForKeyCode(KeyEvent.KEYCODE_BUTTON_MODE))
        assertEquals(GamepadButton.THUMB_L, GamepadMapping.buttonForKeyCode(KeyEvent.KEYCODE_BUTTON_THUMBL))
        assertEquals(GamepadButton.THUMB_R, GamepadMapping.buttonForKeyCode(KeyEvent.KEYCODE_BUTTON_THUMBR))
        assertEquals(GamepadButton.DPAD_UP, GamepadMapping.buttonForKeyCode(KeyEvent.KEYCODE_DPAD_UP))
        assertEquals(GamepadButton.DPAD_DOWN, GamepadMapping.buttonForKeyCode(KeyEvent.KEYCODE_DPAD_DOWN))
        assertEquals(GamepadButton.DPAD_LEFT, GamepadMapping.buttonForKeyCode(KeyEvent.KEYCODE_DPAD_LEFT))
        assertEquals(GamepadButton.DPAD_RIGHT, GamepadMapping.buttonForKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT))
    }

    @Test
    fun `back and unrelated keys stay uncaptured so the screen remains escapable`() {
        assertNull(GamepadMapping.buttonForKeyCode(KeyEvent.KEYCODE_BACK))
        assertNull(GamepadMapping.buttonForKeyCode(KeyEvent.KEYCODE_HOME))
        assertNull(GamepadMapping.buttonForKeyCode(KeyEvent.KEYCODE_ENTER))
        assertNull(GamepadMapping.buttonForKeyCode(KeyEvent.KEYCODE_VOLUME_UP))
    }

    @Test
    fun `dead zone snaps drift to zero and clamps extremes`() {
        assertEquals(0f, GamepadMapping.deadZoned(0.05f))
        assertEquals(0f, GamepadMapping.deadZoned(-0.079f))
        assertEquals(0.5f, GamepadMapping.deadZoned(0.5f))
        assertEquals(-1f, GamepadMapping.deadZoned(-3f))
        assertEquals(1f, GamepadMapping.deadZoned(2f))
    }
}
