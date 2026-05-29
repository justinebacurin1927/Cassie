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
    onAlbumClick: (String) -> Unit = {},
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary) }
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
                AlbumCard(album, playbackManager, playlistStore, onSongClick, onAlbumClick)
            }
        }
    }
}

@Composable
private fun AlbumCard(album: AlbumGroup, playbackManager: PlaybackManager?, playlistStore: PlaylistStore?, onSongClick: (Song) -> Unit, onAlbumClick: (String) -> Unit = {}) {
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
                .clickable { onAlbumClick(album.albumName) }
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
                    AsyncImage(
                        model = remember(firstSong.id) {
                            ImageRequest.Builder(context).data(firstSong.albumArtUri).size(96)
                                .memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED).build()
                        },
                        contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Album, null, tint = TextDim, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(album.albumName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary, maxLines = 1)
                Text(album.artist, fontSize = 12.sp, color = TextSecondary, maxLines = 1)
            }
            Text("${album.songs.size} songs", color = TextDim, fontSize = 13.sp)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.ChevronRight, null, tint = TextDim, modifier = Modifier.size(20.dp))
        }
    }
}
