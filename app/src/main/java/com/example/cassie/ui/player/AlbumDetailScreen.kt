package com.example.cassie.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
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
import com.example.cassie.data.media.PlaylistStore
import com.example.cassie.data.media.Song
import androidx.compose.ui.platform.LocalContext

private val PureBlack     = Color(0xFF000000)
private val CardGrey      = Color(0xFF1E1E1E)
private val SurfaceGrey   = Color(0xFF282828)
private val PurpleAccent  = Color(0xFFBB86FC)
private val TextPrimary   = Color.White
private val TextSecondary = Color.White.copy(alpha = 0.6f)
private val TextDim       = Color.White.copy(alpha = 0.35f)

@Composable
fun AlbumDetailScreen(
    albumName: String,
    songs: List<Song>,
    playbackManager: PlaybackManager?,
    playlistStore: PlaylistStore? = null,
    favoritesStore: FavoritesStore? = null,
    onSongClick: (Song) -> Unit,
    onBack: () -> Unit,
) {
    val albumSongs = remember(songs, albumName) {
        songs.filter { it.album == albumName }
    }
    val artist = albumSongs.firstOrNull()?.artist ?: ""
    val albumArtUri = albumSongs.firstOrNull()?.albumArtUri

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
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
                        Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    // Album art large
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceGrey)
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        if (albumArtUri != null) {
                            val ctx = LocalContext.current
                            AsyncImage(
                                model = remember(albumName) {
                                    ImageRequest.Builder(ctx).data(albumArtUri).size(400)
                                        .memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED).build()
                                },
                                contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Album, null, tint = TextDim, modifier = Modifier.size(64.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(albumName, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                    Spacer(Modifier.height(4.dp))
                    Text(artist, color = TextSecondary, fontSize = 14.sp, maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    Text("${albumSongs.size} songs", color = TextDim, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    // Play all button
                    Button(
                        onClick = {
                            playbackManager?.playInContext(albumSongs.first(), albumSongs)
                            onSongClick(albumSongs.first())
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
            }

            item { Spacer(Modifier.height(8.dp)) }

            // ── Songs ──
            items(albumSongs, key = { it.id }) { song ->
                AlbumSongRow(
                    song = song,
                    onClick = {
                        playbackManager?.playInContext(song, albumSongs)
                        onSongClick(song)
                    },
                    playlistStore = playlistStore,
                    favoritesStore = favoritesStore,
                )
            }
        }
    }
}

@Composable
private fun AlbumSongRow(
    song: Song,
    onClick: () -> Unit,
    playlistStore: PlaylistStore?,
    favoritesStore: FavoritesStore?,
) {
        var showMenu by remember { mutableStateOf(false) }
    val favIds by favoritesStore?.favoriteIds?.collectAsState() ?: remember { mutableStateOf(emptySet()) }
    val isFav = song.id in favIds

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
        // album art small
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(SurfaceGrey),
            contentAlignment = Alignment.Center
        ) {
            if (song.albumArtUri != null) {
                val ctx = LocalContext.current
                AsyncImage(
                    model = remember(song.id) {
                        ImageRequest.Builder(ctx).data(song.albumArtUri).size(80)
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
        // More / Add to playlist
        Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(50)).clickable { showMenu = true },
            contentAlignment = Alignment.Center) {
            Icon(Icons.Default.MoreVert, "More", tint = TextDim, modifier = Modifier.size(16.dp))
        }
        // Playlist picker
        if (showMenu && playlistStore != null) {
            val playlists by playlistStore.playlists.collectAsState()
            AlertDialog(
                onDismissRequest = { showMenu = false },
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
                                        showMenu = false
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
                confirmButton = { TextButton(onClick = { showMenu = false }) { Text("Done", color = PurpleAccent) } }
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
