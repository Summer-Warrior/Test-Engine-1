package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EquipmentEntity
import com.example.model.EquipmentSlotType
import com.example.model.HeroEntity
import com.example.model.RpgSystemOrigin
import com.example.model.SkillCategory
import com.example.model.SkillEntity
import com.example.ui.theme.AstralBorder
import com.example.ui.theme.AstralPanel
import com.example.ui.theme.DeepCrystalBlue
import com.example.ui.theme.HpEmerald
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.MythrilCyan
import com.example.ui.theme.RelicGold
import com.example.ui.theme.SoulBreakMagenta
import com.example.ui.theme.SynergyAzure
import com.example.ui.theme.VoidNavy

@Composable
fun ArmoryVaultView(
  equipmentList: List<EquipmentEntity>,
  skills: List<SkillEntity>,
  heroes: List<HeroEntity>,
  activeStageRealm: String,
  mythrilShards: Int,
  onUpgradeEquipment: (EquipmentEntity) -> Unit,
  onReforgeEquipment: (EquipmentEntity) -> Unit,
  onSocketSkill: (EquipmentEntity, Int) -> Unit,
  onRemoveSocket: (EquipmentEntity, Int) -> Unit,
  onEquipToHero: (HeroEntity, EquipmentEntity) -> Unit,
  modifier: Modifier = Modifier
) {
  var slotFilter by remember { mutableStateOf<EquipmentSlotType?>(null) }
  var systemFilter by remember { mutableStateOf<RpgSystemOrigin?>(null) }
  var selectedTargetHeroId by remember(heroes) {
    mutableStateOf(heroes.firstOrNull { it.inParty }?.id ?: heroes.firstOrNull()?.id)
  }
  var socketingEquipmentId by remember { mutableStateOf<Int?>(null) }

  val filteredEquipment = remember(equipmentList, slotFilter, systemFilter) {
    equipmentList.filter { eq ->
      (slotFilter == null || eq.slotTypeEnum == slotFilter) &&
        (systemFilter == null || eq.rpgSystemEnum == systemFilter)
    }
  }
  val targetHero = heroes.find { it.id == selectedTargetHeroId } ?: heroes.firstOrNull()

  LazyColumn(
    modifier = modifier.testTag("armory_vault_view"),
    contentPadding = PaddingValues(vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    item {
      Surface(
        color = DeepCrystalBlue,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, RelicGold.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "MULTI-RPG ARMORY VAULT",
              style = MaterialTheme.typography.titleMedium,
              color = RelicGold
            )
            Text(
              text = "Mythril: $mythrilShards",
              style = MaterialTheme.typography.labelLarge,
              color = MythrilCyan
            )
          }
          Text(
            text = "Smithy (+5 Lv), Reforge (+10%/tier), FFVII Materia Sockets & Cross-RPG Armor Sets.",
            style = MaterialTheme.typography.labelSmall,
            color = MutedSilver
          )
          Spacer(modifier = Modifier.height(8.dp))

          // Target Hero Quick-Equip Selector
          Text("Quick-Equip Target Hero:", style = MaterialTheme.typography.labelSmall, color = MythrilCyan)
          Spacer(modifier = Modifier.height(4.dp))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(heroes, key = { it.id }) { hero ->
              FilterPill(
                label = hero.name,
                selected = targetHero?.id == hero.id,
                color = HpEmerald,
                onClick = { selectedTargetHeroId = hero.id }
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Slot Filter Row
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
              FilterPill(
                label = "All Slots (${equipmentList.size})",
                selected = slotFilter == null,
                color = RelicGold,
                onClick = { slotFilter = null }
              )
            }
            items(EquipmentSlotType.entries) { slot ->
              FilterPill(
                label = slot.label,
                selected = slotFilter == slot,
                color = RelicGold,
                onClick = { slotFilter = if (slotFilter == slot) null else slot }
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          // RPG Tradition Filter Row
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
              FilterPill(
                label = "All RPGs",
                selected = systemFilter == null,
                color = MythrilCyan,
                onClick = { systemFilter = null }
              )
            }
            items(RpgSystemOrigin.entries) { sys ->
              FilterPill(
                label = sys.shortTag,
                selected = systemFilter == sys,
                color = sys.accentColor,
                onClick = { systemFilter = if (systemFilter == sys) null else sys }
              )
            }
          }
        }
      }
    }

    items(filteredEquipment, key = { it.id }) { eq ->
      val hasSynergy = eq.originRealm.equals(activeStageRealm, ignoreCase = true) || eq.originRealm == "Record Keep"
      val sys = eq.rpgSystemEnum
      val socketedIds = eq.socketedSkillIdList()
      val grantedSkill = eq.grantedSkillId?.let { gid -> skills.find { it.id == gid } }
      val wearer = heroes.find { it.id == eq.equippedByHeroId }
      val isSocketing = socketingEquipmentId == eq.id

      Card(
        colors = CardDefaults.cardColors(containerColor = DeepCrystalBlue),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (hasSynergy) SynergyAzure else sys.accentColor.copy(alpha = 0.7f)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("armory_item_${eq.id}")
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  color = sys.accentColor.copy(alpha = 0.2f),
                  shape = RoundedCornerShape(6.dp),
                  border = BorderStroke(1.dp, sys.accentColor)
                ) {
                  Text(
                    text = sys.shortTag,
                    style = MaterialTheme.typography.labelSmall,
                    color = sys.accentColor,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                  )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "${eq.name} (Lv.${eq.upgradeLevel} • ${eq.rarityStars}★)",
                  style = MaterialTheme.typography.titleMedium,
                  color = Color.White,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              Text(
                text = "${eq.weaponArmorClassEnum.label} • ${eq.originRealm}" +
                  (if (eq.setSeriesName.isNotBlank()) " • Set: ${eq.setSeriesName}" else "") +
                  (if (eq.reforgeTier > 0) " • Reforge +${eq.reforgeTier}" else ""),
                style = MaterialTheme.typography.labelSmall,
                color = if (hasSynergy) SynergyAzure else MutedSilver
              )
            }
            if (wearer != null) {
              Surface(
                color = HpEmerald.copy(alpha = 0.18f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, HpEmerald)
              ) {
                Text(
                  text = wearer.name,
                  style = MaterialTheme.typography.labelSmall,
                  color = HpEmerald,
                  fontSize = 10.sp,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Stats + Multi-RPG Perk Badges
          Text(
            text = "HP+${eq.bonusHp} ATK+${eq.bonusAtk} MAG+${eq.bonusMag} DEF+${eq.bonusDef} RES+${eq.bonusRes} SPD+${eq.bonusSpd}",
            style = MaterialTheme.typography.labelSmall,
            color = RelicGold
          )
          val perkBadges = buildList {
            if (eq.critRateBonus > 0f) add("Crit +${(eq.critRateBonus * 100).toInt()}%")
            if (eq.elementResistPercent > 0) add("${eq.elementResistEnum.label} Res ${eq.elementResistPercent}%")
            if (eq.lifestealPercent > 0) add("Lifesteal ${eq.lifestealPercent}%")
            if (eq.counterChancePercent > 0) add("Counter ${eq.counterChancePercent}%")
            if (eq.spCostReduction > 0) add("-${eq.spCostReduction} SP Cost")
            if (grantedSkill != null) add("Grants: ${grantedSkill.name}")
          }
          if (perkBadges.isNotEmpty()) {
            Text(
              text = perkBadges.joinToString(" • "),
              style = MaterialTheme.typography.labelSmall,
              color = MythrilCyan,
              modifier = Modifier.padding(top = 2.dp)
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          // FFVII Materia Socket Bar
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "Materia (${socketedIds.size}/${eq.materiaSocketsMax}):",
              style = MaterialTheme.typography.labelSmall,
              color = HpEmerald
            )
            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(5.dp),
              modifier = Modifier.weight(1f)
            ) {
              itemsIndexed(socketedIds) { idx, sId ->
                val orbSkill = skills.find { it.id == sId }
                Surface(
                  color = HpEmerald.copy(alpha = 0.16f),
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(1.dp, HpEmerald)
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 6.dp, end = 2.dp, top = 2.dp, bottom = 2.dp)
                  ) {
                    Text(
                      text = orbSkill?.name ?: "Orb #$sId",
                      style = MaterialTheme.typography.labelSmall,
                      color = HpEmerald,
                      fontSize = 10.sp
                    )
                    IconButton(
                      onClick = { onRemoveSocket(eq, idx) },
                      modifier = Modifier.size(28.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Unsocket Materia",
                        tint = HpEmerald,
                        modifier = Modifier.size(12.dp)
                      )
                    }
                  }
                }
              }
              item {
                OutlinedButton(
                  onClick = { socketingEquipmentId = if (isSocketing) null else eq.id },
                  border = BorderStroke(1.dp, HpEmerald),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                  modifier = Modifier.height(30.dp)
                ) {
                  Icon(Icons.Default.AddCircle, contentDescription = "Socket Materia", tint = HpEmerald, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(if (isSocketing) "Close" else "Socket", style = MaterialTheme.typography.labelSmall, color = HpEmerald, fontSize = 10.sp)
                }
              }
            }
          }

          if (isSocketing) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
              color = VoidNavy,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, HpEmerald),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                Text(
                  text = "Tap any Skill / Materia Orb to socket (+2% ATK/MAG & +2 SPD per orb):",
                  style = MaterialTheme.typography.labelSmall,
                  color = HpEmerald
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  items(skills, key = { it.id }) { sk ->
                    Surface(
                      color = AstralPanel,
                      shape = RoundedCornerShape(8.dp),
                      border = BorderStroke(1.dp, sk.elementEnum.color),
                      modifier = Modifier
                        .width(142.dp)
                        .clickable {
                          onSocketSkill(eq, sk.id)
                          socketingEquipmentId = null
                        }
                    ) {
                      Column(modifier = Modifier.padding(6.dp)) {
                        Text(
                          text = sk.name,
                          style = MaterialTheme.typography.labelSmall,
                          color = Color.White,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                        Text(
                          text = "${sk.rpgSystemEnum.shortTag} • ${sk.categoryEnum.label}",
                          style = MaterialTheme.typography.labelSmall,
                          color = sk.elementEnum.color,
                          fontSize = 9.sp
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Action Buttons Row (48dp touch friendly)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            OutlinedButton(
              onClick = { onUpgradeEquipment(eq) },
              enabled = eq.upgradeLevel < 50,
              border = BorderStroke(1.dp, MythrilCyan),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
            ) {
              Icon(Icons.Default.Upgrade, contentDescription = "Upgrade Relic", tint = MythrilCyan, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text("Lv +5", style = MaterialTheme.typography.labelSmall, color = MythrilCyan)
            }

            OutlinedButton(
              onClick = { onReforgeEquipment(eq) },
              enabled = eq.reforgeTier < 3 && mythrilShards >= 25,
              border = BorderStroke(1.dp, SoulBreakMagenta),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
              modifier = Modifier
                .weight(1.1f)
                .height(48.dp)
                .testTag("btn_reforge_${eq.id}")
            ) {
              Icon(Icons.Default.AutoAwesome, contentDescription = "Reforge Tier", tint = SoulBreakMagenta, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = if (eq.reforgeTier >= 3) "MAX +3" else "Reforge (-25)",
                style = MaterialTheme.typography.labelSmall,
                color = SoulBreakMagenta
              )
            }

            if (targetHero != null) {
              Button(
                onClick = { onEquipToHero(targetHero, eq) },
                colors = ButtonDefaults.buttonColors(containerColor = RelicGold, contentColor = VoidNavy),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier
                  .weight(1.15f)
                  .height(48.dp)
                  .testTag("btn_quick_equip_${eq.id}")
              ) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Equip to Hero", modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "Equip ${targetHero.name}",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun SkillGrimoireView(
  skills: List<SkillEntity>,
  heroes: List<HeroEntity>,
  mythrilShards: Int,
  onHoneSkill: (SkillEntity) -> Unit,
  onEquipActiveSkill: (HeroEntity, Int, Int) -> Unit,
  onEquipPassiveSkill: (HeroEntity, Int, Int) -> Unit,
  modifier: Modifier = Modifier
) {
  var categoryFilter by remember { mutableStateOf<SkillCategory?>(null) }
  var selectedHeroId by remember(heroes) {
    mutableStateOf(heroes.firstOrNull { it.inParty }?.id ?: heroes.firstOrNull()?.id)
  }
  val targetHero = heroes.find { it.id == selectedHeroId } ?: heroes.firstOrNull()

  val filteredSkills = remember(skills, categoryFilter) {
    skills.filter { categoryFilter == null || it.categoryEnum == categoryFilter }
  }

  LazyColumn(
    modifier = modifier.testTag("skill_grimoire_view"),
    contentPadding = PaddingValues(vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    item {
      Surface(
        color = DeepCrystalBlue,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SoulBreakMagenta.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "SKILL GRIMOIRE & JOB ARTS",
              style = MaterialTheme.typography.titleMedium,
              color = SoulBreakMagenta
            )
            Text(
              text = "Mythril: $mythrilShards",
              style = MaterialTheme.typography.labelLarge,
              color = RelicGold
            )
          }
          Text(
            text = "Hone FFRK/FFT/KH/FFVI abilities (R1..R5) and assign Active Techs or Passive Traits to heroes.",
            style = MaterialTheme.typography.labelSmall,
            color = MutedSilver
          )
          Spacer(modifier = Modifier.height(8.dp))

          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(heroes, key = { it.id }) { h ->
              FilterPill(
                label = "${h.name} (${h.jobClassName})",
                selected = targetHero?.id == h.id,
                color = HpEmerald,
                onClick = { selectedHeroId = h.id }
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
              FilterPill(
                label = "All (${skills.size})",
                selected = categoryFilter == null,
                color = SoulBreakMagenta,
                onClick = { categoryFilter = null }
              )
            }
            items(SkillCategory.entries) { cat ->
              FilterPill(
                label = cat.label,
                selected = categoryFilter == cat,
                color = SoulBreakMagenta,
                onClick = { categoryFilter = if (categoryFilter == cat) null else cat }
              )
            }
          }
        }
      }
    }

    items(filteredSkills, key = { it.id }) { skill ->
      val origin = skill.rpgSystemEnum
      val isPassive = skill.categoryEnum.isPassive

      Card(
        colors = CardDefaults.cardColors(containerColor = DeepCrystalBlue),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, origin.accentColor.copy(alpha = 0.7f)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("skill_vault_card_${skill.id}")
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Surface(
                color = origin.accentColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, origin.accentColor)
              ) {
                Text(
                  text = origin.shortTag,
                  style = MaterialTheme.typography.labelSmall,
                  color = origin.accentColor,
                  fontSize = 9.sp,
                  modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
              }
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "${skill.name} (R${skill.rankLevel}/${skill.maxRank})",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Text(
              text = if (isPassive) "PASSIVE" else "${skill.spCost} SP • CD ${skill.cooldownTurns}T",
              style = MaterialTheme.typography.labelSmall,
              color = if (isPassive) HpEmerald else RelicGold
            )
          }

          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = skill.description,
            style = MaterialTheme.typography.bodySmall,
            color = MutedSilver
          )
          if (!isPassive) {
            Text(
              text = "${skill.categoryEnum.label} • ${skill.elementEnum.label} • ${"%.2f".format(skill.effectiveMultiplier)}x × ${skill.hitCount} Hits • ${skill.effectTypeEnum.label}",
              style = MaterialTheme.typography.labelSmall,
              color = skill.elementEnum.color,
              modifier = Modifier.padding(top = 3.dp)
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            OutlinedButton(
              onClick = { onHoneSkill(skill) },
              enabled = skill.rankLevel < skill.maxRank && mythrilShards >= 20,
              border = BorderStroke(1.dp, RelicGold),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("btn_hone_skill_${skill.id}")
            ) {
              Icon(Icons.Default.Build, contentDescription = "Hone Skill", tint = RelicGold, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (skill.rankLevel >= skill.maxRank) "MAX R${skill.maxRank}" else "Hone R${skill.rankLevel + 1} (-20)",
                style = MaterialTheme.typography.labelSmall,
                color = RelicGold
              )
            }

            if (targetHero != null) {
              if (isPassive) {
                for (pIdx in 0..1) {
                  Button(
                    onClick = { onEquipPassiveSkill(targetHero, pIdx, skill.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = HpEmerald, contentColor = VoidNavy),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                      .weight(0.7f)
                      .height(48.dp)
                  ) {
                    Text("Set P${pIdx + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                  }
                }
              } else {
                for (sIdx in 0..2) {
                  Button(
                    onClick = { onEquipActiveSkill(targetHero, sIdx, skill.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = MythrilCyan, contentColor = VoidNavy),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier
                      .weight(0.55f)
                      .height(48.dp)
                  ) {
                    Text("Slot ${sIdx + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FilterPill(
  label: String,
  selected: Boolean,
  color: Color,
  onClick: () -> Unit
) {
  Surface(
    color = if (selected) color.copy(alpha = 0.24f) else VoidNavy,
    shape = RoundedCornerShape(8.dp),
    border = BorderStroke(1.dp, if (selected) color else AstralBorder),
    modifier = Modifier
      .clickable(onClick = onClick)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = if (selected) color else MutedSilver,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
    )
  }
}
