package com.example.cassie

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cassie.data.media.EqualizerManager
import com.example.cassie.data.media.FavoritesStore
import com.example.cassie.data.media.PersistenceManager
import com.example.cassie.data.media.PlaybackManager
import com.example.cassie.data.media.PlaylistStore
import com.example.cassie.ui.home.HomeScreen
import com.example.cassie.ui.player.AlbumScreen
import com.example.cassie.ui.player.MiniPlayer
import com.example.cassie.ui.player.NowPlayingScreen
import com.example.cassie.ui.player.PlaylistScreen
import com.example.cassie.ui.player.Top50Screen
import com.example.cassie.ui.theme.CassieTheme

sealed class Screen {
    data object Home : Screen()
    data object NowPlaying : Screen()
    data object Albums : Screen()
    data object Playlists : Screen()
    data object Top50 : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.insetsController?.apply {
            hide(WindowInsets.Type.statusBars())
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        // notification channel for media playback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                "cassie_playback", "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
        }
        setContent {
            CassieTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    CassieApp()
                }
            }
        }
    }
}

// ── Nav bar palette ──────────────────────────────────────────────
private val CardGrey     = Color(0xFF1E1E1E)
private val SurfaceGrey  = Color(0xFF282828)
private val PurpleAccent = Color(0xFFBB86FC)
private val TextPrimary  = Color.White
private val TextDim      = Color.White.copy(alpha = 0.35f)

