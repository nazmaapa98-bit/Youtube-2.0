package com.tonmoy.ytplayer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = YTRed,
    secondary = YTWhite,
    tertiary = AudioModeGreen,
    background = YTDarkBackground,
    surface = YTDarkSurface,
    onPrimary = YTWhite,
    onSecondary = YTBlack,
    onTertiary = YTBlack,
    onBackground = YTWhite,
    onSurface = YTWhite,
)

private val LightColorScheme = lightColorScheme(
    primary = YTRed,
    secondary = YTBlack,
    tertiary = AudioModeGreenDark,
    background = YTLightGray,
    surface = YTWhite,
    onPrimary = YTWhite,
    onSecondary = YTWhite,
    onTertiary = YTWhite,
    onBackground = YTBlack,
    onSurface = YTBlack
)

@Composable
fun YTPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = YTPlayerTypography,
        content = content
    )
}
