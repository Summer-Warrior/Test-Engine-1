package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.BattleArenaState
import com.example.engine.BattleOutcome
import com.example.model.DefaultDeckSlots
import com.example.model.HeroBattleState
import com.example.model.MedalEntity
import com.example.model.RealmStageEntity
import com.example.model.SkillEntity
import com.example.ui.components.BattleStageCanvas
import com.example.ui.theme.AstralBorder
import com.example.ui.theme.AstralPanel
import com.example.ui.theme.AtbAmber
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DeepCrystalBlue
import com.example.ui.theme.HpEmerald
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.MythrilCyan
import com.example.ui.theme.PowerCrimson
import com.example.ui.theme.RelicGold
import com.example.ui.theme.SoulBreakMagenta
import com.example.ui.theme.SynergyAzure
import com.example.ui.theme.VoidNavy

@Composable
fun BattleScreen(
  battleState: BattleArenaState,
  allMedals: List<MedalEntity>,
  allStages: List<RealmStageEntity>,
  onSelectHero: (Int) -> Unit,
  onBasicAttack: () -> Unit,
  onMedalBurst: () -> Unit,
  onDefend: () -> Unit,
  onRotateWheel: () -> Unit,
  onSoulBreak: () -> Unit,
  onExecuteSkill: (SkillEntity) -> Unit = {},
  onToggleWaitMode: () -> Unit,
  onToggleAutoBattle: () -> Unit,
  onToggleSpeed: () -> Unit,
  onRestartEncounter: () -> Unit,
  onSelectStage: (RealmStageEntity) -> Unit,
  isAudioMuted: Boolean = false,
  activeBgmTitle: String? = null,
  onToggleAudioMute: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val selectedHero = battleState.partyStates.getOrNull(battleState.selectedHeroIndex)
    ?: battleState.partyStates.firstOrNull()

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 12.dp, vertical = 6.dp)
      .testTag("battle_screen")
  ) {
    // 1. Engine Header & Real-time Battle Controls
    EngineHeaderToolbar(
      battleState = battleState,
      isAudioMuted = isAudioMuted,
      activeBgmTitle = activeBgmTitle,
      onToggleWaitMode = onToggleWaitMode,
      onToggleAutoBattle = onToggleAutoBattle,
      onToggleSpeed = onToggleSpeed,
      onToggleAudioMute = onToggleAudioMute,
      onRestartEncounter = onRestartEncounter
    )

    Spacer(modifier = Modifier.height(6.dp))

    // 2. 2D Canvas RPG Battle Stage (with Victory/Defeat Overlay if finished)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(255.dp)
    ) {
      BattleStageCanvas(
        battleState = battleState,
        onSelectHero = onSelectHero,
        onTapBossForAttack = onBasicAttack,
        modifier = Modifier.fillMaxSize()
      )

      if (battleState.outcome != BattleOutcome.ONGOING) {
        BattleOutcomeOverlay(
          battleState = battleState,
          allStages = allStages,
          onRestartEncounter = onRestartEncounter,
          onSelectNextStage = onSelectStage,
          modifier = Modifier.fillMaxSize()
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 3. Party Roster Status Cards (FFRK HP + ATB + Soul Break Gauge)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      battleState.partyStates.forEachIndexed { idx, heroState ->
        PartyHeroHudCard(
          heroState = heroState,
          isSelected = idx == battleState.selectedHeroIndex,
          onClick = { onSelectHero(idx) },
          modifier = Modifier
            .weight(1f)
            .testTag("party_hud_card_$idx")
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 4. KH Union X Rotating Medal Wheel & FFRK Command Deck
    if (selectedHero != null) {
      MedalWheelCommandDeck(
        heroState = selectedHero,
        allMedals = allMedals,
        currentSp = battleState.currentSp,
        maxSp = battleState.maxSp,
        onBasicAttack = onBasicAttack,
        onMedalBurst = onMedalBurst,
        onDefend = onDefend,
        onRotateWheel = onRotateWheel,
        onSoulBreak = onSoulBreak,
        onExecuteSkill = onExecuteSkill
      )
    }
  }
}

@Composable
private fun EngineHeaderToolbar(
  battleState: BattleArenaState,
  isAudioMuted: Boolean,
  activeBgmTitle: String?,
  onToggleWaitMode: () -> Unit,
  onToggleAutoBattle: () -> Unit,
  onToggleSpeed: () -> Unit,
  onToggleAudioMute: () -> Unit,
  onRestartEncounter: () -> Unit
) {
  Surface(
    color = DeepCrystalBlue,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, AstralBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = battleState.stage?.stageCode ?: "RK-01",
            style = MaterialTheme.typography.labelLarge,
            color = RelicGold
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = battleState.stage?.title ?: "Clocktower Plaza",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        val bgmSuffix = if (!activeBgmTitle.isNullOrBlank()) " • ♪ $activeBgmTitle" else ""
        Text(
          text = "Realm: ${battleState.stage?.realm ?: "Daybreak"} • Turn ${battleState.turnsTaken}$bgmSuffix",
          style = MaterialTheme.typography.labelSmall,
          color = MutedSilver,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        EngineToggleChip(
          label = if (battleState.isWaitMode) "WAIT" else "ACTV",
          active = battleState.isWaitMode,
          icon = Icons.Default.HourglassTop,
          onClick = onToggleWaitMode,
          testTag = "toggle_wait_mode"
        )
        EngineToggleChip(
          label = "${battleState.battleSpeedMultiplier}x",
          active = battleState.battleSpeedMultiplier > 1,
          icon = Icons.Default.FastForward,
          onClick = onToggleSpeed,
          testTag = "toggle_speed_mode"
        )
        EngineToggleChip(
          label = "AUTO",
          active = battleState.isAutoBattle,
          icon = Icons.Default.AutoMode,
          onClick = onToggleAutoBattle,
          testTag = "toggle_auto_battle"
        )
        IconButton(
          onClick = onToggleAudioMute,
          modifier = Modifier
            .size(48.dp)
            .testTag("btn_toggle_audio_mute")
        ) {
          Icon(
            imageVector = if (isAudioMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = if (isAudioMuted) "Unmute Battle Audio" else "Mute Battle Audio",
            tint = if (isAudioMuted) MutedSilver else RelicGold
          )
        }
        IconButton(
          onClick = onRestartEncounter,
          modifier = Modifier
            .size(48.dp)
            .testTag("btn_restart_battle")
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Restart Encounter",
            tint = MythrilCyan
          )
        }
      }
    }
  }
}

@Composable
private fun EngineToggleChip(
  label: String,
  active: Boolean,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit,
  testTag: String
) {
  Surface(
    color = if (active) RelicGold.copy(alpha = 0.22f) else VoidNavy,
    shape = RoundedCornerShape(8.dp),
    border = BorderStroke(1.dp, if (active) RelicGold else AstralBorder),
    modifier = Modifier
      .clickable(onClick = onClick)
      .testTag(testTag)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (active) RelicGold else MutedSilver,
        modifier = Modifier.size(13.dp)
      )
      Spacer(modifier = Modifier.width(3.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = if (active) RelicGold else MutedSilver,
        fontSize = 10.sp
      )
    }
  }
}

@Composable
private fun PartyHeroHudCard(
  heroState: HeroBattleState,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val borderColor = when {
    isSelected -> RelicGold
    heroState.isAtbReady -> AtbAmber
    heroState.hasRealmSynergy -> SynergyAzure.copy(alpha = 0.7f)
    else -> AstralBorder
  }

  Surface(
    color = if (isSelected) AstralPanel else DeepCrystalBlue,
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
    modifier = modifier.clickable(onClick = onClick)
  ) {
    Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = heroState.hero.name,
          style = MaterialTheme.typography.labelLarge,
          color = if (heroState.isAlive) Color.White else DangerCrimson,
          maxLines = 1
        )
        if (heroState.hasRealmSynergy) {
          Text(
            text = "SYN",
            style = MaterialTheme.typography.labelSmall,
            color = SynergyAzure,
            fontSize = 9.sp
          )
        }
      }

      Text(
        text = "${heroState.currentHp}",
        style = MaterialTheme.typography.labelSmall,
        color = HpEmerald,
        fontSize = 10.sp
      )

      LinearProgressIndicator(
        progress = {
          (heroState.currentHp.toFloat() / heroState.effectiveMaxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(4.dp)
          .clip(RoundedCornerShape(2.dp)),
        color = HpEmerald,
        trackColor = VoidNavy
      )

      Spacer(modifier = Modifier.height(3.dp))

      // ATB Gauge
      LinearProgressIndicator(
        progress = { (heroState.atbPercent / 100f).coerceIn(0f, 1f) },
        modifier = Modifier
          .fillMaxWidth()
          .height(4.dp)
          .clip(RoundedCornerShape(2.dp)),
        color = if (heroState.isAtbReady) RelicGold else MythrilCyan,
        trackColor = VoidNavy
      )

      Spacer(modifier = Modifier.height(3.dp))

      // Soul Break Pips (0..3 Bars)
      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
          for (bar in 1..3) {
            Box(
              modifier = Modifier
                .size(width = 11.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                  if (heroState.soulBreakBars >= bar) SoulBreakMagenta else VoidNavy
                )
            )
          }
        }
        Text(
          text = if (heroState.isAtbReady) "READY" else "${heroState.atbPercent.toInt()}%",
          style = MaterialTheme.typography.labelSmall,
          color = if (heroState.isAtbReady) RelicGold else MutedSilver,
          fontSize = 9.sp
        )
      }
    }
  }
}

