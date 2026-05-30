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
import com.example.cassie.data.media.OnlineMoodDetector
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
    GREETING(R.drawable.greeting_penguin, "Cassie", listOf(
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

// ── Mood keyword trees ──
// Each mood has a list of keyword sets ordered by tier (higher tier = more points).
// Words are tokenized from the title and matched one by one.

private data class KeywordTier(val words: Set<String>, val score: Int)

private val loveKeywords = listOf(
    KeywordTier(setOf("love", "loving", "loved"), 3),
    KeywordTier(setOf("kiss", "kissing", "honey", "sweetheart", "darling", "adore", "cherish", "beloved"), 2),
    KeywordTier(setOf("baby", "sweet", "heart", "hold", "mine", "cherry", "angel", "rose", "together", "forever"), 1),
)

private val sadKeywords = listOf(
    KeywordTier(setOf("cry", "crying", "tears", "tearing", "goodbye", "lonely", "hurt", "broken", "bleeding", "funeral", "grave", "misery"), 3),
    KeywordTier(setOf("pain", "lost", "sorry", "alone", "ghost", "die", "death", "nightmare", "shadow", "haunt"), 2),
    KeywordTier(setOf("gone", "dark", "cold", "rain", "storm", "blues", "nowhere", "empty", "hollow", "sigh"), 1),
)

private val excitedKeywords = listOf(
    KeywordTier(setOf("dance", "dancing", "party", "partying", "groove", "grooving", "wild", "energy", "explode", "explosion"), 3),
    KeywordTier(setOf("boom", "bang", "burn", "burning", "fire", "hot", "fast", "speed", "jump", "fly"), 2),
    KeywordTier(setOf("run", "high", "power", "strong", "loud", "beat", "rhythm", "turn", "go"), 1),
)

private val sleepyKeywords = listOf(
    KeywordTier(setOf("sleep", "sleeping", "asleep", "dream", "dreaming", "dreams", "lullaby", "peaceful", "silence"), 3),
    KeywordTier(setOf("calm", "peace", "moonlight", "starlight", "whisper", "whispering", "night", "moon", "star"), 2),
    KeywordTier(setOf("river", "ocean", "sea", "cloud", "soft", "warm", "gentle", "quiet", "tired", "yawn"), 1),
)

private val chillKeywords = listOf(
    KeywordTier(setOf("vibe", "vibes", "smooth", "breeze", "coastal", "island", "tropical", "casual", "laid"), 3),
    KeywordTier(setOf("cool", "slow", "cruise", "cruising", "coffee", "float", "floating", "drift", "drifting", "wave"), 2),
    KeywordTier(setOf("sun", "sand", "shore", "beach", "light", "easy", "roll", "flow", "golden", "warm"), 1),
)

private val negativeLove = setOf(
    "hate", "lost", "without", "fake", "lies", "liar", "toxic", "poison", "broken",
)

// ── Tokenize title into individual words (split by spaces/punctuation) ──
private fun tokenize(title: String): List<String> {
    return title.lowercase()
        .replace(Regex("[^a-z0-9 ']"), " ")
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
}

// ── Score a set of keywords against the word list ──
private fun scoreKeywords(words: List<String>, tiers: List<KeywordTier>, negatives: Set<String> = emptySet()): Int {
    var score = 0
    // Check for negative keywords first
    for (word in words) {
        if (word in negatives) {
            score -= 3
        }
    }
    // Check each tier: higher tiers checked first, matched words removed
    val remaining = words.toMutableList()
    for (tier in tiers) {
        var matched = false
        val iter = remaining.iterator()
        while (iter.hasNext()) {
            val word = iter.next()
            if (word in tier.words) {
                score += tier.score
                iter.remove()
                matched = true
            }
        }
        // If we matched in this tier, check next tier too
        if (!matched) continue
    }
    return score
}

// ── Mood mapper: tokenize title -> score each mood -> pick winner ──
private fun moodForTitle(title: String): PenguinMood {
    val words = tokenize(title)
    if (words.isEmpty()) return PenguinMood.GREETING

    val loveScore = scoreKeywords(words, loveKeywords, negativeLove)
    val sadScore = scoreKeywords(words, sadKeywords)
    val excitedScore = scoreKeywords(words, excitedKeywords)
    val sleepyScore = scoreKeywords(words, sleepyKeywords)
    val chillScore = scoreKeywords(words, chillKeywords)

    val maxScore = maxOf(loveScore, sadScore, excitedScore, sleepyScore, chillScore)
    if (maxScore <= 0) return PenguinMood.GREETING

    return when (maxScore) {
        loveScore -> PenguinMood.LOVE
        sadScore -> PenguinMood.SAD
        excitedScore -> PenguinMood.EXCITED
        sleepyScore -> PenguinMood.SLEEPY
        chillScore -> PenguinMood.CHILL
        else -> PenguinMood.GREETING
    }
}

// ── Mascot Mood Card ──
@Composable
fun MascotMoodCard(
    playbackManager: PlaybackManager?,
    onlineMoodDetector: OnlineMoodDetector? = null,
) {
    // Create detector inside body so remember works reliably
    val detector = remember { onlineMoodDetector ?: OnlineMoodDetector() }

    val state = playbackManager?.let { it.playerState.collectAsState() }
    val currentSong = state?.value?.currentSong

    // Local keyword-based fallback
    val localMood = remember(currentSong) {
        if (currentSong != null) moodForTitle(currentSong.title)
        else PenguinMood.GREETING
    }

    // Online API mood (starts -1 = pending, 0 = GREETING, 1-5 = specific moods)
    var onlineMoodIndex by remember { mutableIntStateOf(-1) }

    // Fetch from Last.fm API whenever the song changes
    LaunchedEffect(currentSong) {
        try {
            if (currentSong != null) {
                val result = detector.detectMood(currentSong)
                onlineMoodIndex = result
            } else {
                onlineMoodIndex = -1
            }
        } catch (_: Exception) {
            onlineMoodIndex = 0 // fallback to GREETING
        }
    }

    // Final mood: smart merge of online + local detection
    // - If online returns a specific mood (1-5) → use it
    // - If online returns GREETING (0) but local detected something → use local
    // - If online is still loading (-1) → just show GREETING, NO flash of localMood
    val mood = remember(localMood, onlineMoodIndex, currentSong) {
        if (currentSong != null) {
            when {
                onlineMoodIndex > 0 -> PenguinMood.entries[onlineMoodIndex]        // online found a mood
                onlineMoodIndex == 0 && localMood != PenguinMood.GREETING -> localMood // online said GREETING but local knows better
                else -> PenguinMood.GREETING                                         // loading or both agree on GREETING
            }
        } else {
            PenguinMood.GREETING
        }
    }

    var messageIndex by remember(mood) { mutableIntStateOf(kotlin.random.Random.nextInt(mood.messages.size)) }
    val message = mood.messages[messageIndex]

    fun nextMessage() {
        messageIndex = (messageIndex + 1) % mood.messages.size
    }

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
            // ── Text area (directly on card background, no bubble PNG) ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
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
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )

                if (currentSong != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = currentSong.title,
                        color = TextDim,
                        fontSize = 10.sp,
                        maxLines = 1,
                    )
                }
            }

            // ── Penguin (zoomed in to fill container) ──
            Box(
                modifier = Modifier
                    .size(100.dp),
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
