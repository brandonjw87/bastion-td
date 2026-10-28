package com.brandonwallace.bastiontd.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

enum class Sfx {
    CLICK,
    BUILD_TOWER,
    UPGRADE_TOWER,
    SELL_TOWER,
    RESEARCH_TECH,
    VULCAN_FIRE,
    RAILGUN_FIRE,
    MORTAR_BLAST,
    TESLA_ZAP,
    CRYO_PULSE,
    MISSILE_LAUNCH,
    ENEMY_KILL,
    WAVE_CLEAR,
    BOSS_SPAWN,
    BOSS_KILL,
    ABILITY_ORBITAL,
    ABILITY_CRYO,
    ABILITY_OVERCLOCK,
    CORE_SHOCKWAVE,
    MILESTONE
}

/**
 * Real-time procedural audio synthesizer: zero external audio assets required.
 * Automatically pauses the AudioTrack and sleeps when muted or idle to save battery.
 */
class SoundEngine {
    @Volatile var enabled = true
    @Volatile var battleIntensity = 0f
    private val queue = ConcurrentLinkedQueue<Sfx>()
    @Volatile private var running = false
    private var thread: Thread? = null

    private class Voice(val type: Sfx) {
        var t = 0.0
    }

    fun play(sfx: Sfx) {
        if (!enabled) return
        // Avoid flooding rapid weapon fire in 4x speed
        if (queue.size < 24) {
            queue.add(sfx)
        }
    }

