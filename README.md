# Cassie

A dark, local-first Android music player with a personality.

Cassie is a **local-only** music player for Android that plays your downloaded FLAC, MP3, OGG, WAV, AAC, and Opus files. No ads, no accounts, no cloud -- just your music library with a sleek dark UI and a penguin mascot that reacts to what you're listening to.

---

## Features

### Playback
- **Local music playback** via Media3 ExoPlayer -- FLAC, MP3, OGG, Opus, WAV, AAC
- **Gapless playback** -- seamless track transitions (default in Media3)
- **Background playback** with notification controls via `MediaSessionService`
- **Sleep timer** -- 15/30/45/60 min, pauses on expiry
- **Equalizer** -- presets + bass boost (device-dependent)

### UI
- **Pure black (#000000) + purple (#BB86FC) accent** -- OLED-friendly dark theme
- **MiniPlayer** with cava visualizer (animated bars + blur), shuffle, repeat, play/pause
- **NowPlaying screen** -- large album art crossfade, seekbar, lyrics, queue manager
- **Stadium nav bar** -- 5 tabs (Home, Albums, Artists, Playlists, Top 50) with pulse dot when playing
- **Album detail screen** -- full-page per album with large art, Play All, song list
- **Artist screen** -- grouped by artist, expandable cards, play-in-context
- **Global search** -- 300ms debounce across title/artist/album

### Mascot
- **Cassie the Penguin** -- a Pokemon-style mascot that lives on your home screen
- **Mood reacts to genre** -- excited for pop, chill for hip-hop, sleepy for classical, and more
- **Speech bubble** -- 4 meme-style one-liners per mood, tap to cycle through puns
- **Gentle floating animation** -- he bobs up and down while you browse

### Listening Stats
- **Your Vibe card** -- total plays, listening minutes, top artist with animated gradient
- **Top 50 chart** -- most-played songs ranked
- **Play counts** -- persisted locally via JSON

### Library
- **Album browser** -- grouped by album, tap for detail view
- **Artist browser** -- grouped by artist with expandable cards
- **Playlist manager** -- create, rename, delete playlists; add/remove songs with searchable picker
- **Sort options** -- A-Z, Z-A, Recent, Oldest, Artist
- **Favorites** -- heart toggle on every song card, persisted locally

---

## Screenshots

| Home Screen | Now Playing | Albums |
|:---:|:---:|:---:|
| `[home]` | `[now_playing]` | `[albums]` |
| Mascot greeting, Your Vibe, Top Charts | Album art, seekbar, lyrics, queue | Grouped by album, tap for detail |

| Artist View | MiniPlayer | Playlists |
|:---:|:---:|:---:|
| `[artists]` | `[miniplayer]` | `[playlists]` |
| Expandable artist cards with songs | Cava bars, shuffle, repeat, controls | Create, manage, add songs |

| Top 50 | Album Detail | Search |
|:---:|:---:|:---:|
| `[top50]` | `[album_detail]` | `[search]` |
| Most-played chart | Large art, Play All, song list | Global search with sort |

| Mascot Moods | Dark Theme | Notification |
|:---:|:---:|:---:|
| `[mascot_moods]` | `[dark_theme]` | `[notification]` |
| 6 mood variants across genres | Pure black + purple accent | Media3 notification with controls |

---

## Tech Stack

| Layer | Library |
|-------|---------|
| **UI** | Jetpack Compose + Material3 |
| **Player** | AndroidX Media3 ExoPlayer 1.6.1 |
| **Image Loading** | Coil 2.7.0 (250MB disk cache) |
| **Icons** | Material Icons Extended |
| **Persistence** | SharedPreferences + JSON |
| **Backend** | None -- 100% local playback |
| **Min SDK** | 26 |
| **Target SDK** | 36 |
| **Build** | AGP 9.0.1 + Kotlin 2.1.0 |

---

## Building

```bash
git clone https://github.com/justinebacurin1927/Cassie.git
cd Cassie
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

---

## Customization

### Theme Colors
Edit `Theme.kt` to swap the accent color from purple (`#BB86FC`) to your own.

### Penguin Messages
Edit `MascotMoodCard.kt` -- each mood has a `messages` list. Add or change the one-liners.

### Mascot Images
Replace the PNG files in `res/drawable/`:
- `greeting_penguin.png` -- default/idle
- `starry_penguin.png` -- excited
- `chill_penguin.png` -- chill
- `sleepy_penguin.png` -- sleepy
- `love_penguin.png` -- love
- `sad_penguin.png` -- sad

---

## License

MIT -- do whatever you want.
