package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import com.example.model.DefaultDeckSlots
import com.example.model.EquipmentEntity
import com.example.model.EquipmentSlotType
import com.example.model.HeroEntity
import com.example.model.JobClassPresets
import com.example.model.MedalEntity
import com.example.model.RpgLoadoutCalculator
import com.example.model.SkillEntity
import com.example.ui.components.ArmoryVaultView
import com.example.ui.components.SkillGrimoireView
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

private enum class PartySubTab(val label: String, val tag: String) {
  LOADOUTS("Hero Loadouts", "tab_party_loadouts"),
  ARMORY("Armory Vault", "tab_party_armory"),
  SKILLS("Skill Grimoire", "tab_party_skills")
}

@Composable
fun PartyScreen(
  heroes: List<HeroEntity>,
  equipmentList: List<EquipmentEntity>,
  medals: List<MedalEntity>,
  activeStageRealm: String,
  onToggleHeroInParty: (HeroEntity) -> Unit,
  onLevelUpHero: (HeroEntity) -> Unit,
  onDiveRecordBoard: (HeroEntity) -> Unit,
  onEquipRelicToHero: (HeroEntity, EquipmentEntity) -> Unit,
  onUpgradeEquipment: (EquipmentEntity) -> Unit,
  onEquipMedalToSlot: (HeroEntity, Int, Int) -> Unit,
  skills: List<SkillEntity> = emptyList(),
  mythrilShards: Int = 250,
  onReforgeEquipment: (EquipmentEntity) -> Unit = {},
  onSocketSkillIntoEquipment: (EquipmentEntity, Int) -> Unit = { _, _ -> },
  onRemoveSocketFromEquipment: (EquipmentEntity, Int) -> Unit = { _, _ -> },
  onEquipSkillToSlot: (HeroEntity, Int, Int) -> Unit = { _, _, _ -> },
  onEquipPassiveToSlot: (HeroEntity, Int, Int) -> Unit = { _, _, _ -> },
  onChangeHeroJobClass: (HeroEntity, String) -> Unit = { _, _ -> },
  onHoneSkill: (SkillEntity) -> Unit = {},
  modifier: Modifier = Modifier
) {
  var activeSubTab by remember { mutableStateOf(PartySubTab.LOADOUTS) }
  var editingHeroId by remember { mutableStateOf<Int?>(null) }
  var editingSlotIndex by remember { mutableIntStateOf(0) }
  var editingEquipmentHeroId by remember { mutableStateOf<Int?>(null) }
  var editingEquipmentSlotType by remember { mutableStateOf(EquipmentSlotType.WEAPON) }
  var editingSkillHeroId by remember { mutableStateOf<Int?>(null) }
  var editingSkillSlotIndex by remember { mutableIntStateOf(0) }
  var editingSkillIsPassive by remember { mutableStateOf(false) }
  var editingJobHeroId by remember { mutableStateOf<Int?>(null) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 12.dp)
      .testTag("party_screen")
  ) {
    // Sub-Tab Switcher: Hero Loadouts | Armory Vault | Skill Grimoire
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 8.dp, bottom = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      PartySubTab.entries.forEach { tab ->
        val selected = activeSubTab == tab
        Surface(
          color = if (selected) RelicGold else DeepCrystalBlue,
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, if (selected) RelicGold else AstralBorder),
          modifier = Modifier
            .weight(1f)
            .clickable { activeSubTab = tab }
            .testTag(tab.tag)
        ) {
          Text(
            text = tab.label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) VoidNavy else MutedSilver,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
          )
        }
      }
    }

    when (activeSubTab) {
      PartySubTab.ARMORY -> {
        ArmoryVaultView(
          equipmentList = equipmentList,
          skills = skills,
          heroes = heroes,
          activeStageRealm = activeStageRealm,
          mythrilShards = mythrilShards,
          onUpgradeEquipment = onUpgradeEquipment,
          onReforgeEquipment = onReforgeEquipment,
          onSocketSkill = onSocketSkillIntoEquipment,
          onRemoveSocket = onRemoveSocketFromEquipment,
          onEquipToHero = onEquipRelicToHero,
          modifier = Modifier.fillMaxSize()
        )
      }

      PartySubTab.SKILLS -> {
        SkillGrimoireView(
          skills = skills,
          heroes = heroes,
          mythrilShards = mythrilShards,
          onHoneSkill = onHoneSkill,
          onEquipActiveSkill = onEquipSkillToSlot,
          onEquipPassiveSkill = onEquipPassiveToSlot,
          modifier = Modifier.fillMaxSize()
        )
      }

      PartySubTab.LOADOUTS -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          item {
            Surface(
              color = DeepCrystalBlue,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, AstralBorder),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = "MULTI-RPG LOADOUTS, GEAR SETS & JOB ARTS",
                  style = MaterialTheme.typography.titleLarge,
                  color = RelicGold
                )
                Text(
                  text = "Realm: $activeStageRealm • Equip Weapons, Armor & Accessories, socket Materia, and stack 2pc/3pc Set Bonuses.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MutedSilver
                )
              }
            }
          }

          items(heroes, key = { it.id }) { hero ->
            val hasSynergy = hero.realm.equals(activeStageRealm, ignoreCase = true) || hero.realm == "Record Keep"
            val isEditingThisHero = editingHeroId == hero.id
            val isEditingGearForThisHero = editingEquipmentHeroId == hero.id
            val isEditingSkillForThisHero = editingSkillHeroId == hero.id
            val isEditingJobForThisHero = editingJobHeroId == hero.id

            HeroRosterCard(
              hero = hero,
              equipmentList = equipmentList,
              medals = medals,
              skills = skills,
              activeStageRealm = activeStageRealm,
              hasSynergy = hasSynergy,
              isEditingSlot = isEditingThisHero,
              selectedSlotIndex = editingSlotIndex,
              isEditingGear = isEditingGearForThisHero,
              selectedGearSlotType = editingEquipmentSlotType,
              isEditingSkill = isEditingSkillForThisHero,
              selectedSkillSlotIndex = editingSkillSlotIndex,
              editingSkillIsPassive = editingSkillIsPassive,
              isEditingJob = isEditingJobForThisHero,
              onToggleParty = { onToggleHeroInParty(hero) },
              onLevelUp = { onLevelUpHero(hero) },
              onDiveBoard = { onDiveRecordBoard(hero) },
              onToggleJobPicker = {
                editingJobHeroId = if (isEditingJobForThisHero) null else hero.id
              },
              onPickJobClass = { job ->
                onChangeHeroJobClass(hero, job)
                editingJobHeroId = null
              },
              onSelectGearSlot = { slotType ->
                if (editingEquipmentHeroId == hero.id && editingEquipmentSlotType == slotType) {
                  editingEquipmentHeroId = null
                } else {
                  editingEquipmentHeroId = hero.id
                  editingEquipmentSlotType = slotType
                }
              },
              onPickRelicEquipment = { eq ->
                onEquipRelicToHero(hero, eq)
                editingEquipmentHeroId = null
              },
              onUpgradeEquipment = onUpgradeEquipment,
              onSelectSkillSlot = { slotIdx, isPassive ->
                if (editingSkillHeroId == hero.id &&
                  editingSkillSlotIndex == slotIdx &&
                  editingSkillIsPassive == isPassive
                ) {
                  editingSkillHeroId = null
                } else {
                  editingSkillHeroId = hero.id
                  editingSkillSlotIndex = slotIdx
                  editingSkillIsPassive = isPassive
                }
              },
              onPickSkill = { skillId ->
                if (editingSkillIsPassive) {
                  onEquipPassiveToSlot(hero, editingSkillSlotIndex, skillId)
                } else {
                  onEquipSkillToSlot(hero, editingSkillSlotIndex, skillId)
                }
                editingSkillHeroId = null
              },
              onSelectSlotToEdit = { slotIdx ->
                if (editingHeroId == hero.id && editingSlotIndex == slotIdx) {
                  editingHeroId = null
                } else {
                  editingHeroId = hero.id
                  editingSlotIndex = slotIdx
                }
              },
              onPickMedalForSlot = { medalId ->
                onEquipMedalToSlot(hero, editingSlotIndex, medalId)
                editingHeroId = null
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun HeroRosterCard(
  hero: HeroEntity,
  equipmentList: List<EquipmentEntity>,
  medals: List<MedalEntity>,
  skills: List<SkillEntity>,
  activeStageRealm: String,
  hasSynergy: Boolean,
  isEditingSlot: Boolean,
  selectedSlotIndex: Int,
  isEditingGear: Boolean,
  selectedGearSlotType: EquipmentSlotType,
  isEditingSkill: Boolean,
  selectedSkillSlotIndex: Int,
  editingSkillIsPassive: Boolean,
  isEditingJob: Boolean,
  onToggleParty: () -> Unit,
  onLevelUp: () -> Unit,
  onDiveBoard: () -> Unit,
  onToggleJobPicker: () -> Unit,
  onPickJobClass: (String) -> Unit,
  onSelectGearSlot: (EquipmentSlotType) -> Unit,
  onPickRelicEquipment: (EquipmentEntity) -> Unit,
  onUpgradeEquipment: (EquipmentEntity) -> Unit,
  onSelectSkillSlot: (Int, Boolean) -> Unit,
  onPickSkill: (Int) -> Unit,
  onSelectSlotToEdit: (Int) -> Unit,
  onPickMedalForSlot: (Int) -> Unit
) {
  val equippedIds = hero.equippedMedalIdList()
  val equippedWeapon = equipmentList.find { it.id == hero.equippedWeaponId }
  val equippedArmor = equipmentList.find { it.id == hero.equippedArmorId }
  val equippedAccessory = equipmentList.find { it.id == hero.equippedAccessoryId }
  val loadout = remember(hero, equippedWeapon, equippedArmor, equippedAccessory, skills, activeStageRealm) {
    RpgLoadoutCalculator.computeHeroLoadout(
      hero = hero,
      weapon = equippedWeapon,
      armor = equippedArmor,
      accessory = equippedAccessory,
      allSkills = skills,
      activeStageRealm = activeStageRealm
    )
  }

  Card(
    colors = CardDefaults.cardColors(
      containerColor = if (hero.inParty) DeepCrystalBlue else VoidNavy
    ),
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(
      width = if (hero.inParty) 1.5.dp else 1.dp,
      color = when {
        hasSynergy && hero.inParty -> SynergyAzure
        hero.inParty -> RelicGold
        else -> AstralBorder
      }
    ),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("hero_card_${hero.id}")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (hero.portraitUri.isNotBlank()) {
          val imgModel: Any = if (File(hero.portraitUri).exists()) File(hero.portraitUri) else hero.portraitUri
          AsyncImage(
            model = imgModel,
            contentDescription = "${hero.name} Portrait Art",
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .size(46.dp)
              .clip(RoundedCornerShape(10.dp))
          )
          Spacer(modifier = Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "${hero.name} (Lv.${hero.level})",
              style = MaterialTheme.typography.titleLarge,
              color = Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              color = if (hasSynergy) SynergyAzure.copy(alpha = 0.2f) else AstralPanel,
              shape = RoundedCornerShape(6.dp),
              border = BorderStroke(1.dp, if (hasSynergy) SynergyAzure else AstralBorder)
            ) {
              Text(
                text = if (hasSynergy) "${hero.realm} • SYN" else hero.realm,
                style = MaterialTheme.typography.labelSmall,
                color = if (hasSynergy) SynergyAzure else MutedSilver,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp)
          ) {
            Surface(
              color = MythrilCyan.copy(alpha = 0.16f),
              shape = RoundedCornerShape(6.dp),
              border = BorderStroke(1.dp, MythrilCyan.copy(alpha = 0.7f)),
              modifier = Modifier
                .clickable(onClick = onToggleJobPicker)
                .testTag("btn_job_class_${hero.id}")
            ) {
              Text(
                text = "Job: ${hero.jobClassName} ▾",
                style = MaterialTheme.typography.labelSmall,
                color = MythrilCyan,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Dive ${hero.recordDiveTier}/6",
              style = MaterialTheme.typography.labelSmall,
              color = MutedSilver
            )
          }
          if (hero.loreBio.isNotBlank()) {
            Text(
              text = hero.loreBio,
              style = MaterialTheme.typography.labelSmall,
              color = MythrilCyan,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              fontSize = 10.sp,
              modifier = Modifier.padding(top = 2.dp)
            )
          }
          val boundBadges = buildList {
            if (hero.spriteAssetId != null) add("Sprite")
            if (hero.model3dAssetId != null) add("3D Model")
            if (hero.attackSfxAssetId != null) add("SFX")
          }
          if (boundBadges.isNotEmpty()) {
            Text(
              text = "External Bindings: ${boundBadges.joinToString(" • ")}",
              style = MaterialTheme.typography.labelSmall,
              color = HpEmerald,
              fontSize = 10.sp
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
          OutlinedButton(
            onClick = onLevelUp,
            enabled = hero.level < 99,
            border = BorderStroke(1.dp, MythrilCyan),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.testTag("btn_level_up_${hero.id}")
          ) {
            Icon(Icons.Default.ArrowUpward, contentDescription = "Level Up", tint = MythrilCyan, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text("+5 Lv", style = MaterialTheme.typography.labelSmall, color = MythrilCyan)
          }

          OutlinedButton(
            onClick = onDiveBoard,
            enabled = hero.recordDiveTier < 6,
            border = BorderStroke(1.dp, SoulBreakMagenta),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.testTag("btn_dive_board_${hero.id}")
          ) {
            Icon(Icons.Default.AutoFixHigh, contentDescription = "Dive Record Sphere", tint = SoulBreakMagenta, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text("Dive", style = MaterialTheme.typography.labelSmall, color = SoulBreakMagenta)
          }

          Button(
            onClick = onToggleParty,
            colors = ButtonDefaults.buttonColors(
              containerColor = if (hero.inParty) HpEmerald.copy(alpha = 0.22f) else RelicGold,
              contentColor = if (hero.inParty) HpEmerald else VoidNavy
            ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.testTag("btn_toggle_party_${hero.id}")
          ) {
            Icon(
              imageVector = if (hero.inParty) Icons.Default.CheckCircle else Icons.Default.PersonAdd,
              contentDescription = "Party Status",
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = if (hero.inParty) "PARTY" else "JOIN",
              style = MaterialTheme.typography.labelSmall
            )
          }
        }
      }

      if (isEditingJob) {
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(JobClassPresets) { jobName ->
            val selectedJob = hero.jobClassName == jobName
            Surface(
              color = if (selectedJob) MythrilCyan.copy(alpha = 0.25f) else VoidNavy,
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, if (selectedJob) MythrilCyan else AstralBorder),
              modifier = Modifier.clickable { onPickJobClass(jobName) }
            ) {
              Text(
                text = jobName,
                style = MaterialTheme.typography.labelSmall,
                color = if (selectedJob) MythrilCyan else MutedSilver,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Full Computed Multi-RPG Stat Row (HP, ATK, DEF, MAG, RES, SPD)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text("HP ${loadout.effectiveMaxHp}", style = MaterialTheme.typography.labelSmall, color = HpEmerald)
        Text("ATK ${loadout.effectiveAtk}", style = MaterialTheme.typography.labelSmall, color = Color.White)
        Text("DEF ${loadout.effectiveDef}", style = MaterialTheme.typography.labelSmall, color = Color.White)
        Text("MAG ${loadout.effectiveMag}", style = MaterialTheme.typography.labelSmall, color = MythrilCyan)
        Text("RES ${loadout.effectiveRes}", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
        Text("SPD ${loadout.effectiveSpd}", style = MaterialTheme.typography.labelSmall, color = RelicGold)
      }

      // Multi-RPG Combat Traits + Active Set Bonus Strip
      val traitSummary = buildList {
        if (loadout.critRateBonus > 0f) add("Crit +${(loadout.critRateBonus * 100).toInt()}%")
        if (loadout.lifestealPercent > 0) add("Drain ${loadout.lifestealPercent}%")
        if (loadout.counterChancePercent > 0) add("Counter ${loadout.counterChancePercent}%")
        if (loadout.dominantResistPercent > 0) add("${loadout.dominantResistElement.label} Res ${loadout.dominantResistPercent}%")
        if (loadout.spCostReduction > 0) add("-${loadout.spCostReduction} SP Cost")
      }
      if (traitSummary.isNotEmpty()) {
        Text(
          text = traitSummary.joinToString(" • "),
          style = MaterialTheme.typography.labelSmall,
          color = MythrilCyan,
          fontSize = 10.sp,
          modifier = Modifier.padding(top = 2.dp)
        )
      }
      loadout.activeSetBonuses.forEach { bonus ->
        Surface(
          color = HpEmerald.copy(alpha = 0.14f),
          shape = RoundedCornerShape(6.dp),
          border = BorderStroke(1.dp, HpEmerald.copy(alpha = 0.6f)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
        ) {
          Text(
            text = "SET: ${bonus.setName} (${bonus.piecesEquipped}pc) → ${bonus.description}",
            style = MaterialTheme.typography.labelSmall,
            color = HpEmerald,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // 3-Slot Equipped Relic Rack: WEAPON, ARMOR, ACCESSORY
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        val weaponSynergy = equippedWeapon != null &&
          (equippedWeapon.originRealm.equals(activeStageRealm, ignoreCase = true) || equippedWeapon.originRealm == "Record Keep")
        val armorSynergy = equippedArmor != null &&
          (equippedArmor.originRealm.equals(activeStageRealm, ignoreCase = true) || equippedArmor.originRealm == "Record Keep")
        val accSynergy = equippedAccessory != null &&
          (equippedAccessory.originRealm.equals(activeStageRealm, ignoreCase = true) || equippedAccessory.originRealm == "Record Keep")

        RelicEquipmentSlotCard(
          label = "WEAPON",
          equipment = equippedWeapon,
          hasSynergy = weaponSynergy,
          isSelected = isEditingGear && selectedGearSlotType == EquipmentSlotType.WEAPON,
          onClick = { onSelectGearSlot(EquipmentSlotType.WEAPON) },
          modifier = Modifier.weight(1f)
        )
        RelicEquipmentSlotCard(
          label = "ARMOR",
          equipment = equippedArmor,
          hasSynergy = armorSynergy,
          isSelected = isEditingGear && selectedGearSlotType == EquipmentSlotType.ARMOR,
          onClick = { onSelectGearSlot(EquipmentSlotType.ARMOR) },
          modifier = Modifier.weight(1f)
        )
        RelicEquipmentSlotCard(
          label = "ACCESSORY",
          equipment = equippedAccessory,
          hasSynergy = accSynergy,
          isSelected = isEditingGear && selectedGearSlotType == EquipmentSlotType.ACCESSORY,
          onClick = { onSelectGearSlot(EquipmentSlotType.ACCESSORY) },
          modifier = Modifier.weight(1f)
        )
      }

      // Relic Equipment Picker Drawer
      if (isEditingGear) {
        val matchingSlotGear = equipmentList.filter { it.slotTypeEnum == selectedGearSlotType }
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          color = VoidNavy,
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, MythrilCyan),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(8.dp)) {
            Text(
              text = "Select ${selectedGearSlotType.label} from Room Armory (Tap to Equip or Smithy +5):",
              style = MaterialTheme.typography.labelSmall,
              color = MythrilCyan
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(matchingSlotGear, key = { it.id }) { gear ->
                val gearHasSynergy = gear.originRealm.equals(activeStageRealm, ignoreCase = true) || gear.originRealm == "Record Keep"
                Surface(
                  color = if (gearHasSynergy) SynergyAzure.copy(alpha = 0.16f) else AstralPanel,
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(1.dp, if (gearHasSynergy) SynergyAzure else RelicGold.copy(alpha = 0.7f)),
                  modifier = Modifier
                    .width(175.dp)
                    .clickable { onPickRelicEquipment(gear) }
                ) {
                  Column(modifier = Modifier.padding(7.dp)) {
                    Text(
                      text = "${gear.name} (+${gear.upgradeLevel})",
                      style = MaterialTheme.typography.labelSmall,
                      color = Color.White,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = "${gear.originRealm} • ATK+${gear.bonusAtk} MAG+${gear.bonusMag} DEF+${gear.bonusDef}",
                      style = MaterialTheme.typography.labelSmall,
                      color = if (gearHasSynergy) SynergyAzure else RelicGold,
                      fontSize = 9.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                      onClick = { onUpgradeEquipment(gear) },
                      contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                      border = BorderStroke(1.dp, RelicGold),
                      modifier = Modifier.height(24.dp)
                    ) {
                      Text("Smithy +5 Lv", style = MaterialTheme.typography.labelSmall, color = RelicGold, fontSize = 9.sp)
                    }
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Soul Break: ${hero.soulBreakName} (${hero.soulBreakElementEnum.label} ${hero.soulBreakMultiplier}x • ${hero.soulBreakEffectEnum.description})",
        style = MaterialTheme.typography.labelSmall,
        color = SoulBreakMagenta
      )

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "JOB SKILLS (3 Active) & PASSIVE TRAITS (2 Passive):",
        style = MaterialTheme.typography.labelSmall,
        color = MythrilCyan
      )
      Spacer(modifier = Modifier.height(4.dp))
      val heroActiveIds = hero.equippedSkillIdList()
      val heroPassiveIds = hero.equippedPassiveIdList()
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(3) { sIdx ->
          val skId = heroActiveIds.getOrNull(sIdx)
          val sk = skills.find { it.id == skId }
          val isSel = isEditingSkill && !editingSkillIsPassive && selectedSkillSlotIndex == sIdx
          Surface(
            color = if (isSel) MythrilCyan.copy(alpha = 0.25f) else VoidNavy,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, if (isSel) MythrilCyan else (sk?.elementEnum?.color ?: AstralBorder)),
            modifier = Modifier
              .width(126.dp)
              .clickable { onSelectSkillSlot(sIdx, false) }
              .testTag("hero_${hero.id}_skill_slot_$sIdx")
          ) {
            Column(modifier = Modifier.padding(6.dp)) {
              Text(
                text = "ACT ${sIdx + 1} (${sk?.rpgSystemEnum?.shortTag ?: "SKILL"})",
                style = MaterialTheme.typography.labelSmall,
                color = sk?.rpgSystemEnum?.accentColor ?: MutedSilver,
                fontSize = 9.sp
              )
              Text(
                text = sk?.name ?: "Assign Skill",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
        items(2) { pIdx ->
          val pId = heroPassiveIds.getOrNull(pIdx)
          val sk = skills.find { it.id == pId }
          val isSel = isEditingSkill && editingSkillIsPassive && selectedSkillSlotIndex == pIdx
          Surface(
            color = if (isSel) HpEmerald.copy(alpha = 0.25f) else VoidNavy,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, if (isSel) HpEmerald else HpEmerald.copy(alpha = 0.55f)),
            modifier = Modifier
              .width(126.dp)
              .clickable { onSelectSkillSlot(pIdx, true) }
              .testTag("hero_${hero.id}_passive_slot_$pIdx")
          ) {
            Column(modifier = Modifier.padding(6.dp)) {
              Text(
                text = "PASSIVE ${pIdx + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = HpEmerald,
                fontSize = 9.sp
              )
              Text(
                text = sk?.name ?: "Assign Trait",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }

      if (isEditingSkill) {
        val candidateSkills = skills.filter { it.categoryEnum.isPassive == editingSkillIsPassive }
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          color = VoidNavy,
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, if (editingSkillIsPassive) HpEmerald else MythrilCyan),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(8.dp)) {
            Text(
              text = if (editingSkillIsPassive) {
                "Select Passive Trait for Slot ${selectedSkillSlotIndex + 1}:"
              } else {
                "Select Active Skill / Magic / Tactics Art for Slot ${selectedSkillSlotIndex + 1}:"
              },
              style = MaterialTheme.typography.labelSmall,
              color = if (editingSkillIsPassive) HpEmerald else MythrilCyan
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(candidateSkills, key = { it.id }) { sk ->
                Surface(
                  color = AstralPanel,
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(1.dp, sk.elementEnum.color),
                  modifier = Modifier
                    .width(152.dp)
                    .clickable { onPickSkill(sk.id) }
                ) {
                  Column(modifier = Modifier.padding(6.dp)) {
                    Text(
                      text = "${sk.name} (R${sk.rankLevel})",
                      style = MaterialTheme.typography.labelSmall,
                      color = Color.White,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = "${sk.rpgSystemEnum.shortTag} • ${sk.elementEnum.label} • ${sk.spCost}SP",
                      style = MaterialTheme.typography.labelSmall,
                      color = sk.rpgSystemEnum.accentColor,
                      fontSize = 9.sp
                    )
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "5-SLOT MEDAL WHEEL (Tap any slot to swap Medal):",
        style = MaterialTheme.typography.labelSmall,
        color = MutedSilver
      )
      Spacer(modifier = Modifier.height(4.dp))

      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        itemsIndexed(DefaultDeckSlots) { slotIdx, slotCfg ->
          val medalId = equippedIds.getOrNull(slotIdx)
          val medal = medals.find { it.id == medalId } ?: medals.firstOrNull()
          val isResonant = medal != null &&
            medal.attributeEnum == slotCfg.requiredAttribute &&
            medal.alignmentEnum == slotCfg.requiredAlignment
          val isSelectedSlot = isEditingSlot && selectedSlotIndex == slotIdx

          Surface(
            color = if (isSelectedSlot) RelicGold.copy(alpha = 0.25f) else VoidNavy,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(
              width = if (isSelectedSlot) 2.dp else 1.dp,
              color = if (isResonant) SynergyAzure else slotCfg.requiredAttribute.color.copy(alpha = 0.6f)
            ),
            modifier = Modifier
              .width(114.dp)
              .clickable { onSelectSlotToEdit(slotIdx) }
          ) {
            Column(modifier = Modifier.padding(7.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "S${slotIdx + 1} ${slotCfg.requiredAttribute.shortCode}",
                  style = MaterialTheme.typography.labelSmall,
                  color = slotCfg.requiredAttribute.color,
                  fontSize = 10.sp
                )
                Text(
                  text = if (isResonant) "${slotCfg.matchedSlotMultiplier}x★" else "${slotCfg.baseSlotMultiplier}x",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (isResonant) SynergyAzure else MutedSilver,
                  fontSize = 10.sp
                )
              }
              Text(
                text = medal?.name ?: "Empty",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }

      if (isEditingSlot) {
        val targetSlot = DefaultDeckSlots[selectedSlotIndex]
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          color = VoidNavy,
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, RelicGold),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(8.dp)) {
            Text(
              text = "Select Medal for Slot ${selectedSlotIndex + 1} (Best match: ${targetSlot.requiredAlignment.label} ${targetSlot.requiredAttribute.label} → ${targetSlot.matchedSlotMultiplier}x):",
              style = MaterialTheme.typography.labelSmall,
              color = RelicGold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(medals, key = { it.id }) { m ->
                val matches = m.attributeEnum == targetSlot.requiredAttribute &&
                  m.alignmentEnum == targetSlot.requiredAlignment
                Surface(
                  color = if (matches) SynergyAzure.copy(alpha = 0.16f) else AstralPanel,
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(1.dp, if (matches) SynergyAzure else m.attributeEnum.color),
                  modifier = Modifier
                    .width(140.dp)
                    .clickable { onPickMedalForSlot(m.id) }
                ) {
                  Column(modifier = Modifier.padding(6.dp)) {
                    Text(
                      text = m.name,
                      style = MaterialTheme.typography.labelSmall,
                      color = Color.White,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = "${m.attributeEnum.shortCode} • ${m.elementEnum.label} • +${m.guiltBonusPercent}%",
                      style = MaterialTheme.typography.labelSmall,
                      color = m.attributeEnum.color,
                      fontSize = 10.sp
                    )
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
private fun RelicEquipmentSlotCard(
  label: String,
  equipment: EquipmentEntity?,
  hasSynergy: Boolean,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    color = if (isSelected) MythrilCyan.copy(alpha = 0.22f) else VoidNavy,
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(
      width = if (isSelected) 2.dp else 1.dp,
      color = if (hasSynergy) SynergyAzure else AstralBorder
    ),
    modifier = modifier.clickable(onClick = onClick)
  ) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        val tierSuffix = if ((equipment?.reforgeTier ?: 0) > 0) " +${equipment?.reforgeTier}" else ""
        Text(
          text = "$label (${equipment?.rarityStars ?: 5}★$tierSuffix)",
          style = MaterialTheme.typography.labelSmall,
          color = if (hasSynergy) SynergyAzure else RelicGold,
          fontSize = 9.sp
        )
        if (hasSynergy) {
          Text(
            text = "SYN",
            style = MaterialTheme.typography.labelSmall,
            color = SynergyAzure,
            fontSize = 9.sp
          )
        }
      }
      Text(
        text = equipment?.name ?: "Tap to Equip",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      if (equipment != null) {
        val orbs = equipment.socketedSkillIdList().size
        Text(
          text = "Orbs $orbs/${equipment.materiaSocketsMax}" +
            (if (equipment.setSeriesName.isNotBlank()) " • Set" else ""),
          style = MaterialTheme.typography.labelSmall,
          color = MutedSilver,
          fontSize = 9.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}
