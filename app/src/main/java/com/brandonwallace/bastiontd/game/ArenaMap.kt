package com.brandonwallace.bastiontd.game

import kotlin.math.atan2
import kotlin.math.sqrt

data class Vec3(val x: Float, val y: Float, val z: Float)

data class PadSpec(
    val index: Int,
    val x: Float,
    val y: Float,
    val z: Float,
    val ringName: String
)

object ArenaMap {
    const val PAD_HEIGHT = 0.45f
    const val ROAD_WIDTH = 1.65f
    const val PATH_HALF_WIDTH = ROAD_WIDTH * 0.5f
    const val FLYING_ALTITUDE = 2.45f

    /**
     * Waypoints forming a grand S-curve canyon gauntlet from the North Spawn Portal (0, 0, -13.5)
     * down to the South Quantum Core (0, 0, +13.0).
     */
    val WAYPOINTS: List<Vec3> = listOf(
        Vec3(0.0f, 0.0f, -13.5f),   // 0: Spawn Portal Gate
        Vec3(0.0f, 0.0f, -10.5f),   // 1: North entry straight
        Vec3(-5.2f, 0.0f, -10.0f),  // 2: Upper West bend
        Vec3(-5.2f, 0.0f, -5.8f),   // 3: Upper West drop
        Vec3(5.2f, 0.0f, -5.2f),    // 4: Upper East cross
        Vec3(5.2f, 0.0f, -0.8f),    // 5: Mid East drop
        Vec3(-5.2f, 0.0f, -0.2f),   // 6: Center West cross
        Vec3(-5.2f, 0.0f, 4.2f),    // 7: Lower West drop
        Vec3(5.2f, 0.0f, 4.8f),     // 8: Lower East cross
        Vec3(5.2f, 0.0f, 9.2f),     // 9: South East drop
        Vec3(0.0f, 0.0f, 9.6f),     // 10: Core approach center
        Vec3(0.0f, 0.0f, 13.0f)     // 11: Quantum Core Reactor
    )

    private val segLengths: FloatArray
    private val cumLengths: FloatArray
    val TOTAL_LENGTH: Float

    init {
        val n = WAYPOINTS.size - 1
        segLengths = FloatArray(n)
        cumLengths = FloatArray(n + 1)
        var sum = 0f
        for (i in 0 until n) {
            val a = WAYPOINTS[i]
            val b = WAYPOINTS[i + 1]
            val dx = b.x - a.x
            val dz = b.z - a.z
            val len = sqrt(dx * dx + dz * dz)
            segLengths[i] = len
            sum += len
            cumLengths[i + 1] = sum
        }
        TOTAL_LENGTH = sum
    }

    /**
     * 20 Strategic 3D Tactical Pads placed along the inner bends and outer ridges of the canyon road.
     */
    val PADS: List<PadSpec> = listOf(
        // Vanguard North Sector (Pads 0..3)
        PadSpec(0, -2.5f, PAD_HEIGHT, -12.2f, "North Gate West"),
        PadSpec(1, 2.5f, PAD_HEIGHT, -12.2f, "North Gate East"),
        PadSpec(2, -2.4f, PAD_HEIGHT, -7.9f, "Upper Terrace West"),
        PadSpec(3, 2.4f, PAD_HEIGHT, -7.9f, "Upper Terrace East"),

        // Upper Canyon Bends (Pads 4..7)
        PadSpec(4, -7.6f, PAD_HEIGHT, -7.9f, "West Cliff Overlook"),
        PadSpec(5, 0.0f, PAD_HEIGHT, -7.7f, "North Central Bastion"),
        PadSpec(6, -2.4f, PAD_HEIGHT, -3.0f, "Upper Switchback West"),
        PadSpec(7, 2.4f, PAD_HEIGHT, -3.0f, "Upper Switchback East"),

        // Mid-Citadel Killzone (Pads 8..11)
        PadSpec(8, 7.6f, PAD_HEIGHT, -3.0f, "East Cliff Overlook"),
        PadSpec(9, 0.0f, PAD_HEIGHT, -2.7f, "Heartland Spire"),
        PadSpec(10, -2.4f, PAD_HEIGHT, 2.0f, "Lower Switchback West"),
        PadSpec(11, 2.4f, PAD_HEIGHT, 2.0f, "Lower Switchback East"),

        // Lower Canyon Gauntlet (Pads 12..15)
        PadSpec(12, -7.6f, PAD_HEIGHT, 2.0f, "South-West Ridge"),
        PadSpec(13, 0.0f, PAD_HEIGHT, 2.3f, "Central Nexus Pad"),
        PadSpec(14, -2.4f, PAD_HEIGHT, 7.0f, "Inner Sanctum West"),
        PadSpec(15, 2.4f, PAD_HEIGHT, 7.0f, "Inner Sanctum East"),

        // Quantum Core Guard (Pads 16..19)
        PadSpec(16, 7.6f, PAD_HEIGHT, 7.0f, "South-East Ridge"),
        PadSpec(17, 0.0f, PAD_HEIGHT, 7.2f, "Core Vanguard Pylon"),
        PadSpec(18, -2.6f, PAD_HEIGHT, 11.4f, "Quantum Aegis West"),
        PadSpec(19, 2.6f, PAD_HEIGHT, 11.4f, "Quantum Aegis East")
    )

    /**
     * Interpolates world (x, z) and yaw (degrees, 0 = +Z) at [dist] along the path (0..TOTAL_LENGTH).
     * Writes (x, z, yawDeg) into [out3].
     */
    fun sampleAtDistance(dist: Float, out3: FloatArray) {
        val d = dist.coerceIn(0f, TOTAL_LENGTH)
        val n = segLengths.size
        var seg = 0
        while (seg < n - 1 && d > cumLengths[seg + 1]) {
            seg++
        }
        val segLen = segLengths[seg].coerceAtLeast(1e-4f)
        val t = ((d - cumLengths[seg]) / segLen).coerceIn(0f, 1f)
        val a = WAYPOINTS[seg]
        val b = WAYPOINTS[seg + 1]
        out3[0] = a.x + (b.x - a.x) * t
        out3[1] = a.z + (b.z - a.z) * t
        val dx = b.x - a.x
        val dz = b.z - a.z
        out3[2] = Math.toDegrees(atan2(dx.toDouble(), dz.toDouble())).toFloat()
    }

    fun sampleAtDistance(dist: Float): Vec3 {
        val out = FloatArray(3)
        sampleAtDistance(dist, out)
        return Vec3(out[0], 0f, out[1])
    }

    val PORTAL_POS = WAYPOINTS.first()
    val CORE_POS = WAYPOINTS.last()
}
