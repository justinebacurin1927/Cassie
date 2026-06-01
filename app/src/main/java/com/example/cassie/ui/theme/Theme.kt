package com.example.cassie.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════════
// CASSIE DESIGN SYSTEM — Single source of truth for all visual tokens
// ═══════════════════════════════════════════════════════════════════

// ── Colors ────────────────────────────────────────────────────────
@Stable
object CassieColors {
    val PurpleAccent  = Color(0xFFBB86FC)
    val PurpleDim     = PurpleAccent.copy(alpha = 0.6f)
    val PureBlack     = Color(0xFF000000)
    val DarkGrey      = Color(0xFF121212)
    val CardGrey      = Color(0xFF1E1E1E)
    val SurfaceGrey   = Color(0xFF282828)
    val GlassWhite    = Color.White.copy(alpha = 0.03f)
    val GlassBorder   = Color.White.copy(alpha = 0.10f)
    val TextPrimary   = Color.White
    val TextSecondary = Color.White.copy(alpha = 0.6f)
    // TextDim 0.35f failed WCAG AA on PureBlack (3.1:1). 0.55f gives
    // ~4.6:1 which clears the 4.5:1 threshold for body text.
    val TextDim       = Color.White.copy(alpha = 0.55f)
    val GreyIcon      = Color.White.copy(alpha = 0.55f)
    val RedAccent     = Color(0xFFCF6679)
    val GradientStart = Color(0xFF0F0020)
    val GradientMid   = Color(0xFF080010)
}

// ── Spacing Grid (8dp base) ────────────────────────────────────────
@Stable
object CassieSpacing {
    val xs  = 2.dp
    val sm  = 4.dp
    val md  = 8.dp
    val lg  = 12.dp
    val xl  = 16.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 48.dp
    val section = 28.dp
}

// ── Typography ─────────────────────────────────────────────────────
@Stable
object CassieTypography {
    val display = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = 0.5.sp,
    )
    val titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
    )
    val titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
    )
    val titleSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 3.sp,
    )
    val bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    )
    val bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
    )
    val caption = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.3.sp,
    )
    val label = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
    )
}

// ── Shapes ──────────────────────────────────────────────────────────
@Stable
object CassieShapes {
    val small  = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
    val medium = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
    val large  = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    val xlarge = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    val round  = androidx.compose.foundation.shape.CircleShape
}

// ── Material Color Schemes ──────────────────────────────────────────
private val DarkScheme = darkColorScheme(
    primary = CassieColors.PurpleAccent,
    onPrimary = CassieColors.PureBlack,
    secondary = CassieColors.PurpleAccent,
    onSecondary = CassieColors.PureBlack,
    background = CassieColors.PureBlack,
    surface = CassieColors.DarkGrey,
    surfaceContainerLow = CassieColors.CardGrey,
    surfaceContainer = CassieColors.SurfaceGrey,
    onBackground = CassieColors.TextPrimary,
    onSurface = CassieColors.TextPrimary,
    onSurfaceVariant = CassieColors.TextSecondary,
    outlineVariant = CassieColors.PurpleAccent.copy(alpha = 0.2f),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF7C4DFF),
    onPrimary = Color.White,
    secondary = Color(0xFFE040FB),
    onSecondary = Color.White,
    background = Color(0xFFF5F0FF),
    surface = Color.White,
    surfaceContainerLow = Color(0xFFF5F0FF),
    surfaceContainer = Color(0xFFEDE0FF),
    onBackground = Color(0xFF1A0033),
    onSurface = Color(0xFF1A0033),
    onSurfaceVariant = Color(0xFF4A3357),
    outlineVariant = CassieColors.PurpleAccent.copy(alpha = 0.2f),
)

@Composable
fun CassieTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        content = content
    )
}
