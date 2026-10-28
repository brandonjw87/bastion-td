package com.brandonwallace.bastiontd.game

import android.content.SharedPreferences
import com.brandonwallace.bastiontd.audio.Sfx
import com.brandonwallace.bastiontd.audio.SoundEngine
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class TowerInstance(
    val padIndex: Int,
    var type: TowerType,
    var level: Int = 1,
    var targetMode: TargetMode = TargetMode.FIRST,
    var totalInvested: Long = type.baseCost
) {
    var yawDeg: Float = 0f
    var pitchDeg: Float = -10f
    var barrelSpinDeg: Float = 0f
    var recoil: Float = 0f
    var cooldown: Float = 0f
    var lockedEnemyId: Int = -1
    var lockDuration: Float = 0f
    var beamVisibleTimer: Float = 0f
    var beamEndX: Float = 0f
    var beamEndY: Float = 0f
    var beamEndZ: Float = 0f
    var auraFireRateMul: Float = 1f
    var auraRangeMul: Float = 1f
    var auraBountyMul: Float = 1f
    var totalKills: Long = 0L
    var totalDamage: Double = 0.0
}

class EnemyInstance(
    val id: Int,
    val type: EnemyType,
    val waveSpawned: Int,
    var dist: Float,
    val laneOffset: Float,
    var maxHp: Float,
    var hp: Float,
    var maxShield: Float,
    var shield: Float
) {
    var x: Float = 0f
    var y: Float = if (type.isFlying) ArenaMap.FLYING_ALTITUDE else 0f
    var z: Float = 0f
    var yawDeg: Float = 0f
    var animPhase: Float = Random.nextFloat() * 10f
    var slowFactor: Float = 0f      // 0..0.75 speed reduction
    var slowTimer: Float = 0f
    var brittleTimer: Float = 0f
    var burnDps: Float = 0f
    var burnTimer: Float = 0f
    var hitFlash: Float = 0f
    var specialCooldown: Float = 2.2f
    var phaseSurgeTimer: Float = 0f
}

class ProjectileInstance(
    val type: TowerType,
    var x: Float,
    var y: Float,
    var z: Float,
    val startX: Float,
    val startY: Float,
    val startZ: Float,
    var targetX: Float,
    var targetY: Float,
    var targetZ: Float,
    var targetEnemyId: Int,
    val speed: Float,
    val damage: Float,
    val splashRadius: Float,
    val armorPen: Float,
    val shieldMult: Float,
    val isArc: Boolean,
    val sourcePadIndex: Int,
    val isCrit: Boolean
) {
    var progress: Float = 0f
    val totalHorizontalDist: Float = hypot(targetX - startX, targetZ - startZ).coerceAtLeast(0.5f)
}

data class BeamSegment(
    val x0: Float, val y0: Float, val z0: Float,
    val x1: Float, val y1: Float, val z1: Float,
    val r: Float, val g: Float, val b: Float,
    val width: Float,
    var ttl: Float,
    val maxTtl: Float
)

data class ShockwaveEffect(
    val x: Float, val y: Float, val z: Float,
    val maxRadius: Float,
    val r: Float, val g: Float, val b: Float,
    var age: Float,
    val duration: Float
)

data class ParticleEvent(
    val x: Float, val y: Float, val z: Float,
    val r: Float, val g: Float, val b: Float,
    val count: Int,
    val speed: Float,
    val size: Float
)

data class RenderTower(
    val padIndex: Int,
    val type: TowerType,
    val level: Int,
    val visualTier: Int,
    val x: Float, val y: Float, val z: Float,
    val yawDeg: Float,
    val pitchDeg: Float,
    val barrelSpinDeg: Float,
    val recoil: Float,
    val range: Float,
    val hasAuraBoost: Boolean,
    val beamActive: Boolean,
    val beamEndX: Float, val beamEndY: Float, val beamEndZ: Float,
    val lockIntensity: Float
)

data class RenderEnemy(
    val id: Int,
    val type: EnemyType,
    val x: Float, val y: Float, val z: Float,
    val yawDeg: Float,
    val scale: Float,
    val hpFraction: Float,
    val shieldFraction: Float,
    val animPhase: Float,
    val isChilled: Boolean,
    val isBurning: Boolean,
    val isPhaseSurging: Boolean,
    val hitFlash: Float
)

data class RenderProjectile(
    val type: TowerType,
    val x: Float, val y: Float, val z: Float,
    val yawDeg: Float,
    val pitchDeg: Float,
    val isCrit: Boolean
)

class RenderSnapshot(
    val wave: Int,
    val biome: Biome,
    val simTime: Float,
    val coreShieldFraction: Float,
    val overclockActive: Boolean,
    val selectedPadIndex: Int,
    val cameraYawDeg: Float,
    val cameraPitchDeg: Float,
    val cameraZoom: Float,
    val cameraPanX: Float,
    val cameraPanZ: Float,
    val towers: List<RenderTower>,
    val enemies: List<RenderEnemy>,
    val projectiles: List<RenderProjectile>,
    val beams: List<BeamSegment>,
    val shockwaves: List<ShockwaveEffect>,
    val particleEvents: List<ParticleEvent>
)

data class PadUiState(
    val padIndex: Int,
    val ringName: String,
    val towerType: TowerType?,
    val level: Int,
    val tierLabel: String,
    val targetMode: TargetMode,
    val damage: Float,
    val fireRate: Float,
    val range: Float,
    val upgradeCost: Long,
    val sellRefund: Long,
    val totalKills: Long,
    val totalDamage: Long,
    val auraBoosted: Boolean
)

data class UiSnapshot(
    val credits: Long,
    val shards: Int,
    val wave: Int,
    val highestWave: Int,
    val biome: Biome,
    val activeEnemyCount: Int,
    val remainingToSpawn: Int,
    val coreShield: Int,
    val maxCoreShield: Int,
    val totalKills: Long,
    val bossKills: Int,
    val speedMode: Int,
    val autoBuild: Boolean,
    val autoUpgrade: Boolean,
    val autoStrike: Boolean,
    val autoCamera: Boolean,
    val soundEnabled: Boolean,
    val qualityIndex: Int,
    val batterySaver: Boolean,
    val selectedPadIndex: Int,
    val selectedPad: PadUiState?,
    val orbitalCooldown: Float,
    val orbitalMaxCooldown: Float,
    val cryoCooldown: Float,
    val cryoMaxCooldown: Float,
    val overclockCooldown: Float,
    val overclockMaxCooldown: Float,
    val overclockActiveRemaining: Float,
    val bannerTitle: String?,
    val bannerSubtitle: String?,
    val offlineReport: String?,
    val techLevels: Map<String, Int>,
    val unlockedMilestones: Set<String>,
    val totalDpsEstimate: Long
)

class GameEngine(private val sound: SoundEngine? = null) {
    private val lock = Any()

    // Core progression & economy
    var credits: Long = 480L
        private set
    var shards: Int = 3
        private set
    var interestCarryScaled: Int = 0
        private set
    var wave: Int = 1
        private set
    var highestWave: Int = 1
        private set
    var totalKills: Long = 0L
        private set
    var bossKills: Int = 0
        private set

    // Core Shield
    var coreShield: Float = 1000f
        private set
    val maxCoreShield: Float
        get() = 1000f + techLevel("t5_core_capacity") * 250f

    // Automation & settings
    var speedMode: Int = 1 // 1, 2, or 4
        private set
    var autoBuild: Boolean = true
        private set
    var autoUpgrade: Boolean = true
        private set
    var autoStrike: Boolean = true
        private set
    var autoCamera: Boolean = true
        private set
    var soundEnabled: Boolean = true
        private set
    var qualityIndex: Int = 1 // 0 Low, 1 Med, 2 High
        private set
    var batterySaver: Boolean = false
        private set
    var biomeOverride: Int = -1 // -1 = automatic per 10 waves

    // Camera state (3D orbit + zoom + pan)
    var cameraYawDeg: Float = 0f
        private set
    var cameraPitchDeg: Float = 54f
        private set
    var cameraZoom: Float = 1.0f
        private set
    var cameraPanX: Float = 0f
        private set
    var cameraPanZ: Float = 0f
        private set
    var selectedPadIndex: Int = 2
        private set

    // Active entities
    private val towers = arrayOfNulls<TowerInstance>(ArenaMap.PADS.size)
    private val enemies = ArrayList<EnemyInstance>()
    private val projectiles = ArrayList<ProjectileInstance>()
    private val beams = ArrayList<BeamSegment>()
    private val shockwaves = ArrayList<ShockwaveEffect>()
    private val pendingParticles = ArrayList<ParticleEvent>()

    // Tech & Milestones
    private val techLevels = HashMap<String, Int>()
    private val unlockedMilestones = HashSet<String>()

    // Wave spawner state
    private val spawnQueue = ArrayDeque<EnemyType>()
    private var spawnTimer: Float = 0.2f
    private var spawnInterval: Float = 0.85f
    private var waveIntermissionTimer: Float = 0f
    private var nextEnemyId: Int = 1
    private var simTime: Float = 0f
    private var aiDecisionTimer: Float = 0.4f

    // Commander Abilities
    private var orbitalCooldown: Float = 0f
    private var cryoCooldown: Float = 0f
    private var overclockCooldown: Float = 0f
    private var overclockActiveTimer: Float = 0f

    // Banner notifications & offline summary
    private data class BannerMsg(val title: String, val subtitle: String, var ttl: Float)
    private val bannerQueue = ArrayDeque<BannerMsg>()
    private var activeBanner: BannerMsg? = null
    private var offlineReportText: String? = null

    private val rnd = Random(System.nanoTime())
    private val sampleTmp = FloatArray(3)

    init {
        setupStarterFortress()
        prepareWaveQueue(1, announce = true)
    }

    private fun setupStarterFortress() {
        // Deploy two starter turrets so the 3D battle is immediately alive on first launch
        towers[2] = TowerInstance(padIndex = 2, type = TowerType.VULCAN, level = 1)
        towers[3] = TowerInstance(padIndex = 3, type = TowerType.CRYO, level = 1)
        unlockedMilestones.add("m_first_tower")
        recalcBeaconAuras()
    }

    fun currentBiome(): Biome = synchronized(lock) {
        if (biomeOverride in Biome.entries.indices) Biome.entries[biomeOverride]
        else Biome.forWave(wave)
    }

