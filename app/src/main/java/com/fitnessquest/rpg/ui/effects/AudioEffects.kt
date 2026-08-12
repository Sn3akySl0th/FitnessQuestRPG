package com.fitnessquest.rpg.ui.effects

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Procedural Audio Synthesizer for FitnessRPG.
 * Generates crisp RPG sound effects on the fly using Android's native AudioTrack
 * with zero external asset dependencies.
 */
object AudioEffects {

    private val scope = CoroutineScope(Dispatchers.Default)
    var soundEnabled: Boolean = true

    /** Plays a multi-tone pitch sequence (chime, fanfare, slash, etc.) */
    private fun playToneSequence(frequencies: List<Float>, durationsMs: List<Int>, waveType: Int = 0) {
        if (!soundEnabled) return
        scope.launch {
            try {
                val sampleRate = 44100
                val totalDurationMs = durationsMs.sum()
                val numSamples = (sampleRate * (totalDurationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)

                var sampleIndex = 0
                for (i in frequencies.indices) {
                    val freq = frequencies[i]
                    val durMs = durationsMs[i]
                    val toneSamples = (sampleRate * (durMs / 1000.0)).toInt()

                    for (j in 0 until toneSamples) {
                        if (sampleIndex >= numSamples) break
                        val t = j.toDouble() / sampleRate
                        // Envelope to avoid clicks: attack 5ms, release 10ms
                        val attack = (j.toDouble() / (sampleRate * 0.005)).coerceIn(0.0, 1.0)
                        val release = ((toneSamples - j).toDouble() / (sampleRate * 0.010)).coerceIn(0.0, 1.0)
                        val env = attack * release

                        val sampleValue = when (waveType) {
                            1 -> { // Square wave (retro chimes)
                                val sinVal = sin(2.0 * PI * freq * t)
                                if (sinVal >= 0) 0.3 else -0.3
                            }
                            2 -> { // Noise / Slash noise
                                (Math.random() * 2.0 - 1.0) * 0.4
                            }
                            else -> { // Sine wave + harmonics (smooth RPG chimes)
                                val fundamental = sin(2.0 * PI * freq * t)
                                val harmonic = 0.3 * sin(4.0 * PI * freq * t)
                                (fundamental + harmonic) * 0.5
                            }
                        }

                        buffer[sampleIndex++] = (sampleValue * env * Short.MAX_VALUE).toInt().toShort()
                    }
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, sampleIndex)
                audioTrack.play()
                scope.launch {
                    kotlinx.coroutines.delay(totalDurationMs.toLong() + 100)
                    audioTrack.release()
                }
            } catch (e: Exception) {
                // Ignore audio errors gracefully if device audio is occupied
            }
        }
    }

    /** Energetic ascending chime when logging a exercise set (+XP) */
    fun playSetLogged() {
        playToneSequence(
            frequencies = listOf(523.25f, 659.25f, 783.99f), // C5 -> E5 -> G5
            durationsMs = listOf(60, 60, 120),
            waveType = 0
        )
    }

    /** Triumphant 4-note RPG fanfare on Level Up */
    fun playLevelUp() {
        playToneSequence(
            frequencies = listOf(523.25f, 659.25f, 783.99f, 1046.50f), // C5 -> E5 -> G5 -> C6
            durationsMs = listOf(100, 100, 100, 400),
            waveType = 0
        )
    }

    /** Crunchy slash impact sound for Critical Hits */
    fun playCritHit() {
        playToneSequence(
            frequencies = listOf(200f, 150f, 100f),
            durationsMs = listOf(50, 50, 100),
            waveType = 2 // noise
        )
    }

    /** Shimmering magical chime for Loot Drops & PR Records */
    fun playLootDrop() {
        playToneSequence(
            frequencies = listOf(587.33f, 739.99f, 880.00f, 1174.66f, 1479.98f), // D5 -> F#5 -> A5 -> D6 -> F#6
            durationsMs = listOf(70, 70, 70, 70, 250),
            waveType = 1 // retro square wave
        )
    }

    /** Alert whistle/ding when Rest Timer finishes */
    fun playRestDone() {
        playToneSequence(
            frequencies = listOf(880.00f, 1760.00f), // A5 -> A6
            durationsMs = listOf(80, 200),
            waveType = 0
        )
    }

    /** Coin jingle sound when buying gear in Shop */
    fun playCoinJingle() {
        playToneSequence(
            frequencies = listOf(987.77f, 1318.51f), // B5 -> E6
            durationsMs = listOf(50, 150),
            waveType = 1
        )
    }
}
