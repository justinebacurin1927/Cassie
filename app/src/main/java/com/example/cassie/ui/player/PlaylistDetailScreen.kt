package com.example.cassie.ui.player

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import androidx.compose.ui.platform.LocalContext
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
import java.io.File
import java.io.FileOutputStream

private val PureBlack     = Color(0xFF000000)
private val CardGrey      = Color(0xFF1E1E1E)
private val SurfaceGrey   = Color(0xFF282828)
private val PurpleAccent  = Color(0xFFBB86FC)
private val TextPrimary   = Color.White
private val TextSecondary = Color.White.copy(alpha = 0.6f)
private val TextDim       = Color.White.copy(alpha = 0.35f)

/** Copy a content URI to internal storage so the image persists. Returns null on failure. */
private fun copyToInternalStorage(context: Context, sourceUri: Uri, fileName: String): String? {
    return try {
        val dir = File(context.filesDir, "playlist_covers")
        if (!dir.exists()) dir.mkdirs()
        val destFile = File(dir, fileName)
        val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
        val bytesCopied = inputStream.use { input ->
            FileOutputStream(destFile).use { output -> input.copyTo(output) }
        }
        // Validate that the file was actually written
        if (bytesCopied == 0L || !destFile.exists() || destFile.length() == 0L) {
            destFile.delete()
            return null
        }
        destFile.absolutePath
    } catch (e: Exception) {
        try { File(context.filesDir, "playlist_covers/$fileName").delete() } catch (_: Exception) {}
        null
    }
}

/** Delete a cover image from internal storage. */
private fun deleteCoverImage(context: Context, path: String?) {
    if (path != null) {
        try { File(path).delete() } catch (_: Exception) {}
    }
}

@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    allSongs: List<Song>,
    playbackManager: PlaybackManager?,
    playlistStore: PlaylistStore?,
    favoritesStore: FavoritesStore? = null,
    onSongClick: (Song) -> Unit,
    onBack: () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // ── Reactively read the current playlist from the store ──
    val allStorePlaylists by (playlistStore?.playlists?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) })
    val currentPlaylist = remember(allStorePlaylists, playlist.id) {
        allStorePlaylists.find { it.id == playlist.id } ?: playlist
    }
    val currentPlaylistSongs = remember(currentPlaylist, allSongs) {
        allSongs.filter { it.id in currentPlaylist.songIds }
    }

    // Track the current cover URI (custom or fallback)
    var currentCoverUri by remember(currentPlaylist) { mutableStateOf(currentPlaylist.coverUri) }

    // Fallback to first song's album art
    val fallbackCover = remember(currentPlaylistSongs) {
        currentPlaylistSongs.firstOrNull()?.albumArtUri
    }
    val displayCover = currentCoverUri ?: fallbackCover

    val scope = rememberCoroutineScope()

    // ── Photo picker launcher (uses GetContent for broad compatibility) ──
    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && playlistStore != null) {
            scope.launch {
                try {
                    val fileName = "playlist_${currentPlaylist.id}.jpg"
                    // Delete old custom cover if any
                    if (currentPlaylist.coverUri != null) {
                        deleteCoverImage(context, currentPlaylist.coverUri)
                    }
                    // Run file I/O on background thread
                    val savedPath = withContext(Dispatchers.IO) {
                        copyToInternalStorage(context, uri, fileName)
                    }
                    if (savedPath != null) {
                        playlistStore.updateCoverUri(currentPlaylist.id, savedPath)
                        currentCoverUri = savedPath
                        Toast.makeText(context, "Cover updated!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to save cover image", Toast.LENGTH_SHORT).show()
                    }
                } catch (_: Exception) {
                    Toast.makeText(context, "Error saving cover", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(PureBlack)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // ── Header ──
            item {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary) }
                        Row {
                            IconButton(onClick = { showRenameDialog = true }) { Icon(Icons.Default.Edit, "Rename", tint = TextDim, modifier = Modifier.size(22.dp)) }
                            IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(0.6f), modifier = Modifier.size(22.dp)) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    // ── Large cover art (tappable to change) ──
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceGrey)
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        if (displayCover != null) {
                            AsyncImage(
                                model = remember(displayCover) {
                                    ImageRequest.Builder(context).data(displayCover).size(400)
                                        .memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED).build()
                                },
                                contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = TextDim, modifier = Modifier.size(64.dp))
                        }

                        // Camera overlay button
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PurpleAccent.copy(alpha = 0.85f))
                                .clickable { coverPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        // Remove cover button (only if custom cover is set)
                        if (currentCoverUri != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .clickable {
                                        deleteCoverImage(context, currentCoverUri)
                                        playlistStore?.updateCoverUri(currentPlaylist.id, null)
                                        currentCoverUri = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(currentPlaylist.name, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                    Spacer(Modifier.height(4.dp))
                    Text("${currentPlaylistSongs.size} songs", color = TextDim, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))

                    // Play All button
                    if (currentPlaylistSongs.isNotEmpty()) {
                        Button(
                            onClick = {
                                playbackManager?.playInContext(currentPlaylistSongs.first(), currentPlaylistSongs)
                                onSongClick(currentPlaylistSongs.first())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Play All", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    // Add songs button
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleAccent),
                        border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.4f))
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add Songs", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                }
            }

            item { Spacer(Modifier.height(4.dp)) }

            // ── Songs ──
            if (currentPlaylistSongs.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MusicNote, null, tint = TextDim, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Playlist is empty", color = TextSecondary, fontSize = 16.sp)
                            Text("Tap Add Songs to fill it up!", color = TextDim, fontSize = 13.sp)
                        }
                    }
                }
            }

            items(currentPlaylistSongs, key = { it.id }) { song ->
                PlaylistSongRow(
                    song = song,
                    onClick = {
                        playbackManager?.playInContext(song, currentPlaylistSongs)
                        onSongClick(song)
                    },
                    onRemove = { playlistStore?.removeFromPlaylist(currentPlaylist.id, song.id) },
                    favoritesStore = favoritesStore,
                    playlistStore = playlistStore,
                )
            }
        }
    }

    // ── Add Songs Dialog ──
    if (showAddDialog && playlistStore != null) {
        val existingIds = currentPlaylistSongs.map { it.id }.toSet()
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
                                    modifier = Modifier.fillMaxWidth().padding(12.dp).clip(RoundedCornerShape(8.dp)).clickable {
                                        playlistStore.addToPlaylist(currentPlaylist.id, song.id)
                                        showAddDialog = false
                                    },
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

    // ── Rename Dialog ──
    if (showRenameDialog) {
        var newName by remember { mutableStateOf(currentPlaylist.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = CardGrey,
            title = { Text("Rename Playlist", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("Playlist name", color = TextDim) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PurpleAccent, cursorColor = PurpleAccent
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        playlistStore?.rename(currentPlaylist.id, newName.trim())
                        showRenameDialog = false
                    }
                }) { Text("Save", color = PurpleAccent) }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancel", color = TextDim) } }
        )
    }

    // ── Delete Confirmation ──
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = CardGrey,
            title = { Text("Delete Playlist?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("\"${currentPlaylist.name}\" will be permanently deleted.", color = TextSecondary, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    // Delete custom cover image too
                    if (currentPlaylist.coverUri != null) deleteCoverImage(context, currentPlaylist.coverUri)
                    playlistStore?.delete(currentPlaylist.id)
                    showDeleteConfirm = false
                    onBack()
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = TextDim) } }
        )
    }
}