    fun targetFps(): Int = synchronized(lock) {
        if (batterySaver) 30 else 60
    }

    fun techLevel(id: String): Int = techLevels[id] ?: 0

    fun totalTechPurchases(): Int = techLevels.values.sum()

    fun costDiscountMultiplier(): Float {
        val lvl = techLevel("t1_pad_fabricator")
        return (1f - lvl * 0.04f).coerceAtLeast(0.55f)
    }

    fun buildCostFor(type: TowerType): Long = synchronized(lock) {
        (type.baseCost * costDiscountMultiplier()).roundToLong().coerceAtLeast(25L)
    }

    fun upgradeCostFor(tower: TowerInstance): Long {
        return tower.type.upgradeCost(tower.level, costDiscountMultiplier())
    }

    fun sellRefundFor(tower: TowerInstance): Long {
        return (tower.totalInvested * 0.75).roundToLong().coerceAtLeast(15L)
    }

    // --- Effective Tower Stats with Tech & Aura Multipliers ---
    fun effectiveDamage(tower: TowerInstance): Float {
        val base = tower.type.damageAt(tower.level)
        var mul = 1f + techLevel("t6_omega_damage") * 0.22f + techLevel("t6_apex_sovereign") * 0.12f
        if (autoBuild || autoUpgrade) {
            mul += techLevel("t4_auto_overclock") * 0.08f
        }
        if (tower.type.visualTier(tower.level) >= 1) {
            mul += techLevel("t6_mk_evolution") * 0.18f
        }
        if (overclockActiveTimer > 0f) {
            mul *= 1.35f
        }
        when (tower.type) {
            TowerType.VULCAN -> mul *= (1f + techLevel("t1_kinetic_amp") * 0.14f)
            TowerType.MISSILE -> mul *= (1f + techLevel("t1_kinetic_amp") * 0.14f + techLevel("t3_sam_warheads") * 0.16f)
            TowerType.TESLA -> mul *= (1f + techLevel("t2_tesla_voltage") * 0.16f)
            TowerType.RAILGUN -> mul *= (1f + techLevel("t3_rail_capacitors") * 0.18f)
            TowerType.MORTAR -> mul *= (1f + techLevel("t3_thermite_compound") * 0.15f)
            else -> {}
        }
        return base * mul
    }

    fun effectiveFireRate(tower: TowerInstance): Float {
        val base = tower.type.fireRateAt(tower.level)
        var mul = (1f + techLevel("t1_servo_gyros") * 0.06f + techLevel("t6_apex_sovereign") * 0.12f) * tower.auraFireRateMul
        if (tower.type == TowerType.VULCAN) {
            mul *= (1f + techLevel("t1_gatling_spin") * 0.10f)
        }
        if (overclockActiveTimer > 0f) {
            val ocBoost = 2.0f + techLevel("t4_overclock_surge") * 0.10f
            mul *= ocBoost
        }
        return base * mul
    }

    fun effectiveRange(tower: TowerInstance): Float {
        val base = tower.type.rangeAt(tower.level)
        val mul = (1f + techLevel("t1_radar_uplink") * 0.05f) * tower.auraRangeMul
        return base * mul
    }

    private fun recalcBeaconAuras() {
        for (t in towers) {
            if (t != null) {
                t.auraFireRateMul = 1f
                t.auraRangeMul = 1f
                t.auraBountyMul = 1f
            }
        }
        val beaconTechMul = 1f + techLevel("t4_beacon_harmonics") * 0.12f
        for (b in towers) {
            if (b == null || b.type != TowerType.BEACON) continue
            val bPad = ArenaMap.PADS[b.padIndex]
            val bRange = b.type.rangeAt(b.level) * (1f + techLevel("t1_radar_uplink") * 0.05f)
            val rateBonus = (0.22f + (b.level - 1) * 0.025f).coerceAtMost(0.75f) * beaconTechMul
            val rangeBonus = (0.14f + (b.level - 1) * 0.012f).coerceAtMost(0.40f) * beaconTechMul
            val bountyBonus = (0.20f + (b.level - 1) * 0.025f).coerceAtMost(0.80f) * beaconTechMul

            for (other in towers) {
                if (other == null || other.padIndex == b.padIndex) continue
                val oPad = ArenaMap.PADS[other.padIndex]
                val dist = hypot(oPad.x - bPad.x, oPad.z - bPad.z)
                if (dist <= bRange) {
                    other.auraFireRateMul = max(other.auraFireRateMul, 1f + rateBonus)
                    other.auraRangeMul = max(other.auraRangeMul, 1f + rangeBonus)
                    other.auraBountyMul = max(other.auraBountyMul, 1f + bountyBonus)
                }
            }
        }
    }

    // --- Wave Generation & Streaming ---
    private fun prepareWaveQueue(targetWave: Int, announce: Boolean) {
        val w = targetWave.coerceAtLeast(1)
        val count = (8 + (w % 10) * 2 + (w / 5) * 2).coerceAtMost(36)
        for (i in 0 until count) {
            val type = pickEnemyTypeForWave(w, i, count)
            spawnQueue.addLast(type)
        }
        // Every 5th wave has a mini-boss or full Sector Dreadnought Boss
        if (w % 5 == 0) {
            spawnQueue.addLast(EnemyType.DREADNOUGHT)
            sound?.play(Sfx.BOSS_SPAWN)
            if (announce) {
                val biome = Biome.forWave(w)
                pushBanner("WAVE $w · DREADNOUGHT ASSAULT", "${biome.title} Boss Incoming!", 3.2f)
            }
        } else if (announce) {
            val biome = Biome.forWave(w)
            if (w % 10 == 1 && w > 1) {
                pushBanner("SECTOR SHIFT · ${biome.title.uppercase()}", "Wave $w · ${biome.subtitle}", 3.2f)
            } else {
                pushBanner("WAVE $w INCOMING", "${spawnQueue.size} Hostiles Portaling In", 2.0f)
            }
        }
        spawnInterval = (0.88f - (w * 0.008f)).coerceIn(0.28f, 0.88f)
    }

    private fun pickEnemyTypeForWave(w: Int, idx: Int, total: Int): EnemyType {
        if (w == 1) return if (idx % 3 == 2) EnemyType.SWARM else EnemyType.STRIDER
        if (w == 2) return when (idx % 4) {
            0, 1 -> EnemyType.STRIDER
            2 -> EnemyType.SWARM
            else -> EnemyType.GOLIATH
        }
        val pool = ArrayList<EnemyType>()
        pool.add(EnemyType.STRIDER)
        pool.add(EnemyType.SWARM)
        pool.add(EnemyType.GOLIATH)
        if (w >= 3) pool.add(EnemyType.SHIELD_BEARER)
        if (w >= 4) pool.add(EnemyType.VALKYRIE)
        if (w >= 6) pool.add(EnemyType.SPLITTER)
        if (w >= 7) pool.add(EnemyType.MENDER)
        if (w >= 8) pool.add(EnemyType.PHANTOM)
        return pool[(idx * 7 + w * 3) % pool.size]
    }

    private fun pushBanner(title: String, subtitle: String, duration: Float = 2.4f) {
        if (bannerQueue.size < 8) {
            bannerQueue.addLast(BannerMsg(title, subtitle, duration))
        }
    }

    // --- Main Simulation Step ---
    fun step(realDt: Float) = synchronized(lock) {
        val clampedRealDt = realDt.coerceIn(0.001f, 0.1f)
        // Update banner queue in real time so UI notifications read smoothly at any speed
        updateBanners(clampedRealDt)

        val totalSimDt = clampedRealDt * speedMode
        // Sub-step physics at <= 25ms increments so 4x speed never skips collisions or targets
        val steps = max(1, (totalSimDt / 0.022f).toInt())
        val dt = totalSimDt / steps
        for (s in 0 until steps) {
            stepSimulation(dt)
        }

        // Auto-camera gentle orbital drift
        if (autoCamera) {
            cameraYawDeg = (cameraYawDeg + clampedRealDt * 4.2f) % 360f
        }

        // Update ambient audio intensity
        sound?.battleIntensity = (enemies.size / 14f).coerceIn(0.15f, 1.0f)
    }

    private fun updateBanners(realDt: Float) {
        val current = activeBanner
        if (current != null) {
            current.ttl -= realDt
            if (current.ttl <= 0f) {
                activeBanner = bannerQueue.removeFirstOrNull()
            }
        } else if (bannerQueue.isNotEmpty()) {
            activeBanner = bannerQueue.removeFirst()
        }
    }

    private fun stepSimulation(dt: Float) {
        simTime += dt

        // Cooldowns & active buffs
        if (orbitalCooldown > 0f) orbitalCooldown = max(0f, orbitalCooldown - dt)
        if (cryoCooldown > 0f) cryoCooldown = max(0f, cryoCooldown - dt)
        if (overclockCooldown > 0f) overclockCooldown = max(0f, overclockCooldown - dt)
        if (overclockActiveTimer > 0f) overclockActiveTimer = max(0f, overclockActiveTimer - dt)

        // Core shield passive regeneration
        val regenPerSec = (8f + techLevel("t5_core_capacity") * 4f) * (1f + techLevel("t5_core_capacity") * 0.20f)
        coreShield = min(maxCoreShield, coreShield + regenPerSec * dt)

        // Spawn queue processing
        if (spawnQueue.isNotEmpty()) {
            spawnTimer -= dt
            if (spawnTimer <= 0f) {
                val nextType = spawnQueue.removeFirst()
                spawnEnemy(nextType, wave, dist = 0f)
                spawnTimer = if (nextType == EnemyType.SWARM) spawnInterval * 0.45f else spawnInterval
            }
        } else if (enemies.isEmpty()) {
            // Wave cleared! Award wave bonus + hidden 16.769% compound interest on unspent credits!
            waveIntermissionTimer -= dt
            if (waveIntermissionTimer <= 0f) {
                completeCurrentWaveAndStartNext()
            }
        }

        // Update enemies
        stepEnemies(dt)

        // Update towers & firing
        stepTowers(dt)

        // Update projectiles
        stepProjectiles(dt)

        // Update transient 3D beams & shockwaves
        val beamIt = beams.iterator()
        while (beamIt.hasNext()) {
            val b = beamIt.next()
            b.ttl -= dt
            if (b.ttl <= 0f) beamIt.remove()
        }
        val swIt = shockwaves.iterator()
        while (swIt.hasNext()) {
            val sw = swIt.next()
            sw.age += dt
            if (sw.age >= sw.duration) swIt.remove()
        }

        // AI Auto-Commander decisions
        aiDecisionTimer -= dt
        if (aiDecisionTimer <= 0f) {
            aiDecisionTimer = 0.35f
            runAiCommander()
        }
    }

