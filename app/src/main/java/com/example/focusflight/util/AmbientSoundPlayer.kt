package com.example.focusflight.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates and loops a gentle cabin hum using an [AudioTrack] so that the app can
 * ship without bundling large audio assets. The generated sound is a low-pass
 * filtered noise with a soft sine wave to approximate the white noise of an airplane cabin.
 */
class AmbientSoundPlayer {

    private var audioTrack: AudioTrack? = null

    fun start() {
        if (audioTrack != null) return
        val sampleRate = 44100
        val durationSeconds = 4
        val bufferLength = sampleRate * durationSeconds
        val buffer = ShortArray(bufferLength)
        val random = Random(0xF0C055)
        for (i in buffer.indices) {
            val noise = random.nextDouble(-0.3, 0.3)
            val sine = sin(2 * PI * i / (sampleRate / 80.0)) * 0.4
            val sample = (noise + sine) * 0.5
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val track = AudioTrack(
            attributes,
            format,
            buffer.size * 2,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        track.write(buffer, 0, buffer.size)
        track.setLoopPoints(0, buffer.size, -1)
        track.play()
        audioTrack = track
    }

    fun stop() {
        audioTrack?.let { track ->
            try {
                track.stop()
            } catch (_: IllegalStateException) {
                // no-op
            }
            track.release()
        }
        audioTrack = null
    }
}
