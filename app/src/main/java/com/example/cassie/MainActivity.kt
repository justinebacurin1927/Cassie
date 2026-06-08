package com.example.cassie

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
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
import com.example.cassie.data.media.Playlist
import com.example.cassie.data.media.PlaylistStore
import com.example.cassie.ui.home.HomeScreen
import com.example.cassie.ui.player.AlbumDetailScreen
import com.example.cassie.ui.player.AlbumScreen
import com.example.cassie.ui.player.ArtistScreen
import com.example.cassie.ui.player.MiniPlayer
import com.example.cassie.ui.player.NowPlayingScreen
import com.example.cassie.ui.player.PlaylistDetailScreen
import com.example.cassie.ui.player.PlaylistScreen
import com.example.cassie.ui.player.Top50Screen
import com.example.cassie.ui.theme.CassieTheme
import com.example.cassie.ui.theme.CassieColors
import kotlin.math.roundToInt

sealed class Screen {
    data object Home : Screen()
    data object NowPlaying : Screen()
    data object Artists : Screen()
    data object Albums : Screen()
    data class AlbumDetail(val albumName: String) : Screen()
    data object Playlists : Screen()
    data class PlaylistDetail(val playlist: Playlist) : Screen()
    data object Top50 : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
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

// ── Theme Tokens ──────────────────────────────────────────────────
private val CardGrey     = CassieColors.CardGrey
private val SurfaceGrey  = CassieColors.SurfaceGrey
private val PurpleAccent = CassieColors.PurpleAccent
private val TextPrimary  = CassieColors.TextPrimary
private val TextDim      = CassieColors.TextDim

@Composable
private fun CassieApp() {
    val playbackManager: PlaybackManager = viewModel()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val persistenceManager = remember { PersistenceManager(ctx) }
    val playlistStore = remember { PlaylistStore(persistenceManager) }
    val favoritesStore = remember { FavoritesStore(persistenceManager) }
    val equalizerManager = remember { EqualizerManager() }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    // Proper back stack: pressing back pops one entry. Tab switches
    // (bottom nav) reset the stack to a single screen.
    var backStack by remember { mutableStateOf(listOf<Screen>(Screen.Home)) }
    var homeViewModel by remember { mutableStateOf<com.example.cassie.ui.home.HomeViewModel?>(null) }
    // NowPlaying is a MODAL: it doesn't go on the back stack. But when
    // it closes, we want to return to the screen the user was on
    // (Top 50, Artists, Albums, etc.) — not always Home. So we
    // remember the previous screen at the moment NowPlaying opens
    // and restore it on close.
    var previousScreenBeforeNowPlaying by remember { mutableStateOf<Screen?>(null) }

    fun navigateTo(screen: Screen) {
        backStack = backStack + screen
        currentScreen = screen
    }
    fun navigateBack() {
        if (backStack.size > 1) {
            backStack = backStack.dropLast(1)
            currentScreen = backStack.last()
        }
    }
    fun resetTo(screen: Screen) {
        backStack = listOf(screen)
        currentScreen = screen
    }
    /**
     * Open NowPlaying as a modal. Saves the current screen so that
     * closing NowPlaying (via X, back, or swipe-down) returns the
     * user to where they were — not always Home.
     */
    fun openNowPlaying() {
        previousScreenBeforeNowPlaying = currentScreen
        currentScreen = Screen.NowPlaying
    }
    /**
     * Close NowPlaying and restore the screen the user was on when
     * they opened it. Falls back to Home if unknown.
     */
    fun closeNowPlaying() {
        currentScreen = previousScreenBeforeNowPlaying ?: Screen.Home
        previousScreenBeforeNowPlaying = null
    }

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
    // Sub-screen on the back stack → pop it.
    // NowPlaying is a modal (not on the stack) → treat back like close.
    // Home (root) → finish the activity (the system default).
    val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
    BackHandler(enabled = true) {
        when {
            backStack.size > 1 -> navigateBack()
            currentScreen is Screen.NowPlaying -> closeNowPlaying()
            else -> activity?.finish()
        }
    }

    val songs = homeViewModel?.uiState?.collectAsState()?.value?.songs ?: emptyList()

    // Shared LazyListState so scroll position survives AnimatedContent destroy/recreate
    val homeListState = rememberLazyListState()

    // ── nav bar / mini player visibility ────────────────────────────
    val showNavBar = currentScreen !is Screen.NowPlaying

    val playerState = playbackManager.playerState.collectAsState()
    val showMiniPlayer = showNavBar && playerState.value.currentSong != null

    val navBarHeight = 68.dp
    val miniPlayerHeight = 60.dp
    val overlayGap = 8.dp
    val totalBottomOverlay = if (showMiniPlayer) navBarHeight + miniPlayerHeight + overlayGap else navBarHeight

    // ── layout ─────────────────────────────────────────────────────
    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        // main content area with bottom padding for overlays
        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = if (showNavBar) totalBottomOverlay else 0.dp)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(tween(350, easing = FastOutSlowInEasing))
                        .togetherWith(fadeOut(tween(250)))
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
                            onNavigateToPlayer = { openNowPlaying() },
                            onNavigateToAlbums = { currentScreen = Screen.Albums },
                            onNavigateToArtists = { currentScreen = Screen.Artists },
                            onNavigateToPlaylists = { navigateTo(Screen.Playlists) },
                            onPlaylistClick = { playlist -> navigateTo(Screen.PlaylistDetail(playlist)) },
                            onNavigateToTop50 = { navigateTo(Screen.Top50) },
                        )
                    }
                    Screen.NowPlaying -> {
                        NowPlayingScreen(
                            playbackManager = playbackManager,
                            equalizerManager = equalizerManager,
                            onClose = { closeNowPlaying() },
                        )
                    }
                    Screen.Artists -> {
                        ArtistScreen(
                            songs = songs,
                            playbackManager = playbackManager,
                            playlistStore = playlistStore,
                            favoritesStore = favoritesStore,
                            onSongClick = { openNowPlaying() },
                            onBack = { navigateBack() },
                        )
                    }
                    Screen.Albums -> {
                        AlbumScreen(
                            songs = songs,
                            playbackManager = playbackManager,
                            playlistStore = playlistStore,
                            favoritesStore = favoritesStore,
                            onSongClick = { openNowPlaying() },
                            onAlbumClick = { albumName -> navigateTo(Screen.AlbumDetail(albumName)) },
                            onBack = { navigateBack() },
                        )
                    }
                    is Screen.AlbumDetail -> {
                        AlbumDetailScreen(
                            albumName = screen.albumName,
                            songs = songs,
                            playbackManager = playbackManager,
                            playlistStore = playlistStore,
                            favoritesStore = favoritesStore,
                            onSongClick = { openNowPlaying() },
                            onBack = { navigateBack() },
                        )
                    }
                    Screen.Playlists -> {
                        PlaylistScreen(
                            songs = songs,
                            playlistStore = playlistStore,
                            favoritesStore = favoritesStore,
                            playbackManager = playbackManager,
                            onSongClick = { openNowPlaying() },
                            onPlaylistClick = { playlist -> navigateTo(Screen.PlaylistDetail(playlist)) },
                            onBack = { navigateBack() },
                        )
                    }
                    is Screen.PlaylistDetail -> {
                        val pl = screen.playlist
                        PlaylistDetailScreen(
                            playlist = pl,
                            allSongs = songs,
                            playbackManager = playbackManager,
                            playlistStore = playlistStore,
                            favoritesStore = favoritesStore,
                            onSongClick = { openNowPlaying() },
                            onBack = { navigateBack() },
                        )
                    }
                    Screen.Top50 -> {
                        Top50Screen(
                            songs = songs,
                            playbackManager = playbackManager,
                            favoritesStore = favoritesStore,
                            onSongClick = { openNowPlaying() },
                            onBack = { navigateBack() },
                        )
                    }
                }
            }
        }

        // ── Bottom overlays: MiniPlayer stacked above NavBar ──
        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            // mini player (appears above nav bar)
            AnimatedVisibility(
                visible = showMiniPlayer,
                enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(200)),
            ) {
                MiniPlayer(
                    playbackManager = playbackManager,
                    onClick = { openNowPlaying() },
                )
            }

            // visual gap between mini player and nav bar
            if (showMiniPlayer) {
                Spacer(Modifier.height(overlayGap))
            }

            // nav bar at the very bottom
            if (showNavBar) {
                val activeId = when (currentScreen) {
                    Screen.Home -> "home"
                    Screen.Albums -> "albums"
                    Screen.Artists -> "artists"
                    Screen.Playlists, is Screen.PlaylistDetail -> "playlists"
                    Screen.Top50 -> "top50"
                    else -> "home"
                }
                StadiumNavBar(
                    activeItem = activeId,
                    onItemSelected = { id ->
                        val tabScreen: Screen = when (id) {
                            "home" -> Screen.Home
                            "albums" -> Screen.Albums
                            "artists" -> Screen.Artists
                            "playlists" -> Screen.Playlists
                            "top50" -> Screen.Top50
                            else -> Screen.Home
                        }
                        resetTo(tabScreen)
                    },

                )
            }
        }
    }
}

