package com.brandonwallace.bastiontd.game

import kotlin.math.pow
import kotlin.math.roundToLong

enum class TowerType(
    val id: Int,
    val title: String,
    val shortName: String,
    val roleTag: String,
    val summary: String,
    val baseCost: Long,
    val baseDamage: Float,
    val baseFireRate: Float, // shots per second
    val baseRange: Float,    // world units
    val hitsAir: Boolean,
    val splashRadius: Float,
    val armorPen: Float,     // 0..1 fraction of enemy armor ignored
    val shieldMult: Float,   // multiplier vs energy shields
    val accentColor: Long    // ARGB hex for UI
) {
    VULCAN(
        id = 0,
        title = "Vulcan Minigun",
        shortName = "Vulcan",
        roleTag = "KINETIC · RAPID",
        summary = "High-velocity rotary Gatling turret. Shreds single targets and airships with relentless kinetic fire.",
        baseCost = 100L,
        baseDamage = 18f,
        baseFireRate = 4.5f,
        baseRange = 5.6f,
        hitsAir = true,
        splashRadius = 0f,
        armorPen = 0.20f,
        shieldMult = 0.9f,
        accentColor = 0xFFFFB300
    ),
    CRYO(
        id = 1,
        title = "Cryo-Matrix Spire",
        shortName = "Cryo",
        roleTag = "FROST · SLOW & BRITTLE",
        summary = "Emits sub-zero crystalline beams that chill enemies up to 65% and make their armor brittle (+35% damage taken).",
        baseCost = 140L,
        baseDamage = 14f,
        baseFireRate = 3.2f,
        baseRange = 5.2f,
        hitsAir = true,
        splashRadius = 1.6f,
        armorPen = 0.35f,
        shieldMult = 1.1f,
        accentColor = 0xFF00E5FF
    ),
    MORTAR(
        id = 2,
        title = "Hellfire Mortar",
        shortName = "Mortar",
        roleTag = "ARTILLERY · SPLASH",
        summary = "Heavy howitzer launching high-arcing thermite shells that obliterate clustered ground forces.",
        baseCost = 175L,
        baseDamage = 72f,
        baseFireRate = 0.78f,
        baseRange = 7.4f,
        hitsAir = false,
        splashRadius = 2.4f,
        armorPen = 0.45f,
        shieldMult = 1.0f,
        accentColor = 0xFFFF5722
    ),
    TESLA(
        id = 3,
        title = "Tesla Arc Pylon",
        shortName = "Tesla",
        roleTag = "CHAIN · SHIELD BREAKER",
        summary = "Discharges branching 3D lightning arcs across multiple enemies. Deals 250% damage to energy shields.",
        baseCost = 210L,
        baseDamage = 42f,
        baseFireRate = 1.45f,
        baseRange = 5.4f,
        hitsAir = true,
        splashRadius = 0f,
        armorPen = 0.60f,
        shieldMult = 2.5f,
        accentColor = 0xFF7C4DFF
    ),
    RAILGUN(
        id = 4,
        title = "Plasma Railgun",
        shortName = "Railgun",
        roleTag = "PIERCING · SNIPER",
        summary = "Electromagnetic hyper-cannon whose plasma lance pierces every enemy along the firing line. Ignores 80% armor.",
        baseCost = 260L,
        baseDamage = 155f,
        baseFireRate = 0.58f,
        baseRange = 8.8f,
        hitsAir = true,
        splashRadius = 0.85f,
        armorPen = 0.80f,
        shieldMult = 1.4f,
        accentColor = 0xFF18FFFF
    ),
    PRISM(
        id = 5,
        title = "Solar Prism Laser",
        shortName = "Prism",
        roleTag = "THERMAL · RAMPING BEAM",
        summary = "Locks a continuous thermal beam onto a target, ramping damage up to 350% the longer it stays focused.",
        baseCost = 310L,
        baseDamage = 34f,
        baseFireRate = 5.0f,
        baseRange = 6.4f,
        hitsAir = true,
        splashRadius = 0f,
        armorPen = 0.65f,
        shieldMult = 1.35f,
        accentColor = 0xFFFF1744
    ),
    MISSILE(
        id = 6,
        title = "Valkyrie SAM Silo",
        shortName = "Missile",
        roleTag = "HOMING · SALVO",
        summary = "Launches salvos of homing micro-missiles across the map. Deals +75% bonus damage to Flying & Boss units.",
        baseCost = 350L,
        baseDamage = 58f,
        baseFireRate = 0.95f,
        baseRange = 9.2f,
        hitsAir = true,
        splashRadius = 1.8f,
        armorPen = 0.55f,
        shieldMult = 1.25f,
        accentColor = 0xFF69F0AE
    ),
    BEACON(
        id = 7,
        title = "Chrono-Beacon",
        shortName = "Beacon",
        roleTag = "AURA · AMPLIFIER",
        summary = "Boosts fire rate, range, and credit yield of all nearby towers while emitting harmonic shock-pulses.",
        baseCost = 280L,
        baseDamage = 26f,
        baseFireRate = 1.1f,
        baseRange = 5.8f,
        hitsAir = true,
        splashRadius = 5.8f,
        armorPen = 0.50f,
        shieldMult = 1.5f,
        accentColor = 0xFFFFD740
    );

    val colorR: Float get() = ((accentColor shr 16) and 0xFF) / 255f
    val colorG: Float get() = ((accentColor shr 8) and 0xFF) / 255f
    val colorB: Float get() = (accentColor and 0xFF) / 255f
    val role: String get() = roleTag
    val description: String get() = summary
    val icon: String
        get() = when (this) {
            VULCAN -> "🔥"
            CRYO -> "❄️"
            MORTAR -> "💣"
            TESLA -> "⚡"
            RAILGUN -> "💠"
            PRISM -> "🔆"
            MISSILE -> "🚀"
            BEACON -> "📡"
        }

    /** Visual evolution tier: 0 = Mk I (Lv 1-4), 1 = Mk II (Lv 5-9), 2 = Mk III (Lv 10-19), 3 = Omega (Lv 20+). */
    fun visualTier(level: Int): Int = when {
        level >= 20 -> 3
        level >= 10 -> 2
        level >= 5 -> 1
        else -> 0
    }

    fun tierLabel(level: Int): String = when (visualTier(level)) {
        3 -> "OMEGA"
        2 -> "Mk III"
        1 -> "Mk II"
        else -> "Mk I"
    }

    /** Cost to upgrade from [currentLevel] to [currentLevel + 1]. */
    fun upgradeCost(currentLevel: Int, costDiscountMul: Float = 1f): Long {
        val raw = baseCost * 0.72 * (1.28.pow(currentLevel - 1))
        return (raw * costDiscountMul).roundToLong().coerceAtLeast(25L)
    }

    /** Damage per shot at [level] before tech multipliers. */
    fun damageAt(level: Int): Float {
        val lvlGrowth = 1f + (level - 1) * 0.34f + (level - 1) * (level - 1) * 0.032f
        val tierBonus = when (visualTier(level)) {
            3 -> 2.15f
            2 -> 1.50f
            1 -> 1.22f
            else -> 1.0f
        }
        return baseDamage * lvlGrowth * tierBonus
    }

    /** Shots per second at [level] before tech/aura multipliers. */
    fun fireRateAt(level: Int): Float {
        val boost = 1f + (level - 1).coerceAtMost(35) * 0.035f
        return baseFireRate * boost
    }

    /** Range in world units at [level] before tech/aura multipliers. */
    fun rangeAt(level: Int): Float {
        val boost = 1f + (level - 1).coerceAtMost(25) * 0.016f
        return baseRange * boost
    }

    /** Number of chain bounces for TESLA or missiles per salvo for MISSILE. */
    fun multiCountAt(level: Int): Int = when (this) {
        TESLA -> 3 + visualTier(level) + (level / 8).coerceAtMost(3)
        MISSILE -> 2 + visualTier(level) + (level / 10).coerceAtMost(2)
        else -> 1
    }

    companion object {
        fun byId(id: Int): TowerType = entries.getOrElse(id) { VULCAN }
    }
}

enum class TargetMode(val label: String) {
    FIRST("First"),
    STRONGEST("Strongest"),
    WEAKEST("Weakest"),
    FASTEST("Fastest"),
    CLOSEST("Closest");

    fun next(): TargetMode = entries[(ordinal + 1) % entries.size]
}
