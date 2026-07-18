package de.thorstream.butler.feature.controllertest

import android.content.Context
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import de.thorstream.butler.R
import de.thorstream.butler.core.designsystem.ThorCyan
import de.thorstream.butler.core.designsystem.ThorGray
import kotlinx.coroutines.delay

/**
 * Live controller diagnostics: shows pressed buttons, stick positions, and
 * trigger values. Gamepad keys including the D-pad are captured while this
 * screen is open; leave it with touch or the system back gesture.
 */
@Composable
fun ControllerTestRoute(onExit: () -> Unit) {
    var pressed by remember { mutableStateOf(setOf<GamepadButton>()) }
    var axes by remember { mutableStateOf(GamepadAxes()) }
    var lastInput by remember { mutableStateOf<String?>(null) }
    var deviceNames by remember { mutableStateOf(connectedGamepadNames()) }

    LaunchedEffect(Unit) {
        while (true) {
            deviceNames = connectedGamepadNames()
            delay(3_000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.controller_kicker), color = ThorCyan, style = MaterialTheme.typography.labelLarge)
                Text(stringResource(R.string.controller_title), style = MaterialTheme.typography.headlineLarge)
            }
            TextButton(onClick = onExit) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                Text(" " + stringResource(R.string.controller_exit))
            }
        }
        Text(stringResource(R.string.controller_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Invisible focus anchor that receives all gamepad key and motion events.
        AndroidView(
            factory = { context ->
                GamepadCaptureView(context).apply {
                    onButtonEvent = { button, isDown ->
                        pressed = if (isDown) pressed + button else pressed - button
                        if (isDown) lastInput = button.name
                    }
                    onAxesEvent = { axes = it }
                    post { requestFocus() }
                }
            },
            update = { it.requestFocus() },
            modifier = Modifier.fillMaxWidth().height(1.dp),
        )

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.controller_connected), style = MaterialTheme.typography.titleLarge, color = ThorCyan)
                if (deviceNames.isEmpty()) {
                    Text(stringResource(R.string.controller_no_devices), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    deviceNames.forEach { Text("• $it") }
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StickIndicator(stringResource(R.string.controller_left_stick), axes.leftX, axes.leftY)
                    StickIndicator(stringResource(R.string.controller_right_stick), axes.rightX, axes.rightY)
                }
                TriggerBar("L2", axes.leftTrigger, pressed.contains(GamepadButton.L2))
                TriggerBar("R2", axes.rightTrigger, pressed.contains(GamepadButton.R2))
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.controller_buttons), style = MaterialTheme.typography.titleLarge, color = ThorCyan)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        GamepadButton.A, GamepadButton.B, GamepadButton.X, GamepadButton.Y,
                        GamepadButton.L1, GamepadButton.R1,
                        GamepadButton.THUMB_L, GamepadButton.THUMB_R,
                        GamepadButton.SELECT, GamepadButton.START, GamepadButton.MODE,
                    ).forEach { button -> ButtonChip(button.chipLabel(), pressed.contains(button)) }
                }
                Text(stringResource(R.string.controller_dpad), style = MaterialTheme.typography.titleLarge, color = ThorCyan)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ButtonChip("▲", pressed.contains(GamepadButton.DPAD_UP) || axes.hatY < -0.5f)
                    ButtonChip("▼", pressed.contains(GamepadButton.DPAD_DOWN) || axes.hatY > 0.5f)
                    ButtonChip("◀", pressed.contains(GamepadButton.DPAD_LEFT) || axes.hatX < -0.5f)
                    ButtonChip("▶", pressed.contains(GamepadButton.DPAD_RIGHT) || axes.hatX > 0.5f)
                }
                Text(
                    lastInput?.let { stringResource(R.string.controller_last_input, it) }
                        ?: stringResource(R.string.controller_waiting),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun GamepadButton.chipLabel(): String = when (this) {
    GamepadButton.THUMB_L -> "L3"
    GamepadButton.THUMB_R -> "R3"
    else -> name
}

@Composable
private fun StickIndicator(label: String, x: Float, y: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val outline = MaterialTheme.colorScheme.onSurfaceVariant
        Canvas(Modifier.size(96.dp)) {
            val radius = size.minDimension / 2f
            drawCircle(color = outline, radius = radius - 2.dp.toPx(), style = Stroke(width = 2.dp.toPx()))
            drawCircle(color = ThorGray, radius = 2.dp.toPx())
            drawCircle(
                color = ThorCyan,
                radius = 7.dp.toPx(),
                center = center + Offset(x, y) * (radius - 12.dp.toPx()),
            )
        }
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(String.format(composeLocale(), "%.2f / %.2f", x, y), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TriggerBar(label: String, value: Float, digitalPressed: Boolean) {
    val effective = if (digitalPressed && value == 0f) 1f else value
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        LinearProgressIndicator(progress = { effective }, modifier = Modifier.weight(1f))
        Text(String.format(composeLocale(), "%3.0f %%", effective * 100))
    }
}

/** Observable current locale as a java.util.Locale for String.format. */
@Composable
private fun composeLocale(): java.util.Locale = java.util.Locale.forLanguageTag(Locale.current.toLanguageTag())

@Composable
private fun ButtonChip(label: String, isPressed: Boolean) {
    Text(
        label,
        color = if (isPressed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isPressed) ThorCyan else MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

/** Names of currently connected physical gamepads or joysticks. */
private fun connectedGamepadNames(): List<String> =
    InputDevice.getDeviceIds()
        .toList()
        .mapNotNull { InputDevice.getDevice(it) }
        .filter { device ->
            !device.isVirtual && (
                device.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                    device.sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
                )
        }
        .map { it.name }
        .distinct()

/**
 * Focusable view that captures gamepad key and joystick motion events.
 * BACK and all unmapped keys pass through so the screen stays escapable.
 */
private class GamepadCaptureView(context: Context) : View(context) {
    var onButtonEvent: (GamepadButton, Boolean) -> Unit = { _, _ -> }
    var onAxesEvent: (GamepadAxes) -> Unit = {}

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val button = GamepadMapping.buttonForKeyCode(keyCode) ?: return super.onKeyDown(keyCode, event)
        if (event.repeatCount == 0) onButtonEvent(button, true)
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val button = GamepadMapping.buttonForKeyCode(keyCode) ?: return super.onKeyUp(keyCode, event)
        onButtonEvent(button, false)
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.isFromSource(InputDevice.SOURCE_JOYSTICK) && event.actionMasked == MotionEvent.ACTION_MOVE) {
            onAxesEvent(GamepadMapping.axesFrom(event))
            return true
        }
        return super.onGenericMotionEvent(event)
    }
}