// ── Bottom Nav Bar (LumaBar Design) ────────────────────────────
// Faithful Compose translation of the LumaBar React component:
//   React                          Compose
//   ─────                          ──────
//   layoutId="active-indicator"    animated offset + blur glow
//   backdrop-blur-2xl              translucent black bg
//   from-blue-400 to-purple-500    Brush.linearGradient blue→purple
//   animate={{ scale: 1.4 }}       active scale spring
//   blur-2xl (24px)                24px RenderEffect blur (API 31+)
//   border-gray-200/50             white border at 15% alpha
//   z-50 / shadow                  8dp shadow
private data class NavBarItem(
    val id: String,
    val icon: ImageVector,
    val label: String,
)

private val navItems = listOf(
    NavBarItem("home", Icons.Default.Home, "Home"),
    NavBarItem("albums", Icons.Default.Album, "Albums"),
    NavBarItem("artists", Icons.Default.Person, "Artists"),
    NavBarItem("playlists", Icons.AutoMirrored.Filled.QueueMusic, "Playlists"),
    NavBarItem("top50", Icons.AutoMirrored.Filled.TrendingUp, "Top 50"),
)

// Glow colors: from-blue-400 to-purple-500
private val GlowStart = Color(0xFF60A5FA)
private val GlowEnd   = Color(0xFF8B5CF6)