    private fun spawnEnemy(type: EnemyType, forWave: Int, dist: Float) {
        val baseHp = Economy.baseEnemyHpForWave(forWave)
        val hp = baseHp * type.hpMult
        val shield = hp * type.shieldRatio
        val laneOffset = if (type.isBoss) 0f else (rnd.nextFloat() - 0.5f) * 0.72f
        val e = EnemyInstance(
            id = nextEnemyId++,
            type = type,
            waveSpawned = forWave,
            dist = dist,
            laneOffset = laneOffset,
            maxHp = hp,
            hp = hp,
            maxShield = shield,
            shield = shield
        )
        updateEnemyWorldPos(e)
        enemies.add(e)
    }

    private fun updateEnemyWorldPos(e: EnemyInstance) {
        ArenaMap.sampleAtDistance(e.dist, sampleTmp)
        val rad = Math.toRadians(sampleTmp[2].toDouble())
        val perpX = cos(rad).toFloat()
        val perpZ = -sin(rad).toFloat()
        e.x = sampleTmp[0] + perpX * e.laneOffset
        e.z = sampleTmp[1] + perpZ * e.laneOffset
        e.yawDeg = sampleTmp[2]
        e.y = if (e.type.isFlying) {
            ArenaMap.FLYING_ALTITUDE + sin(simTime * 3.2f + e.id) * 0.14f
        } else {
            0f
        }
    }

    private fun completeCurrentWaveAndStartNext() {
        val clearedWave = wave
        val dividendMul = 1.0 + techLevel("t5_wave_dividend") * 0.15 + techLevel("t6_apex_sovereign") * 0.12
        val baseWaveBonus = (Economy.baseWaveClearBonus(clearedWave) * dividendMul).roundToLong()

        // Hidden 16.769% compound interest on unspent credits folded silently into performance bonus
        val interest = Economy.computeWaveInterest(credits, interestCarryScaled)
        interestCarryScaled = interest.newCarryScaled
        val totalWavePayout = baseWaveBonus + interest.wholePaid
        credits = Economy.addCreditsSafe(credits, totalWavePayout)

        // Award Quantum Shards every 5 waves + extra from shard extractor tech
        if (clearedWave % 5 == 0) {
            val baseShards = if (clearedWave % 10 == 0) 3 else 1
            val shardMul = 1f + techLevel("t5_shard_extractor") * 0.20f
            val earnedShards = (baseShards * shardMul).roundToLong().toInt().coerceAtLeast(1)
            shards += earnedShards
        }

        sound?.play(Sfx.WAVE_CLEAR)
        wave = clearedWave + 1
        if (wave > highestWave) highestWave = wave
        checkMilestones()

        prepareWaveQueue(wave, announce = true)
        waveIntermissionTimer = 0.35f
    }

    private fun stepEnemies(dt: Float) {
        val coreHitList = ArrayList<EnemyInstance>()
        val spawnedSubEnemies = ArrayList<Pair<EnemyType, Float>>()

        for (i in enemies.indices) {
            val e = enemies[i]
            e.animPhase += dt * (2.5f + e.type.speed)
            if (e.hitFlash > 0f) e.hitFlash = max(0f, e.hitFlash - dt * 4.5f)

            // Burn DOT
            if (e.burnTimer > 0f) {
                e.burnTimer -= dt
                e.hp -= e.burnDps * dt
            }

            // Slow & Brittle timers
            if (e.slowTimer > 0f) {
                e.slowTimer -= dt
                if (e.slowTimer <= 0f) e.slowFactor = 0f
            }
            if (e.brittleTimer > 0f) {
                e.brittleTimer -= dt
            }

            // Shield regeneration for Shield-Bearer and Boss
            if (e.maxShield > 0f && e.shield < e.maxShield) {
                e.shield = min(e.maxShield, e.shield + e.maxShield * 0.035f * dt)
            }

            // Special enemy behaviors
            e.specialCooldown -= dt
            if (e.specialCooldown <= 0f) {
                when (e.type) {
                    EnemyType.MENDER -> {
                        e.specialCooldown = 2.8f
                        // Heal nearby allies within 3.2 units
                        for (ally in enemies) {
                            if (hypot(ally.x - e.x, ally.z - e.z) <= 3.2f) {
                                ally.hp = min(ally.maxHp, ally.hp + ally.maxHp * 0.10f)
                            }
                        }
                        shockwaves.add(ShockwaveEffect(e.x, 0.15f, e.z, 2.8f, 0.18f, 0.95f, 0.48f, 0f, 0.45f))
                    }
                    EnemyType.PHANTOM -> {
                        e.specialCooldown = 4.2f
                        e.phaseSurgeTimer = 1.35f
                    }
                    EnemyType.DREADNOUGHT -> {
                        e.specialCooldown = 5.0f
                        spawnedSubEnemies.add(EnemyType.SWARM to (e.dist + 0.8f).coerceAtMost(ArenaMap.TOTAL_LENGTH - 1f))
                        spawnedSubEnemies.add(EnemyType.SWARM to (e.dist - 0.8f).coerceAtLeast(0f))
                    }
                    else -> e.specialCooldown = 99f
                }
            }
            if (e.phaseSurgeTimer > 0f) e.phaseSurgeTimer = max(0f, e.phaseSurgeTimer - dt)

            // Movement along path
            val speedMul = (1f - e.slowFactor.coerceIn(0f, 0.72f)) * (if (e.phaseSurgeTimer > 0f) 1.65f else 1.0f)
            e.dist += e.type.speed * speedMul * dt
            updateEnemyWorldPos(e)

            if (e.dist >= ArenaMap.TOTAL_LENGTH) {
                coreHitList.add(e)
            }
        }

        // Spawn any boss escort units
        for ((subType, subDist) in spawnedSubEnemies) {
            if (enemies.size < 60) {
                spawnEnemy(subType, wave, subDist)
            }
        }

        // Handle enemies killed by burn DOT without mutating enemies while iterating
        val burnDead = enemies.filter { it.hp <= 0f }
        for (e in burnDead) {
            enemies.remove(e)
            onEnemyDestroyed(e, killerTower = null)
        }

        // Handle enemies reaching Quantum Core
        if (coreHitList.isNotEmpty()) {
            for (breacher in coreHitList) {
                enemies.remove(breacher)
                val coreDmg = if (breacher.type.isBoss) 450f else 120f
                coreShield -= coreDmg
                pendingParticles.add(ParticleEvent(ArenaMap.CORE_POS.x, 0.8f, ArenaMap.CORE_POS.z, 1.0f, 0.35f, 0.15f, 18, 4.5f, 0.18f))
            }
            if (coreShield <= 0f) {
                triggerQuantumCoreOverload()
            }
        }
    }

    /**
     * Never-stop continuity guarantee: when the Core Shield is depleted, the Quantum Core fires an
     * Overload Shockwave that clears the field, restores full shields, and keeps the endless battle going!
     */
    private fun triggerQuantumCoreOverload() {
        coreShield = maxCoreShield
        sound?.play(Sfx.CORE_SHOCKWAVE)
        val cx = ArenaMap.CORE_POS.x
        val cz = ArenaMap.CORE_POS.z
        shockwaves.add(ShockwaveEffect(cx, 0.3f, cz, 26.0f, 0.0f, 0.92f, 1.0f, 0f, 1.1f))
        pendingParticles.add(ParticleEvent(cx, 1.2f, cz, 0.0f, 0.95f, 1.0f, 45, 8.0f, 0.25f))

        val novaBonusMul = 1f + techLevel("t5_core_nova") * 0.20f
        var salvageCredits = 0L
        for (e in enemies) {
            val reward = (Economy.baseKillRewardForWave(e.waveSpawned) * e.type.rewardMult * 0.5f * novaBonusMul).roundToLong()
            salvageCredits += reward
            pendingParticles.add(ParticleEvent(e.x, e.y + 0.4f, e.z, e.type.colorR, e.type.colorG, e.type.colorB, 10, 3.8f, 0.15f))
        }
        enemies.clear()
        credits = Economy.addCreditsSafe(credits, salvageCredits.coerceAtLeast(50L))
        pushBanner("QUANTUM CORE OVERLOAD!", "EMP Shockwave Cleared Field · Shields Restored 100%", 3.0f)
    }

    private fun stepTowers(dt: Float) {
        for (tower in towers) {
            if (tower == null) continue
            val pad = ArenaMap.PADS[tower.padIndex]
            if (tower.recoil > 0f) tower.recoil = max(0f, tower.recoil - dt * 4.5f)
            if (tower.beamVisibleTimer > 0f) tower.beamVisibleTimer = max(0f, tower.beamVisibleTimer - dt)

            // Continuous rotation for Vulcan barrels and Beacon rings
            if (tower.type == TowerType.BEACON) {
                tower.yawDeg = (tower.yawDeg + dt * 48f) % 360f
                tower.barrelSpinDeg = (tower.barrelSpinDeg + dt * 95f) % 360f
            }

            val range = effectiveRange(tower)
            val target = findTargetForTower(tower, pad.x, pad.z, range)

            if (target != null) {
                val dx = target.x - pad.x
                val dy = (target.y + 0.35f) - (pad.y + 0.65f)
                val dz = target.z - pad.z
                val horiz = hypot(dx, dz).coerceAtLeast(1e-3f)
                val desiredYaw = Math.toDegrees(atan2(dx.toDouble(), dz.toDouble())).toFloat()
                val desiredPitch = Math.toDegrees(atan2(dy.toDouble(), horiz.toDouble())).toFloat().coerceIn(-45f, 45f)

                if (tower.type != TowerType.BEACON) {
                    tower.yawDeg = approachAngle(tower.yawDeg, desiredYaw, dt * 320f)
                    tower.pitchDeg = tower.pitchDeg + (desiredPitch - tower.pitchDeg) * min(1f, dt * 12f)
                }

                if (tower.lockedEnemyId == target.id) {
                    tower.lockDuration += dt
                } else {
                    tower.lockedEnemyId = target.id
                    tower.lockDuration = 0f
                }

                if (tower.type == TowerType.VULCAN || tower.type == TowerType.PRISM) {
                    tower.barrelSpinDeg = (tower.barrelSpinDeg + dt * 540f) % 360f
                }

                tower.cooldown -= dt
                if (tower.cooldown <= 0f) {
                    val rate = effectiveFireRate(tower).coerceAtLeast(0.1f)
                    tower.cooldown = 1f / rate
                    fireTowerAt(tower, pad, target, range)
                }
            } else {
                tower.lockedEnemyId = -1
                tower.lockDuration = 0f
                if (tower.cooldown > 0f) tower.cooldown = max(0f, tower.cooldown - dt)
            }
        }
    }

