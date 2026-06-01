package com.example.cassie.party

/**
 * Translates a stream of [UserEvent]s into a [BehaviorStats] snapshot,
 * then derives a list of [UserPattern]s from that snapshot.
 *
 * The recognizer is the ONLY place that knows what counts as a SKIPPER
 * or a LOOPER. Tune the thresholds in this file; everything else just
 * consumes the result.
 *
 * Design rules:
 *  - All thresholds are conservative (high precision > high recall).
 *    A wrong pattern call is much worse than a missing one — Skipper
 *    never wants to be confidently wrong.
 *  - Patterns are evaluated in a fixed order. Later patterns can read
 *    stats mutated by earlier patterns in the same pass.
 *  - The recognizer is pure: it does not emit events, it does not write
 *    to storage, and it does not generate any text. Side-effects live
 *    in [SkipperEngine].
 */
class PatternRecognizer {

    /**
     * Mutate [stats] in place to reflect [event], then return the new
     * full snapshot. The returned value is the same object as [stats]
     * — we return it for fluent chaining.
     */
    fun applyEvent(stats: BehaviorStats, event: UserEvent): BehaviorStats {
        when (event) {
            is UserEvent.SongStarted -> {
                val isNewSong = stats.currentSongId != event.songId
                stats.currentSongId = event.songId
                stats.currentSongStartedAt = event.timestamp
                stats.currentSongLoopCount = 0
                if (isNewSong) {
                    stats.totalSongsStarted += 1
                    // A new song starting means the previous one was
                    // neither paused-and-resumed nor still playing, so
                    // we don't add anything to recentSkips here.
                }
            }
            is UserEvent.SongSkipped -> {
                stats.totalSongsSkipped += 1
                if (event.positionMs < 30_000L) {
                    stats.totalSkipsBefore30s += 1
                }
                stats.recentSkips = (stats.recentSkips + true).takeLast(RECENT_WINDOW)
            }
            is UserEvent.SongReplayed -> {
                stats.totalSongsReplayed += 1
            }
            is UserEvent.SongLooped -> {
                stats.totalSongLoops += 1
                if (event.songId == stats.currentSongId) {
                    stats.currentSongLoopCount += 1
                }
                stats.loopsPerSong = stats.loopsPerSong.updateAt(event.songId, { (it) + 1 }, 0)
            }
            is UserEvent.SongPaused -> {
                // Pause doesn't change stats; the next SongStarted or
                // SongResumed will move the song pointer.
            }
            is UserEvent.SongResumed -> {
                // Same as pause — no counters change.
            }
            is UserEvent.SongSeeked -> {
                // A seek-to-position that lands within 5 seconds of 0
                // counts as a replay-style "I want to hear it again".
                if (event.toMs < 5_000L && event.fromMs > 10_000L) {
                    stats.totalSongsReplayed += 1
                }
            }
            is UserEvent.SongCompleted -> {
                stats.totalSongsCompleted += 1
                // Natural completion is the opposite of a skip — pad
                // the recent window with a "not skipped" entry so
                // skip-rate stays meaningful across both signals.
                stats.recentSkips = (stats.recentSkips + false).takeLast(RECENT_WINDOW)
            }
            is UserEvent.PartyModeToggled -> {
                if (event.enabled) stats.totalPartyModeToggles += 1
            }
            is UserEvent.RepeatModeChanged -> {
                // Repeat-mode changes only matter for the LOOPER signal,
                // which we compute at detect-time, so no live counter
                // change here.
            }
            is UserEvent.ShuffleToggled -> {
                if (event.enabled) stats.totalShuffleToggles += 1
            }
            is UserEvent.SleepTimerSet -> {
                stats.totalSleepTimerSets += 1
            }
            is UserEvent.FavoriteToggled -> {
                stats.totalFavoriteToggles += 1
            }
            is UserEvent.LyricsOpened -> {
                stats.totalLyricsOpens += 1
            }
            is UserEvent.AppForegrounded -> {
                stats.totalAppForegrounds += 1
                val hour = Clock.hourOfDay(event.timestamp)
                stats.foregroundsByHour = stats.foregroundsByHour.updateAt(hour, { (it) + 1 }, 0)
            }
            is UserEvent.AppBackgrounded -> {
                stats.totalAppBackgrounds += 1
            }
            is UserEvent.MinutesListenedTicked -> {
                // 1 second = 1/60 minute
                stats.totalMinutesListened += 1f / 60f
                stats.minutesPerSong = stats.minutesPerSong.updateAt(event.songId, { (it) + 1f / 60f }, 0f)
            }
        }
        stats.lastUpdatedMs = event.timestamp
        return stats
    }

