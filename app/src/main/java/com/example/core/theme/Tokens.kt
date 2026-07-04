package com.example.core.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.config.AppConfig

data class CortexSpacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp
)

data class CortexDimens(
    val iconSize: Dp = 24.dp,
    val cornerRadius: Dp = 12.dp
)

val LocalCortexSpacing = staticCompositionLocalOf { CortexSpacing() }
val LocalCortexDimens = staticCompositionLocalOf { CortexDimens() }
val LocalAppConfig = staticCompositionLocalOf { AppConfig() }
