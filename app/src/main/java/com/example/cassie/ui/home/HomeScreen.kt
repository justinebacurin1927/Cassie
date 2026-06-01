package com.example.cassie.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.cassie.data.media.FavoritesStore
import com.example.cassie.data.media.PlaybackManager
import com.example.cassie.data.media.Playlist
import com.example.cassie.data.media.PlaylistStore
import com.example.cassie.data.media.Song
import com.example.cassie.party.SkipperEngine
import com.example.cassie.ui.home.AlphabetScrollIndex
import com.example.cassie.ui.party.SkipperCard
import com.example.cassie.ui.theme.CassieColors
import com.example.cassie.ui.theme.CassieDialog

// ── Theme Tokens ──────────────────────────────────────────────────
private val PureBlack     = CassieColors.PureBlack
private val CardGrey      = CassieColors.CardGrey
private val SurfaceGrey   = CassieColors.SurfaceGrey
private val PurpleAccent  = CassieColors.PurpleAccent
private val TextPrimary   = CassieColors.TextPrimary
private val TextSecondary = CassieColors.TextSecondary
private val TextDim       = CassieColors.TextDim
private val GreyIcon      = CassieColors.GreyIcon

// ── Sort Options ──────────────────────────────────────────────────
enum class SortOption(val label: String) {
    TITLE_ASC("A-Z"),
    TITLE_DESC("Z-A"),
    DATE_DESC("Recent"),
    DATE_ASC("Oldest"),
    ARTIST_ASC("Artist"),
}

private fun List<Song>.sortedByOption(option: SortOption): List<Song> = when (option) {
    SortOption.TITLE_ASC   -> sortedBy { it.title }
    SortOption.TITLE_DESC  -> sortedByDescending { it.title }
    SortOption.DATE_DESC   -> sortedByDescending { it.dateAdded }
    SortOption.DATE_ASC    -> sortedBy { it.dateAdded }
    SortOption.ARTIST_ASC  -> sortedBy { it.artist.lowercase() }
}