@Composable
private fun StadiumNavBar(
    activeItem: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeIdx = navItems.indexOfFirst { it.id == activeItem }
    var rowWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val indicatorSizeDp = 48.dp
    val itemCount = navItems.size

    // ── indicator position in pixels (spring-driven like framer-motion) ──
    val indicatorSizePx = with(density) { indicatorSizeDp.toPx() }
    val itemWidthPx = if (rowWidth == 0 || itemCount == 0) 0f
                      else rowWidth.toFloat() / itemCount
    val targetOffsetPx = if (itemWidthPx > 0f) {
        itemWidthPx * activeIdx + (itemWidthPx - indicatorSizePx) / 2f
    } else 0f

    val indicatorOffset by animateFloatAsState(
        targetValue = targetOffsetPx,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessHigh,
        ),
        label = "indicatorSpring",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        // ── Pill container: backdrop-blur-2xl + border + shadow ──
        Box(
            modifier = Modifier
                .shadow(8.dp, RoundedCornerShape(50))
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.25f))  // dark:bg-black/20
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                .padding(6.dp),
        ) {
            // ── Layer 1: Glow indicator (behind items) ──
            //     Reference: w-16 h-16 bg-gradient-to-r from-blue-400 to-purple-500
            //                rounded-full blur-2xl -z-10
            if (rowWidth > 0) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(indicatorOffset.roundToInt(), 0) }
                        .size(indicatorSizeDp)
                        .background(
                            Brush.linearGradient(listOf(GlowStart, GlowEnd)),
                            RoundedCornerShape(50),
                        )
                        .graphicsLayer {
                            alpha = 0.35f
                            if (Build.VERSION.SDK_INT >= 31) {
                                renderEffect = android.graphics.RenderEffect
                                    .createBlurEffect(24f, 24f, android.graphics.Shader.TileMode.CLAMP)
                                    .asComposeRenderEffect()
                            }
                        },
                )
            }

            // ── Layer 2: Item buttons (on top of glow) ──
            //     Reference: flex items-center justify-center w-14 h-14
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { rowWidth = it.width },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                navItems.forEachIndexed { idx, item ->
                    val isActive = idx == activeIdx

                    // animate={{ scale: isActive ? 1.4 : 1 }}
                    val iconScale by animateFloatAsState(
                        targetValue = if (isActive) 1.35f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness    = Spring.StiffnessHigh,
                        ),
                        label = "iconScale",
                    )

                    // Reference: w-14 h-14 flex items-center justify-center
                    //            text-gray-600 dark:text-gray-300
                    IconButton(
                        onClick = { onItemSelected(item.id) },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            tint = if (isActive) Color.White
                                   else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(24.dp)
                                .scale(iconScale),
                        )
                    }
                }
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
