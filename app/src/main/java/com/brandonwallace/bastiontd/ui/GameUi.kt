package com.brandonwallace.bastiontd.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brandonwallace.bastiontd.game.ArenaMap
import com.brandonwallace.bastiontd.game.Economy
import com.brandonwallace.bastiontd.game.GameEngine
import com.brandonwallace.bastiontd.game.TechCategory
import com.brandonwallace.bastiontd.game.TechTree
import com.brandonwallace.bastiontd.game.TowerType
import com.brandonwallace.bastiontd.game.UiSnapshot
import java.util.Locale

@Composable
fun BastionHudOverlay(
    snap: UiSnapshot,
    engine: GameEngine,
    onAction: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0: Pad/Build, 1: Tech Tree, 2: Milestones
    var drawerExpanded by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    var selectedTechCategory by remember { mutableStateOf<TechCategory?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- Top Tactical Command Bar ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            TopTelemetryBar(
                snap = snap,
                onRushWave = {
                    engine.rushNextWave()
                    onAction()
                },
                onToggleSpeed = {
                    engine.toggleSpeedMode()
                    onAction()
                },
                onToggleMute = {
                    engine.toggleSound()
                    onAction()
                },
                onOpenSettings = { showSettings = !showSettings }
            )

            Spacer(modifier = Modifier.height(5.dp))

            // AI Auto-Commander + Active Commander Abilities Row
            CommanderControlStrip(
                snap = snap,
                onToggleAutoBuild = { engine.toggleAutoBuild(); onAction() },
                onToggleAutoUpgrade = { engine.toggleAutoUpgrade(); onAction() },
                onToggleAutoStrike = { engine.toggleAutoStrike(); onAction() },
                onToggleAutoCam = { engine.toggleAutoCamera(); onAction() },
                onOrbital = { engine.triggerOrbitalStrike(); onAction() },
                onCryoNova = { engine.triggerCryoNova(); onAction() },
                onOverclock = { engine.triggerOverclock(); onAction() }
            )

            // Center-top Wave / Boss / Milestone Event Banner
            AnimatedVisibility(
                visible = snap.bannerTitle != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xD90A1428),
                                        Color(0xEE182B4D),
                                        Color(0xD90A1428)
                                    )
                                )
                            )
                            .border(1.dp, CyanAccent.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = snap.bannerTitle ?: "",
                            color = GoldAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        val sub = snap.bannerSubtitle
                        if (!sub.isNullOrBlank()) {
                            Text(
                                text = sub,
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Offline Defense Report Toast
            if (snap.offlineReport != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xEE10243E))
                        .border(1.dp, EmeraldGreen, RoundedCornerShape(10.dp))
                        .clickable {
                            engine.dismissOfflineReport()
                            onAction()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "🛡️ ${snap.offlineReport}",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "OK",
                        color = EmeraldGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        // --- Bottom Collapsible Tactical Command Deck ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, PanelSurface, BgObsidian)
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // Tab Bar + Collapse / Reset Camera Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DeckTabButton(
                    label = "🏗️ PAD #${snap.selectedPadIndex + 1}",
                    selected = drawerExpanded && activeTab == 0,
                    onClick = {
                        activeTab = 0
                        drawerExpanded = true
                    },
                    modifier = Modifier.weight(1f)
                )
                DeckTabButton(
                    label = "🔬 TECH (${snap.techLevels.values.sum()})",
                    selected = drawerExpanded && activeTab == 1,
                    onClick = {
                        activeTab = 1
                        drawerExpanded = true
                    },
                    modifier = Modifier.weight(1f)
                )
                DeckTabButton(
                    label = "🎖️ GOALS (${snap.unlockedMilestones.size}/${TechTree.MILESTONES.size})",
                    selected = drawerExpanded && activeTab == 2,
                    onClick = {
                        activeTab = 2
                        drawerExpanded = true
                    },
                    modifier = Modifier.weight(1f)
                )
                SmallPillButton(
                    text = "🎥 CAM",
                    color = CyanAccent,
                    onClick = {
                        engine.resetCamera()
                        onAction()
                    }
                )
                SmallPillButton(
                    text = if (drawerExpanded) "▼" else "▲",
                    color = TextSecondary,
                    onClick = { drawerExpanded = !drawerExpanded }
                )
            }

            if (drawerExpanded) {
                Spacer(modifier = Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(224.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PanelSurface)
                        .border(1.dp, PanelBorder, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    when (activeTab) {
                        0 -> PadBuildInspector(snap = snap, engine = engine, onAction = onAction)
                        1 -> TechTreePanel(
                            snap = snap,
                            engine = engine,
                            selectedCategory = selectedTechCategory,
                            onSelectCategory = { selectedTechCategory = it },
                            onAction = onAction
                        )
                        else -> MilestonesPanel(snap = snap)
                    }
                }
            }
        }

        // --- Settings Modal ---
        if (showSettings) {
            SettingsDialog(
                snap = snap,
                engine = engine,
                onAction = onAction,
                onDismiss = { showSettings = false }
            )
        }
    }
}

