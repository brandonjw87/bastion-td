package com.brandonwallace.bastiontd.gl

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import com.brandonwallace.bastiontd.game.ArenaMap
import com.brandonwallace.bastiontd.game.GameEngine
import com.brandonwallace.bastiontd.game.RenderSnapshot
import com.brandonwallace.bastiontd.game.TowerType
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Custom OpenGL ES 3.0 3D Renderer for Bastion 3D TD.
 */
class GameRenderer(val engine: GameEngine) : GLSurfaceView.Renderer {
    private val pipeline = Pipeline()
    private var models: Models? = null
    private var particles: ParticleSystem? = null

    private var shadowProg: ShaderProgram? = null
    private var skyProg: ShaderProgram? = null
    private var litProg: ShaderProgram? = null
    private var fxProg: ShaderProgram? = null
    private var particleProg: ShaderProgram? = null
    private var bloomDownProg: ShaderProgram? = null
    private var bloomUpProg: ShaderProgram? = null
    private var compositeProg: ShaderProgram? = null

    private var surfaceW = 1
    private var surfaceH = 1
    private var lastFrameNs = 0L

    // Matrices pre-allocated to avoid per-frame GC allocations
    private val projMat = Mat.new()
    private val viewMat = Mat.new()
    private val vpMat = Mat.new()
    private val invVpMat = Mat.new()
    private val invVpLock = Any()

    private val lightProj = Mat.new()
    private val lightView = Mat.new()
    private val lightVp = Mat.new()
    private val lightMvp = Mat.new()

