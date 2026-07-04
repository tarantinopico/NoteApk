package com.example.core.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.config.AppConfig
import com.example.core.config.FontStyle
import com.example.core.config.ThemeMode

private fun createCortexColorScheme(config: AppConfig, isDark: Boolean): androidx.compose.material3.ColorScheme {
    val accent = Color(config.accentColor)
    return if (isDark) {
        darkColorScheme(
            primary = accent,
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            surfaceVariant = Color(0xFF2D2D2D),
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFE0E0E0)
        )
    } else {
        lightColorScheme(
            primary = accent,
            background = Color(0xFFF5F5F5),
            surface = Color.White,
            surfaceVariant = Color(0xFFE0E0E0),
            onPrimary = Color.White,
            onBackground = Color.Black,
            onSurface = Color.Black,
            onSurfaceVariant = Color(0xFF424242)
        )
    }
}

@Composable
fun CortexTheme(
    appConfig: AppConfig,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (appConfig.themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val context = LocalContext.current
    val colorScheme = if (appConfig.useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        createCortexColorScheme(appConfig, isDark)
    }

    val baseSpacing = appConfig.baseSpacingDp.dp
    val spacing = CortexSpacing(
        extraSmall = baseSpacing / 2,
        small = baseSpacing,
        medium = baseSpacing * 2,
        large = baseSpacing * 3,
        extraLarge = baseSpacing * 4
    )

    val dimens = CortexDimens(
        iconSize = appConfig.iconSizeDp.dp,
        cornerRadius = appConfig.baseRadiusDp.dp
    )

    val fontFamily = when (appConfig.fontStyle) {
        FontStyle.SANS -> FontFamily.SansSerif
        FontStyle.MONOSPACE -> FontFamily.Monospace
    }

    val scale = appConfig.textScale
    val typography = Typography(
        bodyLarge = TextStyle(fontFamily = fontFamily, fontSize = 16.sp * scale),
        bodyMedium = TextStyle(fontFamily = fontFamily, fontSize = 14.sp * scale),
        bodySmall = TextStyle(fontFamily = fontFamily, fontSize = 12.sp * scale),
        titleLarge = TextStyle(fontFamily = fontFamily, fontSize = 22.sp * scale),
        headlineMedium = TextStyle(fontFamily = fontFamily, fontSize = 28.sp * scale),
        labelLarge = TextStyle(fontFamily = fontFamily, fontSize = 14.sp * scale)
    )

    val radius = appConfig.baseRadiusDp.dp
    val shapes = Shapes(
        small = RoundedCornerShape(radius / 2),
        medium = RoundedCornerShape(radius),
        large = RoundedCornerShape(radius * 1.5f)
    )

    CompositionLocalProvider(
        LocalCortexSpacing provides spacing,
        LocalCortexDimens provides dimens,
        LocalAppConfig provides appConfig
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}
