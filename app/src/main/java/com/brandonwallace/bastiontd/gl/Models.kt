package com.brandonwallace.bastiontd.gl

import com.brandonwallace.bastiontd.game.ArenaMap
import com.brandonwallace.bastiontd.game.EnemyType
import com.brandonwallace.bastiontd.game.TowerType
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Procedural 3D model library for Bastion 3D TD.
 * Generates the 3D canyon terrain, S-curve roadway, 20 hexagonal defense pads,
 * North Rift Portal, South Quantum Core, 8x4 evolving turret meshes, and 10 enemy meshes.
 */
class Models {
    val canyonFloor: Mesh = buildCanyonFloor().build()
    val conduitFloor: Mesh = buildConduitFloor().build()
    val roadway: Mesh = buildRoadway().build()
    val perimeterCliffs: Mesh = buildPerimeterCliffs().build()
    val tacticalPadsStatic: Mesh = buildAllTacticalPads().build()
    val portalGate: Mesh = buildPortalGate().build()
    val portalVortexRing: Mesh = buildPortalVortexRing().build()
    val quantumCoreBase: Mesh = buildQuantumCoreBase().build()
    val quantumCoreOrb: Mesh = buildQuantumCoreOrb().build()
    val quantumCoreGyro: Mesh = buildQuantumCoreGyro().build()

    // 4 visual tiers of turret pedestal bases (Mk I, Mk II, Mk III, Omega)
    val towerPedestals: Array<Mesh> = Array(4) { tier ->
        buildTowerPedestal(tier).build()
    }

    // 8 tower archetypes x 4 visual evolution tiers
    val towerTurrets: Array<Array<Mesh>> = Array(TowerType.entries.size) { typeOrd ->
        val type = TowerType.entries[typeOrd]
        Array(4) { tier ->
            buildTowerTurret(type, tier).build()
        }
    }

    // 10 enemy 3D meshes
    val enemyMeshes: Array<Mesh> = Array(EnemyType.entries.size) { ord ->
        buildEnemyMesh(EnemyType.entries[ord]).build()
    }

    // Projectiles & FX primitives
    val projBullet: Mesh = buildProjBullet().build()
    val projMortar: Mesh = buildProjMortar().build()
    val projMissile: Mesh = buildProjMissile().build()
    val shieldDome: Mesh = buildShieldDome().build()
    val unitBeam: Mesh = buildUnitBeam().build()
    val fxRing: Mesh = buildFxRing().build()
    val thinRangeRing: Mesh = buildThinRangeRing().build()
    val barQuad: Mesh = buildBarQuad().build()

    fun release() {
        canyonFloor.release()
        conduitFloor.release()
        roadway.release()
        perimeterCliffs.release()
        tacticalPadsStatic.release()
        portalGate.release()
        portalVortexRing.release()
        quantumCoreBase.release()
        quantumCoreOrb.release()
        quantumCoreGyro.release()
        towerPedestals.forEach { it.release() }
        towerTurrets.forEach { arr -> arr.forEach { it.release() } }
        enemyMeshes.forEach { it.release() }
        projBullet.release()
        projMortar.release()
        projMissile.release()
        shieldDome.release()
        unitBeam.release()
        fxRing.release()
        thinRangeRing.release()
        barQuad.release()
    }

