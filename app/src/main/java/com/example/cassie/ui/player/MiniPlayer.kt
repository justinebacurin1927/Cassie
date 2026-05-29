package com.example.cassie.ui.player

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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

// ── Palette ──
private val CardFill   = Color(0xFF1E1E1E)
private val ArtBg      = Color(0xFF2A2A2A)
private val BtnBg      = Color(0xFF333333)
private val TextWhite  = Color.White
private val TextSec    = Color.White.copy(alpha = 0.65f)
private val PurpleAcc  = Color(0xFFBB86FC)
private val AccDim     = PurpleAcc.copy(alpha = 0.5f)

private val Shape    = RoundedCornerShape(14.dp)
private val ArtShape = RoundedCornerShape(10.dp)

// ── Cava visualizer — animated bars ──────────────────────────────
@Composable
private fun CavaBars() {
    // 6 bars with staggered animation delays
    val barCount = 6
    val delays = listOf(0, 150, 300, 450, 200, 350)
    val anims = delays.map { delay ->
        rememberInfiniteTransition(label = "cava").animateFloat(
            initialValue = 0.15f, targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                tween(700, easing = FastOutSlowInEasing),
                RepeatMode.Reverse,
                StartOffset(delay),
            ),
            label = "bar",
        )
    }
    // Heights ramp up towards center
    val heights = listOf(12.dp, 24.dp, 36.dp, 36.dp, 24.dp, 12.dp)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        heights.forEachIndexed { i, h ->
            val s by anims[i]
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(h * s)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PurpleAcc)
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
            .height(60.dp)
            .clip(Shape)
            .background(CardFill)
            .clickable(onClick = onClick)
    ) {
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

        // Layer 2: Content with semi-transparent background
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(if (state.isPlaying) CardFill.copy(alpha = 0.70f) else CardFill)
                .padding(start = 8.dp, end = 6.dp),
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

            // Play / Pause
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(50))
                    .background(BtnBg)
                    .clickable { playbackManager.togglePlayPause() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = TextWhite,
                    modifier = Modifier.size(18.dp),
                )
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
