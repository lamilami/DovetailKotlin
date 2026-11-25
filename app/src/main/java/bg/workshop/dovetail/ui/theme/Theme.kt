package bg.workshop.dovetail.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Цветове за схемите и известията, които Material няма. */
@Immutable
data class WorkshopColors(
    val wood: Color,
    val woodEdge: Color,
    val woodAlt: Color,
    val metal: Color,
    val metalLight: Color,
    val axis: Color,
    val dimension: Color,
    val warning: Color,
    val onWarning: Color,
)

private val LightWorkshop = WorkshopColors(
    wood = Color(0xFFE6BE86),
    woodEdge = Color(0xFFA9783E),
    woodAlt = Color(0xFFD4A56A),
    metal = Color(0xFF5F7889),
    metalLight = Color(0xFFC9D5DE),
    axis = Color(0xFFD32F2F),
    dimension = Color(0xFF1565C0),
    warning = Color(0xFFFFE8BF),
    onWarning = Color(0xFF4A3000),
)

private val DarkWorkshop = WorkshopColors(
    wood = Color(0xFFB98A52),
    woodEdge = Color(0xFFE2B47C),
    woodAlt = Color(0xFF9A6E3C),
    metal = Color(0xFF9DB4C5),
    metalLight = Color(0xFF3A4752),
    axis = Color(0xFFFF6E6E),
    dimension = Color(0xFF82B6FF),
    warning = Color(0xFF4A3A12),
    onWarning = Color(0xFFFFDFA0),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF8A4B14),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCC0),
    onPrimaryContainer = Color(0xFF2E1500),
    secondary = Color(0xFF4E6577),
    background = Color(0xFFF8F3ED),
    surface = Color(0xFFF8F3ED),
    surfaceVariant = Color(0xFFEDE3D8),
    surfaceContainer = Color(0xFFF1E9E0),
    surfaceContainerHigh = Color(0xFFEBE1D6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF2B37A),
    onPrimary = Color(0xFF4A2800),
    primaryContainer = Color(0xFF6B3A0B),
    onPrimaryContainer = Color(0xFFFFDCC0),
    secondary = Color(0xFFA9C2D4),
    background = Color(0xFF16130F),
    surface = Color(0xFF16130F),
    surfaceVariant = Color(0xFF2E2821),
    surfaceContainer = Color(0xFF221D18),
    surfaceContainerHigh = Color(0xFF2B251F),
)

val LocalWorkshopColors = staticCompositionLocalOf { LightWorkshop }

@Composable
fun DovetailTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalWorkshopColors provides if (dark) DarkWorkshop else LightWorkshop) {
        MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
    }
}