@Composable
private fun TopTelemetryBar(
    snap: UiSnapshot,
    onRushWave: () -> Unit,
    onToggleSpeed: () -> Unit,
    onToggleMute: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PanelSurface)
            .border(1.dp, PanelBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Wave & Biome Badge
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "WAVE ${snap.wave}",
                        color = CyanAccent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = snap.biome.title.uppercase(),
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "Hostiles: ${snap.activeEnemyCount + snap.remainingToSpawn} · DPS: ${Economy.formatCompact(snap.totalDpsEstimate)}",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }

            // Credits & Quantum Shards
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "💰 ${Economy.formatCompact(snap.credits)}",
                        color = GoldAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "💎 ${snap.shards} Shards",
                        color = ShardPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Speed Button (1x / 2x / 4x)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (snap.speedMode > 1) CyanAccent.copy(alpha = 0.22f) else PanelCard)
                        .border(1.dp, if (snap.speedMode > 1) CyanAccent else PanelBorder, RoundedCornerShape(8.dp))
                        .clickable { onToggleSpeed() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${snap.speedMode}×",
                        color = if (snap.speedMode > 1) CyanAccent else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Round Mute / Unmute Button (standing requirement)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (snap.soundEnabled) PanelCard else CrimsonAlert.copy(alpha = 0.25f))
                        .border(
                            1.dp,
                            if (snap.soundEnabled) CyanAccent.copy(alpha = 0.7f) else CrimsonAlert,
                            CircleShape
                        )
                        .clickable { onToggleMute() }
                ) {
                    Text(
                        text = if (snap.soundEnabled) "🔊" else "🔇",
                        fontSize = 14.sp
                    )
                }

                // Settings Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(PanelCard)
                        .border(1.dp, PanelBorder, CircleShape)
                        .clickable { onOpenSettings() }
                ) {
                    Text(text = "⚙️", fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        // Core Shield Bar + Rush Next Wave Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            val shieldFrac = (snap.coreShield.toFloat() / snap.maxCoreShield.coerceAtLeast(1)).coerceIn(0f, 1f)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🛡️ QUANTUM CORE SHIELD",
                        color = CyanAccent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${snap.coreShield} / ${snap.maxCoreShield}",
                        color = TextPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { shieldFrac },
                    color = if (shieldFrac > 0.35f) CyanAccent else CrimsonAlert,
                    trackColor = Color(0xFF1A2942),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Rush Wave Button for instant wave streaming + bonus credits
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(GoldAccent.copy(alpha = 0.20f))
                    .border(1.dp, GoldAccent, RoundedCornerShape(7.dp))
                    .clickable { onRushWave() }
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "⚡ RUSH WAVE",
                    color = GoldAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun CommanderControlStrip(
    snap: UiSnapshot,
    onToggleAutoBuild: () -> Unit,
    onToggleAutoUpgrade: () -> Unit,
    onToggleAutoStrike: () -> Unit,
    onToggleAutoCam: () -> Unit,
    onOrbital: () -> Unit,
    onCryoNova: () -> Unit,
    onOverclock: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        // 3 Active Commander Abilities
        AbilityChip(
            label = "☄️ ORBITAL",
            cooldown = snap.orbitalCooldown,
            accent = CyanAccent,
            onClick = onOrbital
        )
        AbilityChip(
            label = "❄️ CRYO NOVA",
            cooldown = snap.cryoCooldown,
            accent = Color(0xFF4DD0E1),
            onClick = onCryoNova
        )
        AbilityChip(
            label = if (snap.overclockActiveRemaining > 0f) "⚡ ACTIVE ${snap.overclockActiveRemaining.toInt()}s" else "⚡ OVERCLOCK",
            cooldown = snap.overclockCooldown,
            accent = GoldAccent,
            onClick = onOverclock
        )

        // 4 AI Auto-Commander Toggles for Non-Stop Hands-Free Progression
        AutoToggleChip("🤖 BUILD", snap.autoBuild, onToggleAutoBuild)
        AutoToggleChip("🤖 UPG", snap.autoUpgrade, onToggleAutoUpgrade)
        AutoToggleChip("🤖 STRIKE", snap.autoStrike, onToggleAutoStrike)
        AutoToggleChip("🎥 ORBIT", snap.autoCamera, onToggleAutoCam)
    }
}

@Composable
private fun AbilityChip(
    label: String,
    cooldown: Float,
    accent: Color,
    onClick: () -> Unit
) {
    val ready = cooldown <= 0.05f
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (ready) accent.copy(alpha = 0.22f) else PanelCard)
            .border(1.dp, if (ready) accent else PanelBorder, RoundedCornerShape(8.dp))
            .clickable(enabled = ready) { onClick() }
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(
            text = if (ready) label else "$label (${cooldown.toInt() + 1}s)",
            color = if (ready) accent else TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AutoToggleChip(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) EmeraldGreen.copy(alpha = 0.20f) else PanelCard)
            .border(1.dp, if (enabled) EmeraldGreen else PanelBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = "$label ${if (enabled) "ON" else "OFF"}",
            color = if (enabled) EmeraldGreen else TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PadBuildInspector(
    snap: UiSnapshot,
    engine: GameEngine,
    onAction: () -> Unit
) {
    val pad = snap.selectedPad ?: return
    val towerType = pad.towerType

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with Prev / Next Pad Selector
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            SmallPillButton(
                text = "◀ PREV PAD",
                color = CyanAccent,
                onClick = {
                    val prev = (snap.selectedPadIndex - 1 + ArenaMap.PADS.size) % ArenaMap.PADS.size
                    engine.selectPad(prev)
                    onAction()
                }
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "PAD #${pad.padIndex + 1} · ${pad.ringName.uppercase()}",
                    color = CyanAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = if (towerType != null) "${towerType.icon} ${towerType.title} · Lv.${pad.level} (${pad.tierLabel})"
                    else "Tap any 3D Hex Pad or select a turret below to deploy",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
            SmallPillButton(
                text = "NEXT PAD ▶",
                color = CyanAccent,
                onClick = {
                    val next = (snap.selectedPadIndex + 1) % ArenaMap.PADS.size
                    engine.selectPad(next)
                    onAction()
                }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (towerType == null) {
            // Empty Pad: Grid of 8 3D Tower Archetypes to build
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(TowerType.entries) { type ->
                    val cost = engine.buildCostFor(type)
                    val canAfford = snap.credits >= cost
                    val accent = Color(type.colorR, type.colorG, type.colorB)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(9.dp))
                            .background(PanelCard)
                            .border(
                                1.dp,
                                if (canAfford) accent.copy(alpha = 0.8f) else PanelBorder,
                                RoundedCornerShape(9.dp)
                            )
                            .clickable(enabled = canAfford) {
                                engine.buildTower(pad.padIndex, type)
                                onAction()
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${type.icon} ${type.shortName}",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = type.role,
                                color = TextSecondary,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "💰 ${Economy.formatCompact(cost)}",
                            color = if (canAfford) GoldAccent else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        } else {
            // Occupied Pad: Detailed Stats + Upgrade / Target Mode / Sell Controls
            val accent = Color(towerType.colorR, towerType.colorG, towerType.colorB)
            val canUpgrade = snap.credits >= pad.upgradeCost

            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(PanelCard)
                    .border(1.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatBadge("DAMAGE", Economy.formatCompact(pad.damage.toLong()), accent)
                    StatBadge("FIRE RATE", String.format(Locale.US, "%.2f/s", pad.fireRate), CyanAccent)
                    StatBadge("RANGE", String.format(Locale.US, "%.1fm", pad.range), EmeraldGreen)
                    StatBadge("KILLS", Economy.formatCompact(pad.totalKills), GoldAccent)
                }

                Text(
                    text = towerType.description + (if (pad.auraBoosted) " · 🔆 Boosted by Chrono-Beacon!" else ""),
                    color = TextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Upgrade Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1.4f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (canUpgrade) EmeraldGreen.copy(alpha = 0.24f) else BgObsidian)
                            .border(1.dp, if (canUpgrade) EmeraldGreen else PanelBorder, RoundedCornerShape(8.dp))
                            .clickable(enabled = canUpgrade) {
                                engine.upgradeTower(pad.padIndex)
                                onAction()
                            }
                            .padding(vertical = 7.dp)
                    ) {
                        Text(
                            text = "⬆️ UPGRADE Lv.${pad.level + 1} (💰 ${Economy.formatCompact(pad.upgradeCost)})",
                            color = if (canUpgrade) EmeraldGreen else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }

                    // Target Priority Cycle Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(0.9f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BgObsidian)
                            .border(1.dp, CyanAccent.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .clickable {
                                engine.cycleTargetMode(pad.padIndex)
                                onAction()
                            }
                            .padding(vertical = 7.dp)
                    ) {
                        Text(
                            text = "🎯 ${pad.targetMode.label.uppercase()}",
                            color = CyanAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    // Sell / Salvage Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CrimsonAlert.copy(alpha = 0.18f))
                            .border(1.dp, CrimsonAlert.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                            .clickable {
                                engine.sellTower(pad.padIndex)
                                onAction()
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "♻️ +${Economy.formatCompact(pad.sellRefund)}",
                            color = CrimsonAlert,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        Text(text = value, color = color, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun TechTreePanel(
    snap: UiSnapshot,
    engine: GameEngine,
    selectedCategory: TechCategory?,
    onSelectCategory: (TechCategory?) -> Unit,
    onAction: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Category Filter Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            SmallPillButton(
                text = "ALL (36)",
                color = if (selectedCategory == null) ShardPurple else TextSecondary,
                onClick = { onSelectCategory(null) }
            )
            for (cat in TechCategory.entries) {
                SmallPillButton(
                    text = cat.title,
                    color = if (selectedCategory == cat) ShardPurple else TextSecondary,
                    onClick = { onSelectCategory(cat) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val filtered = remember(selectedCategory) {
            if (selectedCategory == null) TechTree.NODES
            else TechTree.NODES.filter { it.category == selectedCategory }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filtered, key = { it.id }) { node ->
                val lvl = snap.techLevels[node.id] ?: 0
                val isMax = lvl >= node.maxLevel
                val cost = node.costForNext(lvl)
                val canBuy = !isMax && snap.shards >= cost

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(PanelCard)
                        .border(
                            1.dp,
                            if (canBuy) ShardPurple else PanelBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable(enabled = canBuy) {
                            engine.buyTech(node.id)
                            onAction()
                        }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = node.title,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Lv.$lvl/${node.maxLevel}",
                                color = ShardPurple,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = node.description,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (canBuy) ShardPurple.copy(alpha = 0.24f) else BgObsidian)
                            .border(1.dp, if (canBuy) ShardPurple else PanelBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (isMax) "MAXED" else "💎 $cost",
                            color = if (isMax) EmeraldGreen else if (canBuy) ShardPurple else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestonesPanel(snap: UiSnapshot) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        ) {
            Text(
                text = "Highest Wave: ${snap.highestWave} · Kills: ${Economy.formatCompact(snap.totalKills)} · Bosses: ${snap.bossKills}",
                color = GoldAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${snap.unlockedMilestones.size}/${TechTree.MILESTONES.size} Completed",
                color = EmeraldGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(TechTree.MILESTONES, key = { it.id }) { m ->
                val done = m.id in snap.unlockedMilestones
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(PanelCard)
                        .border(1.dp, if (done) EmeraldGreen.copy(alpha = 0.7f) else PanelBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (done) "✅" else "🎯",
                        fontSize = 13.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = m.title,
                            color = if (done) EmeraldGreen else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = m.requirementText,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = "+${Economy.formatCompact(m.creditReward)} CR · +${m.shardReward} 💎",
                        color = GoldAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsDialog(
    snap: UiSnapshot,
    engine: GameEngine,
    onAction: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA000000))
            .clickable { onDismiss() }
    ) {
        Surface(
            color = PanelSurface,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .width(320.dp)
                .border(1.dp, CyanAccent, RoundedCornerShape(14.dp))
                .clickable(enabled = false) {}
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "⚙️ BASTION 3D TD SETTINGS",
                    color = CyanAccent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "3D Graphics Quality (Shadows, MSAA, Bloom)",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("LOW", "MEDIUM", "HIGH").forEachIndexed { idx, label ->
                        val active = snap.qualityIndex == idx
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) CyanAccent.copy(alpha = 0.25f) else PanelCard)
                                .border(1.dp, if (active) CyanAccent else PanelBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    engine.setQuality(idx)
                                    onAction()
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (active) CyanAccent else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Frame Rate / Battery Saver
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Frame Rate Cap", color = TextPrimary, fontSize = 12.sp)
                    SmallPillButton(
                        text = if (snap.batterySaver) "30 FPS (Battery Saver)" else "60 FPS (Smooth)",
                        color = if (snap.batterySaver) GoldAccent else EmeraldGreen,
                        onClick = {
                            engine.toggleBatterySaver()
                            onAction()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sound Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Synthesized Audio & SFX", color = TextPrimary, fontSize = 12.sp)
                    SmallPillButton(
                        text = if (snap.soundEnabled) "🔊 SOUND ON" else "🔇 MUTED",
                        color = if (snap.soundEnabled) CyanAccent else CrimsonAlert,
                        onClick = {
                            engine.toggleSound()
                            onAction()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "3D Camera Controls:\n• 1-Finger Drag: Orbit 3D Camera\n• 2-Finger Pinch / Drag: Zoom & Pan\n• Tap any Hex Pad: Select / Build / Upgrade\n• Double-Tap: Reset 3D Camera",
                    color = TextSecondary,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyanAccent.copy(alpha = 0.22f))
                        .border(1.dp, CyanAccent, RoundedCornerShape(8.dp))
                        .clickable { onDismiss() }
                        .padding(vertical = 9.dp)
                ) {
                    Text(
                        text = "RESUME COMMAND",
                        color = CyanAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun DeckTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) CyanAccent.copy(alpha = 0.22f) else PanelSurface)
            .border(1.dp, if (selected) CyanAccent else PanelBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) CyanAccent else TextPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SmallPillButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PanelCard)
            .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
