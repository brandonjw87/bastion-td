package com.brandonwallace.bastiontd.game

enum class EnemyType(
    val id: Int,
    val title: String,
    val hpMult: Float,
    val shieldRatio: Float, // shield relative to maxHp
    val speed: Float,       // world units per second
    val armor: Float,       // 0..0.75 damage reduction before penetration
    val isFlying: Boolean,
    val isBoss: Boolean,
    val rewardMult: Float,
    val scale: Float,
    val colorR: Float,
    val colorG: Float,
    val colorB: Float
) {
    STRIDER(
        id = 0,
        title = "Scout Strider",
        hpMult = 0.85f,
        shieldRatio = 0f,
        speed = 2.85f,
        armor = 0.05f,
        isFlying = false,
        isBoss = false,
        rewardMult = 0.9f,
        scale = 0.72f,
        colorR = 0.95f, colorG = 0.42f, colorB = 0.18f
    ),
    GOLIATH(
        id = 1,
        title = "Armored Goliath",
        hpMult = 2.65f,
        shieldRatio = 0.15f,
        speed = 1.45f,
        armor = 0.48f,
        isFlying = false,
        isBoss = false,
        rewardMult = 1.8f,
        scale = 1.05f,
        colorR = 0.52f, colorG = 0.58f, colorB = 0.64f
    ),
    SWARM(
        id = 2,
        title = "Swarm Drone",
        hpMult = 0.45f,
        shieldRatio = 0f,
        speed = 3.55f,
        armor = 0.0f,
        isFlying = false,
        isBoss = false,
        rewardMult = 0.55f,
        scale = 0.55f,
        colorR = 1.0f, colorG = 0.76f, colorB = 0.12f
    ),
    SHIELD_BEARER(
        id = 3,
        title = "Aegis Vanguard",
        hpMult = 1.40f,
        shieldRatio = 1.25f,
        speed = 1.85f,
        armor = 0.25f,
        isFlying = false,
        isBoss = false,
        rewardMult = 1.6f,
        scale = 0.92f,
        colorR = 0.20f, colorG = 0.75f, colorB = 1.0f
    ),
    SPLITTER(
        id = 4,
        title = "Hydra Carrier",
        hpMult = 2.10f,
        shieldRatio = 0.25f,
        speed = 1.60f,
        armor = 0.30f,
        isFlying = false,
        isBoss = false,
        rewardMult = 1.7f,
        scale = 1.0f,
        colorR = 0.68f, colorG = 0.25f, colorB = 0.92f
    ),
    MITE(
        id = 5,
        title = "Assault Mite",
        hpMult = 0.32f,
        shieldRatio = 0f,
        speed = 3.85f,
        armor = 0f,
        isFlying = false,
        isBoss = false,
        rewardMult = 0.35f,
        scale = 0.44f,
        colorR = 0.88f, colorG = 0.35f, colorB = 0.98f
    ),
    MENDER(
        id = 6,
        title = "Nano-Mender",
        hpMult = 1.55f,
        shieldRatio = 0.40f,
        speed = 1.90f,
        armor = 0.20f,
        isFlying = false,
        isBoss = false,
        rewardMult = 1.65f,
        scale = 0.85f,
        colorR = 0.18f, colorG = 0.92f, colorB = 0.48f
    ),
    PHANTOM(
        id = 7,
        title = "Phantom Stalker",
        hpMult = 1.15f,
        shieldRatio = 0.50f,
        speed = 3.15f,
        armor = 0.15f,
        isFlying = false,
        isBoss = false,
        rewardMult = 1.5f,
        scale = 0.80f,
        colorR = 0.15f, colorG = 0.95f, colorB = 0.88f
    ),
    VALKYRIE(
        id = 8,
        title = "Valkyrie Gunship",
        hpMult = 1.60f,
        shieldRatio = 0.35f,
        speed = 2.45f,
        armor = 0.22f,
        isFlying = true,
        isBoss = false,
        rewardMult = 1.85f,
        scale = 0.95f,
        colorR = 0.96f, colorG = 0.26f, colorB = 0.38f
    ),
    DREADNOUGHT(
        id = 9,
        title = "Sector Dreadnought",
        hpMult = 11.5f,
        shieldRatio = 0.85f,
        speed = 1.05f,
        armor = 0.55f,
        isFlying = false,
        isBoss = true,
        rewardMult = 9.0f,
        scale = 1.58f,
        colorR = 1.0f, colorG = 0.22f, colorB = 0.12f
    );

    companion object {
        fun byId(id: Int): EnemyType = entries.getOrElse(id) { STRIDER }
    }
}