    /**
     * Re-evaluate all patterns against the current [stats] snapshot.
     * Returns the list of patterns that currently apply, in priority
     * order (most-confident first).
     */
    fun detect(stats: BehaviorStats): List<UserPattern> {
        val out = mutableListOf<UserPattern>()
        detectSkippers(stats, out)
        detectLoopers(stats, out)
        detectReplayers(stats, out)
        detectMarathoners(stats, out)
        detectPartiers(stats, out)
        detectExplorers(stats, out)
        detectNightOwls(stats, out)
        detectFavoriteHoarders(stats, out)
        detectLyricsLovers(stats, out)
        return out.sortedByDescending { it.confidence }
    }

    // ── Per-pattern detectors ──────────────────────────────────────

    private fun detectSkippers(s: BehaviorStats, out: MutableList<UserPattern>) {
        // Thresholds lowered so SKIPPER pattern fires fast — after
        // ~3+ skips in the last 5 starts, not 70% of 20.
        if (s.recentSkips.size < 5) return
        val recentSkipped = s.recentSkips.count { it }
        if (s.recentSkipRate >= 0.6f || recentSkipped >= 3) {
            val pct = (s.recentSkipRate * 100).toInt()
            out += UserPattern(
                type = UserPatternType.SKIPPER,
                confidence = clamp(s.recentSkipRate),
                evidence = "skipped $pct% of the last ${s.recentSkips.size} songs",
                detectedAtMs = System.currentTimeMillis(),
            )
        }
    }

    private fun detectLoopers(s: BehaviorStats, out: MutableList<UserPattern>) {
        val currentLoops = s.currentSongLoopCount
        val totalLoops = s.totalSongLoops
        // 2+ loops on the current song is enough to fire — down
        // from 3. The pattern is what makes Skipper say "ok stalker"
        // so the user wants to see it early.
        when {
            currentLoops >= 2 -> out += UserPattern(
                type = UserPatternType.LOOPER,
                confidence = clamp(currentLoops / 5f),
                evidence = "looped the current song $currentLoops times",
                detectedAtMs = System.currentTimeMillis(),
            )
            totalLoops >= 4 -> out += UserPattern(
                type = UserPatternType.LOOPER,
                confidence = 0.4f,
                evidence = "$totalLoops repeat-loops across recent listening",
                detectedAtMs = System.currentTimeMillis(),
            )
        }
    }

    private fun detectReplayers(s: BehaviorStats, out: MutableList<UserPattern>) {
        if (s.minutesPerSong.isEmpty()) return
        val topId = s.topReplaySongId ?: return
        val topMinutes = s.minutesPerSong[topId] ?: 0f
        val totalMinutes = s.totalMinutesListened.coerceAtLeast(0.01f)
        val share = topMinutes / totalMinutes
        if (topMinutes >= 5f && share >= 0.3f) {
            out += UserPattern(
                type = UserPatternType.REPEATER,
                confidence = clamp(share),
                evidence = "one song accounts for ${(share * 100).toInt()}% of recent listening",
                detectedAtMs = System.currentTimeMillis(),
            )
        }
    }

