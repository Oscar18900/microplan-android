package mx.buap.fcc.microplan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class MicroPlanColors(
  val isDark: Boolean,
  val background: Color,
  val surface: Color,
  val surfaceSubtle: Color,
  val border: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val primaryBlue: Color,
  val primaryBlueSoft: Color,
  val successGreen: Color,
  val successGreenSoft: Color,
  val tealProtection: Color,
  val tealSoft: Color,
  val tealPill: Color,
  val tealBorder: Color,
  val amberWarning: Color,
  val amberSoft: Color,
  val dangerRed: Color,
  val dangerSoft: Color,
  val dangerBorder: Color
)

private val LightMicroPlanColors = MicroPlanColors(
  isDark = false,
  background = Color(0xFFF8FAFC),
  surface = Color(0xFFFFFFFF),
  surfaceSubtle = Color(0xFFF1F5F9),
  border = Color(0xFFE2E8F0),
  textPrimary = Color(0xFF0F172A),
  textSecondary = Color(0xFF64748B),
  primaryBlue = BrandPalette.PrimaryBlue,
  primaryBlueSoft = BrandPalette.PrimaryBlueSoftLight,
  successGreen = BrandPalette.SuccessGreen,
  successGreenSoft = BrandPalette.SuccessGreenSoftLight,
  tealProtection = BrandPalette.TealProtection,
  tealSoft = BrandPalette.TealSoftLight,
  tealPill = BrandPalette.TealPillLight,
  tealBorder = BrandPalette.TealBorderLight,
  amberWarning = BrandPalette.AmberWarning,
  amberSoft = BrandPalette.AmberSoftLight,
  dangerRed = BrandPalette.DangerRed,
  dangerSoft = BrandPalette.DangerSoftLight,
  dangerBorder = BrandPalette.DangerBorderLight
)

private val DarkMicroPlanColors = MicroPlanColors(
  isDark = true,
  background = Color(0xFF0B1120),
  surface = Color(0xFF111827),
  surfaceSubtle = Color(0xFF1E293B),
  border = Color(0xFF293548),
  textPrimary = Color(0xFFF1F5F9),
  textSecondary = Color(0xFF94A3B8),
  primaryBlue = Color(0xFF3B82F6),
  primaryBlueSoft = BrandPalette.PrimaryBlueSoftDark,
  successGreen = BrandPalette.SuccessGreenDark,
  successGreenSoft = BrandPalette.SuccessGreenSoftDark,
  tealProtection = BrandPalette.TealProtectionDark,
  tealSoft = BrandPalette.TealSoftDark,
  tealPill = BrandPalette.TealPillDark,
  tealBorder = BrandPalette.TealBorderDark,
  amberWarning = BrandPalette.AmberWarningDark,
  amberSoft = BrandPalette.AmberSoftDark,
  dangerRed = BrandPalette.DangerRedDark,
  dangerSoft = BrandPalette.DangerSoftDark,
  dangerBorder = BrandPalette.DangerBorderDark
)

val LocalMicroPlanColors = staticCompositionLocalOf { LightMicroPlanColors }

private val LightMaterialColors = lightColorScheme(
  primary = BrandPalette.PrimaryBlue,
  onPrimary = Color.White,
  secondary = BrandPalette.TealProtection,
  onSecondary = Color.White,
  background = Color(0xFFF8FAFC),
  onBackground = Color(0xFF0F172A),
  surface = Color(0xFFFFFFFF),
  onSurface = Color(0xFF0F172A),
  surfaceVariant = Color(0xFFF1F5F9),
  onSurfaceVariant = Color(0xFF64748B),
  outline = Color(0xFFE2E8F0),
  error = BrandPalette.DangerRed
)

private val DarkMaterialColors = darkColorScheme(
  primary = Color(0xFF3B82F6),
  onPrimary = Color.White,
  secondary = BrandPalette.TealProtectionDark,
  onSecondary = Color(0xFF0B1120),
  background = Color(0xFF0B1120),
  onBackground = Color(0xFFF1F5F9),
  surface = Color(0xFF111827),
  onSurface = Color(0xFFF1F5F9),
  surfaceVariant = Color(0xFF1E293B),
  onSurfaceVariant = Color(0xFF94A3B8),
  outline = Color(0xFF293548),
  error = BrandPalette.DangerRedDark
)

@Composable
fun MicroPlanTheme(
  darkTheme: Boolean = false,
  content: @Composable () -> Unit
) {
  val customColors = if (darkTheme) DarkMicroPlanColors else LightMicroPlanColors
  val materialColors = if (darkTheme) DarkMaterialColors else LightMaterialColors

  CompositionLocalProvider(LocalMicroPlanColors provides customColors) {
    MaterialTheme(
      colorScheme = materialColors,
      typography = MicroPlanTypography,
      content = content
    )
  }
}

object MicroPlanTheme {
  val colors: MicroPlanColors
    @Composable get() = LocalMicroPlanColors.current
}
