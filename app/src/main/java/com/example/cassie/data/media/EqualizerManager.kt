package com.example.cassie.data.media

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer

class EqualizerManager {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var _currentPreset: Short = -1
    private var _bassBoostEnabled: Boolean = false

    val currentPreset: Short get() = _currentPreset
    val isBassBoostEnabled: Boolean get() = _bassBoostEnabled

    /** Call whenever the audio session changes (e.g. player created). */
    fun attach(audioSessionId: Int) {
        release()
        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
                if (_currentPreset >= 0 && _currentPreset < numberOfPresets) {
                    usePreset(_currentPreset)
                }
            }
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = _bassBoostEnabled
                if (_bassBoostEnabled) setStrength(500)
            }
        } catch (_: Exception) {
            // device may not support equalizer
        }
    }

    fun release() {
        equalizer?.let { it.enabled = false; it.release() }
        bassBoost?.let { it.enabled = false; it.release() }
        equalizer = null
        bassBoost = null
    }

    // ── presets ──

    fun getPresetNames(): List<String> {
        val eq = equalizer ?: return emptyList()
        return (0 until eq.numberOfPresets).map { idx ->
            eq.getPresetName(idx.toShort())
        }
    }

    fun setPreset(index: Short) {
        equalizer?.usePreset(index)
        _currentPreset = index
    }

    // ── bass boost ──

    fun setBassBoostEnabled(enabled: Boolean) {
        _bassBoostEnabled = enabled
        bassBoost?.let {
            it.enabled = enabled
            if (enabled) it.setStrength(500)
        }
    }

    fun cleanup() {
        release()
    }
}