    fun start() {
        if (running) return
        running = true
        thread = Thread({ loop() }, "BastionSynth").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stop() {
        running = false
        thread?.join(400)
        thread = null
    }

    fun resume() = start()

    fun pause() {
        battleIntensity = 0f
    }

    fun release() = stop()

    fun setAmbientPlaying(playing: Boolean) {
        battleIntensity = if (playing) 0.45f else 0f
    }

    private fun loop() {
        val rate = 22050
        val minBuf = AudioTrack.getMinBufferSize(
            rate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(rate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(max(minBuf, 4096))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Exception) {
            running = false
            return
        }

        track.play()
        val buf = ShortArray(512)
        val voices = ArrayList<Voice>()
        val rnd = Random(42)
        val dt = 1.0 / rate
        var humPhase = 0.0
        var humLevel = 0.0
        var asleep = false

        while (running) {
            while (true) {
                val s = queue.poll() ?: break
                // Limit simultaneous voices of the same rapid weapon
                val sameCount = voices.count { it.type == s }
                val maxSame = when (s) {
                    Sfx.VULCAN_FIRE, Sfx.TESLA_ZAP, Sfx.ENEMY_KILL -> 3
                    else -> 5
                }
                if (voices.size < 18 && sameCount < maxSame) {
                    voices.add(Voice(s))
                }
            }
            if (!enabled) {
                voices.clear()
            }
            val targetHum = if (enabled) (battleIntensity.coerceIn(0f, 1f) * 0.045) else 0.0
            if (voices.isEmpty() && targetHum <= 0.001 && humLevel < 1e-4) {
                if (!asleep) {
                    try {
                        track.pause()
                        track.flush()
                    } catch (_: Exception) {}
                    asleep = true
                }
                humLevel = 0.0
                try {
                    Thread.sleep(25)
                } catch (_: InterruptedException) {}
                continue
            }
            if (asleep) {
                try {
                    track.play()
                } catch (_: Exception) {}
                asleep = false
            }

            for (i in buf.indices) {
                humLevel += (targetHum - humLevel) * 0.001
                humPhase += dt * 55.0
                if (humPhase > 1.0) humPhase -= 1.0
                var sample = (sin(2.0 * PI * humPhase) * 0.6 + sin(4.0 * PI * humPhase) * 0.4) * humLevel

                val it = voices.iterator()
                while (it.hasNext()) {
                    val v = it.next()
                    sample += synth(v.type, v.t, rnd)
                    v.t += dt
                    if (v.t > duration(v.type)) {
                        it.remove()
                    }
                }
                buf[i] = (sample.coerceIn(-1.0, 1.0) * 25000.0).toInt().toShort()
            }
            track.write(buf, 0, buf.size)
        }
        try {
            track.stop()
        } catch (_: Exception) {}
        track.release()
    }

    private fun tone(f: Double, t: Double) = sin(2.0 * PI * f * t)

    private fun duration(type: Sfx): Double = when (type) {
        Sfx.CLICK -> 0.05
        Sfx.BUILD_TOWER -> 0.28
        Sfx.UPGRADE_TOWER -> 0.38
        Sfx.SELL_TOWER -> 0.25
        Sfx.RESEARCH_TECH -> 0.52
        Sfx.VULCAN_FIRE -> 0.065
        Sfx.RAILGUN_FIRE -> 0.26
        Sfx.MORTAR_BLAST -> 0.55
        Sfx.TESLA_ZAP -> 0.16
        Sfx.CRYO_PULSE -> 0.24
        Sfx.MISSILE_LAUNCH -> 0.28
        Sfx.ENEMY_KILL -> 0.14
        Sfx.WAVE_CLEAR -> 0.65
        Sfx.BOSS_SPAWN -> 0.95
        Sfx.BOSS_KILL -> 1.45
        Sfx.ABILITY_ORBITAL -> 1.15
        Sfx.ABILITY_CRYO -> 0.75
        Sfx.ABILITY_OVERCLOCK -> 0.65
        Sfx.CORE_SHOCKWAVE -> 1.10
        Sfx.MILESTONE -> 0.60
    }

    private fun synth(type: Sfx, t: Double, rnd: Random): Double = when (type) {
        Sfx.CLICK -> tone(1480.0, t) * exp(-t * 65.0) * 0.22
        Sfx.BUILD_TOWER -> {
            val f = if (t < 0.09) 330.0 else if (t < 0.18) 440.0 else 659.25
            tone(f, t) * exp(-t * 8.0) * 0.25 + (rnd.nextDouble() * 2 - 1) * exp(-t * 35.0) * 0.15
        }
        Sfx.UPGRADE_TOWER -> {
            val k = (t / 0.09).toInt().coerceAtMost(3)
            val f = UPGRADE_NOTES[k]
            (tone(f, t) + 0.35 * tone(f * 2.0, t)) * exp(-(t - k * 0.09) * 10.0) * 0.24
        }
        Sfx.SELL_TOWER -> {
            val f = if (t < 0.10) 587.33 else 392.0
            tone(f, t) * exp(-t * 10.0) * 0.22
        }
        Sfx.RESEARCH_TECH -> {
            val k = (t / 0.11).toInt().coerceAtMost(3)
            val f = RESEARCH_NOTES[k]
            (tone(f, t) + 0.4 * tone(f * 1.5, t)) * exp(-(t - k * 0.11) * 7.5) * 0.25
        }
        Sfx.VULCAN_FIRE -> {
            ((rnd.nextDouble() * 2 - 1) * 0.55 + tone(190.0 - t * 900.0, t) * 0.45) * exp(-t * 48.0) * 0.24
        }
        Sfx.RAILGUN_FIRE -> {
            val sweep = 1600.0 * exp(-t * 14.0) + 140.0
            (tone(sweep, t) * 0.65 + (rnd.nextDouble() * 2 - 1) * 0.35) * exp(-t * 11.0) * 0.30
        }
        Sfx.MORTAR_BLAST -> {
            val noise = (rnd.nextDouble() * 2 - 1) * exp(-t * 5.5) * 0.55
            val boom = tone(72.0 * exp(-t * 2.5) + 32.0, t) * exp(-t * 4.5) * 0.55
            (noise + boom) * 0.38
        }
        Sfx.TESLA_ZAP -> {
            val buzz = tone(920.0 + sin(t * 240.0) * 320.0, t) * 0.5
            val snap = (rnd.nextDouble() * 2 - 1) * 0.5
            (buzz + snap) * exp(-t * 18.0) * 0.22
        }
        Sfx.CRYO_PULSE -> {
            (tone(1174.66, t) + 0.5 * tone(1760.0, t)) * exp(-t * 12.0) * 0.18
        }
        Sfx.MISSILE_LAUNCH -> {
            val whoosh = (rnd.nextDouble() * 2 - 1) * sin(PI * (t / 0.28).coerceIn(0.0, 1.0)) * 0.32
            val ping = tone(784.0 + t * 600.0, t) * exp(-t * 14.0) * 0.18
            whoosh + ping
        }
        Sfx.ENEMY_KILL -> {
            val f = if (t < 0.05) 783.99 else 1046.50
            tone(f, t) * exp(-t * 18.0) * 0.18
        }
        Sfx.WAVE_CLEAR -> {
            val k = (t / 0.12).toInt().coerceAtMost(3)
            val f = WAVE_NOTES[k]
            (tone(f, t) + 0.4 * tone(f * 2.0, t)) * exp(-(t - k * 0.12) * 6.5) * 0.26
        }
        Sfx.BOSS_SPAWN -> {
            val horn = tone(110.0, t) * 0.5 + tone(164.81, t) * 0.35 + tone(220.0, t) * 0.25
            horn * sin(PI * (t / 0.95).coerceIn(0.0, 1.0)) * 0.36
        }
        Sfx.BOSS_KILL -> {
            val k = (t / 0.18).toInt().coerceAtMost(5)
            val f = WIN_NOTES[k]
            val chord = (tone(f, t) + 0.5 * tone(f * 2.0, t)) * exp(-(t - k * 0.18) * 4.5) * 0.28
            val rumble = (rnd.nextDouble() * 2 - 1) * exp(-t * 2.5) * 0.22
            chord + rumble
        }
        Sfx.ABILITY_ORBITAL -> {
            val beam = tone(880.0 * exp(-t * 2.2) + 65.0, t) * 0.55
            val roar = (rnd.nextDouble() * 2 - 1) * exp(-t * 2.4) * 0.45
            (beam + roar) * 0.42
        }
        Sfx.ABILITY_CRYO -> {
            val shimmer = tone(1318.5 + sin(t * 60.0) * 180.0, t) + 0.5 * tone(1975.5, t)
            shimmer * exp(-t * 3.8) * 0.25
        }
        Sfx.ABILITY_OVERCLOCK -> {
            val f = 440.0 + t * 900.0
            (tone(f, t) + 0.4 * tone(f * 1.5, t)) * exp(-t * 4.0) * 0.26
        }
        Sfx.CORE_SHOCKWAVE -> {
            val sub = tone(140.0 * exp(-t * 3.0) + 38.0, t) * 0.65
            val blast = (rnd.nextDouble() * 2 - 1) * exp(-t * 3.2) * 0.45
            (sub + blast) * 0.40
        }
        Sfx.MILESTONE -> {
            val k = (t / 0.12).toInt().coerceAtMost(3)
            tone(RESEARCH_NOTES[k], t) * exp(-(t - k * 0.12) * 7.0) * 0.28
        }
    }

    private companion object {
        val UPGRADE_NOTES = doubleArrayOf(440.0, 554.37, 659.25, 880.0)
        val RESEARCH_NOTES = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
        val WAVE_NOTES = doubleArrayOf(587.33, 739.99, 880.0, 1174.66)
        val WIN_NOTES = doubleArrayOf(392.0, 523.25, 659.25, 783.99, 1046.50, 1318.51)
    }
}