// ── Home Screen ───────────────────────────────────────────────────
@Composable
fun HomeScreen(
    onSongClick: (Song) -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
    playbackManager: PlaybackManager? = null,
    playlistStore: PlaylistStore? = null,
    favoritesStore: FavoritesStore? = null,
    onNavigateToPlayer: () -> Unit = {},
    onNavigateToAlbums: () -> Unit = {},
    onNavigateToArtists: () -> Unit = {},
    onNavigateToPlaylists: () -> Unit = {},
    onPlaylistClick: (Playlist) -> Unit = {},
    onNavigateToTop50: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
) {
    val state by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(viewModel.savedSortOption.value) }

    // persist sort changes
    LaunchedEffect(sortOption) {
        viewModel.saveSortOption(sortOption)
    }

    // debounce search — 300ms delay before filtering
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            debouncedQuery = ""
        } else {
            kotlinx.coroutines.delay(300)
            debouncedQuery = searchQuery
        }
    }

    val listeningCounter = playbackManager?.listeningCounter

    val sortedSongs = remember(state.songs, sortOption) {
        state.songs.sortedByOption(sortOption)
    }

    // Map of letter -> index of the first song starting with that
    // letter in the sorted library. Used by the alphabet sidebar.
    val letterToSongIndex = remember(sortedSongs) {
        val map = mutableMapOf<Char, Int>()
        sortedSongs.forEachIndexed { i, song ->
            val ch = song.title.firstOrNull { it.isLetter() }
                ?.uppercaseChar() ?: '#'
            if (ch !in map) map[ch] = i
        }
        map
    }

    val filteredSongs = remember(sortedSongs, debouncedQuery) {
        val q = debouncedQuery.lowercase()
        if (debouncedQuery.isBlank()) sortedSongs
        else sortedSongs.filter {
            it.title.lowercase().contains(q) ||
            it.artist.lowercase().contains(q) ||
            it.album.lowercase().contains(q)
        }
    }

    val recentPlays = remember(listeningCounter?.counts?.value, state.songs) {
        listeningCounter?.getRecentPlays(state.songs, limit = 5) ?: emptyList()
    }

    // ── songById lookup map: O(1) instead of O(n) for every find()
    //    below. With a 5k-song library the old code did ~10k linear
    //    scans per recomposition of vibeStats + topSongs + each
    //    playlist preview. One map build replaces all of that. ──
    val songById = remember(state.songs) { state.songs.associateBy { it.id } }

    // ── "Your Top 50" — ranked by lifetime minutes listened. ──
    // We deliberately drop the minute count in the UI: the list is
    // just a ranking, the metric is implicit (and only visible to
    // Skipper internally for pattern detection).
    val topByMinutesRaw by SkipperEngine.topSongsByMinutes.collectAsState()
    val topSongs = remember(topByMinutesRaw, songById) {
        topByMinutesRaw
            .mapNotNull { (id, _) -> songById[id] }
            .take(10)
    }

    // ── Playlists preview for home (horizontal scroll) ──
    val playlistPreviews = remember(playlistStore?.playlists?.value) {
        (playlistStore?.playlists?.value ?: emptyList()).take(6)
    }

    // ── "Your Vibe" listening stats ────────────────────────────────────
    val vibeStats = remember(listeningCounter?.counts?.value, songById) {
        val counts = listeningCounter?.counts?.value ?: emptyMap()
        val totalPlays = counts.values.sumOf { it.count }
        val uniqueSongs = counts.size
        // top artist by total plays
        val artistPlays = mutableMapOf<String, Int>()
        for ((songId, pc) in counts) {
            val song = songById[songId]
            if (song != null) {
                artistPlays[song.artist] = (artistPlays[song.artist] ?: 0) + pc.count
            }
        }
        val topArtist = artistPlays.maxByOrNull { it.value }?.key ?: "—"
        val totalMillis = counts.entries.sumOf { (songId, pc) ->
            songById[songId]?.let { it.duration * pc.count } ?: 0L
        }
        val totalMinutes = (totalMillis / 60000).toInt()
        VibeStats(totalPlays = totalPlays, uniqueSongs = uniqueSongs, topArtist = topArtist, totalMinutes = totalMinutes)
    }

    // LazyColumn index where the first song starts. The alphabet
    // sidebar uses this to land jumps exactly on a letter.
    // Playlists section is now ALWAYS visible (empty-state card when
    // there are no playlists), so it always counts as one row.
    val songsStartIndex = remember(
        topSongs.isNotEmpty(),
        vibeStats.totalPlays > 0,
    ) {
        var idx = 1                              // Skipper card
        if (topSongs.isNotEmpty()) idx++        // Your Top 50
        idx++                                   // Playlists (always)
        idx++                                   // separator
        if (vibeStats.totalPlays > 0) idx++     // Vibe card
        idx                                     // Your Library header
    }

    // ── live clock ──────────────────────────────────────────────────
    var clockText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val now = java.time.LocalTime.now()
            clockText = java.time.format.DateTimeFormatter.ofPattern("h:mm a").format(now)
            kotlinx.coroutines.delay(60_000L)
        }
    }

    val contextSongs = if (debouncedQuery.isBlank()) sortedSongs else filteredSongs
    val handleSongClick: (Song) -> Unit = { song ->
        playbackManager?.let { mgr ->
            mgr.playInContext(song, contextSongs)
            onNavigateToPlayer()
        }
        onSongClick(song)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        when {
            state.isLoading -> LoadingDashboard()
            else -> ContentDashboard(
                clockText = clockText,
                songs = sortedSongs,
                filteredSongs = filteredSongs,
                searchQuery = searchQuery,
                debouncedQuery = debouncedQuery,
                onSearchQueryChange = { searchQuery = it },
                sortOption = sortOption,
                onSortOptionChange = { sortOption = it },
                recentPlays = recentPlays,
                topSongs = topSongs,
                playlistPreviews = playlistPreviews,
                vibeStats = vibeStats,
                isEmpty = state.songs.isEmpty(),
                onSongClick = handleSongClick,
                playbackManager = playbackManager,
                playlistStore = playlistStore,
                favoritesStore = favoritesStore,
                onNavigateToPlaylists = onNavigateToPlaylists,
                onPlaylistClick = onPlaylistClick,
                onNavigateToArtists = onNavigateToArtists,
                onNavigateToTop50 = onNavigateToTop50,
                listState = listState,
                sortedSongs = sortedSongs,
                letterToSongIndex = letterToSongIndex,
                songsStartIndex = songsStartIndex,
                songById = songById,
            )
        }

        // mini player and nav bar are now in MainActivity
    }
}

