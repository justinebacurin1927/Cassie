package com.example.cassie.ui.home

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.cassie.data.media.FavoritesStore
import com.example.cassie.data.media.PlaybackManager
import com.example.cassie.data.media.PlaylistStore
import com.example.cassie.data.media.Song

// ── Pure Black + Purple Accent Palette ────────────────────────────
private val PureBlack     = Color(0xFF000000)
private val DarkGrey      = Color(0xFF121212)
private val CardGrey      = Color(0xFF1E1E1E)
private val SurfaceGrey   = Color(0xFF282828)
private val PurpleAccent  = Color(0xFFBB86FC)
private val PurpleAccent20= PurpleAccent.copy(alpha = 0.2f)
private val TextPrimary   = Color.White
private val TextSecondary = Color.White.copy(alpha = 0.6f)
private val TextDim       = Color.White.copy(alpha = 0.35f)
private val GreyIcon      = Color.White.copy(alpha = 0.55f)

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
    onNavigateToPlaylists: () -> Unit = {},
    onNavigateToTop50: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
) {
    val state by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(viewModel.savedSortOption.value) }

    // persist sort changes
    LaunchedEffect(sortOption) {
        viewModel.saveSortOption(sortOption)
    }

    val listeningCounter = playbackManager?.listeningCounter

    val sortedSongs = remember(state.songs, sortOption) {
        state.songs.sortedByOption(sortOption)
    }

    val filteredSongs = remember(sortedSongs, searchQuery) {
        if (searchQuery.isBlank()) sortedSongs
        else {
            val q = searchQuery.lowercase()
            sortedSongs.filter {
                it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q)
            }
        }
    }

    val recentPlays = remember(listeningCounter?.counts?.value, state.songs) {
        listeningCounter?.getRecentPlays(state.songs, limit = 5) ?: emptyList()
    }

    val topSongs = remember(listeningCounter?.counts?.value, state.songs) {
        listeningCounter?.getTop50(state.songs)?.take(3) ?: emptyList()
    }

    val albums = remember(state.songs) {
        state.songs.groupBy { it.album }.entries
            .sortedByDescending { (_, songs) -> songs.size }
            .take(6)
            .map { (albumName, songs) ->
                AlbumPreview(albumName = albumName, artist = songs.first().artist, songCount = songs.size)
            }
    }

    val handleSongClick: (Song) -> Unit = { song ->
        playbackManager?.let { mgr ->
            mgr.play(song)
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
                songs = sortedSongs,
                filteredSongs = filteredSongs,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                sortOption = sortOption,
                onSortOptionChange = { sortOption = it },
                recentPlays = recentPlays,
                topSongs = topSongs,
                albums = albums,
                isEmpty = state.songs.isEmpty(),
                onSongClick = handleSongClick,
                playbackManager = playbackManager,
                playlistStore = playlistStore,
                favoritesStore = favoritesStore,
                onNavigateToAlbums = onNavigateToAlbums,
                onNavigateToPlaylists = onNavigateToPlaylists,
                onNavigateToTop50 = onNavigateToTop50,
                listState = listState,
            )
        }

        // mini player and nav bar are now in MainActivity
    }
}

data class AlbumPreview(val albumName: String, val artist: String, val songCount: Int)

// ── Loading ───────────────────────────────────────────────────────
@Composable
private fun LoadingDashboard() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(8) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardGrey)
            )
        }
    }
}

// ── Content Dashboard ─────────────────────────────────────────────
@Composable
private fun ContentDashboard(
    songs: List<Song>,
    filteredSongs: List<Song>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortOption: SortOption,
    onSortOptionChange: (SortOption) -> Unit,
    recentPlays: List<Song>,
    topSongs: List<Pair<Song, Int>>,
    albums: List<AlbumPreview>,
    isEmpty: Boolean,
    onSongClick: (Song) -> Unit,
    playbackManager: PlaybackManager?,
    playlistStore: PlaylistStore?,
    favoritesStore: FavoritesStore?,
    onNavigateToAlbums: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onNavigateToTop50: () -> Unit,
    listState: LazyListState,
) {
    Column(Modifier.fillMaxSize().background(PureBlack)) {
        // ── header + search (fixed at top) ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PureBlack)
                .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Good evening", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text("${songs.size} songs in library", color = TextSecondary, fontSize = 13.sp)
                }
                Row {
                    IconButton(onClick = onNavigateToPlaylists) { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = GreyIcon, modifier = Modifier.size(22.dp)) }
                    IconButton(onClick = onNavigateToTop50) { Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = GreyIcon, modifier = Modifier.size(22.dp)) }
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
        } else if (searchQuery.isNotBlank()) {
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
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 4.dp)
            ) {
                // ── Featured sections (no nested LazyRows — use Row+horizontalScroll) ──
                if (recentPlays.isNotEmpty() || topSongs.isNotEmpty() || albums.isNotEmpty()) {
                    // Recently Played
                    if (recentPlays.isNotEmpty()) {
                        item {
                            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                                SectionTitle("Recently Played")
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    recentPlays.forEach { song ->
                                        QuickPlayCard(song = song, onClick = { onSongClick(song) })
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }

                    // Top Charts
                    if (topSongs.isNotEmpty()) {
                        item {
                            Column(Modifier.padding(start = 16.dp, end = 16.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    SectionTitle("Top Charts")
                                    TextButton(onClick = onNavigateToTop50) {
                                        Text("See all", color = PurpleAccent.copy(0.7f), fontSize = 11.sp, letterSpacing = 1.sp)
                                    }
                                }
                                Column { topSongs.forEachIndexed { idx, (song, count) ->
                                    TopChartRow(rank = idx + 1, song = song, playCount = count, onClick = { onSongClick(song) })
                                }}
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }

                    // Albums
                    if (albums.isNotEmpty()) {
                        item {
                            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    SectionTitle("Albums")
                                    TextButton(onClick = onNavigateToAlbums) {
                                        Text("See all", color = PurpleAccent.copy(0.7f), fontSize = 11.sp, letterSpacing = 1.sp)
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    albums.forEach { album ->
                                        AlbumPreviewCard(album, onClick = onNavigateToAlbums)
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
    val isFav by remember { derivedStateOf { favoritesStore?.isFavorite(song.id) == true } }
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardGrey)
            .clickable(onClick = onClick)
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
        IconButton(onClick = { favoritesStore?.toggle(song.id) }, modifier = Modifier.size(30.dp)) {
            Icon(
                if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                "Favorite", tint = if (isFav) PurpleAccent else GreyIcon,
                modifier = Modifier.size(18.dp)
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
            AlertDialog(
                onDismissRequest = { showPlaylistPicker = false },
                containerColor = CardGrey,
                title = { Text("Add to Playlist", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
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
                confirmButton = { TextButton(onClick = { showPlaylistPicker = false }) { Text("Done", color = PurpleAccent) } }
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

// ── Top Chart Row ─────────────────────────────────────────────────
@Composable
private fun TopChartRow(rank: Int, song: Song, playCount: Int, onClick: () -> Unit) {
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
        Text("$playCount", color = PurpleAccent.copy(0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Album Preview Card ───────────────────────────────────────────
@Composable
private fun AlbumPreviewCard(album: AlbumPreview, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardGrey)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomStart
    ) {
        Column(Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Album, null, tint = PurpleAccent.copy(0.7f), modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(10.dp))
            Text(album.albumName, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text("${album.songCount} tracks · ${album.artist}", color = TextDim, fontSize = 10.sp, maxLines = 1)
        }
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
