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
import com.example.cassie.data.media.PlaylistStore
import com.example.cassie.data.media.Song
import com.example.cassie.ui.theme.CassieColors
import com.example.cassie.ui.theme.CassieDialog
import androidx.compose.ui.platform.LocalContext

// ── Theme Tokens ──────────────────────────────────────────────────
private val PureBlack     = CassieColors.PureBlack
private val CardGrey      = CassieColors.CardGrey
private val SurfaceGrey   = CassieColors.SurfaceGrey
private val PurpleAccent  = CassieColors.PurpleAccent
private val TextPrimary   = CassieColors.TextPrimary
private val TextSecondary = CassieColors.TextSecondary
private val TextDim       = CassieColors.TextDim

data class ArtistGroup(
    val artist: String,
    val songs: List<Song>,
)

@Composable
fun ArtistScreen(
    songs: List<Song>,
    playbackManager: PlaybackManager?,
    playlistStore: PlaylistStore? = null,
    favoritesStore: FavoritesStore? = null,
    onSongClick: (Song) -> Unit,
    onBack: () -> Unit,
) {
    val artists = remember(songs) {
        songs.groupBy { it.artist }.map { (artistName, artistSongs) ->
            ArtistGroup(artist = artistName, songs = artistSongs)
        }.sortedBy { it.artist }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary) }
                    Text("ARTISTS", color = TextDim, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Spacer(Modifier.width(48.dp))
                }
            }

            if (artists.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                        Text("No artists found", color = TextDim, fontSize = 16.sp)
                    }
                }
            }

            items(artists, key = { it.artist }) { artist ->
                ArtistCard(artist, playbackManager, playlistStore, favoritesStore, onSongClick)
            }
        }
    }
}

@Composable
private fun ArtistCard(
    artist: ArtistGroup,
    playbackManager: PlaybackManager?,
    playlistStore: PlaylistStore?,
    favoritesStore: FavoritesStore?,
    onSongClick: (Song) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current

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
            // artist avatar — first song's album art, or default
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SurfaceGrey),
                contentAlignment = Alignment.Center
            ) {
                val firstSong = artist.songs.firstOrNull()
                if (firstSong?.albumArtUri != null) {
                    AsyncImage(
                        model = remember(firstSong.id) {
                            ImageRequest.Builder(context).data(firstSong.albumArtUri).size(96)
                                .memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED).build()
                        },
                        contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, null, tint = PurpleAccent.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(artist.artist, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary, maxLines = 1)
                Text("${artist.songs.size} songs", fontSize = 12.sp, color = TextSecondary, maxLines = 1)
            }
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = TextDim, modifier = Modifier.size(20.dp))
        }

        if (expanded) {
            HorizontalDivider(color = TextDim.copy(alpha = 0.1f))
            artist.songs.forEach { song ->
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
                    // add to playlist
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(PurpleAccent.copy(0.15f)).clickable { showPlaylistPicker = song.id },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, tint = PurpleAccent.copy(0.7f), modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    // play — queue the entire artist's songs
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(TextDim.copy(0.1f)).clickable {
                            playbackManager?.playInContext(song, artist.songs)
                            onSongClick(song)
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, "Play", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }

                // playlist picker
                if (showPlaylistPicker == song.id && playlistStore != null) {
                    val playlists by playlistStore.playlists.collectAsState()
                    CassieDialog(
                        onDismissRequest = { showPlaylistPicker = null },
                        dialogTitle = { Text("Add to Playlist", color = TextPrimary, fontWeight = FontWeight.Bold) },
                        dialogText = {
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
                                            Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = PurpleAccent, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(10.dp))
                                            Text(pl.name, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        },
                        dialogConfirmButton = { TextButton(onClick = { showPlaylistPicker = null }) { Text("Done", color = PurpleAccent) } }
                    )
                }
            }
        }
    }
}