enum class Biome(
    val id: Int,
    val title: String,
    val subtitle: String,
    val groundTopR: Float, val groundTopG: Float, val groundTopB: Float,
    val cliffR: Float, val cliffG: Float, val cliffB: Float,
    val roadR: Float, val roadG: Float, val roadB: Float,
    val conduitR: Float, val conduitG: Float, val conduitB: Float,
    val skyZenithR: Float, val skyZenithG: Float, val skyZenithB: Float,
    val skyHorizonR: Float, val skyHorizonG: Float, val skyHorizonB: Float
) {
    OBSIDIAN(
        id = 0,
        title = "Obsidian Citadel",
        subtitle = "Volcanic Basalt & Magma Conduits",
        groundTopR = 0.14f, groundTopG = 0.15f, groundTopB = 0.18f,
        cliffR = 0.08f, cliffG = 0.09f, cliffB = 0.11f,
        roadR = 0.22f, roadG = 0.23f, roadB = 0.26f,
        conduitR = 1.0f, conduitG = 0.38f, conduitB = 0.06f,
        skyZenithR = 0.03f, skyZenithG = 0.05f, skyZenithB = 0.10f,
        skyHorizonR = 0.28f, skyHorizonG = 0.11f, skyHorizonB = 0.06f
    ),
    CRYO_GLACIER(
        id = 1,
        title = "Cryo-Glacier Bastion",
        subtitle = "Sub-Zero Permafrost & Aurora Spire",
        groundTopR = 0.20f, groundTopG = 0.30f, groundTopB = 0.42f,
        cliffR = 0.11f, cliffG = 0.18f, cliffB = 0.28f,
        roadR = 0.25f, roadG = 0.35f, roadB = 0.46f,
        conduitR = 0.05f, conduitG = 0.88f, conduitB = 1.0f,
        skyZenithR = 0.02f, skyZenithG = 0.08f, skyZenithB = 0.16f,
        skyHorizonR = 0.06f, skyHorizonG = 0.30f, skyHorizonB = 0.42f
    ),
    CRIMSON_DUNE(
        id = 2,
        title = "Crimson Mars Canyon",
        subtitle = "Iron-Oxide Cliffs & Solar Arrays",
        groundTopR = 0.34f, groundTopG = 0.18f, groundTopB = 0.12f,
        cliffR = 0.22f, cliffG = 0.11f, cliffB = 0.08f,
        roadR = 0.28f, roadG = 0.22f, roadB = 0.20f,
        conduitR = 1.0f, conduitG = 0.68f, conduitB = 0.12f,
        skyZenithR = 0.08f, skyZenithG = 0.04f, skyZenithB = 0.06f,
        skyHorizonR = 0.42f, skyHorizonG = 0.18f, skyHorizonB = 0.10f
    ),
    XENO_SWAMP(
        id = 3,
        title = "Emerald Xeno-Core",
        subtitle = "Bioluminescent Ruins & Acid Trenches",
        groundTopR = 0.11f, groundTopG = 0.22f, groundTopB = 0.16f,
        cliffR = 0.07f, cliffG = 0.14f, cliffB = 0.10f,
        roadR = 0.18f, roadG = 0.25f, roadB = 0.22f,
        conduitR = 0.15f, conduitG = 1.0f, conduitB = 0.45f,
        skyZenithR = 0.02f, skyZenithG = 0.09f, skyZenithB = 0.07f,
        skyHorizonR = 0.06f, skyHorizonG = 0.28f, skyHorizonB = 0.18f
    ),
    VOID_RING(
        id = 4,
        title = "Void Singularity Ring",
        subtitle = "Orbital Dark-Matter Fortress",
        groundTopR = 0.13f, groundTopG = 0.11f, groundTopB = 0.22f,
        cliffR = 0.07f, cliffG = 0.06f, cliffB = 0.14f,
        roadR = 0.21f, roadG = 0.19f, roadB = 0.30f,
        conduitR = 0.72f, conduitG = 0.22f, conduitB = 1.0f,
        skyZenithR = 0.02f, skyZenithG = 0.01f, skyZenithB = 0.06f,
        skyHorizonR = 0.22f, skyHorizonG = 0.08f, skyHorizonB = 0.36f
    );

    val sunR: Float get() = 1.35f
    val sunG: Float get() = 1.28f
    val sunB: Float get() = 1.18f
    val groundTintR: Float get() = groundTopR
    val groundTintG: Float get() = groundTopG
    val groundTintB: Float get() = groundTopB
    val pathTintR: Float get() = roadR
    val pathTintG: Float get() = roadG
    val pathTintB: Float get() = roadB

    companion object {
        fun forWave(wave: Int): Biome = entries[((wave - 1) / 10) % entries.size]
    }
}
