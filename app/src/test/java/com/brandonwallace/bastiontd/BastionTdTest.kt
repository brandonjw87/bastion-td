package com.brandonwallace.bastiontd

import com.brandonwallace.bastiontd.game.ArenaMap
import com.brandonwallace.bastiontd.game.Biome
import com.brandonwallace.bastiontd.game.Economy
import com.brandonwallace.bastiontd.game.EnemyType
import com.brandonwallace.bastiontd.game.GameEngine
import com.brandonwallace.bastiontd.game.TargetMode
import com.brandonwallace.bastiontd.game.TechTree
import com.brandonwallace.bastiontd.game.TowerType
import com.brandonwallace.bastiontd.gl.Mat
import com.brandonwallace.bastiontd.gl.Models
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BastionTdTest {

    @Test
    fun testHiddenCompoundInterestAndCarry() {
        // 1000 * 0.16769 = 167.69 -> whole interest 167, carryScaled 69000
        val r1 = Economy.computeWaveInterest(1000L, 0)
        assertEquals(167L, r1.wholePaid)
        assertEquals(69000, r1.newCarryScaled)

        // Next wave with 1167 balance + 0.69 carry:
        // 1167 * 0.16769 = 195.69423 + 0.69 = 196.38423 -> whole 196, carryScaled 38423
        val r2 = Economy.computeWaveInterest(1167L, r1.newCarryScaled)
        assertEquals(196L, r2.wholePaid)
        assertEquals(38423, r2.newCarryScaled)
    }

    @Test
    fun testTowerTiersAndEconomyScaling() {
        for (type in TowerType.entries) {
            assertEquals(0, type.visualTier(1))
            assertEquals(0, type.visualTier(4))
            assertEquals(1, type.visualTier(5))
            assertEquals(2, type.visualTier(10))
            assertEquals(3, type.visualTier(20))
            assertTrue(type.damageAt(10) > type.damageAt(1))
            assertTrue(type.upgradeCost(5) > type.baseCost)
        }
        var mode = TargetMode.FIRST
        repeat(TargetMode.entries.size) {
            mode = mode.next()
        }
        assertEquals(TargetMode.FIRST, mode)
    }

    @Test
    fun testArenaMapAnd3DRayIntersection() {
        assertEquals(20, ArenaMap.PADS.size)
        assertTrue(ArenaMap.TOTAL_LENGTH > 30f)

        val start = ArenaMap.sampleAtDistance(0f)
        val end = ArenaMap.sampleAtDistance(ArenaMap.TOTAL_LENGTH)
        assertEquals(ArenaMap.PORTAL_POS.x, start.x, 0.05f)
        assertEquals(ArenaMap.PORTAL_POS.z, start.z, 0.05f)
        assertEquals(ArenaMap.CORE_POS.x, end.x, 0.05f)
        assertEquals(ArenaMap.CORE_POS.z, end.z, 0.05f)

        val proj = Mat.new()
        val view = Mat.new()
        val vp = Mat.new()
        val invVp = Mat.new()
        Mat.perspective(proj, 46f, 1080f / 1920f, 1.2f, 85f)
        Mat.lookAt(view, 0f, 20f, 16f, 0f, 0.2f, 0.8f, 0f, 1f, 0f)
        Mat.mul(vp, proj, view)
        assertTrue(Mat.invert(invVp, vp))

        val hit = Mat.intersectHorizontalPlane(invVp, 540f, 960f, 1080f, 1920f, ArenaMap.PAD_HEIGHT)
        assertNotNull(hit)
    }

    @Test
    fun testNonStopSimulationAndAiCommander() {
        val engine = GameEngine(null)
        engine.setSpeed(4)
        // Simulate 180 seconds of 4x speed non-stop gameplay
        for (i in 0 until 3600) {
            engine.step(0.05f)
        }
        val snap = engine.captureUiSnapshot()
        assertTrue("Wave should advance automatically", snap.wave >= 2)
        assertTrue("Total kills should accumulate", snap.totalKills > 0L)
        assertTrue("Core shield stays positive thanks to Never-Stop Quantum Core Overload", snap.coreShield > 0)
    }

    @Test
    fun testSaveLoadAndOfflineProgress() {
        val engine = GameEngine(null)
        engine.rushNextWave()
        engine.buildTower(0, TowerType.TESLA)
        engine.upgradeTower(2)
        engine.buyTech("t1_kinetic_amp")

        val t0 = 1_700_000_000_000L
        val savedJson = engine.saveToJson(nowMs = t0)
        assertTrue(savedJson.isNotBlank())

        val restored = GameEngine(null)
        val ok = restored.loadFromJson(savedJson, nowMs = t0 + 600_000L) // +10 minutes offline
        assertTrue(ok)
        val snap = restored.captureUiSnapshot()
        assertEquals(2, snap.wave)
        assertEquals(1, snap.techLevels["t1_kinetic_amp"])
        assertNotNull("Offline defense report should be generated after 10m away", snap.offlineReport)
    }

    @Test
    fun testAllProcedural3DMeshBuilders() {
        val builders = listOf(
            Models.buildCanyonFloor(),
            Models.buildConduitFloor(),
            Models.buildRoadway(),
            Models.buildPerimeterCliffs(),
            Models.buildAllTacticalPads(),
            Models.buildPortalGate(),
            Models.buildPortalVortexRing(),
            Models.buildQuantumCoreBase(),
            Models.buildQuantumCoreOrb(),
            Models.buildQuantumCoreGyro(),
            Models.buildProjBullet(),
            Models.buildProjMortar(),
            Models.buildProjMissile(),
            Models.buildShieldDome(),
            Models.buildUnitBeam(),
            Models.buildFxRing(),
            Models.buildThinRangeRing(),
            Models.buildBarQuad()
        )
        for (b in builders) {
            assertTrue(b.vertexCount > 0)
            assertTrue(b.indexCount > 0)
        }

        for (tier in 0..3) {
            val ped = Models.buildTowerPedestal(tier)
            assertTrue(ped.vertexCount > 0)
            for (type in TowerType.entries) {
                val tur = Models.buildTowerTurret(type, tier)
                assertTrue(tur.vertexCount > 0)
                assertTrue(tur.indexCount > 0)
            }
        }

        for (eType in EnemyType.entries) {
            val em = Models.buildEnemyMesh(eType)
            assertTrue(em.vertexCount > 0)
            assertTrue(em.indexCount > 0)
        }

        assertEquals(36, TechTree.NODES.size)
        assertEquals(24, TechTree.MILESTONES.size)
        assertEquals(5, Biome.entries.size)
    }
}