    private fun detectMarathoners(s: BehaviorStats, out: MutableList<UserPattern>) {
        val current = s.currentSongId?.let { s.minutesPerSong[it] } ?: 0f
        val max = s.maxMinutesOnOneSong
        // Lowered from 10m to 5m — listening to one song for 5
        // minutes straight is already a marathon.
        when {
            current >= 5f -> out += UserPattern(
                type = UserPatternType.MARATHONER,
                confidence = clamp(current / 20f),
                evidence = "been on the current song for ${current.toInt()} minutes",
                detectedAtMs = System.currentTimeMillis(),
            )
            max >= 20f -> out += UserPattern(
                type = UserPatternType.MARATHONER,
                confidence = 0.5f,
                evidence = "longest single-song session this week: ${max.toInt()}m",
                detectedAtMs = System.currentTimeMillis(),
            )
        }
    }

    private fun detectPartiers(s: BehaviorStats, out: MutableList<UserPattern>) {
        // Lowered from 3 toggles to 1 — the user JUST turned it on,
        // we should react.
        if (s.totalPartyModeToggles >= 1) {
            out += UserPattern(
                type = UserPatternType.PARTIER,
                confidence = clamp(s.totalPartyModeToggles / 4f),
                evidence = "toggled party mode ${s.totalPartyModeToggles} times",
                detectedAtMs = System.currentTimeMillis(),
            )
        }
    }

    private fun detectExplorers(s: BehaviorStats, out: MutableList<UserPattern>) {
        val unique = s.uniqueSongsListenedTo
        // Lowered from 15 to 8 unique songs, and skip rate from
        // 0.4 to 0.5 — easier to fire.
        if (unique >= 8 && s.recentSkipRate < 0.5f) {
            out += UserPattern(
                type = UserPatternType.EXPLORER,
                confidence = clamp(unique / 30f),
                evidence = "listened past 30s on $unique different songs",
                detectedAtMs = System.currentTimeMillis(),
            )
        }
    }

    private fun detectNightOwls(s: BehaviorStats, out: MutableList<UserPattern>) {
        // Lowered the required opens from 5 to 2.
        if (s.totalAppForegrounds < 2) return
        if (s.nightOwlRate >= 0.3f) {
            out += UserPattern(
                type = UserPatternType.NIGHT_OWL,
                confidence = clamp(s.nightOwlRate),
                evidence = "${(s.nightOwlRate * 100).toInt()}% of app opens were after 22:00",
                detectedAtMs = System.currentTimeMillis(),
            )
        }
    }

    private fun detectFavoriteHoarders(s: BehaviorStats, out: MutableList<UserPattern>) {
        // Lowered from 30 to 10 toggles.
        if (s.totalFavoriteToggles >= 10) {
            out += UserPattern(
                type = UserPatternType.FAVORITE_HOARDER,
                confidence = clamp(s.totalFavoriteToggles / 30f),
                evidence = "favorited/unfavorited ${s.totalFavoriteToggles} songs",
                detectedAtMs = System.currentTimeMillis(),
            )
        }
    }

    private fun detectLyricsLovers(s: BehaviorStats, out: MutableList<UserPattern>) {
        // Lowered from 5 to 2 opens.
        if (s.totalLyricsOpens >= 2) {
            out += UserPattern(
                type = UserPatternType.LYRICS_LOVER,
                confidence = clamp(s.totalLyricsOpens / 10f),
                evidence = "opened lyrics ${s.totalLyricsOpens} times",
                detectedAtMs = System.currentTimeMillis(),
            )
        }
    }

    // ── helpers ─────────────────────────────────────────────────────

    private fun <K, V> Map<K, V>.updateAt(key: K, fn: (V) -> V, default: V): Map<K, V> {
        val out = LinkedHashMap(this)
        out[key] = fn(out[key] ?: default)
        return out
    }

    private fun clamp(v: Float): Float = v.coerceIn(0f, 1f)

    companion object {
        /** How many of the most-recent song-starts we keep for skip-rate. */
        const val RECENT_WINDOW = 20
    }
}