    companion object {
        fun buildCanyonFloor(): MeshBuilder = MeshBuilder().apply {
            color(0.10f, 0.13f, 0.19f).material(0.78f, 0.18f)
            disc(36.0f, -0.02f, 48, true)
        }

        fun buildConduitFloor(): MeshBuilder = MeshBuilder().apply {
            color(0.05f, 0.65f, 0.85f).material(0.20f, 0.88f)
            ringDisc(4.35f, 4.60f, -0.012f, 40, true)
            ringDisc(9.10f, 9.38f, -0.012f, 48, true)
            ringDisc(14.10f, 14.45f, -0.012f, 56, true)
        }

        fun buildRoadway(): MeshBuilder = MeshBuilder().apply {
            val pts = ArenaMap.WAYPOINTS
            val halfW = ArenaMap.PATH_HALF_WIDTH
            val curbW = halfW + 0.26f
            val yTop = 0.08f
            val yCurb = 0.17f
            val yBot = -0.08f

            // Compute smooth miter tangents at each waypoint
            val n = pts.size
            val nxArr = FloatArray(n)
            val nzArr = FloatArray(n)
            for (i in 0 until n) {
                var tx = 0f
                var tz = 0f
                if (i > 0) {
                    val dx = pts[i].x - pts[i - 1].x
                    val dz = pts[i].z - pts[i - 1].z
                    val l = hypot(dx, dz).coerceAtLeast(1e-4f)
                    tx += dx / l
                    tz += dz / l
                }
                if (i < n - 1) {
                    val dx = pts[i + 1].x - pts[i].x
                    val dz = pts[i + 1].z - pts[i].z
                    val l = hypot(dx, dz).coerceAtLeast(1e-4f)
                    tx += dx / l
                    tz += dz / l
                }
                val l = hypot(tx, tz).coerceAtLeast(1e-4f)
                tx /= l
                tz /= l
                // Perpendicular right vector in XZ plane
                nxArr[i] = -tz
                nzArr[i] = tx
            }

            // Road surfacedeck
            color(0.16f, 0.20f, 0.28f).material(0.55f, 0.35f)
            for (i in 0 until n - 1) {
                val p0 = pts[i]
                val p1 = pts[i + 1]
                val r0x = nxArr[i]; val r0z = nzArr[i]
                val r1x = nxArr[i + 1]; val r1z = nzArr[i + 1]

                val a = vertex(p0.x - r0x * halfW, yTop, p0.z - r0z * halfW, 0f, 1f, 0f)
                val b = vertex(p0.x + r0x * halfW, yTop, p0.z + r0z * halfW, 0f, 1f, 0f)
                val c = vertex(p1.x + r1x * halfW, yTop, p1.z + r1z * halfW, 0f, 1f, 0f)
                val d = vertex(p1.x - r1x * halfW, yTop, p1.z - r1z * halfW, 0f, 1f, 0f)
                quad(a, d, c, b)
            }

            // Raised armored curbs on both sides of the S-curve path
            color(0.24f, 0.30f, 0.40f).material(0.38f, 0.68f)
            for (side in intArrayOf(-1, 1)) {
                val s = side.toFloat()
                for (i in 0 until n - 1) {
                    val p0 = pts[i]
                    val p1 = pts[i + 1]
                    val r0x = nxArr[i] * s; val r0z = nzArr[i] * s
                    val r1x = nxArr[i + 1] * s; val r1z = nzArr[i + 1] * s

                    val in0x = p0.x + r0x * halfW; val in0z = p0.z + r0z * halfW
                    val out0x = p0.x + r0x * curbW; val out0z = p0.z + r0z * curbW
                    val in1x = p1.x + r1x * halfW; val in1z = p1.z + r1z * halfW
                    val out1x = p1.x + r1x * curbW; val out1z = p1.z + r1z * curbW

                    // Top of curb
                    val t0 = vertex(in0x, yCurb, in0z, 0f, 1f, 0f)
                    val t1 = vertex(out0x, yCurb, out0z, 0f, 1f, 0f)
                    val t2 = vertex(out1x, yCurb, out1z, 0f, 1f, 0f)
                    val t3 = vertex(in1x, yCurb, in1z, 0f, 1f, 0f)
                    if (side > 0) quad(t0, t3, t2, t1) else quad(t0, t1, t2, t3)

                    // Outer wall down to canyon floor
                    val w0 = vertex(out0x, yCurb, out0z, r0x, 0.2f, r0z)
                    val w1 = vertex(out0x, yBot, out0z, r0x, 0.2f, r0z)
                    val w2 = vertex(out1x, yBot, out1z, r1x, 0.2f, r1z)
                    val w3 = vertex(out1x, yCurb, out1z, r1x, 0.2f, r1z)
                    if (side > 0) quad(w0, w3, w2, w1) else quad(w0, w1, w2, w3)
                }
            }

            // Glowing chevron guide nodes along the center of the path
            color(0.0f, 0.85f, 1.0f).material(0.20f, 0.90f)
            var d = 1.8f
            while (d < ArenaMap.TOTAL_LENGTH - 1.5f) {
                val sample = ArenaMap.sampleAtDistance(d)
                box(sample.x, yTop + 0.015f, sample.z, 0.22f, 0.02f, 0.42f)
                d += 2.2f
            }
        }

        fun buildPerimeterCliffs(): MeshBuilder = MeshBuilder().apply {
            val m = Mat.new()
            // Outer canyon rock formations and armored bastion walls
            val cliffCount = 28
            for (i in 0 until cliffCount) {
                val ang = (2.0 * PI * i / cliffCount).toFloat()
                // Leave gaps directly behind North Portal and South Core for clean sightlines
                val deg = ((Math.toDegrees(ang.toDouble()) % 360.0) + 360.0) % 360.0
                val dist = if (deg in 72.0..108.0 || deg in 252.0..288.0) 19.5f else 15.5f + (i % 3) * 1.4f
                val cx = cos(ang) * dist
                val cz = sin(ang) * dist
                val h = 2.2f + (i % 4) * 0.85f
                val rBase = 2.0f + (i % 3) * 0.5f

                Mat.translation(m, cx, -0.1f, cz)
                Mat.rotateY(m, i * 37f)
                transform(m)
                color(0.14f, 0.17f, 0.24f).material(0.78f, 0.18f)
                cone(rBase, rBase * 0.58f, 0f, h, 6, true)

                // Secondary terrace step
                color(0.19f, 0.23f, 0.31f).material(0.68f, 0.28f)
                cone(rBase * 0.72f, rBase * 0.38f, h * 0.55f, h * 1.22f, 6, true)

                // Sci-fi defense pylon every 4th cliff
                if (i % 4 == 0) {
                    color(0.25f, 0.32f, 0.44f).material(0.35f, 0.75f)
                    cylinder(0.28f, h, h + 1.4f, 6, true)
                    color(0.0f, 0.90f, 1.0f).material(0.15f, 0.90f)
                    sphere(0.22f, 8, 6)
                }
            }
            transform(null)
        }

        fun buildAllTacticalPads(): MeshBuilder = MeshBuilder().apply {
            val m = Mat.new()
            for (pad in ArenaMap.PADS) {
                Mat.translation(m, pad.x, 0f, pad.z)
                transform(m)

                // Hexagonal armored foundation from canyon floor (-0.08) up to pad height (0.45)
                color(0.18f, 0.23f, 0.32f).material(0.50f, 0.60f)
                cone(1.22f, 1.06f, -0.08f, pad.y - 0.06f, 6, true)

                // Beveled metallic upper deck
                color(0.28f, 0.35f, 0.46f).material(0.32f, 0.78f)
                cone(1.06f, 0.94f, pad.y - 0.06f, pad.y, 6, true)

                // Glowing tactical cyan ring inset on the pad top
                color(0.0f, 0.85f, 1.0f).material(0.20f, 0.88f)
                ringDisc(0.72f, 0.82f, pad.y + 0.006f, 6, true)

                // Inner dark socket plate
                color(0.12f, 0.16f, 0.22f).material(0.45f, 0.55f)
                disc(0.68f, pad.y + 0.008f, 12, true)
            }
            transform(null)
        }

        fun buildPortalGate(): MeshBuilder = MeshBuilder().apply {
            val px = ArenaMap.PORTAL_POS.x
            val pz = ArenaMap.PORTAL_POS.z
            val m = Mat.new()

            // Portal base platform
            Mat.translation(m, px, 0f, pz)
            transform(m)
            color(0.20f, 0.24f, 0.34f).material(0.42f, 0.70f)
            cone(2.6f, 2.2f, -0.08f, 0.28f, 8, true)

            // Twin obsidian/crimson obelisk pillars
            color(0.26f, 0.18f, 0.36f).material(0.32f, 0.78f)
            box(-1.55f, 1.65f, 0f, 0.52f, 2.9f, 0.62f)
            box(1.55f, 1.65f, 0f, 0.52f, 2.9f, 0.62f)

            // Overhead portal arch lintel
            box(0f, 3.15f, 0f, 3.8f, 0.48f, 0.72f)

            // Glowing rift emitter strips on the pillars
            color(1.0f, 0.22f, 0.55f).material(0.15f, 0.92f)
            box(-1.26f, 1.65f, 0.1f, 0.10f, 2.5f, 0.25f)
            box(1.26f, 1.65f, 0.1f, 0.10f, 2.5f, 0.25f)
            box(0f, 2.88f, 0.1f, 2.5f, 0.12f, 0.25f)
            transform(null)
        }

        fun buildPortalVortexRing(): MeshBuilder = MeshBuilder().apply {
            // Centered at origin so GameRenderer can translate to (PORTAL_POS.x, 1.65f, PORTAL_POS.z) and spin around Z
            val m = Mat.new()
            Mat.rotationX(m, (PI * 0.5).toFloat())
            transform(m)
            color(1.0f, 0.20f, 0.60f).material(0.15f, 0.90f)
            lathe(
                floatArrayOf(
                    0.95f, -0.10f,
                    1.22f, 0.0f,
                    0.95f, 0.10f,
                    0.78f, 0.0f,
                    0.95f, -0.10f
                ),
                16
            )
            // Inner rift core disc
            color(0.75f, 0.12f, 0.95f).material(0.10f, 0.95f)
            disc(0.82f, 0f, 16, true)
            disc(0.82f, 0f, 16, false)
            transform(null)
        }

        fun buildQuantumCoreBase(): MeshBuilder = MeshBuilder().apply {
            val cx = ArenaMap.CORE_POS.x
            val cz = ArenaMap.CORE_POS.z
            val m = Mat.new()
            Mat.translation(m, cx, 0f, cz)
            transform(m)

            // Octagonal bastion platform
            color(0.20f, 0.27f, 0.38f).material(0.35f, 0.75f)
            cone(2.75f, 2.35f, -0.08f, 0.32f, 8, true)
            color(0.28f, 0.38f, 0.52f).material(0.28f, 0.82f)
            cone(2.10f, 1.75f, 0.32f, 0.58f, 8, true)

            // Glowing cyan containment ring
            color(0.0f, 0.92f, 1.0f).material(0.15f, 0.92f)
            ringDisc(1.25f, 1.62f, 0.59f, 16, true)

            // 4 angled energy buttresses
            for (i in 0 until 4) {
                val a = (PI * 0.25 + i * PI * 0.5).toFloat()
                val bx = cos(a) * 1.65f
                val bz = sin(a) * 1.65f
                val tx = cos(a) * 0.95f
                val tz = sin(a) * 0.95f
                color(0.30f, 0.40f, 0.55f).material(0.30f, 0.85f)
                tube(bx, 0.45f, bz, tx, 2.15f, tz, 0.18f, 6)
            }
            transform(null)
        }

        fun buildQuantumCoreOrb(): MeshBuilder = MeshBuilder().apply {
            color(0.0f, 0.95f, 1.0f).material(0.12f, 0.95f)
            sphere(0.68f, 18, 12)
        }

        fun buildQuantumCoreGyro(): MeshBuilder = MeshBuilder().apply {
            color(0.95f, 0.82f, 0.25f).material(0.20f, 0.92f)
            lathe(
                floatArrayOf(
                    1.02f, -0.07f,
                    1.14f, 0.0f,
                    1.02f, 0.07f,
                    0.92f, 0.0f,
                    1.02f, -0.07f
                ),
                20
            )
        }

        fun buildTowerPedestal(tier: Int): MeshBuilder = MeshBuilder().apply {
            // Tier 0 = Mk I, Tier 1 = Mk II, Tier 2 = Mk III, Tier 3 = OMEGA
            val baseR = 0.58f + tier * 0.04f
            color(0.24f, 0.30f, 0.40f).material(0.35f, 0.78f)
            cone(baseR, baseR * 0.82f, 0f, 0.26f, 8, true)

            // Tier collar ring
            when (tier) {
                0 -> color(0.35f, 0.42f, 0.52f)
                1 -> color(0.15f, 0.82f, 0.98f)
                2 -> color(0.75f, 0.35f, 1.0f)
                else -> color(1.0f, 0.82f, 0.22f)
            }
            material(0.22f, 0.88f)
            cylinder(baseR * 0.86f, 0.26f, 0.34f, 8, true)

            if (tier >= 2) {
                // Stabilizer outriggers for Mk III and OMEGA
                for (i in 0 until 4) {
                    val a = (i * PI * 0.5 + PI * 0.25).toFloat()
                    val ox = cos(a) * baseR * 0.92f
                    val oz = sin(a) * baseR * 0.92f
                    box(ox, 0.14f, oz, 0.16f, 0.24f, 0.16f)
                }
            }
        }

        fun buildTowerTurret(type: TowerType, tier: Int): MeshBuilder = MeshBuilder().apply {
            // Local origin is at (0, 0.34f, 0) above the pad; turret points toward +Z
            val cr = type.colorR
            val cg = type.colorG
            val cb = type.colorB
            val trimR = if (tier == 3) 1.0f else cr
            val trimG = if (tier == 3) 0.84f else cg
            val trimB = if (tier == 3) 0.22f else cb

            when (type) {
                TowerType.VULCAN -> {
                    // Armored turret housing
                    color(0.28f, 0.34f, 0.44f).material(0.32f, 0.78f)
                    box(0f, 0.52f, -0.06f, 0.54f, 0.36f, 0.62f)
                    color(trimR, trimG, trimB).material(0.22f, 0.88f)
                    box(0f, 0.72f, -0.05f, 0.42f, 0.08f, 0.48f)

                    // Multi-barrel Gatling assembly pointing +Z
                    val barrelCount = when (tier) {
                        0 -> 3
                        1 -> 4
                        2 -> 6
                        else -> 8
                    }
                    color(0.75f, 0.80f, 0.88f).material(0.25f, 0.92f)
                    for (b in 0 until barrelCount) {
                        val a = (2.0 * PI * b / barrelCount).toFloat()
                        val bx = cos(a) * 0.13f
                        val by = 0.52f + sin(a) * 0.13f
                        tube(bx, by, 0.18f, bx, by, 0.92f + tier * 0.08f, 0.038f, 6)
                    }
                    // Barrel clamp collar
                    color(trimR, trimG, trimB).material(0.20f, 0.90f)
                    tube(0f, 0.52f, 0.62f, 0f, 0.52f, 0.72f, 0.18f, 8)
                }

                TowerType.CRYO -> {
                    // Cryo chamber dome + twin frost projectors
                    color(0.22f, 0.32f, 0.46f).material(0.30f, 0.75f)
                    cylinder(0.36f, 0.34f, 0.68f, 10, true)
                    color(cr, cg, cb).material(0.15f, 0.90f)
                    val m = Mat.new()
                    Mat.translation(m, 0f, 0.75f, 0f)
                    transform(m)
                    sphere(0.28f + tier * 0.03f, 12, 8)
                    transform(null)

                    // Forward cryo emitter nozzles
                    color(trimR, trimG, trimB).material(0.18f, 0.90f)
                    tube(0f, 0.54f, 0.15f, 0f, 0.54f, 0.82f, 0.14f, 8)
                    if (tier >= 1) {
                        tube(-0.22f, 0.52f, 0.10f, -0.22f, 0.52f, 0.74f, 0.08f, 6)
                        tube(0.22f, 0.52f, 0.10f, 0.22f, 0.52f, 0.74f, 0.08f, 6)
                    }
                }

                TowerType.MORTAR -> {
                    // Heavy siege carriage + elevated howitzer cannon
                    color(0.30f, 0.28f, 0.34f).material(0.42f, 0.72f)
                    box(-0.26f, 0.52f, 0f, 0.16f, 0.36f, 0.58f)
                    box(0.26f, 0.52f, 0f, 0.16f, 0.36f, 0.58f)

                    // Angled siege barrel pointing up and forward
                    color(0.42f, 0.44f, 0.50f).material(0.30f, 0.85f)
                    tube(0f, 0.48f, -0.12f, 0f, 0.96f + tier * 0.06f, 0.62f, 0.20f + tier * 0.02f, 10)
                    color(trimR, trimG, trimB).material(0.20f, 0.90f)
                    tube(0f, 0.88f, 0.48f, 0f, 1.02f + tier * 0.06f, 0.68f, 0.24f + tier * 0.02f, 10)
                }

                TowerType.TESLA -> {
                    // Tall insulator column + conductive toroid rings + apex lightning sphere
                    color(0.25f, 0.24f, 0.38f).material(0.35f, 0.78f)
                    cone(0.34f, 0.18f, 0.34f, 1.18f + tier * 0.08f, 10, true)

                    val ringCount = 2 + tier
                    color(trimR, trimG, trimB).material(0.18f, 0.92f)
                    for (r in 0 until ringCount) {
                        val ry = 0.54f + r * 0.20f
                        val rad = 0.36f - r * 0.03f
                        cylinder(rad, ry - 0.03f, ry + 0.03f, 12, true)
                    }
                    val m = Mat.new()
                    Mat.translation(m, 0f, 1.28f + tier * 0.08f, 0f)
                    transform(m)
                    color(0.68f, 0.48f, 1.0f).material(0.12f, 0.95f)
                    sphere(0.24f + tier * 0.03f, 12, 8)
                    transform(null)
                }

                TowerType.RAILGUN -> {
                    // Long twin magnetic accelerator rails with glowing plasma core
                    color(0.24f, 0.30f, 0.40f).material(0.30f, 0.85f)
                    box(0f, 0.52f, -0.18f, 0.52f, 0.32f, 0.66f)

                    val railLen = 1.08f + tier * 0.10f
                    color(0.75f, 0.82f, 0.92f).material(0.22f, 0.92f)
                    box(-0.14f, 0.56f, railLen * 0.42f, 0.09f, 0.16f, railLen)
                    box(0.14f, 0.56f, railLen * 0.42f, 0.09f, 0.16f, railLen)

                    // Glowing accelerator core between the rails
                    color(trimR, trimG, trimB).material(0.15f, 0.95f)
                    box(0f, 0.56f, railLen * 0.38f, 0.10f, 0.08f, railLen * 0.92f)
                }

                TowerType.PRISM -> {
                    // Focusing crystal cradle + floating octahedron prism
                    color(0.32f, 0.22f, 0.38f).material(0.30f, 0.82f)
                    box(-0.28f, 0.62f, 0f, 0.10f, 0.52f, 0.24f)
                    box(0.28f, 0.62f, 0f, 0.10f, 0.52f, 0.24f)

                    color(trimR, trimG, trimB).material(0.12f, 0.95f)
                    cone(0.02f, 0.28f + tier * 0.03f, 0.48f, 0.82f, 6, false)
                    cone(0.28f + tier * 0.03f, 0.02f, 0.82f, 1.18f + tier * 0.06f, 6, false)
                    // Forward focusing lens
                    tube(0f, 0.82f, 0.15f, 0f, 0.82f, 0.58f, 0.12f, 8)
                }

                TowerType.MISSILE -> {
                    // Angled multi-cell VLS / SAM missile battery pod
                    color(0.26f, 0.34f, 0.32f).material(0.38f, 0.75f)
                    box(0f, 0.62f, -0.04f, 0.68f + tier * 0.05f, 0.48f, 0.56f)

                    // Warhead tips peeking out of launch tubes
                    color(trimR, trimG, trimB).material(0.20f, 0.88f)
                    for (ix in intArrayOf(-1, 1)) {
                        for (iy in intArrayOf(0, 1)) {
                            val mx = ix * 0.18f
                            val my = 0.50f + iy * 0.20f
                            tube(mx, my, 0.20f, mx, my, 0.42f, 0.075f, 6)
                        }
                    }
                    // Radar dish on top for Mk II+
                    if (tier >= 1) {
                        color(0.85f, 0.90f, 0.96f).material(0.25f, 0.88f)
                        box(0f, 0.96f, -0.08f, 0.46f, 0.14f, 0.08f)
                    }
                }

                TowerType.BEACON -> {
                    // Chrono-Beacon amplifier spire with golden/cyan harmonic rings
                    color(0.32f, 0.28f, 0.18f).material(0.30f, 0.85f)
                    cone(0.36f, 0.14f, 0.34f, 1.15f, 8, true)

                    color(trimR, trimG, trimB).material(0.15f, 0.95f)
                    ringDisc(0.36f, 0.52f + tier * 0.05f, 0.72f, 12, true)
                    ringDisc(0.36f, 0.52f + tier * 0.05f, 0.72f, 12, false)
                    cone(0.02f, 0.22f, 1.05f, 1.32f, 6, false)
                    cone(0.22f, 0.02f, 1.32f, 1.60f, 6, false)
                }
            }
        }

        fun buildEnemyMesh(type: EnemyType): MeshBuilder = MeshBuilder().apply {
            val cr = type.colorR
            val cg = type.colorG
            val cb = type.colorB

            when (type) {
                EnemyType.STRIDER -> {
                    // Sleek 4-legged cybernetic hound/strider facing +Z
                    color(cr, cg, cb).material(0.32f, 0.78f)
                    box(0f, 0.42f, 0f, 0.38f, 0.24f, 0.68f)
                    // Forward visor head
                    color(1.0f, 0.85f, 0.35f).material(0.18f, 0.92f)
                    box(0f, 0.46f, 0.34f, 0.28f, 0.14f, 0.22f)
                    // 4 angled strider legs
                    color(0.25f, 0.28f, 0.35f).material(0.40f, 0.75f)
                    for (sx in intArrayOf(-1, 1)) {
                        for (sz in intArrayOf(-1, 1)) {
                            tube(sx * 0.18f, 0.38f, sz * 0.22f, sx * 0.30f, 0.04f, sz * 0.28f, 0.055f, 6)
                        }
                    }
                }

                EnemyType.GOLIATH -> {
                    // Massive sloped-armor siege tank / walker
                    color(cr, cg, cb).material(0.40f, 0.72f)
                    box(0f, 0.45f, 0f, 0.76f, 0.44f, 0.92f)
                    // Heavy side treads/shoulders
                    color(0.22f, 0.25f, 0.32f).material(0.50f, 0.65f)
                    box(-0.44f, 0.28f, 0f, 0.20f, 0.36f, 0.98f)
                    box(0.44f, 0.28f, 0f, 0.20f, 0.36f, 0.98f)
                    // Glowing core vent
                    color(1.0f, 0.75f, 0.20f).material(0.18f, 0.92f)
                    box(0f, 0.56f, 0.36f, 0.42f, 0.16f, 0.26f)
                }

                EnemyType.SWARM, EnemyType.MITE -> {
                    // Fast razor-winged dart drone
                    color(cr, cg, cb).material(0.28f, 0.82f)
                    val m = Mat.new()
                    Mat.rotationX(m, (PI * 0.5).toFloat())
                    transform(m)
                    cone(0.26f, 0.02f, -0.32f, 0.42f, 6, true)
                    transform(null)
                    box(0f, 0.32f, -0.05f, 0.64f, 0.06f, 0.28f)
                }

                EnemyType.SHIELD_BEARER -> {
                    // Armored projector chassis with overhead Aegis emitter mast
                    color(cr, cg, cb).material(0.28f, 0.85f)
                    cylinder(0.42f, 0.12f, 0.58f, 8, true)
                    color(0.85f, 0.95f, 1.0f).material(0.15f, 0.95f)
                    val m = Mat.new()
                    Mat.translation(m, 0f, 0.78f, 0f)
                    transform(m)
                    sphere(0.24f, 10, 8)
                    transform(null)
                }

                EnemyType.SPLITTER -> {
                    // Tri-core cluster construct that visibly shows its 3 fission pods
                    color(cr, cg, cb).material(0.28f, 0.80f)
                    for (i in 0 until 3) {
                        val a = (2.0 * PI * i / 3.0).toFloat()
                        val ox = cos(a) * 0.24f
                        val oz = sin(a) * 0.24f
                        val m = Mat.new()
                        Mat.translation(m, ox, 0.42f, oz)
                        transform(m)
                        sphere(0.24f, 10, 8)
                    }
                    transform(null)
                }

                EnemyType.MENDER -> {
                    // Hovering bio-repair vessel with emerald cross-fins
                    color(cr, cg, cb).material(0.24f, 0.85f)
                    cylinder(0.36f, 0.22f, 0.66f, 8, true)
                    color(0.75f, 1.0f, 0.85f).material(0.15f, 0.92f)
                    box(0f, 0.48f, 0f, 0.88f, 0.12f, 0.20f)
                    box(0f, 0.48f, 0f, 0.20f, 0.12f, 0.88f)
                }

                EnemyType.PHANTOM -> {
                    // Angular stealth phase-stalker crystal blade
                    color(cr, cg, cb).material(0.20f, 0.90f)
                    cone(0.04f, 0.36f, 0.10f, 0.55f, 4, false)
                    cone(0.36f, 0.04f, 0.55f, 1.05f, 4, false)
                    box(0f, 0.52f, 0f, 0.72f, 0.08f, 0.42f)
                }

                EnemyType.VALKYRIE -> {
                    // Swept-wing gunship jet (rendered at flying altitude)
                    color(cr, cg, cb).material(0.25f, 0.88f)
                    box(0f, 0f, 0f, 0.28f, 0.20f, 0.96f)
                    // Swept delta wings
                    box(0f, 0.02f, -0.06f, 1.18f, 0.06f, 0.42f)
                    // Twin tail fins
                    box(-0.22f, 0.18f, -0.34f, 0.06f, 0.26f, 0.28f)
                    box(0.22f, 0.18f, -0.34f, 0.06f, 0.26f, 0.28f)
                }

                EnemyType.DREADNOUGHT -> {
                    // Colossal multi-turreted Dreadnought Leviathan Boss
                    color(0.22f, 0.16f, 0.24f).material(0.35f, 0.82f)
                    box(0f, 0.48f, 0f, 0.92f, 0.52f, 1.36f)
                    // Outrigger armored sponsons
                    box(-0.58f, 0.36f, 0f, 0.26f, 0.38f, 1.12f)
                    box(0.58f, 0.36f, 0f, 0.26f, 0.38f, 1.12f)
                    // Command bridge citadel
                    color(cr, cg, cb).material(0.22f, 0.90f)
                    box(0f, 0.88f, -0.18f, 0.56f, 0.38f, 0.62f)
                    // Glowing molten reactor core in the chest
                    color(1.0f, 0.65f, 0.15f).material(0.12f, 0.95f)
                    val m = Mat.new()
                    Mat.translation(m, 0f, 0.62f, 0.42f)
                    transform(m)
                    sphere(0.32f, 12, 8)
                    transform(null)
                }
            }
        }

        fun buildProjBullet(): MeshBuilder = MeshBuilder().apply {
            color(1.0f, 0.85f, 0.28f).material(0.15f, 0.95f)
            box(0f, 0f, 0f, 0.10f, 0.10f, 0.38f)
        }

        fun buildProjMortar(): MeshBuilder = MeshBuilder().apply {
            color(1.0f, 0.46f, 0.14f).material(0.20f, 0.90f)
            sphere(0.20f, 10, 8)
        }

        fun buildProjMissile(): MeshBuilder = MeshBuilder().apply {
            color(0.35f, 0.98f, 0.68f).material(0.18f, 0.92f)
            box(0f, 0f, 0f, 0.12f, 0.12f, 0.48f)
        }

        fun buildShieldDome(): MeshBuilder = MeshBuilder().apply {
            color(0.22f, 0.72f, 1.0f).material(0.15f, 0.90f)
            sphere(1.0f, 14, 10)
        }

        fun buildUnitBeam(): MeshBuilder = MeshBuilder().apply {
            color(1.0f, 1.0f, 1.0f).material(0.10f, 0.95f)
            cylinder(1.0f, 0f, 1f, 8, true)
        }

        fun buildFxRing(): MeshBuilder = MeshBuilder().apply {
            color(1.0f, 1.0f, 1.0f).material(0.15f, 0.90f)
            ringDisc(0.84f, 1.0f, 0f, 32, true)
        }

        fun buildThinRangeRing(): MeshBuilder = MeshBuilder().apply {
            color(1.0f, 1.0f, 1.0f).material(0.15f, 0.90f)
            ringDisc(0.965f, 1.0f, 0f, 48, true)
        }

        fun buildBarQuad(): MeshBuilder = MeshBuilder().apply {
            color(1.0f, 1.0f, 1.0f).material(0.20f, 0.80f)
            box(0f, 0f, 0f, 1.0f, 0.11f, 0.06f)
        }
    }
}
