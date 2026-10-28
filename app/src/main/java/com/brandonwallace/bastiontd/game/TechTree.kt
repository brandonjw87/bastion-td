package com.brandonwallace.bastiontd.game

import kotlin.math.pow
import kotlin.math.roundToLong

enum class TechCategory(val tier: Int, val title: String, val subtitle: String) {
    BALLISTICS(1, "TIER I · BALLISTICS & GRID", "Core kinetic calibration & turret actuators"),
    CRYO_TESLA(2, "TIER II · CRYO & HIGH-VOLTAGE", "Sub-zero brittle matrices & arc conductors"),
    HEAVY_ORDNANCE(3, "TIER III · HEAVY ORDNANCE", "Railgun capacitors, thermite shells & solar optics"),
    AI_DOCTRINE(4, "TIER IV · AUTONOMOUS DOCTRINE", "AI Auto-Commander efficiency & targeting networks"),
    QUANTUM_ECONOMY(5, "TIER V · QUANTUM CORE & BOUNTY", "Core shield matrices, salvage yields & wave bonuses"),
    OMEGA_ASCENSION(6, "TIER VI · OMEGA ASCENSION", "Singularity overdrives & supreme turret amplification")
}

data class TechNode(
    val id: String,
    val category: TechCategory,
    val title: String,
    val description: String,
    val maxLevel: Int,
    val baseShardCost: Int,
    val costGrowth: Float,
    val perLevelStatText: String
) {
    fun costForNext(currentLevel: Int): Int {
        if (currentLevel >= maxLevel) return 0
        val raw = baseShardCost * (costGrowth.toDouble().pow(currentLevel))
        return raw.roundToLong().toInt().coerceAtLeast(1)
    }
}

data class MilestoneSpec(
    val id: String,
    val title: String,
    val description: String,
    val creditReward: Long,
    val shardReward: Int
) {
    val requirementText: String get() = description
}