@Composable
private fun MedalWheelCommandDeck(
  heroState: HeroBattleState,
  allMedals: List<MedalEntity>,
  currentSp: Int,
  maxSp: Int,
  onBasicAttack: () -> Unit,
  onMedalBurst: () -> Unit,
  onDefend: () -> Unit,
  onRotateWheel: () -> Unit,
  onSoulBreak: () -> Unit,
  onExecuteSkill: (SkillEntity) -> Unit
) {
  val medalIds = heroState.hero.equippedMedalIdList()
  val activeSlotIndex = heroState.activeWheelIndex % 5
  val slotConfig = DefaultDeckSlots[activeSlotIndex]
  val activeMedalId = medalIds.getOrNull(activeSlotIndex)
  val activeMedal = allMedals.find { it.id == activeMedalId } ?: allMedals.firstOrNull()

  val isSlotResonant = activeMedal != null &&
    activeMedal.attributeEnum == slotConfig.requiredAttribute &&
    activeMedal.alignmentEnum == slotConfig.requiredAlignment
  val activeSlotMultiplier = if (isSlotResonant) {
    slotConfig.matchedSlotMultiplier
  } else {
    slotConfig.baseSlotMultiplier
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = DeepCrystalBlue),
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.5.dp, AstralBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      // Top Row: Hero Active Command Header + KHUX Special Gauge (SP Pips)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${heroState.hero.name} (${heroState.hero.jobClassName}) • ${heroState.equippedWeaponName.ifBlank { heroState.hero.archetypeEnum.weaponName }}",
            style = MaterialTheme.typography.titleMedium,
            color = RelicGold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "Slot ${activeSlotIndex + 1}/5 [${"%.2f".format(activeSlotMultiplier)}x ${slotConfig.requiredAttribute.shortCode}]" +
              (if (isSlotResonant) " ★ RESONANCE!" else "") +
              (if (heroState.shieldWallSecondsRemaining > 0f) " • WALL ${heroState.shieldWallSecondsRemaining.toInt()}s" else ""),
            style = MaterialTheme.typography.labelSmall,
            color = if (heroState.shieldWallSecondsRemaining > 0f) HpEmerald else if (isSlotResonant) SynergyAzure else MutedSilver,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          if (heroState.activeSetBonuses.isNotEmpty()) {
            Text(
              text = heroState.activeSetBonuses.first(),
              style = MaterialTheme.typography.labelSmall,
              color = MythrilCyan,
              fontSize = 10.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        // KHUX Special Attack Gauge (SP)
        Surface(
          color = VoidNavy,
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, MythrilCyan.copy(alpha = 0.6f))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Bolt,
              contentDescription = "Special Gauge",
              tint = MythrilCyan,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "SP $currentSp / $maxSp",
              style = MaterialTheme.typography.labelLarge,
              color = MythrilCyan
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // 5-Slot Rotating Medal Wheel Strip
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        itemsIndexed(DefaultDeckSlots) { slotIdx, slot ->
          val mId = medalIds.getOrNull(slotIdx)
          val medal = allMedals.find { it.id == mId } ?: allMedals.firstOrNull()
          val isCurrentWheelSlot = slotIdx == activeSlotIndex
          if (medal != null) {
            MedalWheelSlotItem(
              slotIndex = slotIdx,
              medal = medal,
              isActive = isCurrentWheelSlot,
              onClick = onRotateWheel
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Active Medal Details Banner
      if (activeMedal != null) {
        Surface(
          color = VoidNavy.copy(alpha = 0.85f),
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, activeMedal.attributeEnum.color.copy(alpha = 0.65f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${activeMedal.name} (${"★".repeat(activeMedal.rarityStars)})",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1
              )
              Text(
                text = "${activeMedal.elementEnum.label} • ${activeMedal.hitCount} Hits • Guilt +${activeMedal.guiltBonusPercent}% • ${activeMedal.effectTypeEnum.description}",
                style = MaterialTheme.typography.labelSmall,
                color = MutedSilver,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "${activeMedal.spCost} SP",
              style = MaterialTheme.typography.labelLarge,
              color = if (currentSp >= activeMedal.spCost) RelicGold else DangerCrimson
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Multi-RPG Active Skills & Gear-Granted Arts Strip
      if (heroState.availableSkills.isNotEmpty()) {
        Text(
          text = "RPG SKILLS & SET ARTS (Job + Socketed Materia + Relic Skills):",
          style = MaterialTheme.typography.labelSmall,
          color = MythrilCyan,
          fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("battle_skills_strip")
        ) {
          itemsIndexed(heroState.availableSkills, key = { _, sk -> sk.id }) { _, skill ->
            val cooldown = heroState.skillCooldowns[skill.id] ?: 0
            val effectiveCost = (skill.spCost - heroState.spCostReduction).coerceAtLeast(0)
            val canCast = heroState.isAtbReady && cooldown == 0 && currentSp >= effectiveCost
            val sys = skill.rpgSystemEnum

            Surface(
              color = if (canCast) skill.elementEnum.color.copy(alpha = 0.22f) else VoidNavy.copy(alpha = 0.7f),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(
                width = 1.dp,
                color = if (canCast) skill.elementEnum.color else AstralBorder
              ),
              modifier = Modifier
                .width(142.dp)
                .clickable(enabled = canCast) { onExecuteSkill(skill) }
                .testTag("btn_battle_skill_${skill.id}")
            ) {
              Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "${sys.shortTag} R${skill.rankLevel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = sys.accentColor,
                    fontSize = 9.sp
                  )
                  Text(
                    text = if (cooldown > 0) "CD ${cooldown}T" else "${effectiveCost}SP",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (cooldown > 0) DangerCrimson else if (currentSp >= effectiveCost) RelicGold else DangerCrimson,
                    fontSize = 10.sp
                  )
                }
                Text(
                  text = skill.name,
                  style = MaterialTheme.typography.labelLarge,
                  color = if (canCast) Color.White else MutedSilver,
                  fontSize = 11.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "${skill.elementEnum.label} • ${"%.1f".format(skill.effectiveMultiplier)}x",
                  style = MaterialTheme.typography.labelSmall,
                  color = MutedSilver,
                  fontSize = 9.sp
                )
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
      }

      // 4 Primary Tactile RPG Combat Actions (Attack, Medal Burst, Rotate/Guard, Soul Break)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // 1. Basic Tap Attack (+2 SP)
        Button(
          onClick = onBasicAttack,
          enabled = heroState.isAtbReady,
          colors = ButtonDefaults.buttonColors(
            containerColor = AstralPanel,
            contentColor = Color.White
          ),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("btn_basic_attack")
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("STRIKE", style = MaterialTheme.typography.labelLarge)
            Text("+2 SP", style = MaterialTheme.typography.labelSmall, color = HpEmerald, fontSize = 10.sp)
          }
        }

        // 2. KHUX Special Medal Burst
        val spCost = ((activeMedal?.spCost ?: 2) - heroState.spCostReduction).coerceAtLeast(0)
        Button(
          onClick = onMedalBurst,
          enabled = heroState.isAtbReady && currentSp >= spCost,
          colors = ButtonDefaults.buttonColors(
            containerColor = activeMedal?.attributeEnum?.color ?: PowerCrimson,
            contentColor = VoidNavy
          ),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
          modifier = Modifier
            .weight(1.25f)
            .testTag("btn_medal_burst")
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("MEDAL BURST", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text("-$spCost SP • ${activeMedal?.hitCount ?: 6}x Hit", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
          }
        }

        // 3. Defend / Guard (+SP & Heal)
        OutlinedButton(
          onClick = onDefend,
          enabled = heroState.isAtbReady,
          border = BorderStroke(1.dp, MythrilCyan),
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
          modifier = Modifier
            .weight(0.85f)
            .testTag("btn_defend")
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Shield, contentDescription = "Defend", tint = MythrilCyan, modifier = Modifier.size(15.dp))
            Text("GUARD", style = MaterialTheme.typography.labelSmall, color = MythrilCyan, fontSize = 10.sp)
          }
        }

        // 4. FFRK Soul Break Overdrive
        Button(
          onClick = onSoulBreak,
          enabled = heroState.isAlive && heroState.soulBreakBars >= 1,
          colors = ButtonDefaults.buttonColors(
            containerColor = SoulBreakMagenta,
            contentColor = Color.White
          ),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
          modifier = Modifier
            .weight(1.1f)
            .testTag("btn_soul_break")
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SOUL BREAK", style = MaterialTheme.typography.labelLarge, fontSize = 11.sp)
            Text("${heroState.soulBreakBars}/3 Bars", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
          }
        }
      }
    }
  }
}

@Composable
private fun MedalWheelSlotItem(
  slotIndex: Int,
  medal: MedalEntity,
  isActive: Boolean,
  onClick: () -> Unit
) {
  Surface(
    color = if (isActive) medal.attributeEnum.color.copy(alpha = 0.28f) else VoidNavy,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(
      width = if (isActive) 2.dp else 1.dp,
      color = if (isActive) RelicGold else medal.attributeEnum.color.copy(alpha = 0.55f)
    ),
    modifier = Modifier
      .width(102.dp)
      .clickable(onClick = onClick)
      .testTag("medal_wheel_slot_$slotIndex")
  ) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(medal.attributeEnum.color),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = medal.attributeEnum.shortCode.take(1),
            style = MaterialTheme.typography.labelSmall,
            color = VoidNavy,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
          )
        }
        Text(
          text = "${medal.spCost}SP",
          style = MaterialTheme.typography.labelSmall,
          color = RelicGold,
          fontSize = 10.sp
        )
      }
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = medal.name,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = "${"%.1f".format(medal.baseMultiplier)}x • +${medal.guiltBonusPercent}%",
        style = MaterialTheme.typography.labelSmall,
        color = MutedSilver,
        fontSize = 9.sp
      )
    }
  }
}

