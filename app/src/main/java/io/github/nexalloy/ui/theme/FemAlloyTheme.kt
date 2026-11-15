package io.github.nexalloy.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Soft purple fallback palette used when Android's dynamic color is not available
 * (Android 11 and lower, or devices where Monet extraction is unavailable).
 */
private val SoftPurpleScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    inversePrimary = Color(0xFF6750A4),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceTint = Color(0xFFD0BCFF),
    inverseSurface = Color(0xFFE6E0E9),
    inverseOnSurface = Color(0xFF313033),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    scrim = Color.Black,
    surfaceBright = Color(0xFF3B383E),
    surfaceDim = Color(0xFF141218),
    surfaceContainerLowest = Color(0xFF0F0D13),
    surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B),
)

/**
 * The module only ships a single AMOLED theme: true black window background with
 * near-black elevated surfaces. Accent colors come from the wallpaper (dynamic color)
 * on Android 12+, otherwise from the soft purple palette.
 */
private fun ColorScheme.toAmoled(): ColorScheme = copy(
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF2A2A2A),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF222222),
    inverseSurface = Color(0xFFF5F5F5),
    inverseOnSurface = Color(0xFF202020),
)

@Composable
fun FemAlloyTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val baseScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        SoftPurpleScheme
    }
    val colorScheme = baseScheme.toAmoled()

    val view = LocalView.current
    SideEffect {
        val activity = view.context.findActivity() ?: return@SideEffect
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}

fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
