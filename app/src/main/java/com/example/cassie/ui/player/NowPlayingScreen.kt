package com.example.cassie.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.cassie.data.media.EqualizerManager
import com.example.cassie.data.media.LyricsRepository
import com.example.cassie.data.media.PlaybackManager
import com.example.cassie.data.media.Song
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Palette ────────────────────────────────────────────────────────
private val PureBlack    = Color(0xFF000000)
private val DarkGrey     = Color(0xFF121212)
private val CardGrey     = Color(0xFF1E1E1E)
private val SurfaceGrey  = Color(0xFF282828)
private val PurpleAccent = Color(0xFFBB86FC)
private val TextPrimary  = Color.White
private val TextSecondary= Color.White.copy(alpha = 0.6f)
private val TextDim      = Color.White.copy(alpha = 0.35f)

@Composable
fun NowPlayingScreen(
    playbackManager: PlaybackManager,
    equalizerManager: EqualizerManager? = null,
    onClose: () -> Unit,
) {
    val state by playbackManager.playerState.collectAsState()
    val context = LocalContext.current

    // live position
    var position by remember { mutableLongStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(250)
            position = playbackManager.getCurrentPosition()
        }
    }

    val duration = state.duration.coerceAtLeast(1L)
    val song = state.currentSong

    // ── attach equalizer when a song is playing ──
    LaunchedEffect(state.currentSong) {
        if (state.currentSong != null) {
            val id = playbackManager.getAudioSessionId()
            if (id > 0) equalizerManager?.attach(id)
        }
    }

    // ── lyrics state ────────────────────────────────────────────────
    var showLyrics by remember { mutableStateOf(false) }
    var lyricsText by remember { mutableStateOf<String?>(null) }
    var loadingLyrics by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // ── queue dialog ────────────────────────────────────────────────
    var showQueue by remember { mutableStateOf(false) }
    if (showQueue) {
        AlertDialog(
            onDismissRequest = { showQueue = false },
            containerColor = CardGrey,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Up Next", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                val q = state.queue
                if (q.isEmpty()) {
                    Text("Queue is empty", color = TextDim, modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    LazyColumn(Modifier.heightIn(max = 350.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(q.size) { idx ->
                            val qSong = q[idx]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // drag handle (reorder not supported by simple dialog, just index)
                                Text("${idx + 1}.", color = TextDim, fontSize = 13.sp, modifier = Modifier.width(28.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(qSong.title, color = TextPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(qSong.artist, color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                // remove
                                IconButton(onClick = { playbackManager.removeFromQueue(idx) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, "Remove", tint = TextDim, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQueue = false }) {
                    Text("Close", color = PurpleAccent)
                }
            }
        )
    }

    // ── sleep timer state ───────────────────────────────────────────
    var showSleepPicker by remember { mutableStateOf(false) }

    // ── equalizer state ─────────────────────────────────────────────
    var showEqualizer by remember { mutableStateOf(false) }
    val eqPresets = remember { equalizerManager?.getPresetNames() ?: emptyList() }
    var selectedPreset by remember { mutableIntStateOf(equalizerManager?.currentPreset?.toInt() ?: -1) }
    var bassBoostOn by remember { mutableStateOf(equalizerManager?.isBassBoostEnabled ?: false) }
    if (showSleepPicker) {
        AlertDialog(
            onDismissRequest = { showSleepPicker = false },
            containerColor = CardGrey,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Sleep Timer", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    listOf(15, 30, 45, 60).forEach { mins ->
                        TextButton(
                            onClick = {
                                playbackManager.setSleepTimer(mins)
                                showSleepPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("$mins minutes", color = TextSecondary, fontSize = 15.sp)
                        }
                    }
                    if (state.sleepTimerRemainingSec > 0) {
                        HorizontalDivider(color = SurfaceGrey, modifier = Modifier.padding(vertical = 8.dp))
                        TextButton(
                            onClick = {
                                playbackManager.cancelSleepTimer()
                                showSleepPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel Timer", color = PurpleAccent, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepPicker = false }) {
                    Text("Close", color = PurpleAccent)
                }
            }
        )
    }

    // ── equalizer dialog ────────────────────────────────────────────
    if (showEqualizer && equalizerManager != null) {
        AlertDialog(
            onDismissRequest = { showEqualizer = false },
            containerColor = CardGrey,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Equalizer", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
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
                                    .clickable {
                                        selectedPreset = idx
                                        equalizerManager.setPreset(idx.toShort())
                                    }
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(name, color = if (isSelected) PurpleAccent else TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                if (isSelected) Icon(Icons.Default.Check, null, tint = PurpleAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = SurfaceGrey)
                        Spacer(Modifier.height(12.dp))
                        // bass boost
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Bass Boost", color = TextSecondary, fontSize = 14.sp)
                            Switch(
                                checked = bassBoostOn,
                                onCheckedChange = {
                                    bassBoostOn = it
                                    equalizerManager.setBassBoostEnabled(it)
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = PurpleAccent)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEqualizer = false }) {
                    Text("Done", color = PurpleAccent)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(36.dp))

            // ── header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.KeyboardArrowDown, "Minimize", tint = TextPrimary, modifier = Modifier.size(28.dp))
                }
                Text("NOW PLAYING", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Row {
                    // lyrics toggle
                    IconButton(onClick = {
                        showLyrics = !showLyrics
                        if (showLyrics && lyricsText == null && song != null && !loadingLyrics) {
                            loadingLyrics = true
                            scope.launch {
                                lyricsText = LyricsRepository.fetchLyrics(song.artist, song.title)?.plainLyrics
                                loadingLyrics = false
                            }
                        }
                    }) {
                        Icon(
                            if (showLyrics) Icons.Default.MusicNote else Icons.Default.Lyrics,
                            "Lyrics", tint = if (showLyrics) PurpleAccent else TextDim,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    // equalizer
                    if (equalizerManager != null) {
                        IconButton(onClick = { showEqualizer = true }) {
                            Icon(Icons.Default.Tune, "Equalizer", tint = TextDim, modifier = Modifier.size(24.dp))
                        }
                    }
                    // queue
                    IconButton(onClick = { showQueue = true }) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, "Queue", tint = TextDim, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── album art ──
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardGrey),
                contentAlignment = Alignment.Center
            ) {
                if (song != null && song.albumArtUri != null) {
                    AsyncImage(
                        model = remember(song.id) {
                            ImageRequest.Builder(context).data(song.albumArtUri).size(560)
                                .memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED).crossfade(true).build()
                        },
                        contentDescription = "Album Art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(PurpleAccent.copy(0.3f), PurpleAccent.copy(0.1f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(80.dp))
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── song info ──
            Text(
                song?.title ?: "No Track",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                song?.artist ?: "Unknown Artist",
                fontSize = 15.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            // ── seekbar ──
            val density = LocalDensity.current
            Column(Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                playbackManager.seekTo((fraction * duration).toLong())
                            }
                        }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, _ ->
                                change.consume()
                                val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                playbackManager.seekTo((fraction * duration).toLong())
                            }
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(TextDim.copy(alpha = 0.3f))
                    ) {
                        val progress = (position.toFloat() / duration).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .clip(RoundedCornerShape(2.dp))
                                .background(PurpleAccent)
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(position), color = TextDim, fontSize = 11.sp)
                    Text(formatTime(duration), color = TextDim, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── playback controls ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // shuffle with dot indicator
                Box(contentAlignment = Alignment.Center) {
                    IconButton(onClick = { playbackManager.toggleShuffle() }) {
                        Icon(Icons.Default.Shuffle, "Shuffle",
                            tint = if (state.shuffleMode) PurpleAccent else TextDim,
                            modifier = Modifier.size(24.dp))
                    }
                    if (state.shuffleMode) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 2.dp)
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(PurpleAccent)
                        )
                    }
                }
                // previous
                IconButton(onClick = { playbackManager.skipToPrevious() }) {
                    Icon(Icons.Default.SkipPrevious, "Previous",
                        tint = TextPrimary, modifier = Modifier.size(28.dp))
                }
                // play/pause — no shape, skeleton style
                IconButton(
                    onClick = { playbackManager.togglePlayPause() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(52.dp)
                    )
                }
                // next
                IconButton(onClick = { playbackManager.skipToNext() }) {
                    Icon(Icons.Default.SkipNext, "Next",
                        tint = TextPrimary, modifier = Modifier.size(28.dp))
                }
                // repeat with dot indicator
                Box(contentAlignment = Alignment.Center) {
                    IconButton(onClick = { playbackManager.cycleRepeat() }) {
                        Icon(
                            when (state.repeatMode) {
                                Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            },
                            "Repeat",
                            tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) PurpleAccent else TextDim,
                            modifier = Modifier.size(24.dp))
                    }
                    if (state.repeatMode != Player.REPEAT_MODE_OFF) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 2.dp)
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(PurpleAccent)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── sleep timer ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardGrey)
                    .clickable { showSleepPicker = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sleep Timer", color = TextSecondary, fontSize = 14.sp)
                if (state.sleepTimerRemainingSec > 0) {
                    val mins = state.sleepTimerRemainingSec / 60
                    val secs = state.sleepTimerRemainingSec % 60
                    Text(
                        "${mins}:%02d".format(secs),
                        color = PurpleAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(Icons.Default.Timer, null, tint = TextDim, modifier = Modifier.size(20.dp))
                }
            }

            // ── lyrics (below sleep timer, when toggled) ──
            if (showLyrics) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp, max = 250.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardGrey)
                        .padding(16.dp)
                ) {
                    when {
                        loadingLyrics -> Box(Modifier.fillMaxWidth().fillMaxHeight(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PurpleAccent, modifier = Modifier.size(24.dp))
                        }
                        lyricsText != null -> Box(Modifier.fillMaxWidth().fillMaxHeight().verticalScroll(rememberScrollState())) {
                            Text(
                                lyricsText!!, color = TextSecondary, fontSize = 14.sp,
                                lineHeight = 22.sp
                            )
                        }
                        else -> Text(
                            "No lyrics found", color = TextDim, fontSize = 14.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
