package com.example.cassie.ui.player

import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import android.os.Build
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.cassie.data.media.EqualizerManager
import com.example.cassie.data.media.LyricsRepository
import com.example.cassie.data.media.PersistenceManager
import com.example.cassie.data.media.PlaybackManager
import com.example.cassie.data.media.Song
import com.example.cassie.data.media.AlbumArtColorExtractor
import com.example.cassie.data.media.AlbumArtColors
import com.example.cassie.data.media.TimedLyricLine
import com.example.cassie.data.media.parseLrc
import com.example.cassie.ui.theme.CassieColors
import com.example.cassie.ui.theme.CassieDialog
import com.example.cassie.ui.theme.CassieSpacing
import com.example.cassie.ui.theme.CassieTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

// ── Palette (legacy alias — will migrate fully) ────────────────────
private val PureBlack     = CassieColors.PureBlack
private val DarkGrey      = CassieColors.DarkGrey
private val CardGrey      = CassieColors.CardGrey
private val SurfaceGrey   = CassieColors.SurfaceGrey
private val PurpleAccent  = CassieColors.PurpleAccent
private val TextPrimary   = CassieColors.TextPrimary
private val TextSecondary = CassieColors.TextSecondary
private val TextDim       = CassieColors.TextDim

// ── File-scope constants (avoid per-frame allocation) ──────────────
private val LyricsLineWidths = listOf(0.85f, 0.6f, 0.75f, 0.5f, 0.8f, 0.55f)

