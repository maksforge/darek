package app.darek.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// GENERATED — do not hand-edit individual roles.
//
// Both schemes are derived from four brand seeds by Material's tonal palette
// construction (CAM16/HCT): the seeds are the judgement, the ~36 roles are
// arithmetic. Hand-picking a role breaks the fixed tonal relationship it holds
// with the rest of its family, and the failure shows up as a contrast bug on one
// screen out of thirty rather than as a visible mistake here.
//
//   primary   #2B5FA8  — brand: filled buttons, FAB, selected tab
//   secondary #5C6878  — quiet containers: chips, tonal buttons, selected segment
//   tertiary  #C8892B  — accents and caution markers, used sparingly
//   neutral   #DFE2E8  — seeds every surface, so it decides warm or cool overall
//
// Error stays M3 baseline (hue 25, chroma 84): a safety signal, not a brand choice.
//
// To change the palette, edit the seeds in scripts/gen_scheme.py and re-run it.
// Colours come from MaterialTheme, never literals, and dynamic colour defaults
// off (ADR-0006) so this file is what a user actually sees.

private val AppLightColors =
    lightColorScheme(
        primary = Color(0xFF2A5EA7),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD5E3FF),
        onPrimaryContainer = Color(0xFF001B3F),
        inversePrimary = Color(0xFFA8C7FF),
        secondary = Color(0xFF535F6F),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD7E3F6),
        onSecondaryContainer = Color(0xFF101C29),
        tertiary = Color(0xFF855400),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDDB3),
        onTertiaryContainer = Color(0xFF2A1700),
        error = Color(0xFFBA1B1B),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD4),
        onErrorContainer = Color(0xFF410001),
        background = Color(0xFFF6F9FF),
        onBackground = Color(0xFF181C20),
        surface = Color(0xFFF6F9FF),
        onSurface = Color(0xFF181C20),
        surfaceVariant = Color(0xFFDEE3EB),
        onSurfaceVariant = Color(0xFF41474D),
        surfaceDim = Color(0xFFD7DAE0),
        surfaceBright = Color(0xFFF6F9FF),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF1F4FA),
        surfaceContainer = Color(0xFFEBEEF4),
        surfaceContainerHigh = Color(0xFFE5E8EE),
        surfaceContainerHighest = Color(0xFFDFE2E8),
        inverseSurface = Color(0xFF2D3136),
        inverseOnSurface = Color(0xFFEEF1F7),
        outline = Color(0xFF72787F),
        outlineVariant = Color(0xFFC2C7CE),
        scrim = Color(0xFF000000),
    )

private val AppDarkColors =
    darkColorScheme(
        primary = Color(0xFFA8C7FF),
        onPrimary = Color(0xFF002F66),
        primaryContainer = Color(0xFF00468D),
        onPrimaryContainer = Color(0xFFD5E3FF),
        inversePrimary = Color(0xFF2A5EA7),
        secondary = Color(0xFFBBC7DA),
        onSecondary = Color(0xFF25313F),
        secondaryContainer = Color(0xFF3C4857),
        onSecondaryContainer = Color(0xFFD7E3F6),
        tertiary = Color(0xFFFFB958),
        onTertiary = Color(0xFF462A00),
        tertiaryContainer = Color(0xFF643F00),
        onTertiaryContainer = Color(0xFFFFDDB3),
        error = Color(0xFFFFB4A9),
        onError = Color(0xFF680003),
        errorContainer = Color(0xFF930006),
        onErrorContainer = Color(0xFFFFDAD4),
        background = Color(0xFF101418),
        onBackground = Color(0xFFDFE2E8),
        surface = Color(0xFF101418),
        onSurface = Color(0xFFDFE2E8),
        surfaceVariant = Color(0xFF41474D),
        onSurfaceVariant = Color(0xFFC2C7CE),
        surfaceDim = Color(0xFF101418),
        surfaceBright = Color(0xFF363A3E),
        surfaceContainerLowest = Color(0xFF0B0F13),
        surfaceContainerLow = Color(0xFF181C20),
        surfaceContainer = Color(0xFF1C2024),
        surfaceContainerHigh = Color(0xFF262A2E),
        surfaceContainerHighest = Color(0xFF31353A),
        inverseSurface = Color(0xFFDFE2E8),
        inverseOnSurface = Color(0xFF2D3136),
        outline = Color(0xFF8C9198),
        outlineVariant = Color(0xFF41474D),
        scrim = Color(0xFF000000),
    )

internal val LightColors = AppLightColors
internal val DarkColors = AppDarkColors