data class VibeStats(val totalPlays: Int, val uniqueSongs: Int, val topArtist: String, val totalMinutes: Int = 0)

// ── Loading Skeleton ──────────────────────────────────────────────
// Mirrors the structure of ContentDashboard exactly. Every section
// of the real home screen has a corresponding grey box here with
// the same height/width, so when real data loads the layout doesn't
// shift. Hit-and-miss random boxes are the wrong pattern.
@Composable
private fun LoadingDashboard() {
    val infinite = rememberInfiniteTransition(label = "homeSkeletonPulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.18f, targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val skelColor = CardGrey.copy(alpha = pulse)

    Column(Modifier.fillMaxSize().background(PureBlack)) {

        // ── Header (matches real header) ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PureBlack)
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    // clock (28sp height matches the real 28sp text)
                    Box(
                        Modifier
                            .width(120.dp)
                            .height(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(skelColor)
                    )
                    Spacer(Modifier.height(6.dp))
                    // "N songs in library" subtitle
                    Box(
                        Modifier
                            .width(140.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(skelColor)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // 3 icon buttons (matches the real row of icons)
                    repeat(3) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(skelColor)
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            // Search bar (matches the real SearchBar height)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(skelColor)
            )
        }

        // ── Scrollable content (mirrors real sections) ──
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(bottom = 4.dp),
        ) {

            // ── Skipper card ──
            item {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardGrey)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            // live dot + label row
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(skelColor)
                                )
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    Modifier
                                        .width(110.dp)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(skelColor)
                                )
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    Modifier
                                        .width(60.dp)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(skelColor)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            // line text (2 placeholder rows to mimic wrapping)
                            Box(
                                Modifier
                                    .fillMaxWidth(0.95f)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(skelColor)
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth(0.65f)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(skelColor)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        // penguin placeholder (matches real 100dp box)
                        Box(
                            Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(skelColor)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // ── Your Top 50 section ──
            item {
                Column(Modifier.padding(start = 16.dp, end = 16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .width(120.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(skelColor)
                        )
                        Box(
                            Modifier
                                .width(50.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(skelColor)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    // 10 top-50 rows (mirrors the real layout exactly)
                    repeat(10) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CardGrey)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(skelColor)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(0.7f)
                                        .height(13.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(skelColor)
                                )
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    Modifier
                                        .fillMaxWidth(0.4f)
                                        .height(11.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(skelColor)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── Playlists section ──
            item {
                Column(Modifier.padding(start = 16.dp, end = 16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .width(80.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(skelColor)
                        )
                        Box(
                            Modifier
                                .width(50.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(skelColor)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // 4 playlist squares (matches the real horizontal row)
                        repeat(4) {
                            Column {
                                Box(
                                    Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(skelColor)
                                )
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    Modifier
                                        .width(80.dp)
                                        .height(11.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(skelColor)
                                )
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    Modifier
                                        .width(50.dp)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(skelColor)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // ── Separator (matches real layout) ──
            item {
                Box(
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(CardGrey)
                )
            }

            // ── Your Library header + sort bar ──
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(skelColor)
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(
                            Modifier
                                .width(110.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(skelColor)
                        )
                        Spacer(Modifier.weight(1f))
                        Box(
                            Modifier
                                .width(60.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(skelColor)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    // sort bar (4 small pills)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(4) {
                            Box(
                                Modifier
                                    .width(58.dp)
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CardGrey)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── 6 song cards (matches the real list start) ──
            items(6) {
                Box(
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 3.dp)
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardGrey)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // album-art square
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(skelColor)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Box(
                                Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(13.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(skelColor)
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth(0.45f)
                                    .height(11.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(skelColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Content Dashboard ─────────────────────────────────────────────
@Composable
private fun ContentDashboard(
    clockText: String,
    songs: List<Song>,
    filteredSongs: List<Song>,
    searchQuery: String,
    debouncedQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortOption: SortOption,
    onSortOptionChange: (SortOption) -> Unit,
    recentPlays: List<Song>,
    topSongs: List<Song>,
    playlistPreviews: List<Playlist>,
    vibeStats: VibeStats,
    isEmpty: Boolean,
    onSongClick: (Song) -> Unit,
    playbackManager: PlaybackManager?,
    playlistStore: PlaylistStore?,
    favoritesStore: FavoritesStore?,
    onNavigateToPlaylists: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit = {},
    sortedSongs: List<Song> = songs,
    letterToSongIndex: Map<Char, Int> = emptyMap(),
    songsStartIndex: Int = 0,
    songById: Map<Long, Song> = emptyMap(),
    onNavigateToArtists: () -> Unit,
    onNavigateToTop50: () -> Unit,
    listState: LazyListState,
) {
    Column(Modifier.fillMaxSize().background(PureBlack)) {
        // ── header + search (fixed at top) ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PureBlack)
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(clockText, color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(Modifier.height(2.dp))
                    Text("${songs.size} songs in library", color = TextSecondary, fontSize = 13.sp)
                }
                Row {
                    IconButton(onClick = onNavigateToArtists) { Icon(Icons.Default.Person, "Artists", tint = GreyIcon, modifier = Modifier.size(22.dp)) }
                    IconButton(onClick = onNavigateToPlaylists) { Icon(Icons.AutoMirrored.Filled.QueueMusic, "Playlists", tint = GreyIcon, modifier = Modifier.size(22.dp)) }
                    IconButton(onClick = onNavigateToTop50) { Icon(Icons.AutoMirrored.Filled.TrendingUp, "Top 50", tint = GreyIcon, modifier = Modifier.size(22.dp)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            SearchBar(searchQuery, onSearchQueryChange)
        }

        // ── scrollable content (PureBlack throughout) ──
        if (isEmpty) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("Grant music permission to see your songs", color = TextDim, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
        } else if (debouncedQuery.isNotBlank()) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item { Spacer(Modifier.height(8.dp)); SectionTitle("Results (${filteredSongs.size})") }
                if (filteredSongs.isEmpty()) {
                    item { Text("No songs match", color = TextDim, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp)) }
                }
                items(filteredSongs, key = { it.id }) { song ->
                    SongCard(song = song, onClick = { onSongClick(song) }, playbackManager = playbackManager, playlistStore = playlistStore, favoritesStore = favoritesStore)
                }
            }
        } else {
            Box(Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 4.dp)
                ) {
                // ── Skipper card (live, auto-updating) ──
                item {
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                        SkipperCard()
                        Spacer(Modifier.height(12.dp))
                    }
                }

                // ── Featured sections ──
                // Your Top 50 (gated on having data; Playlists shortcut
                // is ALWAYS shown — the empty state still surfaces the
                // "create your first playlist" path).
                if (topSongs.isNotEmpty()) {
                    item {
                        Column(Modifier.padding(start = 16.dp, end = 16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                SectionTitle("Your Top 50")
                                TextButton(onClick = onNavigateToTop50) {
                                    Text("See all", color = PurpleAccent.copy(0.7f), fontSize = 11.sp, letterSpacing = 1.sp)
                                }
                            }
                            Column { topSongs.forEachIndexed { idx, song ->
                                Top50ByMinutesRow(rank = idx + 1, song = song, onClick = { onSongClick(song) })
                            }}
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }

                // Playlists shortcut — always visible on Home. Horizontal
                // scroll of previews when playlists exist; empty-state
                // card otherwise so the user can tap to create one.
                item {
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            SectionTitle("Playlists")
                            TextButton(onClick = onNavigateToPlaylists) {
                                Text("See all", color = PurpleAccent.copy(0.7f), fontSize = 11.sp, letterSpacing = 1.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (playlistPreviews.isEmpty()) {
                            // Empty state — still a tap target, takes the
                            // user to the Playlists screen where they can
                            // create their first one.
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(96.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardGrey)
                                    .clickable { onNavigateToPlaylists() }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.PlaylistAdd,
                                        contentDescription = null,
                                        tint = PurpleAccent,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        "No playlists yet — tap to create one",
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                playlistPreviews.forEach { playlist ->
                                    val coverSong = playlist.songIds.firstNotNullOfOrNull { songById[it] }
                                    PlaylistPreviewCard(
                                        playlist = playlist,
                                        coverSong = coverSong,
                                        onClick = onNavigateToPlaylists,
                                        onPlayClick = { onPlaylistClick(playlist) },
                                    )
                                }
                            }
                        }
                    }
                }

                // separator
                item {
                    Box(
                        Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(CardGrey)
                    )
                }

                    // ── Your Vibe: listening stats card ──
                if (vibeStats.totalPlays > 0) {
                    item { Spacer(Modifier.height(4.dp)) }
                    item { VibeCard(stats = vibeStats) }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                // ── Your Library header ──
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LibraryMusic, null, tint = PurpleAccent.copy(0.7f), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Your Library", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("${songs.size} songs", color = TextDim, fontSize = 12.sp)
                        }
                        // sort bar under Your Library header
                        SortBar(selected = sortOption, onSelect = onSortOptionChange)
                        Spacer(Modifier.height(6.dp))
                    }
                }

                // Songs (PureBlack background, each card has CardGrey bg)
                items(songs, key = { it.id }) { song ->
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
                        SongCard(song = song, onClick = { onSongClick(song) }, playbackManager = playbackManager, playlistStore = playlistStore, favoritesStore = favoritesStore)
                    }
                }
                }
                AlphabetScrollIndex(
                    listState = listState,
                    songsStartIndex = songsStartIndex,
                    letterToSongIndex = letterToSongIndex,
                    // fillMaxSize so the bubble can anchor at the
                    // center of the full home area, not the column.
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}


@Composable
private fun SectionTitle(text: String) {
    Text(text, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardGrey)
            .padding(horizontal = 14.dp, vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null, tint = TextDim, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search songs, artists, albums", color = TextDim, fontSize = 14.sp) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = TextPrimary, fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = PurpleAccent,
                ),
                modifier = Modifier.weight(1f)
            )
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Clear, null, tint = TextDim, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun SongCard(song: Song, onClick: () -> Unit, playbackManager: PlaybackManager? = null, playlistStore: PlaylistStore? = null, favoritesStore: FavoritesStore? = null) {
    var showMenu by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    val favIds by favoritesStore?.favoriteIds?.collectAsState() ?: remember { mutableStateOf(emptySet()) }
    val isFav = song.id in favIds
    val context = LocalContext.current

    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clip(RoundedCornerShape(10.dp))
            .background(CardGrey)
            .pointerInput(onClick) {
                detectTapGestures(
                    onPress = { isPressed = true; tryAwaitRelease(); isPressed = false },
                    onTap = { onClick() }
                )
            }
            .padding(vertical = 10.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // album art
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceGrey),
            contentAlignment = Alignment.Center
        ) {
            if (song.albumArtUri != null) {
                AsyncImage(
                    model = remember(song.id) {
                        ImageRequest.Builder(context)
                            .data(song.albumArtUri)
                            .size(96)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build()
                    },
                    contentDescription = "Album Art",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.MusicNote, null, tint = PurpleAccent.copy(alpha = 0.5f), modifier = Modifier.size(22.dp))
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(song.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary, maxLines = 1)
            Spacer(Modifier.height(3.dp))
            Text(song.artist, fontSize = 13.sp, color = TextSecondary, maxLines = 1)
        }

        Spacer(Modifier.width(6.dp))

        // heart
        val heartScale by animateFloatAsState(
            targetValue = if (isFav) 1.15f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "heartScale"
        )
        IconButton(onClick = { favoritesStore?.toggle(song.id) }, modifier = Modifier.size(30.dp)) {
            Icon(
                if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                "Favorite", tint = if (isFav) PurpleAccent else GreyIcon,
                modifier = Modifier.size(18.dp * heartScale)
            )
        }

        // more (now includes Play + Add to Playlist)
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable { showMenu = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MoreVert, "More", tint = GreyIcon, modifier = Modifier.size(16.dp))
        }

        // dropdown menu
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, containerColor = SurfaceGrey) {
            DropdownMenuItem(
                text = { Text("Play", color = TextPrimary, fontSize = 14.sp) },
                onClick = { showMenu = false; playbackManager?.play(song) },
                leadingIcon = { Icon(Icons.Default.PlayArrow, null, tint = PurpleAccent, modifier = Modifier.size(18.dp)) }
            )
            DropdownMenuItem(
                text = { Text("Add to Playlist", color = TextPrimary, fontSize = 14.sp) },
                onClick = { showMenu = false; showPlaylistPicker = true },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, tint = PurpleAccent, modifier = Modifier.size(18.dp)) }
            )
        }

        // playlist picker dialog
        if (showPlaylistPicker && playlistStore != null) {
            val playlists by playlistStore.playlists.collectAsState()
            CassieDialog(
                onDismissRequest = { showPlaylistPicker = false },
                dialogTitle = { Text("Add to Playlist", color = TextPrimary, fontWeight = FontWeight.Bold) },
                dialogText = {
                    if (playlists.isEmpty()) {
                        Text("No playlists yet. Create one first!", color = TextDim, fontSize = 14.sp)
                    } else {
                        LazyColumn(Modifier.height(200.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(playlists, key = { it.id }) { pl ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable {
                                        playlistStore.addToPlaylist(pl.id, song.id)
                                        showPlaylistPicker = false
                                    }.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = PurpleAccent, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(pl.name, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                    Text("${pl.songCount}", color = TextDim, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                },
                dialogConfirmButton = { TextButton(onClick = { showPlaylistPicker = false }) { Text("Done", color = PurpleAccent) } }
            )
        }
    }
}

// ── Quick Play Card (recent plays) ───────────────────────────────
@Composable
private fun QuickPlayCard(song: Song, onClick: () -> Unit = {}) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .width(140.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardGrey)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomStart
    ) {
        // album art background if available
        if (song.albumArtUri != null) {
            AsyncImage(
                model = remember(song.id) {
                    ImageRequest.Builder(context)
                        .data(song.albumArtUri)
                        .size(280)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()
                },
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        // dark bottom overlay for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.BottomStart
        ) {
            Column(Modifier.padding(10.dp)) {
                Text(song.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(song.artist, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

// ── Top 50 by Minutes Row ────────────────────────────────────────
// Ranked by lifetime minutes listened. NO metric shown — the row
// is just rank, art, title, artist. The user infers the ranking
// from position alone.
@Composable
private fun Top50ByMinutesRow(rank: Int, song: Song, onClick: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
            if (rank == 1) {
                Icon(Icons.Default.MilitaryTech, null, tint = Color(0xFFFFD700), modifier = Modifier.size(22.dp))
            } else if (rank == 2) {
                Icon(Icons.Default.MilitaryTech, null, tint = Color(0xFFC0C0C0), modifier = Modifier.size(20.dp))
            } else if (rank == 3) {
                Icon(Icons.Default.MilitaryTech, null, tint = Color(0xFFCD7F32), modifier = Modifier.size(18.dp))
            } else {
                Text("$rank", color = TextDim, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(SurfaceGrey),
            contentAlignment = Alignment.Center
        ) {
            if (song.albumArtUri != null) {
                AsyncImage(
                    model = remember(song.id) {
                        ImageRequest.Builder(context)
                            .data(song.albumArtUri)
                            .size(80)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build()
                    },
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.MusicNote, null, tint = PurpleAccent.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary, maxLines = 1)
            Text(song.artist, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
        }
    }
}

// ── Playlist Preview Card (horizontal home row) ──────────────────
@Composable
private fun PlaylistPreviewCard(
    playlist: Playlist,
    coverSong: Song?,
    onClick: () -> Unit = {},
    onPlayClick: () -> Unit = {},
) {
    val context = LocalContext.current
    // Use custom cover if set, else first song's album art
    val coverUri = playlist.coverUri ?: coverSong?.albumArtUri

    Box(
        modifier = Modifier
            .width(130.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardGrey)
            .clickable(onClick = onClick),
    ) {
        // Full image background
        if (coverUri != null) {
            AsyncImage(
                model = remember(playlist.id, coverUri) {
                    ImageRequest.Builder(context).data(coverUri).size(280)
                        .memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED).build()
                },
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(Modifier.fillMaxSize().background(PurpleAccent.copy(0.15f)), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = PurpleAccent.copy(0.5f), modifier = Modifier.size(40.dp))
            }
        }

        // Subtle grey overlay (5% opacity) covering the entire image
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.05f)))

        // Small play button in bottom-right corner
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White)
                .align(Alignment.BottomEnd)
                .padding(3.dp)
                .clickable(onClick = onPlayClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, "Play", tint = Color.Black, modifier = Modifier.size(14.dp))
        }

        // Floating playlist name at bottom-left (no background bar)
        Text(
            text = playlist.name,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 8.dp, end = 40.dp),
        )
    }
}

// ── Bottom Nav Bar (Stadium / Capsule Design) ────────────────────
private data class NavBarItem(
    val id: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
)

private val navItems = listOf(
    NavBarItem("home", Icons.Default.Home, "Home"),
    NavBarItem("albums", Icons.Default.Album, "Albums"),
    NavBarItem("playlists", Icons.AutoMirrored.Filled.QueueMusic, "Playlists"),
    NavBarItem("top50", Icons.AutoMirrored.Filled.TrendingUp, "Top 50"),
)

@Composable
private fun StadiumNavBar(
    activeItem: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
        color = CardGrey,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(start = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Active pill on far left
            val activeItemData = navItems.first { it.id == activeItem }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(SurfaceGrey)
                    .clickable { onItemSelected(activeItem) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    activeItemData.icon,
                    null,
                    tint = PurpleAccent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    activeItemData.label,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Spacer pushes inactive items to the right
            Spacer(Modifier.weight(1f))

            // Inactive items — icons only, evenly distributed
            navItems.filter { it.id != activeItem }.forEach { item ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(TextDim.copy(alpha = 0.08f))
                        .clickable { onItemSelected(item.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = TextDim,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
            }
        }
    }
}

// ── Sort Bar — skeleton chips (no shape, dot indicator) ─────────
@Composable
private fun SortBar(selected: SortOption, onSelect: (SortOption) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SortOption.entries.forEach { option ->
            val isSelected = option == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelect(option) }
            ) {
                Text(
                    option.label,
                    color = if (isSelected) PurpleAccent else TextDim,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    letterSpacing = 0.5.sp,
                )
                // dot indicator for active sort
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) PurpleAccent else Color.Transparent)
                )
            }
        }
    }
}

// ── Your Vibe Card — listening stats dashboard ────────────────────
private val VibePurple1 = Color(0xFF2A0A4A)
private val VibePurple2 = Color(0xFF1A0033)

@Composable
private fun VibeCard(stats: VibeStats) {
    val infinite = rememberInfiniteTransition(label = "vibeGrad")
    val offset by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse),
        label = "gradOffset"
    )
    // ── Gradient glow animation ──
    val glowInfinite = rememberInfiniteTransition(label = "vibeGlow")
    val glowIntensity by glowInfinite.animateFloat(
        initialValue = 0.15f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowIntensity"
    )
    val glowOffsetY by glowInfinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse),
        label = "glowOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
    ) {
        // ── Base dark gradient ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(VibePurple1, VibePurple2)
                    )
                )
        )
        // ── Gradient glow — purple light radiating from within ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PurpleAccent.copy(alpha = glowIntensity * 0.8f),
                            Color.Transparent,
                            PurpleAccent.copy(alpha = glowIntensity * 0.3f),
                        )
                    )
                )
        )
        // ── Top glowing accent bar ──
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(PurpleAccent.copy(alpha = glowIntensity + 0.3f))
                .align(Alignment.TopCenter)
        )
        // ── Content ──
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Insights, null,
                    tint = PurpleAccent.copy(alpha = 0.8f + glowIntensity * 0.2f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Your Vibe", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(label = "Plays", value = "${stats.totalPlays}")
                StatItem(label = "Minutes", value = "${stats.totalMinutes}")
                StatItem(label = "Top Artist", value = stats.topArtist, maxLines = 1)
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, maxLines: Int = Int.MAX_VALUE) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            maxLines = maxLines, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(label.uppercase(), color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, letterSpacing = 1.sp)
    }
}