object TechTree {
    val NODES: List<TechNode> = listOf(
        // TIER 1: BALLISTICS & GRID (6 nodes)
        TechNode("t1_kinetic_amp", TechCategory.BALLISTICS, "Tungsten Sabot Rounds", "Increases Vulcan Minigun and Missile damage by +14% per level.", 10, 1, 1.45f, "+14% Kinetic DMG"),
        TechNode("t1_servo_gyros", TechCategory.BALLISTICS, "High-Torque Servo Gyros", "Increases fire rate of all turrets by +6% per level.", 10, 1, 1.50f, "+6% Global Fire Rate"),
        TechNode("t1_radar_uplink", TechCategory.BALLISTICS, "Phased-Array Radar Uplink", "Extends firing range of all turrets by +5% per level.", 8, 2, 1.55f, "+5% Global Range"),
        TechNode("t1_gatling_spin", TechCategory.BALLISTICS, "Magnetic Barrel Bearings", "Vulcan Minigun gains +10% fire rate and +4% armor shred per level.", 8, 2, 1.50f, "+10% Vulcan Rate"),
        TechNode("t1_pad_fabricator", TechCategory.BALLISTICS, "Nano-Fabricator Discounts", "Reduces tower build and upgrade credit costs by 4% per level.", 8, 2, 1.60f, "-4% Build/Upgrade Cost"),
        TechNode("t1_crit_optics", TechCategory.BALLISTICS, "Precision Laser Rangefinders", "All towers gain +4% critical hit chance (2.2x damage) per level.", 10, 2, 1.55f, "+4% Crit Chance"),

        // TIER 2: CRYO & HIGH-VOLTAGE (6 nodes)
        TechNode("t2_cryo_deep", TechCategory.CRYO_TESLA, "Liquid Helium Injectors", "Increases Cryo-Matrix slow potency and duration by +12% per level.", 8, 3, 1.55f, "+12% Cryo Slow"),
        TechNode("t2_brittle_fracture", TechCategory.CRYO_TESLA, "Crystalline Resonance", "Chilled enemies take an extra +6% bonus damage from all towers per level.", 8, 4, 1.60f, "+6% Brittle Vulnerability"),
        TechNode("t2_tesla_voltage", TechCategory.CRYO_TESLA, "Superconducting Coils", "Increases Tesla Arc Pylon damage by +16% and shield damage by +15% per level.", 10, 3, 1.52f, "+16% Tesla DMG"),
        TechNode("t2_tesla_chains", TechCategory.CRYO_TESLA, "Ionized Arc Branching", "Tesla Arc Pylon lightning jumps to +1 additional enemy per 2 levels (+10% arc range).", 6, 5, 1.70f, "+Arc Chain Reach"),
        TechNode("t2_emp_overload", TechCategory.CRYO_TESLA, "Shield Disruptor Harmonics", "All energy towers deal +15% extra damage to enemy Aegis shields per level.", 8, 4, 1.55f, "+15% Shield Break"),
        TechNode("t2_frost_nova_cd", TechCategory.CRYO_TESLA, "Cryo-Capacitor Recycling", "Reduces Absolute Zero Nova ability cooldown by 7% per level.", 6, 4, 1.60f, "-7% Cryo Nova Cooldown"),

        // TIER 3: HEAVY ORDNANCE (6 nodes)
        TechNode("t3_rail_capacitors", TechCategory.HEAVY_ORDNANCE, "Hyper-Velocity Rail Banks", "Increases Plasma Railgun damage by +18% and pierce width by +8% per level.", 10, 5, 1.55f, "+18% Railgun DMG"),
        TechNode("t3_thermite_compound", TechCategory.HEAVY_ORDNANCE, "Aluminothermic Shells", "Increases Hellfire Mortar blast damage by +15% and burn DOT by +20% per level.", 10, 5, 1.55f, "+15% Mortar & Burn"),
        TechNode("t3_shockwave_radius", TechCategory.HEAVY_ORDNANCE, "Concussive Blast Shapers", "Expands splash radius of Mortar, Missile, and Cryo towers by +7% per level.", 8, 6, 1.60f, "+7% AoE Splash Radius"),
        TechNode("t3_prism_lenses", TechCategory.HEAVY_ORDNANCE, "Diamond Focusing Prisms", "Solar Prism Laser ramps up 20% faster and reaches +25% higher max beam multiplier per level.", 8, 6, 1.62f, "+25% Max Prism Ramp"),
        TechNode("t3_sam_warheads", TechCategory.HEAVY_ORDNANCE, "Seeker Swarm Micro-Warheads", "Valkyrie SAM Silo deals +16% damage and +12% extra vs Flying & Bosses per level.", 10, 6, 1.55f, "+16% Missile DMG"),
        TechNode("t3_armor_pierce", TechCategory.HEAVY_ORDNANCE, "Depleted Neutronium Cores", "All towers ignore an additional +5% of enemy armor plating per level.", 8, 7, 1.65f, "+5% Global Armor Pen"),

        // TIER 4: AUTONOMOUS DOCTRINE (6 nodes)
        TechNode("t4_beacon_harmonics", TechCategory.AI_DOCTRINE, "Chrono-Harmonic Amplifiers", "Increases Chrono-Beacon fire-rate and range aura bonus by +12% per level.", 8, 8, 1.60f, "+12% Beacon Aura"),
        TechNode("t4_auto_overclock", TechCategory.AI_DOCTRINE, "Neural Fire-Control Mesh", "While AI Auto-Upgrade or Auto-Build is active, all towers gain +8% damage per level.", 8, 8, 1.60f, "+8% Autonomous DMG"),
        TechNode("t4_orbital_focused", TechCategory.AI_DOCTRINE, "Stratospheric Ion Array", "Increases Orbital Ion Cannon strike damage by +25% and reduces cooldown by 6% per level.", 8, 9, 1.65f, "+25% Orbital Strike"),
        TechNode("t4_overclock_surge", TechCategory.AI_DOCTRINE, "Tachyon Field Generators", "Increases Hyper-Overclock duration by +0.8s and fire rate boost by +10% per level.", 8, 9, 1.65f, "+0.8s Overclock Surge"),
        TechNode("t4_boss_hunter", TechCategory.AI_DOCTRINE, "Leviathan Threat Protocol", "All towers deal +15% bonus damage to Sector Dreadnought bosses per level.", 10, 10, 1.58f, "+15% Boss Damage"),
        TechNode("t4_air_superiority", TechCategory.AI_DOCTRINE, "Sky-Guard Flak Network", "All air-capable towers deal +15% bonus damage to Valkyrie Gunships per level.", 8, 8, 1.55f, "+15% Anti-Air DMG"),

        // TIER 5: QUANTUM CORE & BOUNTY (6 nodes)
        TechNode("t5_kill_bounty", TechCategory.QUANTUM_ECONOMY, "Nanite Scrap Harvesters", "Increases credits earned from every enemy destroyed by +12% per level.", 10, 6, 1.52f, "+12% Kill Credits"),
        TechNode("t5_wave_dividend", TechCategory.QUANTUM_ECONOMY, "Sector Command Logistics", "Increases wave clear credit bonuses by +15% per level.", 10, 7, 1.55f, "+15% Wave Bonus"),
        TechNode("t5_core_capacity", TechCategory.QUANTUM_ECONOMY, "Quantum Aegis Containment", "Increases maximum Core Shield capacity by +250 and shield regen by +20% per level.", 10, 6, 1.50f, "+250 Core Shield"),
        TechNode("t5_core_nova", TechCategory.QUANTUM_ECONOMY, "Overload Feedback Matrix", "Core shockwaves deal +35% more damage and grant +20% bonus credits when triggered.", 6, 8, 1.65f, "+35% Core Nova"),
        TechNode("t5_shard_extractor", TechCategory.QUANTUM_ECONOMY, "Zero-Point Shard Distillers", "Increases Quantum Shard drops from Bosses and every 5th wave by +20% per level.", 8, 12, 1.75f, "+20% Shard Yield"),
        TechNode("t5_offline_sim", TechCategory.QUANTUM_ECONOMY, "Autonomous Command Relay", "Increases offline/background defense wave speed and credit efficiency by +15% per level.", 8, 8, 1.60f, "+15% Offline Yield"),

        // TIER 6: OMEGA ASCENSION (6 nodes)
        TechNode("t6_omega_damage", TechCategory.OMEGA_ASCENSION, "Singularity Core Infusion", "Multiplies total damage of all towers by +22% per level.", 15, 15, 1.55f, "+22% All Tower DMG"),
        TechNode("t6_crit_devastation", TechCategory.OMEGA_ASCENSION, "Hyper-Lethal Criticals", "Increases critical hit damage multiplier by +0.35x per level.", 10, 16, 1.60f, "+0.35x Crit Multiplier"),
        TechNode("t6_mk_evolution", TechCategory.OMEGA_ASCENSION, "Omega Chassis Overdrive", "Mk II, Mk III, and Omega evolved towers deal an extra +18% damage per level.", 10, 18, 1.62f, "+18% Evolved Tower DMG"),
        TechNode("t6_executioner", TechCategory.OMEGA_ASCENSION, "Quantum Disintegrator", "Enemies below 15% (+1% per level) HP take 300% damage from all weapons.", 8, 20, 1.70f, "+Execute Threshold"),
        TechNode("t6_eternal_fortress", TechCategory.OMEGA_ASCENSION, "Chrono-Continuum Loop", "Reduces all Commander Ability cooldowns by an extra 5% per level.", 8, 22, 1.70f, "-5% All Cooldowns"),
        TechNode("t6_apex_sovereign", TechCategory.OMEGA_ASCENSION, "Bastion Sovereign Protocol", "Boosts all damage, fire rate, and credit income by +12% per level.", 20, 25, 1.50f, "+12% All Stats & Income")
    )

