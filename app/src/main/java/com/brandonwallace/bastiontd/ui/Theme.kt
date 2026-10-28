package com.brandonwallace.bastiontd.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgObsidian = Color(0xFF070B14)
val PanelSurface = Color(0xD80D1526)
val PanelCard = Color(0xE6142038)
val PanelBorder = Color(0xFF243B61)
val CyanAccent = Color(0xFF00E5FF)
val GoldAccent = Color(0xFFFFD54F)
val ShardPurple = Color(0xFFD580FF)
val EmeraldGreen = Color(0xFF2EE68A)
val CrimsonAlert = Color(0xFFFF4D6D)
val TextPrimary = Color(0xFFF0F6FF)
val TextSecondary = Color(0xFF9EB4D4)

private val BastionColorScheme = darkColorScheme(
    primary = CyanAccent,
    secondary = GoldAccent,
    tertiary = ShardPurple,
    background = BgObsidian,
    surface = PanelSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun BastionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BastionColorScheme,
        content = content
    )
}