    private val modelMat = Mat.new()
    private val tempMat = Mat.new()
    private val nrmMat = FloatArray(9)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.03f, 0.05f, 0.09f, 1.0f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)

        pipeline.init()
        models = Models()
        particles = ParticleSystem(Quality.HIGH.maxParticles).also { it.initGl() }

        shadowProg = ShaderProgram("shadow", Shaders.SHADOW_VS, Shaders.SHADOW_FS)
        skyProg = ShaderProgram("sky", Shaders.FULL_VS, Shaders.SKY_FS)
        litProg = ShaderProgram("lit", Shaders.LIT_VS, Shaders.LIT_FS)
        fxProg = ShaderProgram("fx", Shaders.FX_VS, Shaders.FX_FS)
        particleProg = ShaderProgram("particle", Shaders.PARTICLE_VS, Shaders.PARTICLE_FS)
        bloomDownProg = ShaderProgram("bloomDown", Shaders.FULL_VS, Shaders.BLOOM_DOWN_FS)
        bloomUpProg = ShaderProgram("bloomUp", Shaders.FULL_VS, Shaders.BLOOM_UP_FS)
        compositeProg = ShaderProgram("composite", Shaders.FULL_VS, Shaders.COMPOSITE_FS)

        lastFrameNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceW = width.coerceAtLeast(1)
        surfaceH = height.coerceAtLeast(1)
        val snap = engine.captureUiSnapshot()
        pipeline.configure(surfaceW, surfaceH, Quality.of(snap.qualityIndex))
    }

    override fun onDrawFrame(gl: GL10?) {
        val nowNs = System.nanoTime()
        val dt = ((nowNs - lastFrameNs) * 1e-9f).coerceIn(0.001f, 0.05f)
        lastFrameNs = nowNs

        // Advance simulation & capture immutable render snapshot
        engine.step(dt)
        val snap = engine.captureRenderSnapshot()
        val uiSnap = engine.captureUiSnapshot()

        val desiredQuality = Quality.of(uiSnap.qualityIndex)
        if (desiredQuality != pipeline.quality || surfaceW != pipeline.width || surfaceH != pipeline.height) {
            pipeline.configure(surfaceW, surfaceH, desiredQuality)
        }

        val m = models ?: return
        val partSys = particles ?: return

        // Spawn new 3D particle bursts
        if (snap.particleEvents.isNotEmpty()) {
            partSys.emit(snap.particleEvents, pipeline.quality.maxParticles)
        }
        val partDt = dt * uiSnap.speedMode.coerceIn(1, 2)

        // Compute 3D Camera & Directional Sun Light matrices
        val aspect = surfaceW.toFloat() / surfaceH.toFloat()
        Mat.perspective(projMat, 46f, aspect, 1.2f, 85f)

        val yawRad = (snap.cameraYawDeg * PI / 180.0).toFloat()
        val pitchRad = (snap.cameraPitchDeg * PI / 180.0).toFloat()
        val camDist = 28.5f * snap.cameraZoom
        val horizDist = camDist * cos(pitchRad)
        val camY = (camDist * sin(pitchRad)).coerceAtLeast(4.5f)
        val targetX = snap.cameraPanX
        val targetY = 0.2f
        val targetZ = snap.cameraPanZ + 0.4f
        val camX = targetX + sin(yawRad) * horizDist
        val camZ = targetZ + cos(yawRad) * horizDist

        Mat.lookAt(viewMat, camX, camY, camZ, targetX, targetY, targetZ, 0f, 1f, 0f)
        Mat.mul(vpMat, projMat, viewMat)
        synchronized(invVpLock) {
            Mat.invert(invVpMat, vpMat)
        }

        // Directional Sun Light for Shadow Map & PBR
        val sunDirX = 0.42f
        val sunDirY = 0.81f
        val sunDirZ = 0.41f
        Mat.ortho(lightProj, -18.5f, 18.5f, -18.5f, 18.5f, 2.0f, 48.0f)
        Mat.lookAt(
            lightView,
            sunDirX * 22f, sunDirY * 22f, sunDirZ * 22f,
            0f, 0f, 0f,
            0f, 1f, 0f
        )
        Mat.mul(lightVp, lightProj, lightView)

        // 1. Shadow Depth Pass
        renderShadowPass(m, snap)

        // 2. Main HDR Scene Pass
        pipeline.bindScene()
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        renderSkyPass(snap)
        renderOpaqueLitPass(m, snap, camX, camY, camZ, sunDirX, sunDirY, sunDirZ)
        renderFxAndParticlesPass(m, partSys, snap, partDt)

        // 3. MSAA Resolve + HDR Bloom + ACES Tone-Mapped Composite
        pipeline.resolve()
        renderPostProcess()
    }

    private fun renderShadowPass(m: Models, snap: RenderSnapshot) {
        if (pipeline.shadowSize <= 0) return
        val prog = shadowProg ?: return
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, pipeline.shadowFbo)
        GLES30.glViewport(0, 0, pipeline.shadowSize, pipeline.shadowSize)
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthMask(true)

        prog.use()

        fun drawShadowMesh(mesh: Mesh, model: FloatArray) {
            Mat.mul(lightMvp, lightVp, model)
            prog.m4("uLightMVP", lightMvp)
            mesh.draw()
        }

        Mat.identity(modelMat)
        drawShadowMesh(m.tacticalPadsStatic, modelMat)
        drawShadowMesh(m.portalGate, modelMat)
        drawShadowMesh(m.quantumCoreBase, modelMat)

        for (t in snap.towers) {
            val tier = t.visualTier.coerceIn(0, 3)
            Mat.translation(modelMat, t.x, t.y, t.z)
            drawShadowMesh(m.towerPedestals[tier], modelMat)

            Mat.rotateY(modelMat, (t.yawDeg * PI / 180.0).toFloat())
            drawShadowMesh(m.towerTurrets[t.type.ordinal][tier], modelMat)
        }

        for (e in snap.enemies) {
            Mat.translation(modelMat, e.x, e.y, e.z)
            Mat.rotateY(modelMat, (e.yawDeg * PI / 180.0).toFloat())
            Mat.scale(modelMat, e.scale, e.scale, e.scale)
            drawShadowMesh(m.enemyMeshes[e.type.ordinal], modelMat)
        }
    }

    private fun renderSkyPass(snap: RenderSnapshot) {
        val prog = skyProg ?: return
        val b = snap.biome
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthMask(false)
        prog.use()
        prog.f3("uZenith", b.skyZenithR, b.skyZenithG, b.skyZenithB)
        prog.f3("uHorizon", b.skyHorizonR, b.skyHorizonG, b.skyHorizonB)
        prog.f3("uConduit", b.conduitR, b.conduitG, b.conduitB)
        prog.f("uTime", snap.simTime)
        prog.f("uOutScale", pipeline.outScale)
        pipeline.drawQuad()
    }

    private fun renderOpaqueLitPass(
        m: Models,
        snap: RenderSnapshot,
        camX: Float, camY: Float, camZ: Float,
        sunDirX: Float, sunDirY: Float, sunDirZ: Float
    ) {
        val prog = litProg ?: return
        val b = snap.biome

        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthMask(true)
        GLES30.glDisable(GLES30.GL_BLEND)
        GLES30.glDisable(GLES30.GL_CULL_FACE)

        prog.use()
        prog.m4("uVP", vpMat)
        prog.m4("uShadowVP", lightVp)
        prog.f3("uCamPos", camX, camY, camZ)
        prog.f3("uSunDir", sunDirX, sunDirY, sunDirZ)
        prog.f3("uSunCol", b.sunR, b.sunG, b.sunB)
        prog.f3("uSkyAmb", b.skyHorizonR * 1.35f, b.skyHorizonG * 1.35f, b.skyHorizonB * 1.35f)
        prog.f3("uGroundAmb", b.groundTintR * 0.45f, b.groundTintG * 0.45f, b.groundTintB * 0.45f)
        prog.f3("uConduitCol", b.conduitR, b.conduitG, b.conduitB)
        prog.f("uTime", snap.simTime)
        prog.f("uOutScale", pipeline.outScale)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, pipeline.shadowTex)
        prog.i("uShadowMap", 0)
        prog.f("uShadowOn", if (pipeline.shadowSize > 0) 1f else 0f)
        prog.f("uShadowTexel", if (pipeline.shadowSize > 0) 1f / pipeline.shadowSize else 0.001f)
        prog.i("uShadowTaps", pipeline.quality.shadowTaps)

        fun drawLit(
            mesh: Mesh,
            model: FloatArray,
            matType: Int = 0,
            tintR: Float = 1f, tintG: Float = 1f, tintB: Float = 1f,
            emR: Float = 0f, emG: Float = 0f, emB: Float = 0f
        ) {
            Mat.normalMatrix(nrmMat, model)
            prog.m4("uModel", model)
            prog.m3("uNrmMat", nrmMat)
            prog.i("uMatType", matType)
            prog.f3("uTint", tintR, tintG, tintB)
            prog.f3("uEmissive", emR, emG, emB)
            mesh.draw()
        }

        // Static Arena Environment
        Mat.identity(modelMat)
        drawLit(
            m.canyonFloor, modelMat, matType = 1,
            tintR = b.groundTintR * 2.4f, tintG = b.groundTintG * 2.4f, tintB = b.groundTintB * 2.4f
        )
        drawLit(m.conduitFloor, modelMat, matType = 3)
        drawLit(
            m.roadway, modelMat, matType = 2,
            tintR = b.pathTintR * 2.8f, tintG = b.pathTintG * 2.8f, tintB = b.pathTintB * 2.8f
        )
        drawLit(
            m.perimeterCliffs, modelMat, matType = 1,
            tintR = b.groundTintR * 3.0f, tintG = b.groundTintG * 3.0f, tintB = b.groundTintB * 3.0f
        )
        drawLit(m.tacticalPadsStatic, modelMat, matType = 0)
        drawLit(m.portalGate, modelMat, matType = 0, emR = 0.22f, emG = 0.04f, emB = 0.14f)
        drawLit(m.quantumCoreBase, modelMat, matType = 0, emR = 0.0f, emG = 0.15f, emB = 0.22f)

        // Spinning North Rift Portal Vortex Ring
        Mat.translation(modelMat, ArenaMap.PORTAL_POS.x, 1.65f, ArenaMap.PORTAL_POS.z)
        Mat.rotateZ(modelMat, snap.simTime * 1.85f)
        val portalPulse = 0.85f + 0.25f * sin(snap.simTime * 4.2f)
        drawLit(
            m.portalVortexRing, modelMat, matType = 0,
            emR = 1.25f * portalPulse, emG = 0.18f * portalPulse, emB = 0.78f * portalPulse
        )

        // South Quantum Core Orb + Counter-Rotating Gyroscope Rings
        val coreBob = 1.38f + 0.12f * sin(snap.simTime * 2.8f)
        Mat.translation(modelMat, ArenaMap.CORE_POS.x, coreBob, ArenaMap.CORE_POS.z)
        val coreGlow = 0.9f + 0.3f * sin(snap.simTime * 5.0f)
        drawLit(
            m.quantumCoreOrb, modelMat, matType = 0,
            emR = 0.08f, emG = 1.15f * coreGlow, emB = 1.45f * coreGlow
        )

        // Outer Gyro Ring
        Mat.translation(modelMat, ArenaMap.CORE_POS.x, coreBob, ArenaMap.CORE_POS.z)
        Mat.rotateY(modelMat, snap.simTime * 1.4f)
        Mat.rotateX(modelMat, snap.simTime * 0.9f)
        drawLit(m.quantumCoreGyro, modelMat, matType = 0, emR = 0.65f, emG = 0.50f, emB = 0.12f)

        // Inner Gyro Ring
        Mat.translation(modelMat, ArenaMap.CORE_POS.x, coreBob, ArenaMap.CORE_POS.z)
        Mat.rotateZ(modelMat, -snap.simTime * 1.9f)
        Mat.rotateX(modelMat, 0.8f)
        Mat.scale(modelMat, 0.82f, 0.82f, 0.82f)
        drawLit(m.quantumCoreGyro, modelMat, matType = 0, emR = 0.0f, emG = 0.75f, emB = 0.95f)

        // Active 3D Towers (Pedestal + Rotating/Recoiling Turret)
        val overclockGlow = if (snap.overclockActive) 0.45f else 0.08f
        for (t in snap.towers) {
            val tier = t.visualTier.coerceIn(0, 3)
            val isSelected = t.padIndex == snap.selectedPadIndex
            val selBoost = if (isSelected) 0.18f else 0f

            Mat.translation(modelMat, t.x, t.y, t.z)
            drawLit(
                m.towerPedestals[tier], modelMat, matType = 0,
                emR = t.type.colorR * (overclockGlow + selBoost),
                emG = t.type.colorG * (overclockGlow + selBoost),
                emB = t.type.colorB * (overclockGlow + selBoost)
            )

            val yawRadT = (t.yawDeg * PI / 180.0).toFloat()
            val recoilOffset = -t.recoil * 0.11f
            val rx = sin(yawRadT) * recoilOffset
            val rz = cos(yawRadT) * recoilOffset

            Mat.translation(modelMat, t.x + rx, t.y, t.z + rz)
            Mat.rotateY(modelMat, yawRadT)
            val emFactor = 0.22f + tier * 0.08f + overclockGlow + t.recoil * 0.35f + selBoost
            drawLit(
                m.towerTurrets[t.type.ordinal][tier], modelMat, matType = 0,
                emR = t.type.colorR * emFactor,
                emG = t.type.colorG * emFactor,
                emB = t.type.colorB * emFactor
            )
        }

        // Active 3D Enemies
        for (e in snap.enemies) {
            val bob = if (e.type.isFlying) {
                0.14f * sin(e.animPhase)
            } else {
                abs(sin(e.animPhase)) * 0.08f
            }
            Mat.translation(modelMat, e.x, e.y + bob, e.z)
            Mat.rotateY(modelMat, (e.yawDeg * PI / 180.0).toFloat())
            Mat.scale(modelMat, e.scale, e.scale, e.scale)

            var tR = 1.0f
            var tG = 1.0f
            var tB = 1.0f
            if (e.isChilled) {
                tR = 0.55f; tG = 0.88f; tB = 1.25f
            } else if (e.isBurning) {
                tR = 1.25f; tG = 0.68f; tB = 0.45f
            }

            val flash = e.hitFlash * 0.85f + (if (e.isPhaseSurging) 0.45f else 0.14f)
            drawLit(
                m.enemyMeshes[e.type.ordinal], modelMat, matType = 0,
                tintR = tR, tintG = tG, tintB = tB,
                emR = e.type.colorR * flash + e.hitFlash * 0.5f,
                emG = e.type.colorG * flash + e.hitFlash * 0.5f,
                emB = e.type.colorB * flash + e.hitFlash * 0.5f
            )
        }

        // Active 3D Projectiles
        for (p in snap.projectiles) {
            Mat.translation(modelMat, p.x, p.y, p.z)
            Mat.rotateY(modelMat, (p.yawDeg * PI / 180.0).toFloat())
            Mat.rotateX(modelMat, (-p.pitchDeg * PI / 180.0).toFloat())
            if (p.isCrit) {
                Mat.scale(modelMat, 1.35f, 1.35f, 1.35f)
            }
            val mesh = when (p.type) {
                TowerType.MORTAR -> m.projMortar
                TowerType.MISSILE -> m.projMissile
                else -> m.projBullet
            }
            drawLit(
                mesh, modelMat, matType = 0,
                emR = p.type.colorR * 1.4f,
                emG = p.type.colorG * 1.4f,
                emB = p.type.colorB * 1.4f
            )
        }
    }

    private fun renderFxAndParticlesPass(
        m: Models,
        partSys: ParticleSystem,
        snap: RenderSnapshot,
        partDt: Float
    ) {
        val prog = fxProg ?: return
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthMask(false)
        GLES30.glEnable(GLES30.GL_BLEND)
        // Additive blending for glowing energy rings, lasers, lightning, and shields
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)

        prog.use()
        prog.m4("uVP", vpMat)
        prog.f("uOutScale", pipeline.outScale)

        fun drawFx(mesh: Mesh, model: FloatArray, r: Float, g: Float, b: Float, a: Float) {
            prog.m4("uModel", model)
            prog.f4("uColor", r, g, b, a)
            mesh.draw()
        }

        // 1. Selected Pad Highlight Ring + Range Preview Circle
        val selPad = ArenaMap.PADS.getOrNull(snap.selectedPadIndex)
        if (selPad != null) {
            val pulse = 0.75f + 0.25f * sin(snap.simTime * 5.5f)
            Mat.translation(modelMat, selPad.x, selPad.y + 0.025f, selPad.z)
            Mat.scale(modelMat, 1.05f, 1f, 1.05f)
            drawFx(m.fxRing, modelMat, 0.0f, 0.95f * pulse, 1.0f, 0.85f)

            val selTower = snap.towers.firstOrNull { it.padIndex == snap.selectedPadIndex }
            val previewRange = selTower?.range ?: 6.2f
            val rColR = selTower?.type?.colorR ?: 0.0f
            val rColG = selTower?.type?.colorG ?: 0.92f
            val rColB = selTower?.type?.colorB ?: 1.0f
            Mat.translation(modelMat, selPad.x, 0.14f, selPad.z)
            Mat.scale(modelMat, previewRange, 1f, previewRange)
            drawFx(m.thinRangeRing, modelMat, rColR, rColG, rColB, 0.55f)
        }

        // 2. Chrono-Beacon Aura Rings & Continuous Tower Beams (Cryo / Prism)
        for (t in snap.towers) {
            if (t.type == TowerType.BEACON) {
                val auraPulse = 0.45f + 0.20f * sin(snap.simTime * 3.2f + t.padIndex)
                Mat.translation(modelMat, t.x, t.y + 0.04f, t.z)
                Mat.scale(modelMat, t.range, 1f, t.range)
                drawFx(m.thinRangeRing, modelMat, 1.0f, 0.84f, 0.22f, auraPulse)
            }
            if (t.beamActive) {
                val startX = t.x + sin((t.yawDeg * PI / 180.0).toFloat()) * 0.45f
                val startY = t.y + 0.72f
                val startZ = t.z + cos((t.yawDeg * PI / 180.0).toFloat()) * 0.45f
                val radius = if (t.type == TowerType.PRISM) (0.07f + t.lockIntensity * 0.09f) else 0.09f
                val intensity = if (t.type == TowerType.PRISM) (1.2f + t.lockIntensity * 1.4f) else 1.2f
                if (orientBeamMatrix(modelMat, startX, startY, startZ, t.beamEndX, t.beamEndY, t.beamEndZ, radius)) {
                    drawFx(
                        m.unitBeam, modelMat,
                        t.type.colorR * intensity,
                        t.type.colorG * intensity,
                        t.type.colorB * intensity,
                        0.88f
                    )
                }
            }
        }

        // 3. Transient Beams (Railgun slugs, Tesla chain lightning, Mender repair beams, Orbital Strike sky-beam)
        for (bm in snap.beams) {
            val alpha = (bm.ttl / bm.maxTtl.coerceAtLeast(1e-3f)).coerceIn(0f, 1f)
            if (orientBeamMatrix(modelMat, bm.x0, bm.y0, bm.z0, bm.x1, bm.y1, bm.z1, bm.width)) {
                drawFx(m.unitBeam, modelMat, bm.r * 1.6f, bm.g * 1.6f, bm.b * 1.6f, alpha)
            }
        }

        // 4. Expanding Shockwave Rings
        for (sw in snap.shockwaves) {
            val progFrac = (sw.age / sw.duration.coerceAtLeast(1e-3f)).coerceIn(0f, 1f)
            val rad = (0.3f + (sw.maxRadius - 0.3f) * progFrac).coerceAtLeast(0.2f)
            val alpha = (1f - progFrac) * 0.85f
            Mat.translation(modelMat, sw.x, sw.y, sw.z)
            Mat.scale(modelMat, rad, 1f, rad)
            drawFx(m.fxRing, modelMat, sw.r * 1.5f, sw.g * 1.5f, sw.b * 1.5f, alpha)
        }

        // 5. Quantum Core Shield Dome & Enemy Aegis Shield Domes
        if (snap.coreShieldFraction > 0.02f) {
            val domeScale = 2.05f + 0.04f * sin(snap.simTime * 3.5f)
            Mat.translation(modelMat, ArenaMap.CORE_POS.x, 1.15f, ArenaMap.CORE_POS.z)
            Mat.scale(modelMat, domeScale, domeScale * 0.85f, domeScale)
            drawFx(m.shieldDome, modelMat, 0.0f, 0.88f, 1.0f, 0.16f * snap.coreShieldFraction)
        }

        for (e in snap.enemies) {
            if (e.shieldFraction > 0.02f) {
                val r = e.scale * 0.72f
                Mat.translation(modelMat, e.x, e.y + 0.42f * e.scale, e.z)
                Mat.scale(modelMat, r, r, r)
                drawFx(m.shieldDome, modelMat, 0.25f, 0.78f, 1.0f, 0.28f * e.shieldFraction)
            }
        }

        // 6. Floating 3D Health & Shield Bars above damaged or Boss enemies
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        for (e in snap.enemies) {
            if (e.hpFraction < 0.995f || e.shieldFraction > 0.01f || e.type.isBoss) {
                val barW = if (e.type.isBoss) 1.65f else 0.88f
                val barY = e.y + e.scale * (if (e.type.isBoss) 1.45f else 1.12f)

                // Dark backing bar
                Mat.translation(modelMat, e.x, barY, e.z)
                Mat.scale(modelMat, barW, 1.0f, 1.0f)
                drawFx(m.barQuad, modelMat, 0.05f, 0.08f, 0.12f, 0.82f)

                // HP fill bar (green -> amber -> crimson)
                val hpFrac = e.hpFraction.coerceIn(0.02f, 1f)
                val hpR = (2f * (1f - hpFrac)).coerceIn(0f, 1f)
                val hpG = (2f * hpFrac).coerceIn(0.15f, 1f)
                val offset = -(barW * (1f - hpFrac)) * 0.5f
                Mat.translation(modelMat, e.x + offset, barY + 0.01f, e.z + 0.01f)
                Mat.scale(modelMat, barW * hpFrac, 0.82f, 1.1f)
                drawFx(m.barQuad, modelMat, hpR, hpG, 0.22f, 0.95f)

                // Shield bar above HP bar if shielded
                if (e.shieldFraction > 0.01f) {
                    val shFrac = e.shieldFraction.coerceIn(0.02f, 1f)
                    val shOffset = -(barW * (1f - shFrac)) * 0.5f
                    Mat.translation(modelMat, e.x + shOffset, barY + 0.13f, e.z + 0.01f)
                    Mat.scale(modelMat, barW * shFrac, 0.62f, 1.1f)
                    drawFx(m.barQuad, modelMat, 0.15f, 0.85f, 1.0f, 0.95f)
                }
            }
        }

        // 7. 3D Point-Sprite Particle System (Additive)
        val pProg = particleProg
        if (pProg != null) {
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)
            pProg.use()
            pProg.m4("uVP", vpMat)
            partSys.stepAndDraw(partDt, pProg, pipeline.sceneH.toFloat(), pipeline.outScale)
        }

        GLES30.glDisable(GLES30.GL_BLEND)
        GLES30.glDepthMask(true)
    }

    private fun renderPostProcess() {
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthMask(false)
        GLES30.glDisable(GLES30.GL_BLEND)

        val downProg = bloomDownProg ?: return
        val upProg = bloomUpProg ?: return
        val compProg = compositeProg ?: return

        val levels = pipeline.bloomLevels
        if (levels > 0) {
            downProg.use()
            downProg.i("uTex", 0)
            downProg.f("uOutScale", pipeline.outScale)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)

            var srcTex = pipeline.sceneTex
            var srcW = pipeline.sceneW
            var srcH = pipeline.sceneH
            for (i in 0 until levels) {
                GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, pipeline.bloomFbo[i])
                GLES30.glViewport(0, 0, pipeline.bloomW[i], pipeline.bloomH[i])
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, srcTex)
                downProg.f2("uTexel", 1f / srcW, 1f / srcH)
                downProg.i("uFirstPass", if (i == 0) 1 else 0)
                pipeline.drawQuad()
                srcTex = pipeline.bloomTex[i]
                srcW = pipeline.bloomW[i]
                srcH = pipeline.bloomH[i]
            }

            GLES30.glEnable(GLES30.GL_BLEND)
            GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE)
            upProg.use()
            upProg.i("uTex", 0)
            for (i in levels - 1 downTo 1) {
                GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, pipeline.bloomFbo[i - 1])
                GLES30.glViewport(0, 0, pipeline.bloomW[i - 1], pipeline.bloomH[i - 1])
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, pipeline.bloomTex[i])
                upProg.f2("uTexel", 1f / pipeline.bloomW[i], 1f / pipeline.bloomH[i])
                pipeline.drawQuad()
            }
            GLES30.glDisable(GLES30.GL_BLEND)
        }

        // Final ACES Filmic Composite to Screen
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, surfaceW, surfaceH)
        compProg.use()
        compProg.f("uOutScale", pipeline.outScale)
        compProg.f("uBloomOn", if (levels > 0) 1f else 0f)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, pipeline.sceneTex)
        compProg.i("uScene", 0)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, if (levels > 0) pipeline.bloomTex[0] else pipeline.sceneTex)
        compProg.i("uBloom", 1)

        pipeline.drawQuad()
        GLES30.glDepthMask(true)
    }

    /**
     * Builds a 4x4 model matrix that stretches `unitBeam` (0,0,0 -> 0,1,0) from (x0,y0,z0) to (x1,y1,z1) with radius `r`.
     */
    private fun orientBeamMatrix(
        out: FloatArray,
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        r: Float
    ): Boolean {
        val dx = x1 - x0
        val dy = y1 - y0
        val dz = z1 - z0
        val len = sqrt(dx * dx + dy * dy + dz * dz)
        if (len < 1e-4f) return false
        val ux = dx / len
        val uy = dy / len
        val uz = dz / len
        var ax = 1f
        val ay = 0f
        var az = 0f
        if (abs(ux) > 0.9f) {
            ax = 0f
            az = 1f
        }
        var cx = ay * uz - az * uy
        var cy = az * ux - ax * uz
        var cz = ax * uy - ay * ux
        val cl = sqrt(cx * cx + cy * cy + cz * cz).coerceAtLeast(1e-9f)
        cx /= cl; cy /= cl; cz /= cl
        val bx = uy * cz - uz * cy
        val by = uz * cx - ux * cz
        val bz = ux * cy - uy * cx

        out[0] = bx * r; out[1] = by * r; out[2] = bz * r; out[3] = 0f
        out[4] = dx;     out[5] = dy;     out[6] = dz;     out[7] = 0f
        out[8] = cx * r; out[9] = cy * r; out[10] = cz * r; out[11] = 0f
        out[12] = x0;    out[13] = y0;    out[14] = z0;    out[15] = 1f
        return true
    }

    /**
     * Unprojects a 2D touch tap on the screen into a 3D ray and selects the nearest tactical pad on the y = 0.45 plane.
     */
    fun handleTap(screenX: Float, screenY: Float, viewW: Int, viewH: Int): Int {
        if (viewW <= 0 || viewH <= 0) return -1
        val localInvVp = FloatArray(16)
        synchronized(invVpLock) {
            System.arraycopy(invVpMat, 0, localInvVp, 0, 16)
        }
        val hit = Mat.intersectHorizontalPlane(
            invVp = localInvVp,
            screenX = screenX,
            screenY = screenY,
            viewportW = viewW.toFloat(),
            viewportH = viewH.toFloat(),
            planeY = ArenaMap.PAD_HEIGHT
        ) ?: return -1
        return engine.selectPadNearWorld(hit.first, hit.second)
    }
}
