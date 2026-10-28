package com.brandonwallace.bastiontd.gl

import android.opengl.GLES30
import com.brandonwallace.bastiontd.game.ParticleEvent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Zero-allocation 3D particle pool for explosions, cryo shatters, muzzle sparks, and Core shockwaves.
 */
class ParticleSystem(private val maxParticles: Int = 2000) {
    private val px = FloatArray(maxParticles)
    private val py = FloatArray(maxParticles)
    private val pz = FloatArray(maxParticles)
    private val vx = FloatArray(maxParticles)
    private val vy = FloatArray(maxParticles)
    private val vz = FloatArray(maxParticles)
    private val pr = FloatArray(maxParticles)
    private val pg = FloatArray(maxParticles)
    private val pb = FloatArray(maxParticles)
    private val size = FloatArray(maxParticles)
    private val life = FloatArray(maxParticles)
    private val maxLife = FloatArray(maxParticles)

    private var activeCount = 0
    private val vertexData = FloatArray(maxParticles * 8)
    private val vertexBuffer = floatBuffer(maxParticles * 8)
    private val vao = IntArray(1)
    private val vbo = IntArray(1)
    private val rnd = Random(77)

    fun initGl() {
        GLES30.glGenVertexArrays(1, vao, 0)
        GLES30.glGenBuffers(1, vbo, 0)
        GLES30.glBindVertexArray(vao[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, maxParticles * 8 * 4, null, GLES30.GL_DYNAMIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 4, GLES30.GL_FLOAT, false, 32, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 4, GLES30.GL_FLOAT, false, 32, 16)
        GLES30.glBindVertexArray(0)
    }

    fun emit(events: List<ParticleEvent>, limit: Int) {
        val cap = limit.coerceIn(100, maxParticles)
        for (ev in events) {
            for (k in 0 until ev.count) {
                val slot = if (activeCount < cap) {
                    activeCount++
                } else {
                    rnd.nextInt(cap)
                }
                val az = (rnd.nextFloat() * 2.0 * PI).toFloat()
                val el = (rnd.nextFloat() * 0.45 * PI + 0.08 * PI).toFloat()
                val spd = ev.speed * (0.45f + rnd.nextFloat() * 0.75f)
                px[slot] = ev.x + (rnd.nextFloat() - 0.5f) * 0.2f
                py[slot] = ev.y + (rnd.nextFloat() - 0.5f) * 0.2f
                pz[slot] = ev.z + (rnd.nextFloat() - 0.5f) * 0.2f
                vx[slot] = cos(el) * cos(az) * spd
                vy[slot] = sin(el) * spd + 1.2f
                vz[slot] = cos(el) * sin(az) * spd
                pr[slot] = (ev.r + (rnd.nextFloat() - 0.5f) * 0.15f).coerceIn(0.1f, 1.0f)
                pg[slot] = (ev.g + (rnd.nextFloat() - 0.5f) * 0.15f).coerceIn(0.1f, 1.0f)
                pb[slot] = (ev.b + (rnd.nextFloat() - 0.5f) * 0.15f).coerceIn(0.1f, 1.0f)
                size[slot] = ev.size * (0.75f + rnd.nextFloat() * 0.5f)
                val dur = 0.35f + rnd.nextFloat() * 0.45f
                life[slot] = dur
                maxLife[slot] = dur
            }
        }
    }

    fun stepAndDraw(dt: Float, prog: ShaderProgram, viewportH: Float, outScale: Float) {
        if (activeCount == 0) return
        val clampedDt = dt.coerceIn(0.001f, 0.08f)
        var i = 0
        while (i < activeCount) {
            life[i] -= clampedDt
            if (life[i] <= 0f) {
                val last = activeCount - 1
                if (i != last) {
                    px[i] = px[last]; py[i] = py[last]; pz[i] = pz[last]
                    vx[i] = vx[last]; vy[i] = vy[last]; vz[i] = vz[last]
                    pr[i] = pr[last]; pg[i] = pg[last]; pb[i] = pb[last]
                    size[i] = size[last]; life[i] = life[last]; maxLife[i] = maxLife[last]
                }
                activeCount--
                continue
            }
            px[i] += vx[i] * clampedDt
            py[i] += vy[i] * clampedDt
            pz[i] += vz[i] * clampedDt
            vy[i] -= 7.8f * clampedDt
            if (py[i] < 0.05f) {
                py[i] = 0.05f
                vy[i] = -vy[i] * 0.35f
                vx[i] *= 0.7f
                vz[i] *= 0.7f
            }
            val alpha = (life[i] / maxLife[i]).coerceIn(0f, 1f)
            val base = i * 8
            vertexData[base] = px[i]
            vertexData[base + 1] = py[i]
            vertexData[base + 2] = pz[i]
            vertexData[base + 3] = size[i] * (0.5f + 0.5f * alpha)
            vertexData[base + 4] = pr[i]
            vertexData[base + 5] = pg[i]
            vertexData[base + 6] = pb[i]
            vertexData[base + 7] = alpha
            i++
        }

        if (activeCount == 0) return
        vertexBuffer.position(0)
        vertexBuffer.put(vertexData, 0, activeCount * 8)
        vertexBuffer.position(0)

        prog.use()
        prog.f("uViewportH", viewportH)
        prog.f("uOutScale", outScale)
        GLES30.glBindVertexArray(vao[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0])
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, activeCount * 8 * 4, vertexBuffer)
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, activeCount)
        GLES30.glBindVertexArray(0)
    }
}
