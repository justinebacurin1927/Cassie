package com.example.cassie.ui.player

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.cassie.data.media.PlaybackManager
import com.example.cassie.ui.theme.CassieColors
import com.example.cassie.ui.theme.CassieShapes
import com.example.cassie.ui.theme.CassieSpacing

// ── Theme Tokens ──
private val CardFill   = CassieColors.CardGrey
private val ArtBg      = Color(0xFF2A2A2A)
private val TextWhite  = CassieColors.TextPrimary
private val TextSec    = CassieColors.TextSecondary
private val PurpleAcc  = CassieColors.PurpleAccent
private val AccDim     = CassieColors.PurpleDim

private val Shape    = CassieShapes.large
private val ArtShape = CassieShapes.medium

// ── Cava visualizer — animated wave ──────────────────────────────
@Composable
private fun CavaBars() {
    val barCount = 10
    val delays = listOf(0, 90, 180, 270, 360, 80, 170, 260, 350, 440)
    val anims = delays.map { delay ->
        rememberInfiniteTransition(label = "cava").animateFloat(
            initialValue = 0.1f, targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                tween(600, easing = FastOutSlowInEasing),
                RepeatMode.Reverse,
                StartOffset(delay),
            ),
            label = "bar",
        )
    }
    // Wave-shaped heights (rises to center, falls)
    val heights = listOf(8.dp, 16.dp, 26.dp, 34.dp, 38.dp, 38.dp, 34.dp, 26.dp, 16.dp, 8.dp)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        val barW = size.width / barCount
        val gap = barW * 0.2f
        val barWidth = barW - gap

        heights.forEachIndexed { i, h ->
            val s by anims[i]
            val barH = h.toPx() * s
            val x = i * barW + gap / 2
            val y = size.height - barH

            // Rounded-top wave bar with gradient glow
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PurpleAcc,
                        PurpleAcc.copy(alpha = 0.3f),
                    ),
                    startY = y,
                    endY = size.height,
                ),
                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                size = androidx.compose.ui.geometry.Size(barWidth, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2, barWidth / 2),
            )
        }
    }
}

// ── MiniPlayer ───────────────────────────────────────────────────
@Composable
fun MiniPlayer(
    playbackManager: PlaybackManager,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by playbackManager.playerState.collectAsState()
    val song = state.currentSong ?: return
    val context = LocalContext.current
    val canBlur = Build.VERSION.SDK_INT >= 31

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(Shape)
            .background(CardFill)
            .clickable(onClick = onClick)
    ) {
        // Layer 0: Glass base with subtle border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CassieColors.GlassWhite)
                .border(0.5.dp, CassieColors.GlassBorder, Shape)
        )

        // Layer 1: Cava visualizer (visible when playing)
        if (state.isPlaying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (canBlur) Modifier.blur(radius = 18.dp) else Modifier)
            ) {
                CavaBars()
            }
        }

        // Layer 2: Content with glass overlay
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(if (state.isPlaying) CardFill.copy(alpha = 0.55f) else CardFill)
                .padding(start = CassieSpacing.lg, end = CassieSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Album art
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(ArtShape)
                    .background(ArtBg),
                contentAlignment = Alignment.Center,
            ) {
                if (song.albumArtUri != null) {
                    AsyncImage(
                        model = remember(song.id) {
                            ImageRequest.Builder(context)
                                .data(song.albumArtUri).size(96)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build()
                        },
                        contentDescription = "Album Art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(Icons.Default.MusicNote, null,
                        tint = AccDim, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.width(10.dp))

            // Track info
            Column(Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    color = TextSec,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(4.dp))

            // Shuffle
            IconButton(
                onClick = { playbackManager.toggleShuffle() },
                modifier = Modifier.size(26.dp),
            ) {
                Icon(Icons.Default.Shuffle,
                    tint = if (state.shuffleMode) PurpleAcc else TextSec,
                    contentDescription = "Shuffle",
                    modifier = Modifier.size(18.dp))
            }

            Spacer(Modifier.width(2.dp))

            // Repeat
            IconButton(
                onClick = { playbackManager.cycleRepeat() },
                modifier = Modifier.size(26.dp),
            ) {
                val repeatIcon = when (state.repeatMode) {
                    Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                }
                Icon(repeatIcon,
                    tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) PurpleAcc else TextSec,
                    contentDescription = "Repeat",
                    modifier = Modifier.size(18.dp))
            }

            Spacer(Modifier.width(2.dp))

            // Play / Pause (smooth tween scale — was bouncy spring)
            val miniPlayScale by animateFloatAsState(
                targetValue = if (state.isPlaying) 1f else 0.9f,
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                label = "miniPlay"
            )
            Surface(
                onClick = { playbackManager.togglePlayPause() },
                shape = RoundedCornerShape(50),
                color = PurpleAcc,
                tonalElevation = 4.dp,
                modifier = Modifier.size(34.dp).graphicsLayer {
                    scaleX = miniPlayScale
                    scaleY = miniPlayScale
                },
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.width(2.dp))

            // Next
            IconButton(
                onClick = { playbackManager.skipToNext() },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(Icons.Default.SkipNext, "Next",
                    tint = TextSec, modifier = Modifier.size(20.dp))
            }
        }
    }
}
