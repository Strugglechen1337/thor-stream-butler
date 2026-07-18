package de.thorstream.butler.feature.controllertest

import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.abs

/** Buttons the controller test can visualize. */
enum class GamepadButton {
    A, B, X, Y,
    L1, R1, L2, R2,
    SELECT, START, MODE,
    THUMB_L, THUMB_R,
    DPAD_UP, DPAD_DOWN, DPAD_LEFT, DPAD_RIGHT,
}

/** Analog axis snapshot of a gamepad; all values are in [-1, 1] (triggers [0, 1]). */
data class GamepadAxes(
    val leftX: Float = 0f,
    val leftY: Float = 0f,
    val rightX: Float = 0f,
    val rightY: Float = 0f,
    val leftTrigger: Float = 0f,
    val rightTrigger: Float = 0f,
    val hatX: Float = 0f,
    val hatY: Float = 0f,
)

object GamepadMapping {
    /** Maps a key code to a visualized button, or null for keys the test does not capture. */
    fun buttonForKeyCode(keyCode: Int): GamepadButton? = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_A -> GamepadButton.A
        KeyEvent.KEYCODE_BUTTON_B -> GamepadButton.B
        KeyEvent.KEYCODE_BUTTON_X -> GamepadButton.X
        KeyEvent.KEYCODE_BUTTON_Y -> GamepadButton.Y
        KeyEvent.KEYCODE_BUTTON_L1 -> GamepadButton.L1
        KeyEvent.KEYCODE_BUTTON_R1 -> GamepadButton.R1
        KeyEvent.KEYCODE_BUTTON_L2 -> GamepadButton.L2
        KeyEvent.KEYCODE_BUTTON_R2 -> GamepadButton.R2
        KeyEvent.KEYCODE_BUTTON_SELECT -> GamepadButton.SELECT
        KeyEvent.KEYCODE_BUTTON_START -> GamepadButton.START
        KeyEvent.KEYCODE_BUTTON_MODE -> GamepadButton.MODE
        KeyEvent.KEYCODE_BUTTON_THUMBL -> GamepadButton.THUMB_L
        KeyEvent.KEYCODE_BUTTON_THUMBR -> GamepadButton.THUMB_R
        KeyEvent.KEYCODE_DPAD_UP -> GamepadButton.DPAD_UP
        KeyEvent.KEYCODE_DPAD_DOWN -> GamepadButton.DPAD_DOWN
        KeyEvent.KEYCODE_DPAD_LEFT -> GamepadButton.DPAD_LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> GamepadButton.DPAD_RIGHT
        else -> null
    }

    /** Snaps small stick drift to zero so idle sticks render centered. */
    fun deadZoned(value: Float, deadZone: Float = 0.08f): Float =
        if (abs(value) < deadZone) 0f else value.coerceIn(-1f, 1f)

    /**
     * Reads all supported axes from a joystick move event. Right sticks report
     * on Z/RZ on most pads and on RX/RY on some; the larger magnitude wins.
     * Triggers likewise vary between LTRIGGER/RTRIGGER and BRAKE/GAS.
     */
    fun axesFrom(event: MotionEvent): GamepadAxes = GamepadAxes(
        leftX = deadZoned(event.getAxisValue(MotionEvent.AXIS_X)),
        leftY = deadZoned(event.getAxisValue(MotionEvent.AXIS_Y)),
        rightX = deadZoned(dominant(event.getAxisValue(MotionEvent.AXIS_Z), event.getAxisValue(MotionEvent.AXIS_RX))),
        rightY = deadZoned(dominant(event.getAxisValue(MotionEvent.AXIS_RZ), event.getAxisValue(MotionEvent.AXIS_RY))),
        leftTrigger = dominant(event.getAxisValue(MotionEvent.AXIS_LTRIGGER), event.getAxisValue(MotionEvent.AXIS_BRAKE)).coerceIn(0f, 1f),
        rightTrigger = dominant(event.getAxisValue(MotionEvent.AXIS_RTRIGGER), event.getAxisValue(MotionEvent.AXIS_GAS)).coerceIn(0f, 1f),
        hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X),
        hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y),
    )

    private fun dominant(primary: Float, fallback: Float): Float =
        if (abs(primary) >= abs(fallback)) primary else fallback
}