@Composable
private fun BattleOutcomeOverlay(
  battleState: BattleArenaState,
  allStages: List<RealmStageEntity>,
  onRestartEncounter: () -> Unit,
  onSelectNextStage: (RealmStageEntity) -> Unit,
  modifier: Modifier = Modifier
) {
  val isVictory = battleState.outcome == BattleOutcome.VICTORY
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .background(VoidNavy.copy(alpha = 0.88f))
      .padding(16.dp)
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = DeepCrystalBlue),
      border = BorderStroke(2.dp, if (isVictory) RelicGold else DangerCrimson),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
      ) {
        Text(
          text = if (isVictory) "CHAMPION MASTERY!" else "PARTY FALLEN",
          style = MaterialTheme.typography.headlineMedium,
          color = if (isVictory) RelicGold else DangerCrimson
        )
        if (isVictory) {
          Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 4.dp)
          ) {
            for (s in 1..3) {
              Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Mastery Star",
                tint = if (s <= battleState.starsEarned) RelicGold else MutedSilver,
                modifier = Modifier.size(26.dp)
              )
            }
          }
        }
        Text(
          text = "Turns: ${battleState.turnsTaken} • Peak Chain: ${battleState.peakChainCount}x • Total DMG: %,d".format(
            battleState.totalDamageDealt
          ),
          style = MaterialTheme.typography.labelSmall,
          color = MythrilCyan
        )
        if (isVictory) {
          Text(
            text = "Rewards: +25 Mythril Shards & Realm Record Mastery!",
            style = MaterialTheme.typography.bodyMedium,
            color = HpEmerald,
            modifier = Modifier.padding(top = 4.dp)
          )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedButton(
            onClick = onRestartEncounter,
            border = BorderStroke(1.dp, MythrilCyan)
          ) {
            Text("Replay Stage", color = MythrilCyan)
          }
          if (allStages.size > 1) {
            val currentIdx = allStages.indexOfFirst { it.id == battleState.stage?.id }
            val nextStage = allStages[(currentIdx + 1) % allStages.size]
            Button(
              onClick = { onSelectNextStage(nextStage) },
              colors = ButtonDefaults.buttonColors(containerColor = RelicGold, contentColor = VoidNavy)
            ) {
              Text("Next: ${nextStage.stageCode}")
            }
          }
        }
      }
    }
  }
}
