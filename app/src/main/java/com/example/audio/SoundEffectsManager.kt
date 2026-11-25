package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundEffectsManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    var enabled: Boolean = true

    fun playClick() {
        if (!enabled) return
        playNotes(listOf(620.0 to 45))
    }

    fun playTick() {
        if (!enabled) return
        playNotes(listOf(880.0 to 35))
    }

    fun playCorrect() {
        if (!enabled) return
        playNotes(listOf(523.25 to 90, 659.25 to 90, 783.99 to 140))
    }

    fun playCombo() {
        if (!enabled) return
        playNotes(listOf(587.33 to 80, 739.99 to 80, 880.0 to 90, 1174.66 to 170))
    }

    fun playWrong() {
        if (!enabled) return
        playNotes(listOf(330.0 to 130, 246.94 to 200))
    }

    fun playVictory() {
        if (!enabled) return
        playNotes(listOf(523.25 to 95, 659.25 to 95, 783.99 to 95, 1046.50 to 240))
    }

    private fun playNotes(notes: List<Pair<Double, Int>>) {
        scope.launch {
            runCatching {
                val sampleRate = 22050
                val totalSamples = notes.sumOf { (sampleRate * it.second) / 1000 }
                if (totalSamples <= 0) return@runCatching
                val pcm = ShortArray(totalSamples)
                var offset = 0
                for ((freq, durationMs) in notes) {
                    val count = (sampleRate * durationMs) / 1000
                    for (i in 0 until count) {
                        val t = i.toDouble() / sampleRate
                        val envelope = exp(-4.5 * i / count.coerceAtLeast(1))
                        val wave = sin(2.0 * PI * freq * t) * envelope
                        pcm[offset + i] = (wave * Short.MAX_VALUE * 0.45).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                    offset += count
                }
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferBytes = maxOf(minBuf, pcm.size * 2)
                val track = AudioTrack.Builder()
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
                    .setBufferSizeInBytes(bufferBytes)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                val totalMs = notes.sumOf { it.second }.toLong() + 80L
                Thread.sleep(totalMs)
                track.stop()
                track.release()
            }
        }
    }
}