    private fun findTargetForTower(tower: TowerInstance, px: Float, pz: Float, range: Float): EnemyInstance? {
        val r2 = range * range
        var best: EnemyInstance? = null
        var bestScore = Float.NEGATIVE_INFINITY

        // Keep locked target for Prism Laser if still alive and in range
        if (tower.type == TowerType.PRISM && tower.lockedEnemyId != -1) {
            val locked = enemies.firstOrNull { it.id == tower.lockedEnemyId && it.hp > 0f }
            if (locked != null) {
                val dx = locked.x - px
                val dz = locked.z - pz
                if (dx * dx + dz * dz <= r2) return locked
            }
        }

        for (e in enemies) {
            if (e.hp <= 0f) continue
            if (e.type.isFlying && !tower.type.hitsAir) continue
            val dx = e.x - px
            val dz = e.z - pz
            val d2 = dx * dx + dz * dz
            if (d2 > r2) continue

            val score = when (tower.targetMode) {
                TargetMode.FIRST -> e.dist
                TargetMode.STRONGEST -> e.hp + e.shield
                TargetMode.WEAKEST -> -(e.hp + e.shield)
                TargetMode.FASTEST -> e.type.speed
                TargetMode.CLOSEST -> -d2
            } + (if (e.type.isBoss) 5f else 0f)

            if (score > bestScore) {
                bestScore = score
                best = e
            }
        }
        return best
    }

    private fun fireTowerAt(tower: TowerInstance, pad: PadSpec, target: EnemyInstance, range: Float) {
        tower.recoil = 1.0f
        val baseDmg = effectiveDamage(tower)
        val critChance = (0.05f + techLevel("t1_crit_optics") * 0.04f).coerceAtMost(0.65f)
        val isCrit = rnd.nextFloat() < critChance
        val critMult = if (isCrit) (2.2f + techLevel("t6_crit_devastation") * 0.35f) else 1.0f
        val finalDmg = baseDmg * critMult
        val armorPen = (tower.type.armorPen + techLevel("t3_armor_pierce") * 0.05f +
            (if (tower.type == TowerType.VULCAN) techLevel("t1_gatling_spin") * 0.04f else 0f)).coerceAtMost(0.95f)
        val shieldMult = tower.type.shieldMult * (1f + techLevel("t2_emp_overload") * 0.15f)
        val splashMul = 1f + techLevel("t3_shockwave_radius") * 0.07f

        val muzzleX = pad.x + sin(Math.toRadians(tower.yawDeg.toDouble())).toFloat() * 0.55f
        val muzzleY = pad.y + 0.72f
        val muzzleZ = pad.z + cos(Math.toRadians(tower.yawDeg.toDouble())).toFloat() * 0.55f

        when (tower.type) {
            TowerType.VULCAN -> {
                sound?.play(Sfx.VULCAN_FIRE)
                projectiles.add(
                    ProjectileInstance(
                        type = TowerType.VULCAN,
                        x = muzzleX, y = muzzleY, z = muzzleZ,
                        startX = muzzleX, startY = muzzleY, startZ = muzzleZ,
                        targetX = target.x, targetY = target.y + 0.35f, targetZ = target.z,
                        targetEnemyId = target.id,
                        speed = 24f,
                        damage = finalDmg,
                        splashRadius = 0f,
                        armorPen = armorPen,
                        shieldMult = shieldMult,
                        isArc = false,
                        sourcePadIndex = tower.padIndex,
                        isCrit = isCrit
                    )
                )
            }
            TowerType.CRYO -> {
                sound?.play(Sfx.CRYO_PULSE)
                tower.beamVisibleTimer = 0.22f
                tower.beamEndX = target.x
                tower.beamEndY = target.y + 0.35f
                tower.beamEndZ = target.z
                val splashR = tower.type.splashRadius * splashMul
                val slowPotency = (0.38f + tower.level * 0.015f) * (1f + techLevel("t2_cryo_deep") * 0.12f)
                val slowDur = 2.6f * (1f + techLevel("t2_cryo_deep") * 0.12f)
                for (e in ArrayList(enemies)) {
                    if (hypot(e.x - target.x, e.z - target.z) <= splashR) {
                        e.slowFactor = max(e.slowFactor, slowPotency.coerceAtMost(0.72f))
                        e.slowTimer = max(e.slowTimer, slowDur)
                        e.brittleTimer = max(e.brittleTimer, slowDur)
                        applyDamageToEnemy(e, finalDmg, armorPen, shieldMult, tower)
                    }
                }
                shockwaves.add(ShockwaveEffect(target.x, target.y + 0.1f, target.z, splashR, 0.0f, 0.88f, 1.0f, 0f, 0.28f))
            }
            TowerType.MORTAR -> {
                projectiles.add(
                    ProjectileInstance(
                        type = TowerType.MORTAR,
                        x = muzzleX, y = muzzleY + 0.2f, z = muzzleZ,
                        startX = muzzleX, startY = muzzleY + 0.2f, startZ = muzzleZ,
                        targetX = target.x, targetY = 0.1f, targetZ = target.z,
                        targetEnemyId = -1,
                        speed = 9.5f,
                        damage = finalDmg,
                        splashRadius = tower.type.splashRadius * splashMul,
                        armorPen = armorPen,
                        shieldMult = shieldMult,
                        isArc = true,
                        sourcePadIndex = tower.padIndex,
                        isCrit = isCrit
                    )
                )
            }
            TowerType.TESLA -> {
                sound?.play(Sfx.TESLA_ZAP)
                val maxChains = tower.type.multiCountAt(tower.level) + techLevel("t2_tesla_chains") / 2
                val chainReach = 3.6f * (1f + techLevel("t2_tesla_chains") * 0.10f)
                val hitIds = HashSet<Int>()
                var currX = muzzleX
                var currY = muzzleY + 0.35f
                var currZ = muzzleZ
                var nextTarget: EnemyInstance? = target
                var chainDmg = finalDmg

                for (c in 0 until maxChains) {
                    val t = nextTarget ?: break
                    hitIds.add(t.id)
                    addJaggedLightning(currX, currY, currZ, t.x, t.y + 0.4f, t.z)
                    applyDamageToEnemy(t, chainDmg, armorPen, shieldMult, tower)
                    currX = t.x
                    currY = t.y + 0.4f
                    currZ = t.z
                    chainDmg *= 0.82f
                    nextTarget = enemies
                        .filter { it.hp > 0f && it.id !in hitIds && hypot(it.x - currX, it.z - currZ) <= chainReach }
                        .minByOrNull { hypot(it.x - currX, it.z - currZ) }
                }
            }
            TowerType.RAILGUN -> {
                sound?.play(Sfx.RAILGUN_FIRE)
                // Piercing line shot from muzzle through target out to full range
                val dx = target.x - muzzleX
                val dz = target.z - muzzleZ
                val len = hypot(dx, dz).coerceAtLeast(1e-3f)
                val ux = dx / len
                val uz = dz / len
                val endX = muzzleX + ux * range
                val endZ = muzzleZ + uz * range
                val endY = target.y + 0.35f
                beams.add(BeamSegment(muzzleX, muzzleY, muzzleZ, endX, endY, endZ, 0.1f, 0.98f, 1.0f, 0.16f, 0.24f, 0.24f))
                val pierceWidth = 0.78f * (1f + techLevel("t3_rail_capacitors") * 0.08f)
                for (e in ArrayList(enemies)) {
                    if (e.hp <= 0f) continue
                    val px = e.x - muzzleX
                    val pz = e.z - muzzleZ
                    val proj = px * ux + pz * uz
                    if (proj in 0f..(range + 0.5f)) {
                        val perp = abs(px * uz - pz * ux)
                        if (perp <= pierceWidth) {
                            applyDamageToEnemy(e, finalDmg, armorPen, shieldMult, tower)
                            pendingParticles.add(ParticleEvent(e.x, e.y + 0.4f, e.z, 0.1f, 0.95f, 1.0f, 6, 3.2f, 0.12f))
                        }
                    }
                }
            }
            TowerType.PRISM -> {
                val maxRamp = 3.5f + techLevel("t3_prism_lenses") * 0.25f
                val rampRate = 0.65f * (1f + techLevel("t3_prism_lenses") * 0.20f)
                val rampMul = (1f + tower.lockDuration * rampRate).coerceAtMost(maxRamp)
                tower.beamVisibleTimer = 0.26f
                tower.beamEndX = target.x
                tower.beamEndY = target.y + 0.38f
                tower.beamEndZ = target.z
                applyDamageToEnemy(target, finalDmg * rampMul, armorPen, shieldMult, tower)
            }
            TowerType.MISSILE -> {
                sound?.play(Sfx.MISSILE_LAUNCH)
                val salvoCount = tower.type.multiCountAt(tower.level)
                val candidates = enemies.filter { it.hp > 0f && hypot(it.x - pad.x, it.z - pad.z) <= range }
                if (candidates.isNotEmpty()) {
                    for (m in 0 until salvoCount) {
                        val mTarget = candidates[m % candidates.size]
                        val spreadAngle = (m - (salvoCount - 1) * 0.5f) * 0.28f
                        val ox = muzzleX + cos(spreadAngle) * 0.25f
                        val oz = muzzleZ + sin(spreadAngle) * 0.25f
                        projectiles.add(
                            ProjectileInstance(
                                type = TowerType.MISSILE,
                                x = ox, y = muzzleY + 0.25f, z = oz,
                                startX = ox, startY = muzzleY + 0.25f, startZ = oz,
                                targetX = mTarget.x, targetY = mTarget.y + 0.35f, targetZ = mTarget.z,
                                targetEnemyId = mTarget.id,
                                speed = 15.5f,
                                damage = finalDmg,
                                splashRadius = tower.type.splashRadius * splashMul,
                                armorPen = armorPen,
                                shieldMult = shieldMult,
                                isArc = true,
                                sourcePadIndex = tower.padIndex,
                                isCrit = isCrit
                            )
                        )
                    }
                }
            }
            TowerType.BEACON -> {
                // Emits harmonic pulse shockwave damaging all enemies in range
                shockwaves.add(ShockwaveEffect(pad.x, pad.y, pad.z, range, 1.0f, 0.84f, 0.25f, 0f, 0.45f))
                for (e in ArrayList(enemies)) {
                    if (e.hp > 0f && hypot(e.x - pad.x, e.z - pad.z) <= range) {
                        applyDamageToEnemy(e, finalDmg, armorPen, shieldMult, tower)
                    }
                }
            }
        }
    }