    val MILESTONES: List<MilestoneSpec> = listOf(
        MilestoneSpec("m_first_tower", "First Line of Defense", "Deploy your first 3D turret on a tactical pad.", 150L, 2),
        MilestoneSpec("m_full_grid_6", "Perimeter Established", "Maintain 6 active turrets simultaneously.", 400L, 3),
        MilestoneSpec("m_full_grid_12", "Fortress Network", "Maintain 12 active turrets simultaneously.", 1_200L, 5),
        MilestoneSpec("m_full_grid_20", "Total Bastion Saturation", "Fill all 20 tactical pads with active 3D turrets.", 4_500L, 10),
        MilestoneSpec("m_mk2_upgrade", "Mk II Evolution", "Upgrade any turret to Level 5 (Mk II chassis).", 500L, 3),
        MilestoneSpec("m_mk3_upgrade", "Mk III Evolution", "Upgrade any turret to Level 10 (Mk III chassis).", 2_000L, 6),
        MilestoneSpec("m_omega_upgrade", "Omega Ascension", "Upgrade any turret to Level 20 (Omega chassis).", 10_000L, 15),
        MilestoneSpec("m_all_archetypes", "Combined Arms Doctrine", "Have all 8 turret archetypes active on the battlefield at once.", 3_500L, 8),
        MilestoneSpec("m_wave_5", "Vanguard Repelled", "Reach Wave 5 in endless defense.", 350L, 2),
        MilestoneSpec("m_wave_10", "Obsidian Victor", "Clear Wave 10 and defeat the first Sector Dreadnought.", 1_200L, 5),
        MilestoneSpec("m_wave_20", "Glacier Conqueror", "Reach Wave 20 in Cryo-Glacier Bastion.", 3_500L, 8),
        MilestoneSpec("m_wave_30", "Mars Canyon Commander", "Reach Wave 30 in Crimson Mars Canyon.", 8_000L, 12),
        MilestoneSpec("m_wave_40", "Xeno-Core Purifier", "Reach Wave 40 in Emerald Xeno-Core.", 18_000L, 16),
        MilestoneSpec("m_wave_50", "Singularity Sovereign", "Reach Wave 50 in Void Singularity Ring.", 40_000L, 25),
        MilestoneSpec("m_wave_75", "Endless Centurion", "Reach Wave 75 in non-stop defense.", 100_000L, 40),
        MilestoneSpec("m_wave_100", "Immortal Bastion", "Reach Wave 100 in non-stop defense.", 250_000L, 75),
        MilestoneSpec("m_kills_100", "Scrap Collector", "Destroy 100 enemy units.", 300L, 2),
        MilestoneSpec("m_kills_500", "Steel Grinder", "Destroy 500 enemy units.", 1_500L, 5),
        MilestoneSpec("m_kills_2500", "Legion Annihilator", "Destroy 2,500 enemy units.", 8_000L, 12),
        MilestoneSpec("m_kills_10000", "Extinction Event", "Destroy 10,000 enemy units.", 35_000L, 25),
        MilestoneSpec("m_boss_kills_5", "Dreadnought Slayer", "Destroy 5 Sector Dreadnought bosses.", 5_000L, 10),
        MilestoneSpec("m_tech_10", "R&D Pioneer", "Purchase 10 Command Tech Tree upgrades.", 1_500L, 4),
        MilestoneSpec("m_tech_35", "Tech Singularity", "Purchase 35 Command Tech Tree upgrades.", 9_000L, 12),
        MilestoneSpec("m_tech_80", "Supreme Architect", "Purchase 80 Command Tech Tree upgrades.", 45_000L, 30)
    )
}
