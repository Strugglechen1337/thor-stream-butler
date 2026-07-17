package de.thorstream.butler.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val ThorCyan = Color(0xFF72E6FF)
val ThorPurple = Color(0xFF8B5CF6)
val ThorGreen = Color(0xFF4ADE80)
val ThorYellow = Color(0xFFFACC15)
val ThorRed = Color(0xFFFB7185)
val ThorGray = Color(0xFF94A3B8)
val ThorBackground = Color(0xFF090B11)
val ThorSurface = Color(0xFF141824)

private val ThorColors = darkColorScheme(
    primary = ThorCyan,
    secondary = ThorPurple,
    tertiary = ThorGreen,
    background = ThorBackground,
    surface = ThorSurface,
    surfaceVariant = Color(0xFF202636),
    onPrimary = Color(0xFF001F27),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = ThorRed,
)

@Composable
fun ThorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ThorColors,
        typography = MaterialTheme.typography.copy(
            headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 30.sp),
            headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 22.sp),
            titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 20.sp),
            bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp),
        ),
        content = content,
    )
}

