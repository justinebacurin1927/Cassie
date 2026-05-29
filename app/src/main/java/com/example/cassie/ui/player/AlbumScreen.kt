package com.example.cassie.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.cassie.data.media.FavoritesStore
import com.example.cassie.data.media.PlaybackManager
import com.example.cassie.data.media.PlaylistStore
import com.example.cassie.data.media.Song

// ── Palette ───────────────────────────────────────────────────────
private val PureBlack     = Color(0xFF000000)
private val DarkGrey      = Color(0xFF121212)
private val CardGrey      = Color(0xFF1E1E1E)
private val SurfaceGrey   = Color(0xFF282828)
private val PurpleAccent  = Color(0xFFBB86FC)
private val TextPrimary   = Color.White
private val TextSecondary = Color.White.copy(alpha = 0.6f)
private val TextDim       = Color.White.copy(alpha = 0.35f)

data class AlbumGroup(
    val albumName: String,
    val artist: String,
    val songs: List<Song>,
)

@Composable
fun AlbumScreen(
    songs: List<Song>,
    playbackManager: PlaybackManager?,
    playlistStore: PlaylistStore? = null,
    favoritesStore: FavoritesStore? = null,
    onSongClick: (Song) -> Unit,
    onBack: () -> Unit,
) {
    val albums = remember(songs) {
        songs.groupBy { it.album }.map { (albumName, albumSongs) ->
            AlbumGroup(albumName = albumName, artist = albumSongs.first().artist, songs = albumSongs)
        }.sortedBy { it.albumName }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary) }
                    Text("ALBUMS", color = TextDim, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Spacer(Modifier.width(48.dp))
                }
            }

            if (albums.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                        Text("No albums found", color = TextDim, fontSize = 16.sp)
                    }
                }
            }

            items(albums, key = { it.albumName }) { album ->
                AlbumCard(album, playbackManager, playlistStore, onSongClick)
            }
        }
    }
}

@Composable
private fun AlbumCard(album: AlbumGroup, playbackManager: PlaybackManager?, playlistStore: PlaylistStore?, onSongClick: (Song) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardGrey)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // album art
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceGrey),
                contentAlignment = Alignment.Center
            ) {
                val firstSong = album.songs.firstOrNull()
                if (firstSong?.albumArtUri != null) {
                    AsyncImage(model = firstSong.albumArtUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.Album, null, tint = TextDim, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(album.albumName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary, maxLines = 1)
                Text(album.artist, fontSize = 12.sp, color = TextSecondary, maxLines = 1)
            }
            Text("${album.songs.size}", color = TextDim, fontSize = 13.sp)
            Spacer(Modifier.width(4.dp))
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = TextDim, modifier = Modifier.size(20.dp))
        }

        if (expanded) {
            HorizontalDivider(color = TextDim.copy(alpha = 0.1f))
            album.songs.forEach { song ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // album art small
                    Box(
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)).background(SurfaceGrey),
                        contentAlignment = Alignment.Center
                    ) {
                        if (song.albumArtUri != null) {
                            AsyncImage(model = song.albumArtUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Icon(Icons.Default.MusicNote, null, tint = TextDim, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(song.title, color = TextPrimary, fontSize = 14.sp, maxLines = 1, modifier = Modifier.weight(1f))
                    // add to playlist
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(PurpleAccent.copy(0.15f)).clickable { showPlaylistPicker = song.id },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlaylistAdd, null, tint = PurpleAccent.copy(0.7f), modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    // play
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(TextDim.copy(0.1f)).clickable { playbackManager?.play(song); onSongClick(song) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, "Play", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }

                // playlist picker
                if (showPlaylistPicker == song.id && playlistStore != null) {
                    val playlists by playlistStore.playlists.collectAsState()
                    AlertDialog(
                        onDismissRequest = { showPlaylistPicker = null },
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
                                                showPlaylistPicker = null
                                            }.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.QueueMusic, null, tint = PurpleAccent, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(10.dp))
                                            Text(pl.name, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = { TextButton(onClick = { showPlaylistPicker = null }) { Text("Done", color = PurpleAccent) } }
                    )
                }
            }
        }
    }
}
