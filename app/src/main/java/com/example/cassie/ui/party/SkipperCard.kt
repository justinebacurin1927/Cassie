package com.example.cassie.ui.party

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cassie.R
import com.example.cassie.party.MascotIntent
import com.example.cassie.party.SkipperEngine
import com.example.cassie.party.SkipperLine
import com.example.cassie.party.SkipperMood
import com.example.cassie.party.UserPatternType

// ── Palette (matches the rest of the app) ────────────────────────
private val CardGrey      = Color(0xFF1E1E1E)
private val TextPrimary   = Color.White
private val TextDim       = Color.White.copy(alpha = 0.35f)
private val PurpleAccent  = Color(0xFFBB86FC)
private val TealAccent    = Color(0xFF03DAC5)
private val LivePink      = Color(0xFFFF4081)

/**
 * The live Skipper card.
 *
 * Auto-updates every 6 seconds (driven by [SkipperEngine]'s
 * rotation coroutine) and also reacts to user behavior events
 * (skips, loops, party mode, etc.) immediately.
 *
 * NOT clickable. The card is a passive display — the user doesn't
 * need to do anything. Skipper just keeps talking.
 *
 * Visual structure:
 *  - Full-bleed dark card, rounded corners
 *  - Right side: penguin illustration (mood-driven)
 *  - Left side: live-dot · intent label · pattern tag · the line
 *  - The line crossfades when it changes
 *  - A small pulsing "live" dot in the top-left to communicate
 *    that this is a feed, not a one-shot quote
 */
@Composable
fun SkipperCard(
    modifier: Modifier = Modifier,
) {
    val line by SkipperEngine.currentLine.collectAsState()
    val patterns by SkipperEngine.currentPatterns.collectAsState()

    // Live pulse animation for the "live" dot
    val infinite = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardGrey),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // ── Text area ─────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                // Top row: live dot + intent label + pattern tag
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Live indicator dot (pulsing)
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(LivePink.copy(alpha = pulseAlpha)),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = line?.intentLabel() ?: "SKIPPER",
                        color = PurpleAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                    if (patterns.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "· ${patterns.first().type.shortName()}",
                            color = TealAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // The actual line, with a crossfade when it changes
                AnimatedContent(
                    targetState = line?.text ?: "...",
                    transitionSpec = {
                        (fadeIn(tween(280)) togetherWith fadeOut(tween(220)))
                    },
                    label = "skipper_line_swap",
                    modifier = Modifier.fillMaxWidth(),
                ) { text ->
                    Text(
                        text = text,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                }
            }

            // ── Penguin illustration ──────────────────────────────
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(
                        id = line?.mood?.penguinDrawable() ?: R.drawable.greeting_penguin
                    ),
                    contentDescription = "Skipper the penguin",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

// ── Mapping helpers (kept here so the UI is self-contained) ───────

private fun SkipperLine.intentLabel(): String = when (intent) {
    MascotIntent.OBSERVE -> "SKIPPER · noticing"
    MascotIntent.ROAST -> "SKIPPER · roasting"
    MascotIntent.PRAISE -> "SKIPPER · hyping"
    MascotIntent.CONFESS -> "SKIPPER · thinking"
    MascotIntent.QUESTION -> "SKIPPER · asking"
}

private fun UserPatternType.shortName(): String = when (this) {
    UserPatternType.SKIPPER -> "skipper"
    UserPatternType.LOOPER -> "looper"
    UserPatternType.REPEATER -> "repeater"
    UserPatternType.MARATHONER -> "marathoner"
    UserPatternType.PARTIER -> "partier"
    UserPatternType.EXPLORER -> "explorer"
    UserPatternType.NIGHT_OWL -> "night owl"
    UserPatternType.FAVORITE_HOARDER -> "hoarder"
    UserPatternType.LYRICS_LOVER -> "lyric reader"
}

private fun SkipperMood.penguinDrawable(): Int = when (this) {
    SkipperMood.WHATEVER -> R.drawable.greeting_penguin
    SkipperMood.NOSY -> R.drawable.greeting_penguin
    SkipperMood.HYPED -> R.drawable.starry_penguin
    SkipperMood.CHILL -> R.drawable.chill_penguin
    SkipperMood.MUSED -> R.drawable.love_penguin
    SkipperMood.DRAMATIC -> R.drawable.sad_penguin
    SkipperMood.EEPY -> R.drawable.sleepy_penguin
    SkipperMood.WITTY -> R.drawable.chill_penguin
    SkipperMood.CHAOTIC -> R.drawable.starry_penguin
}
