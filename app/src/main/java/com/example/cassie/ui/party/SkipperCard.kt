package com.example.cassie.ui.party

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.cassie.party.SkipperEngine
import com.example.cassie.party.SkipperLine
import com.example.cassie.party.SkipperMood

// ── Palette (matches MascotMoodCard) ──────────────────────────────
private val CardGrey      = Color(0xFF1E1E1E)
private val TextPrimary   = Color.White
private val TextDim       = Color.White.copy(alpha = 0.35f)
private val PurpleAccent  = Color(0xFFBB86FC)
private val TealAccent    = Color(0xFF03DAC5)

/**
 * The real Skipper card. Shows the current generated line, the
 * penguin mascot, and reacts to taps by asking the engine for a new
 * line. This replaces [SkipperDebugCard] in production builds.
 *
 * Visual structure:
 *  - Full-bleed dark card, rounded corners (matches MascotMoodCard)
 *  - Right side: penguin illustration (mood-driven)
 *  - Left side: small label (intent) + the line itself
 *  - Bottom-right: tiny "tap" hint so the user knows it's tappable
 */
@Composable
fun SkipperCard(
    modifier: Modifier = Modifier,
) {
    val line by SkipperEngine.currentLine.collectAsState()
    val patterns by SkipperEngine.currentPatterns.collectAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardGrey)
            .clickable { SkipperEngine.refreshLine() },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // ── Text area ─────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                // top label: intent + active pattern (if any)
                Row(verticalAlignment = Alignment.CenterVertically) {
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

                Spacer(Modifier.height(6.dp))

                // the actual line, with a crossfade when it changes
                AnimatedContent(
                    targetState = line?.text ?: "...",
                    transitionSpec = {
                        (fadeIn(tween(220)) togetherWith fadeOut(tween(180)))
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

        // ── tap hint at bottom-right ───────────────────────────────
        Text(
            text = "tap to refresh",
            color = TextDim,
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 110.dp, bottom = 6.dp),
        )
    }
}

// ── Mapping helpers (kept here so the UI is self-contained) ───────

private fun SkipperLine.intentLabel(): String = when (intent) {
    com.example.cassie.party.MascotIntent.OBSERVE -> "SKIPPER · noticing"
    com.example.cassie.party.MascotIntent.ROAST -> "SKIPPER · roasting"
    com.example.cassie.party.MascotIntent.PRAISE -> "SKIPPER · hyping"
    com.example.cassie.party.MascotIntent.CONFESS -> "SKIPPER · venting"
    com.example.cassie.party.MascotIntent.QUESTION -> "SKIPPER · asking"
}

private fun com.example.cassie.party.UserPatternType.shortName(): String = when (this) {
    com.example.cassie.party.UserPatternType.SKIPPER -> "skipper"
    com.example.cassie.party.UserPatternType.LOOPER -> "looper"
    com.example.cassie.party.UserPatternType.REPEATER -> "repeater"
    com.example.cassie.party.UserPatternType.MARATHONER -> "marathoner"
    com.example.cassie.party.UserPatternType.PARTIER -> "partier"
    com.example.cassie.party.UserPatternType.EXPLORER -> "explorer"
    com.example.cassie.party.UserPatternType.NIGHT_OWL -> "night owl"
    com.example.cassie.party.UserPatternType.FAVORITE_HOARDER -> "hoarder"
    com.example.cassie.party.UserPatternType.LYRICS_LOVER -> "lyric reader"
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
