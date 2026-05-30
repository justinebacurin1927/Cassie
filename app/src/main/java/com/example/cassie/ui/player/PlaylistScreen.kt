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
import com.example.cassie.ui.theme.CassieColors
import com.example.cassie.ui.theme.CassieDialog

// ── Theme Tokens ──
private val PureBlack     = CassieColors.PureBlack
private val CardGrey      = CassieColors.CardGrey
private val SurfaceGrey   = CassieColors.SurfaceGrey
private val PurpleAccent  = CassieColors.PurpleAccent
private val TextPrimary   = CassieColors.TextPrimary
private val TextSecondary = CassieColors.TextSecondary
private val TextDim       = CassieColors.TextDim

@Composable
fun PlaylistScreen(
    songs: List<Song>,
    playlistStore: PlaylistStore?,
    favoritesStore: FavoritesStore? = null,
    playbackManager: PlaybackManager?,
    onSongClick: (Song) -> Unit,
    onPlaylistClick: (Playlist) -> Unit = {},
    onBack: () -> Unit,
) {
    val playlists by (playlistStore?.playlists?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    var showCreateDialog by remember { mutableStateOf(false) }

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
                    songs = songs,
                    onClick = { onPlaylistClick(playlist) },
                    onDelete = { playlistStore?.delete(playlist.id) },
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
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current

    // Get cover art: custom cover > first song's album art > null
    val coverSong = remember(playlist, songs) {
        songs.firstOrNull { it.id in playlist.songIds }
    }
    val coverUri = playlist.coverUri ?: coverSong?.albumArtUri

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardGrey)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover image (from first song's album art, or icon fallback)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(PurpleAccent.copy(0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (coverUri != null) {
                    AsyncImage(
                        model = remember(playlist.id, coverUri) {
                            ImageRequest.Builder(context)
                                .data(coverUri)
                                .size(96)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build()
                        },
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = PurpleAccent, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(playlist.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text("${playlist.songCount} songs", fontSize = 12.sp, color = TextSecondary, maxLines = 1)
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, "Delete", tint = TextDim, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.ChevronRight, null, tint = TextDim, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    CassieDialog(
        onDismissRequest = onDismiss,
        dialogTitle = { Text("New Playlist", color = TextPrimary, fontWeight = FontWeight.Bold) },
        dialogText = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Playlist name", color = TextDim) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PurpleAccent, cursorColor = PurpleAccent
                )
            )
        },
        dialogConfirmButton = { TextButton(onClick = { if (name.isNotBlank()) onCreate(name.trim()) }) { Text("Create", color = PurpleAccent) } },
        dialogDismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextDim) } }
    )
}