@Composable
private fun CassieApp() {
    val playbackManager: PlaybackManager = viewModel()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val persistenceManager = remember { PersistenceManager(ctx) }
    val playlistStore = remember { PlaylistStore(persistenceManager) }
    val favoritesStore = remember { FavoritesStore(persistenceManager) }
    val equalizerManager = remember { EqualizerManager() }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var homeViewModel by remember { mutableStateOf<com.example.cassie.ui.home.HomeViewModel?>(null) }

    // ── permission state ──────────────────────────────────────────
    val requiredPermission = if (Build.VERSION.SDK_INT >= 33)
        Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    var permissionGranted by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }

    // check permission on composition
    LaunchedEffect(Unit) {
        permissionGranted = ContextCompat.checkSelfPermission(
            ctx, requiredPermission
        ) == PackageManager.PERMISSION_GRANTED
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        permissionRequested = true
        if (granted) {
            homeViewModel?.refreshLibrary()
        }
    }

    // auto-request on first launch
    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(requiredPermission)
        }
    }

    // ── request notification permission (Android 13+) ──────────────
    if (Build.VERSION.SDK_INT >= 33) {
        val notifPermission = Manifest.permission.POST_NOTIFICATIONS
        val notifLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { /* granted or not — media session handles gracefully */ }
        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(ctx, notifPermission) != PackageManager.PERMISSION_GRANTED) {
                notifLauncher.launch(notifPermission)
            }
        }
    }

    // ── content ──────────────────────────────────────────────────
    if (!permissionGranted && permissionRequested) {
        PermissionDeniedScreen(onRetry = { permissionLauncher.launch(requiredPermission) })
        return
    }

    // ── system back button ─────────────────────────────────────────
    BackHandler(currentScreen != Screen.Home) {
        currentScreen = Screen.Home
    }

    val songs = homeViewModel?.uiState?.collectAsState()?.value?.songs ?: emptyList()

    // Shared LazyListState so scroll position survives AnimatedContent destroy/recreate
    val homeListState = rememberLazyListState()

    // ── nav bar / mini player visibility ────────────────────────────
    val showNavBar = currentScreen !is Screen.NowPlaying
    val playerState = playbackManager.playerState.collectAsState()
    val showMiniPlayer = showNavBar && playerState.value.currentSong != null

    val navBarHeight = 56.dp
    val miniPlayerHeight = 68.dp
    val totalBottomOverlay = if (showMiniPlayer) navBarHeight + miniPlayerHeight else navBarHeight

    // ── layout ─────────────────────────────────────────────────────
    Box(Modifier.fillMaxSize()) {
        // main content area with bottom padding for overlays
        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = if (showNavBar) totalBottomOverlay else 0.dp)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith
                        fadeOut(animationSpec = tween(200))
                },
                label = "nav_transition"
            ) { screen ->
                when (screen) {
                    Screen.Home -> {
                        val vm: com.example.cassie.ui.home.HomeViewModel = viewModel()
                        LaunchedEffect(Unit) { homeViewModel = vm }

                        HomeScreen(
                            viewModel = vm,
                            playbackManager = playbackManager,
                            playlistStore = playlistStore,
                            favoritesStore = favoritesStore,
                            listState = homeListState,
                            onNavigateToPlayer = { currentScreen = Screen.NowPlaying },
                            onNavigateToAlbums = { currentScreen = Screen.Albums },
                            onNavigateToPlaylists = { currentScreen = Screen.Playlists },
                            onNavigateToTop50 = { currentScreen = Screen.Top50 },
                        )
                    }
                    Screen.NowPlaying -> {
                        NowPlayingScreen(
                            playbackManager = playbackManager,
                            equalizerManager = equalizerManager,
                            onClose = { currentScreen = Screen.Home },
                        )
                    }
                    Screen.Albums -> {
                        AlbumScreen(
                            songs = songs,
                            playbackManager = playbackManager,
                            playlistStore = playlistStore,
                            favoritesStore = favoritesStore,
                            onSongClick = { currentScreen = Screen.NowPlaying },
                            onBack = { currentScreen = Screen.Home },
                        )
                    }
                    Screen.Playlists -> {
                        PlaylistScreen(
                            songs = songs,
                            playlistStore = playlistStore,
                            favoritesStore = favoritesStore,
                            playbackManager = playbackManager,
                            onSongClick = { currentScreen = Screen.NowPlaying },
                            onBack = { currentScreen = Screen.Home },
                        )
                    }
                    Screen.Top50 -> {
                        Top50Screen(
                            songs = songs,
                            listeningCounter = playbackManager.listeningCounter,
                            playbackManager = playbackManager,
                            favoritesStore = favoritesStore,
                            onSongClick = { currentScreen = Screen.NowPlaying },
                            onBack = { currentScreen = Screen.Home },
                        )
                    }
                }
            }
        }

        // mini player (above nav bar)
        if (showMiniPlayer) {
            MiniPlayer(
                playbackManager = playbackManager,
                onClick = { currentScreen = Screen.NowPlaying },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = navBarHeight)
            )
        }

        // nav bar at the very bottom
        if (showNavBar) {
            StadiumNavBar(
                activeItem = when (currentScreen) {
                    Screen.Home -> "home"
                    Screen.Albums -> "albums"
                    Screen.Playlists -> "playlists"
                    Screen.Top50 -> "top50"
                    else -> "home"
                },
                onItemSelected = { id ->
                    currentScreen = when (id) {
                        "home" -> Screen.Home
                        "albums" -> Screen.Albums
                        "playlists" -> Screen.Playlists
                        "top50" -> Screen.Top50
                        else -> Screen.Home
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// ── Bottom Nav Bar (Stadium / Capsule Design) ────────────────────
private data class NavBarItem(
    val id: String,
    val icon: ImageVector,
    val label: String,
)

private val navItems = listOf(
    NavBarItem("home", Icons.Default.Home, "Home"),
    NavBarItem("albums", Icons.Default.Album, "Albums"),
    NavBarItem("playlists", Icons.AutoMirrored.Filled.QueueMusic, "Playlists"),
    NavBarItem("top50", Icons.AutoMirrored.Filled.TrendingUp, "Top 50"),
)

@Composable
private fun StadiumNavBar(
    activeItem: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
        color = CardGrey,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 10.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Active pill on far left
            val activeItemData = navItems.first { it.id == activeItem }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(SurfaceGrey)
                    .clickable { onItemSelected(activeItem) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    activeItemData.icon,
                    null,
                    tint = PurpleAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    activeItemData.label,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Spacer pushes inactive items to the right
            Spacer(Modifier.weight(1f))

            // Inactive items — icons only
            navItems.filter { it.id != activeItem }.forEach { item ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(TextDim.copy(alpha = 0.08f))
                        .clickable { onItemSelected(item.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = TextDim,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
            }
        }
    }
}

@Composable
private fun PermissionDeniedScreen(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF2A0A4A),
                        Color(0xFF1A0033),
                        Color(0xFF0D001A),
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.MusicNote, null, tint = Color(0xFFBB86FC), modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(20.dp))
            Text(
                "Music Permission Needed",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Cassie needs access to your music library to play your songs.",
                color = Color.White.copy(0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC)),
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text("Grant Permission", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
