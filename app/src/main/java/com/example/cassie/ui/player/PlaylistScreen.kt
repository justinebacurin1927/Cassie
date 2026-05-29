package com.example.cassie.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.cassie.data.media.FavoritesStore
import com.example.cassie.data.media.PlaybackManager
import com.example.cassie.data.media.Playlist
import com.example.cassie.data.media.PlaylistStore
import com.example.cassie.data.media.Song
import androidx.compose.ui.platform.LocalContext

// ── Palette ───────────────────────────────────────────────────────
private val PureBlack     = Color(0xFF000000)
private val DarkGrey      = Color(0xFF121212)
private val CardGrey      = Color(0xFF1E1E1E)
private val SurfaceGrey   = Color(0xFF282828)
private val PurpleAccent  = Color(0xFFBB86FC)
private val TextPrimary   = Color.White
private val TextSecondary = Color.White.copy(alpha = 0.6f)
private val TextDim       = Color.White.copy(alpha = 0.35f)

@Composable
fun PlaylistScreen(
    songs: List<Song>,
    playlistStore: PlaylistStore?,
    favoritesStore: FavoritesStore? = null,
    playbackManager: PlaybackManager?,
    onSongClick: (Song) -> Unit,
    onBack: () -> Unit,
) {
    val playlists by (playlistStore?.playlists?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    var showCreateDialog by remember { mutableStateOf(false) }
    var expandedPlaylistId by remember { mutableStateOf<Long?>(null) }

    Box(
        modifier = Modifier.fillMaxSize().background(PureBlack)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary) }
                    Text("PLAYLISTS", color = TextDim, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    IconButton(onClick = { showCreateDialog = true }) { Icon(Icons.Default.Add, "Create", tint = TextPrimary) }
                }
            }

            if (playlists.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = TextDim, modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No playlists yet", color = TextSecondary, fontSize = 16.sp)
                            Text("Tap + to create one!", color = TextDim, fontSize = 13.sp)
                        }
                    }
                }
            }

            items(playlists, key = { it.id }) { playlist ->
                PlaylistCard(
                    playlist = playlist,
                    songs = playlistStore?.getSongsForPlaylist(playlist.id, songs) ?: emptyList(),
                    allSongs = songs,
                    isExpanded = expandedPlaylistId == playlist.id,
                    onToggle = { expandedPlaylistId = if (expandedPlaylistId == playlist.id) null else playlist.id },
                    onDelete = { playlistStore?.delete(playlist.id) },
                    onSongClick = { song -> playbackManager?.play(song); onSongClick(song) },
                    onRemove = { songId -> playlistStore?.removeFromPlaylist(playlist.id, songId) },
                    onAdd = { songId -> playlistStore?.addToPlaylist(playlist.id, songId) },
                )
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name -> playlistStore?.create(name); showCreateDialog = false }
        )
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    songs: List<Song>,
    allSongs: List<Song>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onSongClick: (Song) -> Unit,
    onRemove: (Long) -> Unit,
    onAdd: (Long) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CardGrey)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(PurpleAccent.copy(0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = PurpleAccent, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(playlist.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary, maxLines = 1)
                Text("${playlist.songCount} songs", fontSize = 12.sp, color = TextSecondary, maxLines = 1)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, "Delete", tint = TextDim, modifier = Modifier.size(20.dp))
            }
            Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = TextDim, modifier = Modifier.size(20.dp))
        }

        if (isExpanded) {
            HorizontalDivider(color = TextDim.copy(alpha = 0.1f))

            if (songs.isEmpty()) {
                Text("Empty playlist. Tap + below to add songs.", color = TextDim, fontSize = 13.sp, modifier = Modifier.padding(14.dp))
            }

            songs.forEach { song ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)).background(SurfaceGrey),
                        contentAlignment = Alignment.Center
                    ) {
                        if (song.albumArtUri != null) {
                            AsyncImage(
                                model = remember(song.id) {
                                    ImageRequest.Builder(context).data(song.albumArtUri).size(72)
                                        .memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED).build()
                                },
                                contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.MusicNote, null, tint = PurpleAccent.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(song.title, color = TextPrimary, fontSize = 14.sp, maxLines = 1, modifier = Modifier.weight(1f))
                    Text(song.artist, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                    Spacer(Modifier.width(8.dp))
                    // remove
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.Red.copy(0.15f)).clickable { onRemove(song.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.RemoveCircleOutline, "Remove", tint = Color.Red.copy(0.6f), modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Add songs button
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clickable { showAddDialog = true }.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AddCircleOutline, null, tint = PurpleAccent.copy(0.7f), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Songs from Library", color = PurpleAccent.copy(0.7f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(4.dp))
        }
    }

    // Add song dialog
    if (showAddDialog) {
        val existingIds = songs.map { it.id }.toSet()
        val allAvailable = allSongs.filter { it.id !in existingIds }
        var addSearch by remember { mutableStateOf("") }
        val filteredAvailable = remember(allAvailable, addSearch) {
            if (addSearch.isBlank()) allAvailable
            else allAvailable.filter {
                it.title.contains(addSearch, ignoreCase = true) ||
                it.artist.contains(addSearch, ignoreCase = true)
            }
        }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = CardGrey,
            title = { Text("Add Songs", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    // search
                    OutlinedTextField(
                        value = addSearch,
                        onValueChange = { addSearch = it },
                        placeholder = { Text("Search songs...", color = TextDim, fontSize = 14.sp) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = TextDim, modifier = Modifier.size(20.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PurpleAccent, cursorColor = PurpleAccent,
                            unfocusedBorderColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    if (allAvailable.isEmpty()) {
                        Text("All songs are already in this playlist!", color = TextDim, fontSize = 14.sp)
                    } else if (filteredAvailable.isEmpty() && addSearch.isNotBlank()) {
                        Text("No songs match your search", color = TextDim, fontSize = 14.sp)
                    } else {
                        LazyColumn(Modifier.height(300.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(filteredAvailable, key = { it.id }) { song ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onAdd(song.id) }.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)).background(SurfaceGrey),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (song.albumArtUri != null) {
                                            AsyncImage(
                                                model = remember(song.id) {
                                                    ImageRequest.Builder(context).data(song.albumArtUri).size(72)
                                                        .memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED).build()
                                                },
                                                contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(Icons.Default.MusicNote, null, tint = PurpleAccent.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(song.title, color = TextPrimary, fontSize = 14.sp, maxLines = 1)
                                        Text(song.artist, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                                    }
                                    Icon(Icons.Default.Add, null, tint = PurpleAccent, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAddDialog = false }) { Text("Done", color = PurpleAccent) } }
        )
    }
}

@Composable
private fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardGrey,
        title = { Text("New Playlist", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Playlist name", color = TextDim) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = PurpleAccent, cursorColor = PurpleAccent)
            )
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onCreate(name.trim()) }) { Text("Create", color = PurpleAccent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextDim) } }
    )
}