@Composable
fun NowPlayingScreen(
    playbackManager: PlaybackManager,
    equalizerManager: EqualizerManager? = null,
    onClose: () -> Unit,
) {
    val state by playbackManager.playerState.collectAsState()
    val context = LocalContext.current

    // ── entrance animation ──────────────────────────────────────────
    // Drives translateY from +60dp → 0 and alpha 0 → 1 on first composition
    val offsetY = remember { Animatable(60f) }
    val alpha   = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            offsetY.animateTo(
                targetValue = 0f,
                // Was a medium-bouncy spring — felt 'hanging' on first
                // open. Replaced with a slow smooth tween.
                animationSpec = tween(
                    durationMillis = 360,
                    easing         = FastOutSlowInEasing,
                )
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 320,
                    easing         = FastOutSlowInEasing,
                )
            )
        }
    }

    // live position
    var position by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(250)
            position = playbackManager.getCurrentPosition()
        }
    }

    val duration = state.duration.coerceAtLeast(1L)
    val song     = state.currentSong

    // ── dynamic album art colors ──
    var albumColors by remember(song?.albumId) { mutableStateOf(AlbumArtColors()) }
    LaunchedEffect(song?.albumId, song?.albumArtUri) {
        albumColors = AlbumArtColorExtractor.extract(context, song?.albumArtUri, song?.albumId ?: -1L)
    }
    val accentColor by remember { derivedStateOf { albumColors.accent } }

    // ── attach equalizer when a song is playing ──
    LaunchedEffect(state.currentSong) {
        if (state.currentSong != null) {
            val id = playbackManager.getAudioSessionId()
            if (id > 0) equalizerManager?.attach(id)
        }
    }

    // ── lyrics state ────────────────────────────────────────────────
    var showLyrics       by remember { mutableStateOf(false) }
    var lyricsText       by remember { mutableStateOf<String?>(null) }
    var syncedLines      by remember { mutableStateOf<List<TimedLyricLine>>(emptyList()) }
    var loadingLyrics    by remember { mutableStateOf(false) }
    var lyricsError      by remember { mutableStateOf(false) }
    var lyricsAttempted  by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    /** Fetch lyrics with timeout and proper error handling. */
    val fetchLyrics: suspend () -> Unit = fetchLyrics@{
        if (!loadingLyrics) {
            // Re-read the current song inside the lambda — the captured
            // `song` at composition time may be stale by the time the
            // user taps Retry, and `song!!` would NPE.
            val s = state.currentSong
            if (s == null) {
                lyricsError = true
                lyricsAttempted = true
                return@fetchLyrics
            }
            loadingLyrics = true
            lyricsError = false
            try {
                val result = withTimeout(10_000L) {
                    val prefs = PersistenceManager(context)
                    LyricsRepository.fetchLyrics(
                        artist = s.artist,
                        title = s.title,
                        songFilePath = s.filePath,
                        prefs = prefs,
                    )
                }
                lyricsText = result?.plainLyrics
                syncedLines = if (result?.syncedLyrics != null) parseLrc(result.syncedLyrics) else emptyList()
                lyricsAttempted = true
                lyricsError = false
            } catch (_: Exception) {
                lyricsText = null
                syncedLines = emptyList()
                lyricsError = true
                lyricsAttempted = true
            } finally {
                loadingLyrics = false
            }
        }
    }

    // ── queue dialog ────────────────────────────────────────────────
    var showQueue by remember { mutableStateOf(false) }
    if (showQueue) {
        CassieDialog(
            onDismissRequest = { showQueue = false },
            dialogTitle = { Text("Up Next", fontWeight = FontWeight.Bold, color = TextPrimary) },
            dialogText  = {
                val q = state.queue
                if (q.isEmpty()) {
                    Text("Queue is empty", color = TextDim, modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    LazyColumn(
                        Modifier.heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(q.size) { idx ->
                            val qSong = q[idx]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("${idx + 1}.", color = TextDim, fontSize = 13.sp, modifier = Modifier.width(28.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(qSong.title,  color = TextPrimary,   fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(qSong.artist, color = TextDim,       fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(onClick = { playbackManager.removeFromQueue(idx) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, "Remove", tint = TextDim, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            dialogConfirmButton = {
                TextButton(onClick = { showQueue = false }) { Text("Close", color = PurpleAccent) }
            }
        )
    }

    // ── sleep timer state ───────────────────────────────────────────
    var showSleepPicker by remember { mutableStateOf(false) }

    // ── equalizer state ─────────────────────────────────────────────
    var showEqualizer  by remember { mutableStateOf(false) }
    val eqPresets      = remember { equalizerManager?.getPresetNames() ?: emptyList() }
    var selectedPreset by remember { mutableIntStateOf(equalizerManager?.currentPreset?.toInt() ?: -1) }
    var bassBoostOn    by remember { mutableStateOf(equalizerManager?.isBassBoostEnabled ?: false) }

    if (showSleepPicker) {
        CassieDialog(
            onDismissRequest = { showSleepPicker = false },
            dialogTitle = { Text("Sleep Timer", fontWeight = FontWeight.Bold, color = TextPrimary) },
            dialogText  = {
                Column {
                    listOf(15, 30, 45, 60).forEach { mins ->
                        TextButton(
                            onClick  = { playbackManager.setSleepTimer(mins); showSleepPicker = false },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("$mins minutes", color = TextSecondary, fontSize = 15.sp)
                        }
                    }
                    if (state.sleepTimerRemainingSec > 0) {
                        HorizontalDivider(color = SurfaceGrey, modifier = Modifier.padding(vertical = 8.dp))
                        TextButton(
                            onClick  = { playbackManager.cancelSleepTimer(); showSleepPicker = false },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Cancel Timer", color = PurpleAccent, fontSize = 15.sp)
                        }
                    }
                }
            },
            dialogConfirmButton = {
                TextButton(onClick = { showSleepPicker = false }) { Text("Close", color = PurpleAccent) }
            }
        )
    }

    if (showEqualizer && equalizerManager != null) {
        CassieDialog(
            onDismissRequest = { showEqualizer = false },
            dialogTitle = { Text("Equalizer", fontWeight = FontWeight.Bold, color = TextPrimary) },
            dialogText  = {
                Column {
                    if (eqPresets.isEmpty()) {
                        Text("Equalizer not available on this device", color = TextDim, fontSize = 14.sp)
                    } else {
                        Text("Preset", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        eqPresets.forEachIndexed { idx, name ->
                            val isSelected = selectedPreset == idx
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) PurpleAccent.copy(0.2f) else Color.Transparent)
                                    .clickable { selectedPreset = idx; equalizerManager.setPreset(idx.toShort()) }
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(name, color = if (isSelected) PurpleAccent else TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                if (isSelected) Icon(Icons.Default.Check, null, tint = PurpleAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = SurfaceGrey)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Bass Boost", color = TextSecondary, fontSize = 14.sp)
                            Switch(
                                checked        = bassBoostOn,
                                onCheckedChange = { bassBoostOn = it; equalizerManager.setBassBoostEnabled(it) },
                                colors         = SwitchDefaults.colors(checkedTrackColor = PurpleAccent),
                            )
                        }
                    }
                }
            },
            dialogConfirmButton = {
                TextButton(onClick = { showEqualizer = false }) { Text("Done", color = PurpleAccent) }
            }
        )
    }

    // ── root — entrance transform applied here ──────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = offsetY.value.dp.toPx()
                this.alpha   = alpha.value
            }
    ) {
        // ── immersive background: album art → blur → gradient overlay ─
        if (song?.albumArtUri != null) {
            AsyncImage(
                model = remember(song.id, song.albumArtUri) {
                    ImageRequest.Builder(context).data(song.albumArtUri).size(1080)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .crossfade(true).build()
                },
                contentDescription = null,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = 1.4f; scaleY = 1.4f
                    if (Build.VERSION.SDK_INT >= 31) {
                        renderEffect = android.graphics.RenderEffect
                            .createBlurEffect(60f, 60f, android.graphics.Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
                    }
                },
                contentScale = ContentScale.Crop,
            )
        }
        // Black gradient overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PureBlack.copy(alpha = 0.35f),
                            PureBlack.copy(alpha = 0.55f),
                            PureBlack.copy(alpha = 0.75f),
                            PureBlack,
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(36.dp))

            // ── header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.KeyboardArrowDown, "Minimize", tint = TextPrimary, modifier = Modifier.size(28.dp))
                }
                Text("NOW PLAYING", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Row {
                    IconButton(onClick = {
                        showLyrics = !showLyrics
                        if (showLyrics && (lyricsText == null || lyricsError) && !loadingLyrics && song != null) {
                            scope.launch { fetchLyrics() }
                        }
                    }) {
                        Icon(
                            if (showLyrics) Icons.Default.MusicNote else Icons.Default.Lyrics,
                            "Lyrics",
                            tint     = if (showLyrics) PurpleAccent else TextDim,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    if (equalizerManager != null) {
                        IconButton(onClick = { showEqualizer = true }) {
                            Icon(Icons.Default.Tune, "Equalizer", tint = TextDim, modifier = Modifier.size(24.dp))
                        }
                    }
                    IconButton(onClick = { showQueue = true }) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, "Queue", tint = TextDim, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── album art ──
            AnimatedContent(
                targetState = song?.id,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith
                        fadeOut(animationSpec = tween(400))
                },
                label = "albumArt"
            ) { songId ->
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardGrey)
                        .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    val currentSong = song
                    if (currentSong?.albumArtUri != null && songId != null) {
                        AsyncImage(
                            model = remember(songId) {
                                ImageRequest.Builder(context).data(currentSong.albumArtUri).size(560)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .crossfade(true).build()
                            },
                            contentDescription = "Album Art",
                            modifier           = Modifier.fillMaxSize(),
                            contentScale       = ContentScale.Crop,
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(listOf(accentColor.copy(0.3f), accentColor.copy(0.1f)))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(80.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── song info ──
            Text(
                song?.title ?: "No Track",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                textAlign  = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                song?.artist ?: "Unknown Artist",
                fontSize  = 15.sp,
                color     = TextSecondary,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))

            // ── seekbar (own composable so the 4Hz position tick only
            //    recomposes the seekbar; the rest of NowPlayingScreen
            //    is skipped by Compose's structural equality) ──
            SeekBar(
                position = position,
                duration = duration,
                accentColor = accentColor,
                onSeek = { playbackManager.seekTo(it) },
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 2.dp, end = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(formatTime(position), color = TextSecondary.copy(alpha = 0.6f), style = CassieTypography.caption)
                Text(formatTime(duration), color = TextSecondary.copy(alpha = 0.6f), style = CassieTypography.caption)
            }

            Spacer(Modifier.height(28.dp))

            // ── playback controls ──
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    IconButton(onClick = { playbackManager.toggleShuffle() }) {
                        Icon(Icons.Default.Shuffle, "Shuffle",
                            tint     = if (state.shuffleMode) PurpleAccent else TextDim,
                            modifier = Modifier.size(24.dp))
                    }
                    if (state.shuffleMode) {
                        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp).size(4.dp).clip(CircleShape).background(PurpleAccent))
                    }
                }
                // ── micro-animated skip previous ──
                IconButton(
                    onClick = { playbackManager.skipToPrevious() },
                    modifier = Modifier.size(42.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    // haptic-like visual press feedback
                                    awaitRelease()
                                }
                            )
                        }
                ) {
                    Icon(Icons.Default.SkipPrevious, "Previous", tint = TextPrimary.copy(alpha = 0.85f), modifier = Modifier.size(28.dp))
                }
                // ── clean play/pause button ──
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                        .clickable { playbackManager.togglePlayPause() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp),
                    )
                }
                // ── micro-animated skip next ──
                IconButton(
                    onClick = { playbackManager.skipToNext() },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(Icons.Default.SkipNext, "Next", tint = TextPrimary.copy(alpha = 0.85f), modifier = Modifier.size(28.dp))
                }
                Box(contentAlignment = Alignment.Center) {
                    IconButton(onClick = { playbackManager.cycleRepeat() }) {
                        Icon(
                            when (state.repeatMode) {
                                Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                                else                   -> Icons.Default.Repeat
                            },
                            "Repeat",
                            tint     = if (state.repeatMode != Player.REPEAT_MODE_OFF) PurpleAccent else TextDim,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    if (state.repeatMode != Player.REPEAT_MODE_OFF) {
                        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp).size(4.dp).clip(CircleShape).background(PurpleAccent))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── sleep timer row ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardGrey)
                    .clickable { showSleepPicker = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text("Sleep Timer", color = TextSecondary, fontSize = 14.sp)
                if (state.sleepTimerRemainingSec > 0) {
                    val mins = state.sleepTimerRemainingSec / 60
                    val secs = state.sleepTimerRemainingSec % 60
                    Text("${mins}:%02d".format(secs), color = PurpleAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Timer, null, tint = TextDim, modifier = Modifier.size(20.dp))
                }
            }

            // ── lyrics (Spotify-style synced) ──
            if (showLyrics) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp, max = 320.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    CardGrey,
                                    accentColor.copy(alpha = 0.18f),
                                    accentColor.copy(alpha = 0.08f),
                                )
                            )
                        )
                        .shadow(20.dp, RoundedCornerShape(16.dp), spotColor = accentColor.copy(alpha = 0.35f))
                        .padding(vertical = 12.dp),
                ) {
                    when {
                        // ── loading: animated skeleton ──
                        loadingLyrics -> LyricsSkeleton()
                        // ── synced karaoke ──
                        syncedLines.isNotEmpty() -> SyncedLyricsDisplay(
                            timedLines = syncedLines,
                            currentPositionMs = position,
                            isPlaying = state.isPlaying,
                            accentColor = accentColor,
                            modifier = Modifier.fillMaxSize(),
                        )
                        // ── plain lyrics available (no timed version) ──
                        !lyricsText.isNullOrBlank() -> Box(
                            Modifier.fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.TopStart,
                        ) {
                            Text(
                                text = lyricsText!!,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                            )
                        }
                        // ── error / retry ──
                        lyricsError -> Column(
                            Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text("Couldn't load lyrics", color = TextDim, fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { scope.launch { fetchLyrics() } }) {
                                Text("Retry", color = PurpleAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                        // ── no lyrics found (clean attempt + no result) ──
                        lyricsAttempted -> Text("No lyrics found", color = TextDim, fontSize = 14.sp, modifier = Modifier.align(Alignment.Center).padding(16.dp))
                        // ── fallback (shouldn't happen) ──
                        else -> Text("No lyrics found", color = TextDim, fontSize = 14.sp, modifier = Modifier.align(Alignment.Center).padding(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Seekbar (extracted so the 4Hz position tick doesn't recompose
//    the whole player) ───────────────────────────────────────────
@Composable
private fun SeekBar(
    position: Long,
    duration: Long,
    accentColor: Color,
    onSeek: (Long) -> Unit,
) {
    val safeDuration = duration.coerceAtLeast(1L)
    val progress = (position.toFloat() / safeDuration).coerceIn(0f, 1f)
    val isDragging = remember { mutableStateOf(false) }
    val animProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(150, easing = LinearEasing),
        label = "seek",
    )

    // Hoist allocations out of the per-frame Canvas draw.
    val fillBrush = remember(accentColor) {
        Brush.horizontalGradient(listOf(accentColor, accentColor.copy(alpha = 0.6f)))
    }
    val density = LocalDensity.current
    val trackCornerRadius = remember(density) {
        with(density) {
            androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
        }
    }
    val thumbRadius: Float = with(density) { 10.dp.toPx() }
    val trackInnerR: Float = with(density) { 10.dp.toPx() * 0.35f }
    val trackH: Float = with(density) { 4.dp.toPx() }
    val trackTopOffset: Float = with(density) { 2.dp.toPx() }

    Column(Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .pointerInput(safeDuration) {
                    detectTapGestures { offset ->
                        onSeek(((offset.x / size.width) * safeDuration).toLong())
                    }
                }
                .pointerInput(safeDuration) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        isDragging.value = true
                        onSeek(((change.position.x / size.width) * safeDuration).toLong())
                    }
                }
        ) {
            val trackTop = size.height / 2 - trackTopOffset
            val fillEnd = animProgress * size.width

            // Track background
            drawRoundRect(
                color = TextDim.copy(alpha = 0.15f),
                topLeft = androidx.compose.ui.geometry.Offset(0f, trackTop),
                size = androidx.compose.ui.geometry.Size(size.width, trackH),
                cornerRadius = trackCornerRadius,
            )
            // Gradient fill
            drawRoundRect(
                brush = fillBrush,
                topLeft = androidx.compose.ui.geometry.Offset(0f, trackTop),
                size = androidx.compose.ui.geometry.Size(fillEnd, trackH),
                cornerRadius = trackCornerRadius,
            )
            // Thumb
            if (isDragging.value) {
                drawCircle(
                    color = accentColor,
                    radius = thumbRadius,
                    center = androidx.compose.ui.geometry.Offset(fillEnd, size.height / 2),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.5f),
                    radius = trackInnerR,
                    center = androidx.compose.ui.geometry.Offset(fillEnd, size.height / 2),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 2.dp, end = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatTime(position), color = TextSecondary.copy(alpha = 0.6f), style = CassieTypography.caption)
            Text(formatTime(duration), color = TextSecondary.copy(alpha = 0.6f), style = CassieTypography.caption)
        }
    }
}
@Composable
private fun LyricsSkeleton(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "skeletonPulse")
    val alpha by infinite.animateFloat(
        initialValue = 0.2f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // simulate 6 lyric lines at varying widths.
        // Widths are file-scope (allocated once, not per frame).
        LyricsLineWidths.forEachIndexed { idx, w ->
            val isDim = idx < 2 || idx > 3
            Box(
                modifier = Modifier
                    .fillMaxWidth(w)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(TextPrimary.copy(alpha = alpha * if (isDim) 0.5f else 1f))
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}