    private fun addJaggedLightning(x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float) {
        val mx = (x0 + x1) * 0.5f + (rnd.nextFloat() - 0.5f) * 0.45f
        val my = (y0 + y1) * 0.5f + (rnd.nextFloat() - 0.3f) * 0.35f
        val mz = (z0 + z1) * 0.5f + (rnd.nextFloat() - 0.5f) * 0.45f
        beams.add(BeamSegment(x0, y0, z0, mx, my, mz, 0.58f, 0.38f, 1.0f, 0.09f, 0.16f, 0.16f))
        beams.add(BeamSegment(mx, my, mz, x1, y1, z1, 0.58f, 0.38f, 1.0f, 0.09f, 0.16f, 0.16f))
    }

    private fun stepProjectiles(dt: Float) {
        val it = projectiles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            // Homing update if targetEnemyId is still alive
            if (p.targetEnemyId != -1) {
                val target = enemies.firstOrNull { it.id == p.targetEnemyId && it.hp > 0f }
                if (target != null) {
                    p.targetX = target.x
                    p.targetY = target.y + 0.35f
                    p.targetZ = target.z
                }
            }

            if (p.isArc) {
                val stepProgress = (p.speed * dt) / p.totalHorizontalDist
                p.progress += stepProgress
                val t = p.progress.coerceIn(0f, 1f)
                p.x = p.startX + (p.targetX - p.startX) * t
                p.z = p.startZ + (p.targetZ - p.startZ) * t
                val arcPeak = if (p.type == TowerType.MORTAR) 3.4f else 1.6f
                val linearY = p.startY + (p.targetY - p.startY) * t
                p.y = linearY + 4f * arcPeak * t * (1f - t)
                if (p.progress >= 1f) {
                    detonateProjectile(p)
                    it.remove()
                }
            } else {
                val dx = p.targetX - p.x
                val dy = p.targetY - p.y
                val dz = p.targetZ - p.z
                val dist = sqrt(dx * dx + dy * dy + dz * dz)
                val move = p.speed * dt
                if (dist <= move + 0.25f) {
                    detonateProjectile(p)
                    it.remove()
                } else {
                    p.x += (dx / dist) * move
                    p.y += (dy / dist) * move
                    p.z += (dz / dist) * move
                }
            }
        }
    }

    private fun detonateProjectile(p: ProjectileInstance) {
        val sourceTower = towers.getOrNull(p.sourcePadIndex)
        if (p.splashRadius > 0.05f) {
            if (p.type == TowerType.MORTAR) sound?.play(Sfx.MORTAR_BLAST)
            val colR = if (p.type == TowerType.MORTAR) 1.0f else 0.4f
            val colG = if (p.type == TowerType.MORTAR) 0.42f else 0.95f
            val colB = if (p.type == TowerType.MORTAR) 0.12f else 0.65f
            shockwaves.add(ShockwaveEffect(p.targetX, p.targetY, p.targetZ, p.splashRadius, colR, colG, colB, 0f, 0.35f))
            pendingParticles.add(ParticleEvent(p.targetX, p.targetY + 0.2f, p.targetZ, colR, colG, colB, 14, 4.2f, 0.16f))

            for (e in ArrayList(enemies)) {
                if (e.hp <= 0f) continue
                if (e.type.isFlying && p.type == TowerType.MORTAR) continue
                val d = hypot(e.x - p.targetX, e.z - p.targetZ)
                if (d <= p.splashRadius) {
                    val falloff = (1f - (d / p.splashRadius) * 0.35f).coerceIn(0.5f, 1f)
                    applyDamageToEnemy(e, p.damage * falloff, p.armorPen, p.shieldMult, sourceTower)
                    if (p.type == TowerType.MORTAR) {
                        val burnBonus = 1f + techLevel("t3_thermite_compound") * 0.20f
                        e.burnDps = max(e.burnDps, p.damage * 0.28f * burnBonus)
                        e.burnTimer = max(e.burnTimer, 3.2f)
                    }
                }
            }
        } else {
            val target = enemies.firstOrNull { it.id == p.targetEnemyId && it.hp > 0f }
                ?: enemies.firstOrNull { it.hp > 0f && hypot(it.x - p.targetX, it.z - p.targetZ) <= 0.85f }
            if (target != null) {
                applyDamageToEnemy(target, p.damage, p.armorPen, p.shieldMult, sourceTower)
                pendingParticles.add(ParticleEvent(target.x, target.y + 0.35f, target.z, 1.0f, 0.82f, 0.25f, 5, 2.6f, 0.10f))
            }
        }
    }

    private fun applyDamageToEnemy(
        enemy: EnemyInstance,
        rawDamage: Float,
        armorPen: Float,
        shieldMult: Float,
        sourceTower: TowerInstance?
    ) {
        if (enemy.hp <= 0f) return
        var dmg = rawDamage

        // Brittle bonus from Cryo
        if (enemy.brittleTimer > 0f) {
            val brittleBonus = 0.35f + techLevel("t2_brittle_fracture") * 0.06f
            dmg *= (1f + brittleBonus)
        }
        // Anti-Boss & Anti-Air bonuses
        if (enemy.type.isBoss) {
            dmg *= (1f + techLevel("t4_boss_hunter") * 0.15f)
            if (sourceTower?.type == TowerType.MISSILE) {
                dmg *= (1.75f + techLevel("t3_sam_warheads") * 0.12f)
            }
        }
        if (enemy.type.isFlying) {
            dmg *= (1f + techLevel("t4_air_superiority") * 0.15f)
            if (sourceTower?.type == TowerType.MISSILE) {
                dmg *= (1.75f + techLevel("t3_sam_warheads") * 0.12f)
            }
        }
        // Executioner threshold
        val execThreshold = 0.15f + techLevel("t6_executioner") * 0.01f
        if (techLevel("t6_executioner") > 0 && (enemy.hp / enemy.maxHp) <= execThreshold) {
            dmg *= 3.0f
        }

        // First absorb via Aegis Shield if present
        if (enemy.shield > 0f) {
            val shieldDmg = dmg * shieldMult
            if (shieldDmg <= enemy.shield) {
                enemy.shield -= shieldDmg
                enemy.hitFlash = 1.0f
                sourceTower?.let { it.totalDamage += shieldDmg }
                return
            } else {
                val leftoverFraction = (shieldDmg - enemy.shield) / shieldDmg.coerceAtLeast(1e-3f)
                enemy.shield = 0f
                dmg *= leftoverFraction
            }
        }

        val effectiveArmor = (enemy.type.armor * (1f - armorPen.coerceIn(0f, 0.95f))).coerceIn(0f, 0.75f)
        val hpDmg = (dmg * (1f - effectiveArmor)).coerceAtLeast(1f)
        enemy.hp -= hpDmg
        enemy.hitFlash = 1.0f
        sourceTower?.let { it.totalDamage += hpDmg }

        if (enemy.hp <= 0f) {
            enemies.remove(enemy)
            onEnemyDestroyed(enemy, sourceTower)
        }
    }

    private fun onEnemyDestroyed(enemy: EnemyInstance, killerTower: TowerInstance?) {
        totalKills++
        killerTower?.let { it.totalKills++ }

        val bountyTechMul = 1.0 + techLevel("t5_kill_bounty") * 0.12 + techLevel("t6_apex_sovereign") * 0.12
        val auraMul = killerTower?.auraBountyMul?.toDouble() ?: 1.0
        val earnedCredits = (Economy.baseKillRewardForWave(enemy.waveSpawned) * enemy.type.rewardMult * bountyTechMul * auraMul)
            .roundToLong().coerceAtLeast(2L)
        credits = Economy.addCreditsSafe(credits, earnedCredits)

        pendingParticles.add(
            ParticleEvent(
                enemy.x, enemy.y + 0.45f, enemy.z,
                enemy.type.colorR, enemy.type.colorG, enemy.type.colorB,
                count = if (enemy.type.isBoss) 36 else 12,
                speed = if (enemy.type.isBoss) 6.5f else 3.6f,
                size = if (enemy.type.isBoss) 0.24f else 0.14f
            )
        )

        if (enemy.type == EnemyType.SPLITTER && enemies.size < 65) {
            for (k in -1..1) {
                spawnEnemy(EnemyType.MITE, enemy.waveSpawned, (enemy.dist + k * 0.45f).coerceIn(0f, ArenaMap.TOTAL_LENGTH - 0.5f))
            }
        }

        if (enemy.type.isBoss) {
            bossKills++
            val shardMul = 1f + techLevel("t5_shard_extractor") * 0.20f
            val bossShards = (2 * shardMul).roundToLong().toInt().coerceAtLeast(2)
            shards += bossShards
            sound?.play(Sfx.BOSS_KILL)
            shockwaves.add(ShockwaveEffect(enemy.x, 0.2f, enemy.z, 5.5f, 1.0f, 0.55f, 0.12f, 0f, 0.75f))
            pushBanner("DREADNOUGHT DESTROYED!", "+${Economy.formatCompact(earnedCredits)} CR · +$bossShards Quantum Shards", 2.8f)
        } else {
            sound?.play(Sfx.ENEMY_KILL)
        }

        checkMilestones()
    }

    // --- AI Auto-Commander (Hands-Free Non-Stop Progression) ---
    private fun runAiCommander() {
        // 1. Auto-Strike Commander Abilities when density or Boss demands it
        if (autoStrike && enemies.isNotEmpty()) {
            val hasBoss = enemies.any { it.type.isBoss }
            if (orbitalCooldown <= 0f && (enemies.size >= 5 || hasBoss)) {
                triggerOrbitalStrikeInternal()
            } else if (cryoCooldown <= 0f && (enemies.size >= 6 || hasBoss)) {
                triggerCryoNovaInternal()
            } else if (overclockCooldown <= 0f && (enemies.size >= 4 || hasBoss)) {
                triggerOverclockInternal()
            }
        }

        // 2. Auto-Build on empty pads using balanced combined-arms doctrine
        if (autoBuild) {
            val emptyPadIndex = BUILD_PRIORITY_PADS.firstOrNull { towers[it] == null }
            if (emptyPadIndex != null) {
                val desiredType = pickNextDoctrineTowerType(emptyPadIndex)
                val cost = buildCostFor(desiredType)
                if (credits >= cost) {
                    buildTowerInternal(emptyPadIndex, desiredType, playSfx = false)
                    return
                }
            }
        }

        // 3. Auto-Upgrade lowest-cost existing tower so all pads evolve smoothly to Mk II -> Mk III -> Omega
        if (autoUpgrade) {
            val candidate = towers.filterNotNull().minByOrNull { upgradeCostFor(it) }
            if (candidate != null) {
                val cost = upgradeCostFor(candidate)
                if (credits >= cost) {
                    upgradeTowerInternal(candidate.padIndex, playSfx = false)
                }
            }
        }
    }

    private fun pickNextDoctrineTowerType(padIndex: Int): TowerType {
        // Central pads (5, 9, 13, 17) are prime Chrono-Beacon amplifier hubs once at least 4 towers exist
        val activeTowers = towers.filterNotNull()
        if (padIndex in listOf(5, 9, 13, 17) && activeTowers.size >= 4 && activeTowers.count { it.type == TowerType.BEACON } < 2) {
            return TowerType.BEACON
        }
        val counts = TowerType.entries.associateWith { t -> activeTowers.count { it.type == t } }
        val order = listOf(
            TowerType.VULCAN,
            TowerType.CRYO,
            TowerType.TESLA,
            TowerType.MORTAR,
            TowerType.RAILGUN,
            TowerType.PRISM,
            TowerType.MISSILE,
            TowerType.BEACON
        )
        return order.minByOrNull { counts[it] ?: 0 } ?: TowerType.VULCAN
    }

    // --- Commander Abilities ---
    fun orbitalMaxCooldown(): Float = 22f * (1f - techLevel("t4_orbital_focused") * 0.06f) * (1f - techLevel("t6_eternal_fortress") * 0.05f)
    fun cryoMaxCooldown(): Float = 18f * (1f - techLevel("t2_frost_nova_cd") * 0.07f) * (1f - techLevel("t6_eternal_fortress") * 0.05f)
    fun overclockMaxCooldown(): Float = 20f * (1f - techLevel("t6_eternal_fortress") * 0.05f)

    fun triggerOrbitalStrike(): Boolean = synchronized(lock) {
        if (orbitalCooldown > 0f || enemies.isEmpty()) return false
        triggerOrbitalStrikeInternal()
        return true
    }

    private fun triggerOrbitalStrikeInternal() {
        val target = enemies.firstOrNull { it.type.isBoss } ?: enemies.maxByOrNull { it.dist } ?: return
        val tx = target.x
        val tz = target.z
        orbitalCooldown = orbitalMaxCooldown().coerceAtLeast(5f)
        sound?.play(Sfx.ABILITY_ORBITAL)

        // Colossal sky-beam from y = 14 down to ground
        beams.add(BeamSegment(tx, 14f, tz, tx, 0f, tz, 0.25f, 0.92f, 1.0f, 0.95f, 0.85f, 0.85f))
        shockwaves.add(ShockwaveEffect(tx, 0.2f, tz, 5.8f, 0.2f, 0.9f, 1.0f, 0f, 0.75f))
        pendingParticles.add(ParticleEvent(tx, 0.6f, tz, 0.2f, 0.95f, 1.0f, 40, 7.5f, 0.22f))

        val baseStrikeDmg = Economy.baseEnemyHpForWave(wave) * 3.8f * (1f + techLevel("t4_orbital_focused") * 0.25f) *
            (1f + techLevel("t6_omega_damage") * 0.22f)
        for (e in ArrayList(enemies)) {
            if (hypot(e.x - tx, e.z - tz) <= 5.5f) {
                applyDamageToEnemy(e, baseStrikeDmg, 0.90f, 2.5f, null)
            }
        }
    }

    fun triggerCryoNova(): Boolean = synchronized(lock) {
        if (cryoCooldown > 0f || enemies.isEmpty()) return false
        triggerCryoNovaInternal()
        return true
    }

    private fun triggerCryoNovaInternal() {
        cryoCooldown = cryoMaxCooldown().coerceAtLeast(5f)
        sound?.play(Sfx.ABILITY_CRYO)
        shockwaves.add(ShockwaveEffect(0f, 0.25f, 0f, 18.0f, 0.0f, 0.90f, 1.0f, 0f, 0.85f))
        val frostDmg = Economy.baseEnemyHpForWave(wave) * 0.95f * (1f + techLevel("t2_cryo_deep") * 0.15f)
        for (e in ArrayList(enemies)) {
            e.slowFactor = 0.70f
            e.slowTimer = 6.5f
            e.brittleTimer = 6.5f
            applyDamageToEnemy(e, frostDmg, 0.75f, 1.5f, null)
        }
    }

    fun triggerOverclock(): Boolean = synchronized(lock) {
        if (overclockCooldown > 0f) return false
        triggerOverclockInternal()
        return true
    }

    private fun triggerOverclockInternal() {
        overclockCooldown = overclockMaxCooldown().coerceAtLeast(6f)
        overclockActiveTimer = 8.0f + techLevel("t4_overclock_surge") * 0.8f
        sound?.play(Sfx.ABILITY_OVERCLOCK)
        for (t in towers) {
            if (t != null) {
                val p = ArenaMap.PADS[t.padIndex]
                shockwaves.add(ShockwaveEffect(p.x, p.y + 0.1f, p.z, 2.2f, 1.0f, 0.82f, 0.18f, 0f, 0.45f))
            }
        }
    }

    // --- Player Actions ---
    fun rushNextWave(): Boolean = synchronized(lock) {
        val rushBonus = (Economy.baseWaveClearBonus(wave) * 0.35).roundToLong().coerceAtLeast(25L)
        credits = Economy.addCreditsSafe(credits, rushBonus)
        wave++
        if (wave > highestWave) highestWave = wave
        prepareWaveQueue(wave, announce = true)
        sound?.play(Sfx.WAVE_CLEAR)
        checkMilestones()
        return true
    }

    fun selectPad(index: Int) = synchronized(lock) {
        if (index in ArenaMap.PADS.indices) {
            selectedPadIndex = index
            sound?.play(Sfx.CLICK)
        }
    }

    fun buildTower(padIndex: Int, type: TowerType): Boolean = synchronized(lock) {
        buildTowerInternal(padIndex, type, playSfx = true)
    }

    private fun buildTowerInternal(padIndex: Int, type: TowerType, playSfx: Boolean): Boolean {
        if (padIndex !in towers.indices) return false
        if (towers[padIndex] != null) return false
        val cost = buildCostFor(type)
        if (credits < cost) return false
        credits -= cost
        // Spending credits resets fractional interest carry per standing design
        interestCarryScaled = 0
        towers[padIndex] = TowerInstance(padIndex = padIndex, type = type, level = 1, totalInvested = cost)
        recalcBeaconAuras()
        val pad = ArenaMap.PADS[padIndex]
        shockwaves.add(ShockwaveEffect(pad.x, pad.y + 0.05f, pad.z, 1.8f, 0.0f, 0.9f, 1.0f, 0f, 0.35f))
        if (playSfx) sound?.play(Sfx.BUILD_TOWER)
        checkMilestones()
        return true
    }

    fun upgradeTower(padIndex: Int): Boolean = synchronized(lock) {
        upgradeTowerInternal(padIndex, playSfx = true)
    }

    private fun upgradeTowerInternal(padIndex: Int, playSfx: Boolean): Boolean {
        val t = towers.getOrNull(padIndex) ?: return false
        val cost = upgradeCostFor(t)
        if (credits < cost) return false
        credits -= cost
        interestCarryScaled = 0
        t.level++
        t.totalInvested += cost
        recalcBeaconAuras()
        val pad = ArenaMap.PADS[padIndex]
        shockwaves.add(ShockwaveEffect(pad.x, pad.y + 0.1f, pad.z, 2.1f, 1.0f, 0.85f, 0.2f, 0f, 0.38f))
        if (playSfx) sound?.play(Sfx.UPGRADE_TOWER)
        checkMilestones()
        return true
    }

    fun cycleTargetMode(padIndex: Int): Unit = synchronized(lock) {
        val t = towers.getOrNull(padIndex)
        if (t != null) {
            t.targetMode = t.targetMode.next()
            sound?.play(Sfx.CLICK)
        }
    }

    fun sellTower(padIndex: Int): Boolean = synchronized(lock) {
        val t = towers.getOrNull(padIndex) ?: return false
        val refund = sellRefundFor(t)
        credits = Economy.addCreditsSafe(credits, refund)
        towers[padIndex] = null
        recalcBeaconAuras()
        sound?.play(Sfx.SELL_TOWER)
        return true
    }

    fun buyTech(nodeId: String): Boolean = synchronized(lock) {
        val node = TechTree.NODES.firstOrNull { it.id == nodeId } ?: return false
        val cur = techLevel(nodeId)
        if (cur >= node.maxLevel) return false
        val cost = node.costForNext(cur)
        if (shards < cost) return false
        shards -= cost
        techLevels[nodeId] = cur + 1
        recalcBeaconAuras()
        sound?.play(Sfx.RESEARCH_TECH)
        checkMilestones()
        return true
    }

    fun toggleSpeedMode() = synchronized(lock) {
        speedMode = when (speedMode) {
            1 -> 2
            2 -> 4
            else -> 1
        }
        sound?.play(Sfx.CLICK)
    }

    fun setSpeed(mode: Int) = synchronized(lock) {
        speedMode = mode.coerceIn(1, 4)
        sound?.play(Sfx.CLICK)
    }

    fun toggleAutoBuild() = synchronized(lock) {
        autoBuild = !autoBuild
        sound?.play(Sfx.CLICK)
    }

    fun toggleAutoUpgrade() = synchronized(lock) {
        autoUpgrade = !autoUpgrade
        sound?.play(Sfx.CLICK)
    }

    fun toggleAutoStrike() = synchronized(lock) {
        autoStrike = !autoStrike
        sound?.play(Sfx.CLICK)
    }

    fun toggleAutoCamera() = synchronized(lock) {
        autoCamera = !autoCamera
        sound?.play(Sfx.CLICK)
    }

    fun toggleSound() = synchronized(lock) {
        soundEnabled = !soundEnabled
        sound?.enabled = soundEnabled
        if (soundEnabled) sound?.play(Sfx.CLICK)
    }

    fun setQuality(index: Int) = synchronized(lock) {
        qualityIndex = index.coerceIn(0, 2)
        sound?.play(Sfx.CLICK)
    }

    fun toggleBatterySaver() = synchronized(lock) {
        batterySaver = !batterySaver
        sound?.play(Sfx.CLICK)
    }

    fun dismissOfflineReport() = synchronized(lock) {
        offlineReportText = null
        sound?.play(Sfx.CLICK)
    }

    // --- 3D Camera Orbit / Zoom / Reset ---
    fun orbitCamera(deltaYawDeg: Float, deltaPitchDeg: Float) = synchronized(lock) {
        cameraYawDeg = (cameraYawDeg + deltaYawDeg) % 360f
        cameraPitchDeg = (cameraPitchDeg + deltaPitchDeg).coerceIn(26f, 78f)
    }

    fun zoomCamera(scaleFactor: Float) = synchronized(lock) {
        cameraZoom = (cameraZoom / scaleFactor.coerceIn(0.7f, 1.4f)).coerceIn(0.58f, 1.55f)
    }

    fun panCamera(deltaWorldX: Float, deltaWorldZ: Float) = synchronized(lock) {
        cameraPanX = (cameraPanX + deltaWorldX).coerceIn(-6.0f, 6.0f)
        cameraPanZ = (cameraPanZ + deltaWorldZ).coerceIn(-8.0f, 8.0f)
    }

    fun resetCamera() = synchronized(lock) {
        cameraYawDeg = 0f
        cameraPitchDeg = 54f
        cameraZoom = 1.0f
        cameraPanX = 0f
        cameraPanZ = 0f
        sound?.play(Sfx.CLICK)
    }

    fun selectPadNearWorld(wx: Float, wz: Float): Int = synchronized(lock) {
        var bestIdx = -1
        var bestD2 = 2.4f * 2.4f
        for (pad in ArenaMap.PADS) {
            val dx = pad.x - wx
            val dz = pad.z - wz
            val d2 = dx * dx + dz * dz
            if (d2 < bestD2) {
                bestD2 = d2
                bestIdx = pad.index
            }
        }
        if (bestIdx != -1) {
            selectedPadIndex = bestIdx
            sound?.play(Sfx.CLICK)
        }
        bestIdx
    }

    // --- Milestones ---
    private fun checkMilestones() {
        val activeTowers = towers.filterNotNull()
        val count = activeTowers.size
        val maxLvl = activeTowers.maxOfOrNull { it.level } ?: 0
        val distinctTypes = activeTowers.map { it.type }.toSet().size
        val totalTechs = totalTechPurchases()

        for (m in TechTree.MILESTONES) {
            if (m.id in unlockedMilestones) continue
            val reached = when (m.id) {
                "m_first_tower" -> count >= 1
                "m_full_grid_6" -> count >= 6
                "m_full_grid_12" -> count >= 12
                "m_full_grid_20" -> count >= 20
                "m_mk2_upgrade" -> maxLvl >= 5
                "m_mk3_upgrade" -> maxLvl >= 10
                "m_omega_upgrade" -> maxLvl >= 20
                "m_all_archetypes" -> distinctTypes >= 8
                "m_wave_5" -> highestWave >= 5
                "m_wave_10" -> highestWave >= 10
                "m_wave_20" -> highestWave >= 20
                "m_wave_30" -> highestWave >= 30
                "m_wave_40" -> highestWave >= 40
                "m_wave_50" -> highestWave >= 50
                "m_wave_75" -> highestWave >= 75
                "m_wave_100" -> highestWave >= 100
                "m_kills_100" -> totalKills >= 100L
                "m_kills_500" -> totalKills >= 500L
                "m_kills_2500" -> totalKills >= 2_500L
                "m_kills_10000" -> totalKills >= 10_000L
                "m_boss_kills_5" -> bossKills >= 5
                "m_tech_10" -> totalTechs >= 10
                "m_tech_35" -> totalTechs >= 35
                "m_tech_80" -> totalTechs >= 80
                else -> false
            }
            if (reached) {
                unlockedMilestones.add(m.id)
                credits = Economy.addCreditsSafe(credits, m.creditReward)
                shards += m.shardReward
                sound?.play(Sfx.MILESTONE)
                pushBanner("MILESTONE · ${m.title.uppercase()}", "+${Economy.formatCompact(m.creditReward)} CR · +${m.shardReward} Shards", 3.0f)
            }
        }
    }

    // --- Snapshots for Renderer & Compose UI ---
    fun captureRenderSnapshot(): RenderSnapshot = synchronized(lock) {
        val rTowers = ArrayList<RenderTower>(towers.size)
        for (t in towers) {
            if (t == null) continue
            val p = ArenaMap.PADS[t.padIndex]
            rTowers.add(
                RenderTower(
                    padIndex = t.padIndex,
                    type = t.type,
                    level = t.level,
                    visualTier = t.type.visualTier(t.level),
                    x = p.x, y = p.y, z = p.z,
                    yawDeg = t.yawDeg,
                    pitchDeg = t.pitchDeg,
                    barrelSpinDeg = t.barrelSpinDeg,
                    recoil = t.recoil,
                    range = effectiveRange(t),
                    hasAuraBoost = t.auraFireRateMul > 1.01f,
                    beamActive = t.beamVisibleTimer > 0f,
                    beamEndX = t.beamEndX,
                    beamEndY = t.beamEndY,
                    beamEndZ = t.beamEndZ,
                    lockIntensity = (t.lockDuration / 3.0f).coerceIn(0f, 1f)
                )
            )
        }

        val rEnemies = ArrayList<RenderEnemy>(enemies.size)
        for (e in enemies) {
            rEnemies.add(
                RenderEnemy(
                    id = e.id,
                    type = e.type,
                    x = e.x, y = e.y, z = e.z,
                    yawDeg = e.yawDeg,
                    scale = e.type.scale,
                    hpFraction = (e.hp / e.maxHp).coerceIn(0f, 1f),
                    shieldFraction = if (e.maxShield > 0f) (e.shield / e.maxShield).coerceIn(0f, 1f) else 0f,
                    animPhase = e.animPhase,
                    isChilled = e.slowTimer > 0f,
                    isBurning = e.burnTimer > 0f,
                    isPhaseSurging = e.phaseSurgeTimer > 0f,
                    hitFlash = e.hitFlash
                )
            )
        }

        val rProj = ArrayList<RenderProjectile>(projectiles.size)
        for (p in projectiles) {
            val dx = p.targetX - p.x
            val dy = p.targetY - p.y
            val dz = p.targetZ - p.z
            val yaw = Math.toDegrees(atan2(dx.toDouble(), dz.toDouble())).toFloat()
            val pitch = Math.toDegrees(atan2(dy.toDouble(), hypot(dx, dz).coerceAtLeast(1e-3f).toDouble())).toFloat()
            rProj.add(RenderProjectile(p.type, p.x, p.y, p.z, yaw, pitch, p.isCrit))
        }

        val rBeams = ArrayList(beams)
        val rShockwaves = ArrayList(shockwaves)
        val rParticles = ArrayList(pendingParticles)
        pendingParticles.clear()

        val biome = if (biomeOverride in Biome.entries.indices) Biome.entries[biomeOverride] else Biome.forWave(wave)

        RenderSnapshot(
            wave = wave,
            biome = biome,
            simTime = simTime,
            coreShieldFraction = (coreShield / maxCoreShield).coerceIn(0f, 1f),
            overclockActive = overclockActiveTimer > 0f,
            selectedPadIndex = selectedPadIndex,
            cameraYawDeg = cameraYawDeg,
            cameraPitchDeg = cameraPitchDeg,
            cameraZoom = cameraZoom,
            cameraPanX = cameraPanX,
            cameraPanZ = cameraPanZ,
            towers = rTowers,
            enemies = rEnemies,
            projectiles = rProj,
            beams = rBeams,
            shockwaves = rShockwaves,
            particleEvents = rParticles
        )
    }

    fun captureUiSnapshot(): UiSnapshot = synchronized(lock) {
        val padSpec = ArenaMap.PADS.getOrNull(selectedPadIndex)
        val selTower = towers.getOrNull(selectedPadIndex)
        val padUi = if (padSpec != null) {
            if (selTower != null) {
                PadUiState(
                    padIndex = selectedPadIndex,
                    ringName = padSpec.ringName,
                    towerType = selTower.type,
                    level = selTower.level,
                    tierLabel = selTower.type.tierLabel(selTower.level),
                    targetMode = selTower.targetMode,
                    damage = effectiveDamage(selTower),
                    fireRate = effectiveFireRate(selTower),
                    range = effectiveRange(selTower),
                    upgradeCost = upgradeCostFor(selTower),
                    sellRefund = sellRefundFor(selTower),
                    totalKills = selTower.totalKills,
                    totalDamage = selTower.totalDamage.roundToLong(),
                    auraBoosted = selTower.auraFireRateMul > 1.01f
                )
            } else {
                PadUiState(
                    padIndex = selectedPadIndex,
                    ringName = padSpec.ringName,
                    towerType = null,
                    level = 0,
                    tierLabel = "EMPTY PAD",
                    targetMode = TargetMode.FIRST,
                    damage = 0f,
                    fireRate = 0f,
                    range = 0f,
                    upgradeCost = 0L,
                    sellRefund = 0L,
                    totalKills = 0L,
                    totalDamage = 0L,
                    auraBoosted = false
                )
            }
        } else null

        var totalDps = 0.0
        for (t in towers) {
            if (t != null) {
                totalDps += effectiveDamage(t) * effectiveFireRate(t)
            }
        }

        val biome = if (biomeOverride in Biome.entries.indices) Biome.entries[biomeOverride] else Biome.forWave(wave)

        UiSnapshot(
            credits = credits,
            shards = shards,
            wave = wave,
            highestWave = highestWave,
            biome = biome,
            activeEnemyCount = enemies.size,
            remainingToSpawn = spawnQueue.size,
            coreShield = coreShield.toInt(),
            maxCoreShield = maxCoreShield.toInt(),
            totalKills = totalKills,
            bossKills = bossKills,
            speedMode = speedMode,
            autoBuild = autoBuild,
            autoUpgrade = autoUpgrade,
            autoStrike = autoStrike,
            autoCamera = autoCamera,
            soundEnabled = soundEnabled,
            qualityIndex = qualityIndex,
            batterySaver = batterySaver,
            selectedPadIndex = selectedPadIndex,
            selectedPad = padUi,
            orbitalCooldown = orbitalCooldown,
            orbitalMaxCooldown = orbitalMaxCooldown(),
            cryoCooldown = cryoCooldown,
            cryoMaxCooldown = cryoMaxCooldown(),
            overclockCooldown = overclockCooldown,
            overclockMaxCooldown = overclockMaxCooldown(),
            overclockActiveRemaining = overclockActiveTimer,
            bannerTitle = activeBanner?.title,
            bannerSubtitle = activeBanner?.subtitle,
            offlineReport = offlineReportText,
            techLevels = HashMap(techLevels),
            unlockedMilestones = HashSet(unlockedMilestones),
            totalDpsEstimate = totalDps.roundToLong()
        )
    }

    // --- Save / Load with Schema Version & Offline Simulation (Pure Kotlin, zero stub issues) ---
    fun saveToJson(nowMs: Long = System.currentTimeMillis()): String = synchronized(lock) {
        val sb = StringBuilder(512)
        sb.append("schema=").append(SAVE_SCHEMA_VERSION).append('\n')
        sb.append("credits=").append(credits).append('\n')
        sb.append("shards=").append(shards).append('\n')
        sb.append("ic=").append(interestCarryScaled).append('\n')
        sb.append("wave=").append(wave).append('\n')
        sb.append("highestWave=").append(highestWave).append('\n')
        sb.append("totalKills=").append(totalKills).append('\n')
        sb.append("bossKills=").append(bossKills).append('\n')
        sb.append("speedMode=").append(speedMode).append('\n')
        sb.append("autoBuild=").append(autoBuild).append('\n')
        sb.append("autoUpgrade=").append(autoUpgrade).append('\n')
        sb.append("autoStrike=").append(autoStrike).append('\n')
        sb.append("autoCamera=").append(autoCamera).append('\n')
        sb.append("soundEnabled=").append(soundEnabled).append('\n')
        sb.append("qualityIndex=").append(qualityIndex).append('\n')
        sb.append("batterySaver=").append(batterySaver).append('\n')
        sb.append("savedAt=").append(nowMs).append('\n')

        for (t in towers) {
            if (t == null) continue
            sb.append("tower=")
                .append(t.padIndex).append(',')
                .append(t.type.id).append(',')
                .append(t.level).append(',')
                .append(t.targetMode.ordinal).append(',')
                .append(t.totalInvested).append(',')
                .append(t.totalKills).append(',')
                .append(t.totalDamage)
                .append('\n')
        }
        for ((k, v) in techLevels) {
            sb.append("tech=").append(k).append(':').append(v).append('\n')
        }
        for (m in unlockedMilestones) {
            sb.append("ms=").append(m).append('\n')
        }
        return sb.toString()
    }

    fun loadFromJson(raw: String, nowMs: Long = System.currentTimeMillis()): Boolean = synchronized(lock) {
        if (raw.isBlank()) return false
        return try {
            val map = HashMap<String, String>()
            val towerLines = ArrayList<String>()
            val techLines = ArrayList<String>()
            val msLines = ArrayList<String>()

            for (line in raw.lineSequence()) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue
                val eq = trimmed.indexOf('=')
                if (eq <= 0) continue
                val key = trimmed.substring(0, eq)
                val value = trimmed.substring(eq + 1)
                when (key) {
                    "tower" -> towerLines.add(value)
                    "tech" -> techLines.add(value)
                    "ms" -> msLines.add(value)
                    else -> map[key] = value
                }
            }
            if ("schema" !in map && "credits" !in map) return false

            credits = (map["credits"]?.toLongOrNull() ?: 480L).coerceIn(0L, Economy.MAX_CREDITS)
            shards = (map["shards"]?.toIntOrNull() ?: 3).coerceAtLeast(0)
            interestCarryScaled = (map["ic"]?.toIntOrNull() ?: 0).coerceIn(0, 99_999)
            wave = (map["wave"]?.toIntOrNull() ?: 1).coerceAtLeast(1)
            highestWave = max(wave, map["highestWave"]?.toIntOrNull() ?: wave)
            totalKills = (map["totalKills"]?.toLongOrNull() ?: 0L).coerceAtLeast(0L)
            bossKills = (map["bossKills"]?.toIntOrNull() ?: 0).coerceAtLeast(0)
            speedMode = (map["speedMode"]?.toIntOrNull() ?: 1).coerceIn(1, 4)
            autoBuild = map["autoBuild"]?.toBooleanStrictOrNull() ?: true
            autoUpgrade = map["autoUpgrade"]?.toBooleanStrictOrNull() ?: true
            autoStrike = map["autoStrike"]?.toBooleanStrictOrNull() ?: true
            autoCamera = map["autoCamera"]?.toBooleanStrictOrNull() ?: true
            soundEnabled = map["soundEnabled"]?.toBooleanStrictOrNull() ?: true
            sound?.enabled = soundEnabled
            qualityIndex = (map["qualityIndex"]?.toIntOrNull() ?: 1).coerceIn(0, 2)
            batterySaver = map["batterySaver"]?.toBooleanStrictOrNull() ?: false

            for (i in towers.indices) towers[i] = null
            for (tl in towerLines) {
                val parts = tl.split(',')
                if (parts.size >= 5) {
                    val pad = parts[0].toIntOrNull() ?: -1
                    if (pad in towers.indices) {
                        val type = TowerType.byId(parts[1].toIntOrNull() ?: 0)
                        val lvl = (parts[2].toIntOrNull() ?: 1).coerceAtLeast(1)
                        val mode = TargetMode.entries.getOrElse(parts[3].toIntOrNull() ?: 0) { TargetMode.FIRST }
                        val inv = parts[4].toLongOrNull() ?: type.baseCost
                        val inst = TowerInstance(pad, type, lvl, mode, inv)
                        if (parts.size >= 6) inst.totalKills = parts[5].toLongOrNull() ?: 0L
                        if (parts.size >= 7) inst.totalDamage = parts[6].toDoubleOrNull() ?: 0.0
                        towers[pad] = inst
                    }
                }
            }

            techLevels.clear()
            for (tk in techLines) {
                val colon = tk.indexOf(':')
                if (colon > 0) {
                    val k = tk.substring(0, colon)
                    val v = (tk.substring(colon + 1).toIntOrNull() ?: 0).coerceAtLeast(0)
                    techLevels[k] = v
                }
            }

            unlockedMilestones.clear()
            for (m in msLines) {
                unlockedMilestones.add(m)
            }

            recalcBeaconAuras()
            coreShield = maxCoreShield
            enemies.clear()
            projectiles.clear()
            spawnQueue.clear()
            prepareWaveQueue(wave, announce = false)

            // Calculate offline defense progress if away for >= 45 seconds
            val savedAt = map["savedAt"]?.toLongOrNull() ?: 0L
            if (savedAt > 0L && nowMs > savedAt + 45_000L) {
                val elapsedSec = ((nowMs - savedAt) / 1000L).coerceAtMost(12L * 3600L)
                val offlineMul = 1.0 + techLevel("t5_offline_sim") * 0.15
                val wavesClearedOffline = ((elapsedSec / 28.0) * offlineMul).toInt().coerceIn(1, 240)
                val avgCreditPerWave = (Economy.baseWaveClearBonus(wave) + Economy.baseKillRewardForWave(wave) * 10)
                val earnedOfflineCredits = (wavesClearedOffline * avgCreditPerWave * offlineMul).roundToLong()
                val earnedOfflineShards = (wavesClearedOffline / 5).coerceAtLeast(if (elapsedSec >= 300) 1 else 0)
                credits = Economy.addCreditsSafe(credits, earnedOfflineCredits)
                shards += earnedOfflineShards
                totalKills += wavesClearedOffline * 12L
                val mins = elapsedSec / 60
                offlineReportText = "While away (${mins}m), your AI Bastion repelled $wavesClearedOffline waves and secured +${Economy.formatCompact(earnedOfflineCredits)} CR" +
                    (if (earnedOfflineShards > 0) " & +$earnedOfflineShards Shards!" else "!")
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun save(prefs: SharedPreferences) = synchronized(lock) {
        val currentRaw = prefs.getString(KEY_SAVE, null)
        val json = saveToJson()
        val editor = prefs.edit()
        if (!currentRaw.isNullOrBlank()) {
            editor.putString(KEY_BACKUP, currentRaw)
        }
        editor.putString(KEY_SAVE, json)
        editor.apply()
    }

    fun load(prefs: SharedPreferences): Boolean = synchronized(lock) {
        val primary = prefs.getString(KEY_SAVE, null)
        if (!primary.isNullOrBlank() && loadFromJson(primary)) {
            return true
        }
        val backup = prefs.getString(KEY_BACKUP, null)
        if (!backup.isNullOrBlank() && loadFromJson(backup)) {
            return true
        }
        return false
    }

    private fun approachAngle(current: Float, target: Float, maxStep: Float): Float {
        var diff = (target - current) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        return if (abs(diff) <= maxStep) target else current + (if (diff > 0) maxStep else -maxStep)
    }

    companion object {
        const val PREFS_NAME = "bastion_3d_td"
        const val KEY_SAVE = "save_v1"
        const val KEY_BACKUP = "save_backup_v1"
        const val SAVE_SCHEMA_VERSION = 1

        val BUILD_PRIORITY_PADS = intArrayOf(
            2, 3, 6, 7, 9, 10, 11, 13, 14, 15, 5, 17, 0, 1, 4, 8, 12, 16, 18, 19
        )
    }
}
