package com.example.cassie.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cassie.R
import com.example.cassie.data.media.PlaybackManager

// ── Palette ──
private val CardGrey      = Color(0xFF1E1E1E)
private val TextPrimary   = Color.White
private val TextDim       = Color.White.copy(alpha = 0.35f)
private val PurpleAccent  = Color(0xFFBB86FC)

// ── Moods ──
enum class PenguinMood(
    val drawableId: Int,
    val label: String,
    val messages: List<String>,
) {
    GREETING(R.drawable.greeting_penguin, "Ahem", listOf(
        "Pick a song already.",
        "You gonna play something or what",
        "I don't sing I judge.",
        "Your music taste is showing.",
    )),
    EXCITED(R.drawable.starry_penguin, "Banger", listOf(
        "This slaps.",
        "Okay this goes hard.",
        "Banger detected.",
        "Who turned up the heat.",
    )),
    CHILL(R.drawable.chill_penguin, "Vibes", listOf(
        "Vibes are immaculate.",
        "I fw this heavy.",
        "Now this is a vibe.",
        "This is a whole mood.",
    )),
    SLEEPY(R.drawable.sleepy_penguin, "Mellow", listOf(
        "This is nap music.",
        "Too chill for my own good.",
        "Yawn... in a good way.",
        "Putting me to sleep literally.",
    )),
    LOVE(R.drawable.love_penguin, "Feels", listOf(
        "This hits different.",
        "Right in the feels.",
        "This one's for the ex.",
        "Who hurt you.",
    )),
    SAD(R.drawable.sad_penguin, "Emo", listOf(
        "Who's cutting onions.",
        "It's not a phase mom.",
        "This is my villain arc.",
        "The rain... it's here.",
    )),
}

// ── Mood mapper: genre string -> PenguinMood ──
private fun moodForGenre(genre: String): PenguinMood {
    val g = genre.lowercase().trim()
    return when {
        g.contains("pop") || g.contains("dance") || g.contains("electronic") ||
        g.contains("edm") || g.contains("house") || g.contains("techno") ||
        g.contains("disco") || g.contains("funk") -> PenguinMood.EXCITED

        g.contains("hip") || g.contains("rap") || g.contains("r&b") ||
        g.contains("soul") || g.contains("reggae") || g.contains("lofi") ||
        g.contains("jazz") || g.contains("blues") -> PenguinMood.CHILL

        g.contains("classical") || g.contains("acoustic") || g.contains("ambient") ||
        g.contains("folk") || g.contains("country") || g.contains("instrumental") ||
        g.contains("piano") -> PenguinMood.SLEEPY

        g.contains("love") || g.contains("romance") || g.contains("ballad") ||
        g.contains("indie") -> PenguinMood.LOVE

        g.contains("sad") || g.contains("melancholic") || g.contains("emo") ||
        g.contains("gospel") -> PenguinMood.SAD

        g.contains("rock") || g.contains("metal") || g.contains("punk") ||
        g.contains("alternative") || g.contains("hardcore") -> PenguinMood.EXCITED

        genre.isBlank() -> PenguinMood.GREETING
        else -> PenguinMood.CHILL
    }
}

// ── Mascot Mood Card ──
@Composable
fun MascotMoodCard(
    playbackManager: PlaybackManager?,
) {
    val state = playbackManager?.let { it.playerState.collectAsState() }
    val currentSong = state?.value?.currentSong

    val mood = remember(currentSong) {
        if (currentSong != null) {
            val genre = currentSong.genre
            if (genre.isNotBlank()) moodForGenre(genre)
            else PenguinMood.GREETING
        } else {
            PenguinMood.GREETING
        }
    }

    var messageIndex by remember(mood) { mutableIntStateOf(kotlin.random.Random.nextInt(mood.messages.size)) }
    val message = mood.messages[messageIndex]

    fun nextMessage() {
        messageIndex = (messageIndex + 1) % mood.messages.size
    }

    // gentle floating animation for the penguin
    val infinite = rememberInfiniteTransition(label = "penguinFloat")
    val floatOffset by infinite.animateFloat(
        initialValue = 0f, targetValue = -4f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "float",
    )

    // full-bleed card -- no gaps, tight rectangle
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardGrey)
            .clickable { nextMessage() },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // ── Speech Bubble (using PNG, no canvas arrow) ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
            ) {
                // bubble background image fills the box
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.bubble_chat),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                )

                // text overlaid on the bubble
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    // mood label
                    Text(
                        text = mood.label,
                        color = PurpleAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = message,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )

                    if (currentSong != null && currentSong.genre.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${currentSong.genre}",
                            color = TextDim,
                            fontSize = 10.sp,
                        )
                    }
                }
            }

            // ── Penguin (zoomed in to fill container) ──
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .offset(y = floatOffset.dp),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = mood.drawableId),
                    contentDescription = "Cassie Penguin",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}