// ── Song Row in Playlist ──
@Composable
private fun PlaylistSongRow(
    song: Song,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    favoritesStore: FavoritesStore?,
    playlistStore: PlaylistStore?,
) {
    var showPlaylistMenu by remember { mutableStateOf(false) }
    val favIds by favoritesStore?.favoriteIds?.collectAsState() ?: remember { mutableStateOf(emptySet()) }
    val isFav = song.id in favIds
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CardGrey)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(SurfaceGrey),
            contentAlignment = Alignment.Center
        ) {
            if (song.albumArtUri != null) {
                AsyncImage(
                    model = remember(song.id) {
                        ImageRequest.Builder(context).data(song.albumArtUri).size(80)
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
            Spacer(Modifier.height(2.dp))
            Text(formatDuration(song.duration), color = TextDim, fontSize = 11.sp)
        }
        // Heart
        IconButton(onClick = { favoritesStore?.toggle(song.id) }, modifier = Modifier.size(28.dp)) {
            Icon(
                if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                "Favorite", tint = if (isFav) PurpleAccent else TextDim,
                modifier = Modifier.size(16.dp)
            )
        }
        // Remove
        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.RemoveCircleOutline, "Remove", tint = Color.Red.copy(0.6f), modifier = Modifier.size(18.dp))
        }
        // More / Add to playlist
        Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(50)).clickable { showPlaylistMenu = true },
            contentAlignment = Alignment.Center) {
            Icon(Icons.Default.MoreVert, "More", tint = TextDim, modifier = Modifier.size(16.dp))
        }

        if (showPlaylistMenu && playlistStore != null) {
            val playlists by playlistStore.playlists.collectAsState()
            AlertDialog(
                onDismissRequest = { showPlaylistMenu = false },
                containerColor = CardGrey,
                title = { Text("Add to Playlist", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    if (playlists.isEmpty()) {
                        Text("No playlists yet!", color = TextDim, fontSize = 14.sp)
                    } else {
                        LazyColumn(Modifier.height(200.dp)) {
                            items(playlists, key = { it.id }) { pl ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable {
                                        playlistStore.addToPlaylist(pl.id, song.id)
                                        showPlaylistMenu = false
                                    }.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = PurpleAccent, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(pl.name, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showPlaylistMenu = false }) { Text("Done", color = PurpleAccent) } }
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSec = millis / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
