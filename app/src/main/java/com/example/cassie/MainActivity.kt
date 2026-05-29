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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.NotificationUtil
import com.example.cassie.data.media.EqualizerManager
import com.example.cassie.data.media.FavoritesStore
import com.example.cassie.data.media.PersistenceManager
import com.example.cassie.data.media.PlaybackManager
import com.example.cassie.data.media.PlaylistStore
import com.example.cassie.ui.home.HomeScreen
import com.example.cassie.ui.player.AlbumScreen
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

    // ── content ──────────────────────────────────────────────────
    if (!permissionGranted && permissionRequested) {
        PermissionDeniedScreen(onRetry = { permissionLauncher.launch(requiredPermission) })
        return
    }

    // ── system back button ─────────────────────────────────────────
    BackHandler(currentScreen != Screen.Home) {
        currentScreen = Screen.Home
    }

    // ── media session (notification controls) ──────────────────────
    LaunchedEffect(Unit) {
        playbackManager.initMediaSession(ctx as android.app.Activity)
    }

    val songs = homeViewModel?.uiState?.collectAsState()?.value?.songs ?: emptyList()

    // Shared LazyListState so scroll position survives AnimatedContent destroy/recreate
    val homeListState = rememberLazyListState()

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
