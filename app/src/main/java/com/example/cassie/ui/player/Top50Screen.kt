package com.example.cassie.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.cassie.data.media.ListeningCounter
import com.example.cassie.data.media.PlaybackManager
import com.example.cassie.data.media.Song

// ── Palette ───────────────────────────────────────────────────────
private val PureBlack     = Color(0xFF000000)
private val CardGrey      = Color(0xFF1E1E1E)
private val SurfaceGrey   = Color(0xFF282828)
private val PurpleAccent  = Color(0xFFBB86FC)
private val TextPrimary   = Color.White
private val TextSecondary = Color.White.copy(alpha = 0.6f)
private val TextDim       = Color.White.copy(alpha = 0.35f)

@Composable
fun Top50Screen(
    songs: List<Song>,
    listeningCounter: ListeningCounter?,
    playbackManager: PlaybackManager?,
    favoritesStore: FavoritesStore? = null,
    onSongClick: (Song) -> Unit,
    onBack: () -> Unit,
) {
    val top50 = remember(songs, listeningCounter?.counts?.value) {
        listeningCounter?.getTop50(songs) ?: emptyList()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(PureBlack)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary) }
                    Text("TOP 50", color = TextDim, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Text("${top50.size} songs", color = TextDim, fontSize = 11.sp)
                }
            }

            if (top50.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.TrendingUp, null, tint = TextDim, modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No plays yet", color = TextSecondary, fontSize = 16.sp)
                            Text("Start listening to build your chart!", color = TextDim, fontSize = 13.sp)
                        }
                    }
                }
            }

            itemsIndexed(top50, key = { _, pair -> pair.first.id }) { index, (song, count) ->
                Top50Row(index + 1, song, count, onClick = { playbackManager?.play(song); onSongClick(song) })
            }
        }
    }
}

@Composable
private fun Top50Row(index: Int, song: Song, playCount: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // rank
        Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
            if (index == 1) {
                Icon(Icons.Default.MilitaryTech, null, tint = Color(0xFFFFD700), modifier = Modifier.size(24.dp))
            } else if (index == 2) {
                Icon(Icons.Default.MilitaryTech, null, tint = Color(0xFFC0C0C0), modifier = Modifier.size(22.dp))
            } else if (index == 3) {
                Icon(Icons.Default.MilitaryTech, null, tint = Color(0xFFCD7F32), modifier = Modifier.size(20.dp))
            } else {
                Text("$index", color = TextDim, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(10.dp))

        // album art
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)).background(SurfaceGrey),
            contentAlignment = Alignment.Center
        ) {
            if (song.albumArtUri != null) {
                AsyncImage(model = song.albumArtUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.MusicNote, null, tint = TextDim, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(song.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary, maxLines = 1)
            Text(song.artist, fontSize = 12.sp, color = TextSecondary, maxLines = 1)
        }

        Text("$playCount", color = PurpleAccent.copy(0.8f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(4.dp))
        Text("plays", color = TextDim, fontSize = 11.sp)
    }
}
