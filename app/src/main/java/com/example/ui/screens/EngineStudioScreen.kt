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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.net.Uri
import com.example.data.RpgDatabase
import com.example.model.AssetBindTargetType
import com.example.model.BattleLogEntity
import com.example.model.BossArchetype
import com.example.model.ElementType
import com.example.model.EngineConfigEntity
import com.example.model.EquipmentSlotType
import com.example.model.ExternalSearchResult
import com.example.model.HeroEntity
import com.example.model.ImportedAssetEntity
import com.example.model.ImportedAssetType
import com.example.model.MedalAlignment
import com.example.model.MedalAttribute
import com.example.model.ParsedMesh3D
import com.example.model.RealmMetadataEntity
import com.example.model.RealmStageEntity
import com.example.model.RpgSystemOrigin
import com.example.model.SfxCombatCue
import com.example.model.SkillCategory
import com.example.model.SkillCombatEffect
import com.example.model.SkillEntity
import com.example.model.SpecialEffectType
import com.example.model.WeaponArmorClass
import com.example.ui.components.AssetImporterSection
import com.example.ui.theme.AstralBorder
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DeepCrystalBlue
import com.example.ui.theme.HpEmerald
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.MythrilCyan
import com.example.ui.theme.RelicGold
import com.example.ui.theme.SoulBreakMagenta
import com.example.ui.theme.SynergyAzure
import com.example.ui.theme.VoidNavy
import kotlin.math.roundToInt

@Composable
fun EngineStudioScreen(
  engineConfig: EngineConfigEntity,
  heroesCount: Int,
  equipmentCount: Int,
  realmsCount: Int,
  medalsCount: Int,
  stagesCount: Int,
  battleLogs: List<BattleLogEntity>,
  heroes: List<HeroEntity> = emptyList(),
  stages: List<RealmStageEntity> = emptyList(),
  realms: List<RealmMetadataEntity> = emptyList(),
  skills: List<SkillEntity> = emptyList(),
  weaponsCount: Int = 0,
  armorsCount: Int = 0,
  importedAssets: List<ImportedAssetEntity> = emptyList(),
  assetSearchResults: List<ExternalSearchResult> = emptyList(),
  openCc0Catalog: List<ExternalSearchResult> = emptyList(),
  importerLoading: Boolean = false,
  importerStatusMessage: String? = null,
  inspectedMesh3D: ParsedMesh3D? = null,
  onSearchWiki: (String, String) -> Unit = { _, _ -> },
  onImportSearchResult: (ExternalSearchResult) -> Unit = {},
  onImportDirectUrl: (String, ImportedAssetType?, String, Int, Int, Int) -> Unit = { _, _, _, _, _, _ -> },
  onImportManifestUrl: (String) -> Unit = {},
  onImportLocalUri: (Uri, ImportedAssetType?, Int, Int, Int) -> Unit = { _, _, _, _, _ -> },
  onInspect3DModel: (ImportedAssetEntity) -> Unit = {},
  onClose3DViewer: () -> Unit = {},
  onPreviewAudio: (ImportedAssetEntity) -> Unit = {},
  onAutoBindAsset: (ImportedAssetEntity, AssetBindTargetType, Int?, SfxCombatCue) -> Unit = { _, _, _, _ -> },
  onDeleteImportedAsset: (Int) -> Unit = {},
  onUpdateEngineConfig: (EngineConfigEntity) -> Unit,
  onCreateCustomEquipment: (
    name: String,
    slotType: EquipmentSlotType,
    originRealm: String,
    elementBoost: ElementType,
    bonusAtk: Int,
    bonusMag: Int,
    bonusDef: Int,
    rpgSystem: RpgSystemOrigin,
    weaponArmorClass: WeaponArmorClass,
    setSeriesName: String,
    critRateBonus: Float,
    elementResistElement: ElementType,
    elementResistPercent: Int,
    lifestealPercent: Int,
    counterChancePercent: Int,
    grantedSkillId: Int?
  ) -> Unit,
  onCreateCustomSkill: (
    name: String,
    rpgSystem: RpgSystemOrigin,
    category: SkillCategory,
    element: ElementType,
    attribute: MedalAttribute,
    spCost: Int,
    cooldownTurns: Int,
    powerMultiplier: Float,
    hitCount: Int,
    effectType: SkillCombatEffect,
    description: String
  ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _ -> },
  onCreateCustomMedal: (
    name: String,
    attribute: MedalAttribute,
    alignment: MedalAlignment,
    element: ElementType,
    spCost: Int,
    multiplier: Float,
    guiltPercent: Int,
    effect: SpecialEffectType
  ) -> Unit,
  onCreateCustomBossStage: (
    title: String,
    realm: String,
    bossName: String,
    archetype: BossArchetype,
    difficulty: Int,
    maxHp: Int,
    weakness: ElementType,
    bossAttribute: MedalAttribute,
    signatureMove: String
  ) -> Unit,
  onClearBattleLogs: () -> Unit,
  modifier: Modifier = Modifier
) {
  // Custom Relic Equipment Builder State
  var customEquipName by remember { mutableStateOf("Ultima Keyblade VII") }
  var customEquipSlot by remember { mutableStateOf(EquipmentSlotType.WEAPON) }
  var customEquipRealm by remember { mutableStateOf("Daybreak Town") }
  var customEquipElem by remember { mutableStateOf(ElementType.HOLY) }
  var customEquipAtk by remember { mutableIntStateOf(240) }
  var customEquipMag by remember { mutableIntStateOf(210) }
  var customEquipDef by remember { mutableIntStateOf(165) }
  var customEquipSystem by remember { mutableStateOf(RpgSystemOrigin.CHRONO_NEXUS) }
  var customEquipClass by remember { mutableStateOf(WeaponArmorClass.KEYBLADE) }
  var customEquipSetSeries by remember { mutableStateOf("Daybreak Regalia") }
  var customEquipCritPercent by remember { mutableIntStateOf(10) }
  var customEquipResistPercent by remember { mutableIntStateOf(15) }
  var customEquipLifesteal by remember { mutableIntStateOf(8) }
  var customEquipCounter by remember { mutableIntStateOf(10) }
  var customEquipGrantedSkillId by remember { mutableStateOf<Int?>(null) }
  var equipCreatedBanner by remember { mutableStateOf(false) }

  // Custom Multi-RPG Skill Builder State
  var customSkillName by remember { mutableStateOf("Ultima Dual-Tech") }
  var customSkillSystem by remember { mutableStateOf(RpgSystemOrigin.CHRONO_NEXUS) }
  var customSkillCategory by remember { mutableStateOf(SkillCategory.ACTIVE_TECH) }
  var customSkillElem by remember { mutableStateOf(ElementType.HOLY) }
  var customSkillAttr by remember { mutableStateOf(MedalAttribute.POWER) }
  var customSkillEffect by remember { mutableStateOf(SkillCombatEffect.CHAIN_BUILD) }
  var customSkillSp by remember { mutableIntStateOf(2) }
  var customSkillCd by remember { mutableIntStateOf(1) }
  var customSkillMult by remember { mutableFloatStateOf(3.4f) }
  var customSkillHits by remember { mutableIntStateOf(5) }
  var skillCreatedBanner by remember { mutableStateOf(false) }

  // Custom Medal Builder State
  var customMedalName by remember { mutableStateOf("Key Art Ultima EX") }
  var customMedalAttr by remember { mutableStateOf(MedalAttribute.POWER) }
  var customMedalAlign by remember { mutableStateOf(MedalAlignment.UPRIGHT) }
  var customMedalElem by remember { mutableStateOf(ElementType.HOLY) }
  var customMedalSpCost by remember { mutableIntStateOf(2) }
  var customMedalMult by remember { mutableFloatStateOf(4.4f) }
  var customMedalGuilt by remember { mutableIntStateOf(165) }
  var customMedalEffect by remember { mutableStateOf(SpecialEffectType.CHAIN_IGNITER) }
  var medalCreatedBanner by remember { mutableStateOf(false) }

  // Custom Boss Stage Builder State
  var customStageTitle by remember { mutableStateOf("Astral Singularity Core") }
  var customStageRealm by remember { mutableStateOf("Daybreak Town") }
  var customBossName by remember { mutableStateOf("Twilight Xemnas Armor") }
  var customBossArchetype by remember { mutableStateOf(BossArchetype.CHAOS_SERAPH) }
  var customBossDiff by remember { mutableIntStateOf(260) }
  var customBossHp by remember { mutableIntStateOf(320000) }
  var customBossWeakness by remember { mutableStateOf(ElementType.HOLY) }
  var customBossAttr by remember { mutableStateOf(MedalAttribute.MAGIC) }
  var customBossMove by remember { mutableStateOf("Ethereal Blades of Nothingness") }
  var bossCreatedBanner by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 12.dp)
      .testTag("engine_studio_screen"),
    contentPadding = PaddingValues(vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // 0. RoomDatabase Schema & DAO Status Inspector
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = DeepCrystalBlue),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, SynergyAzure),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("room_schema_inspector_card")
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Storage, contentDescription = "Room Database Schema", tint = SynergyAzure)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "ROOM DATABASE SCHEMA & DAO INSPECTOR",
              style = MaterialTheme.typography.titleLarge,
              color = SynergyAzure
            )
          }
          Text(
            text = "Database: ${com.example.data.AppDatabase.DATABASE_NAME} (v${com.example.data.AppDatabase.DATABASE_VERSION}) • Weapon/Armor/Skill tables + RpgTypeConverters + Hilt DI.",
            style = MaterialTheme.typography.labelSmall,
            color = MutedSilver,
            modifier = Modifier.padding(top = 4.dp)
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            DaoMetricChip("WeaponDao", "$weaponsCount Weapons", RelicGold)
            DaoMetricChip("ArmorDao", "$armorsCount Armors", MythrilCyan)
            DaoMetricChip("SkillDao", "${skills.size} Skills", HpEmerald)
          }
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            DaoMetricChip("HeroDao", "$heroesCount Heroes", SoulBreakMagenta)
            DaoMetricChip("RealmMetadataDao", "$realmsCount Realms", SynergyAzure)
            DaoMetricChip("ImportedAssetDao", "${importedAssets.size} Assets", RelicGold)
          }
        }
      }
    }

    // 0b. External Asset Importer (Sprites, Sheets, Concept Art, Wikis, 3D + Textures, BGM/SFX)
    item {
      AssetImporterSection(
        importedAssets = importedAssets,
        heroes = heroes,
        stages = stages,
        realms = realms,
        searchResults = assetSearchResults,
        openCc0Catalog = openCc0Catalog,
        isLoading = importerLoading,
        statusMessage = importerStatusMessage,
        inspectedMesh3D = inspectedMesh3D,
        onSearchWiki = onSearchWiki,
        onImportSearchResult = onImportSearchResult,
        onImportDirectUrl = onImportDirectUrl,
        onImportManifestUrl = onImportManifestUrl,
        onImportLocalUri = onImportLocalUri,
        onInspect3DModel = onInspect3DModel,
        onClose3DViewer = onClose3DViewer,
        onPreviewAudio = onPreviewAudio,
        onAutoBindAsset = onAutoBindAsset,
        onDeleteAsset = onDeleteImportedAsset
      )
    }

    // 1. Live RPG Engine Math & Parameter Tuner
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = DeepCrystalBlue),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, MythrilCyan),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Build, contentDescription = "Engine Parameters", tint = MythrilCyan)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "RPG ENGINE CORE PARAMETERS (LIVE TUNING)",
              style = MaterialTheme.typography.titleLarge,
              color = MythrilCyan
            )
          }
          Text(
            text = "Formula: (Stat × 26 / BossDef) × MedalMult × KeybladeSlotMult × Guilt × AttrAdv × ElemWeakness × Chain × Crit",
            style = MaterialTheme.typography.labelSmall,
            color = RelicGold,
            modifier = Modifier.padding(top = 4.dp)
          )

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = "ATB Fill Speed Rate: ${engineConfig.atbBaseFillRate.roundToInt()} units/sec",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
          )
          Slider(
            value = engineConfig.atbBaseFillRate,
            onValueChange = { onUpdateEngineConfig(engineConfig.copy(atbBaseFillRate = it)) },
            valueRange = 18f..65f,
            colors = SliderDefaults.colors(thumbColor = RelicGold, activeTrackColor = MythrilCyan)
          )

          Text(
            text = "Elemental Weakness Multiplier: ${"%.2f".format(engineConfig.weaknessMultiplier)}x",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
          )
          Slider(
            value = engineConfig.weaknessMultiplier,
            onValueChange = { onUpdateEngineConfig(engineConfig.copy(weaknessMultiplier = it)) },
            valueRange = 1.2f..2.5f,
            colors = SliderDefaults.colors(thumbColor = RelicGold, activeTrackColor = HpEmerald)
          )

          Text(
            text = "Realm Synergy Stat Bonus: +${((engineConfig.realmSynergyStatMultiplier - 1f) * 100).roundToInt()}% All Stats",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
          )
          Slider(
            value = engineConfig.realmSynergyStatMultiplier,
            onValueChange = { onUpdateEngineConfig(engineConfig.copy(realmSynergyStatMultiplier = it)) },
            valueRange = 1.10f..1.60f,
            colors = SliderDefaults.colors(thumbColor = SynergyAzure, activeTrackColor = SynergyAzure)
          )

          Text(
            text = "Break Damage Limit Cap (Per Hit): %,d".format(engineConfig.damageCapBreakLimit),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
          )
          Slider(
            value = engineConfig.damageCapBreakLimit.toFloat(),
            onValueChange = {
              onUpdateEngineConfig(engineConfig.copy(damageCapBreakLimit = it.roundToInt()))
            },
            valueRange = 9999f..99999f,
            colors = SliderDefaults.colors(thumbColor = SoulBreakMagenta, activeTrackColor = SoulBreakMagenta)
          )
        }
      }
    }

    // 2. Custom Multi-RPG Relic Equipment Architect (EquipmentDao)
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = DeepCrystalBlue),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, HpEmerald),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "MULTI-RPG 7★ WEAPON, ARMOR & ACCESSORY ARCHITECT",
            style = MaterialTheme.typography.titleLarge,
            color = HpEmerald
          )
          Text(
            text = "Forge Weapons, Armor & Accessories with Set Bonuses, Crit, Lifesteal, Counter & Resistances.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedSilver
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = customEquipName,
              onValueChange = { customEquipName = it },
              label = { Text("Relic Name") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = customEquipRealm,
              onValueChange = { customEquipRealm = it },
              label = { Text("Origin Realm") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = customEquipSetSeries,
            onValueChange = { customEquipSetSeries = it },
            label = { Text("Armor/Relic Set Series (e.g. Daybreak Regalia, SOLDIER First Class)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text("Slot Type:", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(vertical = 4.dp)
          ) {
            EquipmentSlotType.entries.forEach { slot ->
              SelectableOptionChip(
                label = slot.label,
                selected = customEquipSlot == slot,
                color = HpEmerald,
                onClick = {
                  customEquipSlot = slot
                  customEquipClass = WeaponArmorClass.entries.first { it.slotType == slot }
                }
              )
            }
          }
          Text("RPG Tradition:", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(vertical = 4.dp)
          ) {
            items(RpgSystemOrigin.entries) { sys ->
              SelectableOptionChip(
                label = sys.shortTag,
                selected = customEquipSystem == sys,
                color = sys.accentColor,
                onClick = { customEquipSystem = sys }
              )
            }
          }
          Text("Weapon / Armor Class:", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(vertical = 4.dp)
          ) {
            items(WeaponArmorClass.entries.filter { it.slotType == customEquipSlot }) { cls ->
              SelectableOptionChip(
                label = cls.label,
                selected = customEquipClass == cls,
                color = MythrilCyan,
                onClick = { customEquipClass = cls }
              )
            }
          }
          Text(
            text = "Crit +${customEquipCritPercent}% • Res +${customEquipResistPercent}% • Drain ${customEquipLifesteal}% • Counter ${customEquipCounter}%",
            style = MaterialTheme.typography.labelSmall,
            color = RelicGold
          )
          Slider(
            value = customEquipCritPercent.toFloat(),
            onValueChange = { customEquipCritPercent = it.roundToInt() },
            valueRange = 0f..25f,
            colors = SliderDefaults.colors(thumbColor = RelicGold, activeTrackColor = HpEmerald)
          )
          if (skills.isNotEmpty()) {
            Text("Granted Skill:", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.padding(vertical = 4.dp)
            ) {
              item {
                SelectableOptionChip(
                  label = "None",
                  selected = customEquipGrantedSkillId == null,
                  color = MutedSilver,
                  onClick = { customEquipGrantedSkillId = null }
                )
              }
              items(skills.filter { !it.categoryEnum.isPassive }.take(8), key = { it.id }) { sk ->
                SelectableOptionChip(
                  label = sk.name,
                  selected = customEquipGrantedSkillId == sk.id,
                  color = sk.elementEnum.color,
                  onClick = { customEquipGrantedSkillId = sk.id }
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Button(
            onClick = {
              onCreateCustomEquipment(
                customEquipName,
                customEquipSlot,
                customEquipRealm,
                customEquipElem,
                customEquipAtk,
                customEquipMag,
                customEquipDef,
                customEquipSystem,
                customEquipClass,
                customEquipSetSeries,
                customEquipCritPercent / 100f,
                customEquipElem,
                customEquipResistPercent,
                customEquipLifesteal,
                customEquipCounter,
                customEquipGrantedSkillId
              )
              equipCreatedBanner = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = HpEmerald, contentColor = VoidNavy),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_create_custom_equipment")
          ) {
            Icon(Icons.Default.AddCircle, contentDescription = "Forge Relic")
            Spacer(modifier = Modifier.width(6.dp))
            Text("SAVE RELIC TO EQUIPMENT DAO", fontWeight = FontWeight.Bold)
          }
          if (equipCreatedBanner) {
            Text(
              text = "✓ Custom 7★ Multi-RPG Relic saved via EquipmentDao! Check Party → Armory Vault.",
              style = MaterialTheme.typography.labelLarge,
              color = HpEmerald,
              modifier = Modifier.padding(top = 6.dp)
            )
          }
        }
      }
    }

    // 2b. Custom Multi-RPG Skill & Passive Architect (SkillDao)
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = DeepCrystalBlue),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MythrilCyan),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "MULTI-RPG SKILL & PASSIVE ARCHITECT (SKILL DAO)",
            style = MaterialTheme.typography.titleLarge,
            color = MythrilCyan
          )
          Text(
            text = "Author Job Arts, Black/White Magic, Limit Techs, or Passive Materia persisted in SkillDao.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedSilver
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = customSkillName,
            onValueChange = { customSkillName = it },
            label = { Text("Skill / Art Name") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_custom_skill_name")
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text("Category:", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(vertical = 4.dp)
          ) {
            items(SkillCategory.entries) { cat ->
              SelectableOptionChip(
                label = cat.label,
                selected = customSkillCategory == cat,
                color = MythrilCyan,
                onClick = { customSkillCategory = cat }
              )
            }
          }
          Text("Combat Effect:", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(vertical = 4.dp)
          ) {
            items(SkillCombatEffect.entries.filter { it != SkillCombatEffect.PASSIVE_STAT_BOOST }) { eff ->
              SelectableOptionChip(
                label = eff.label,
                selected = customSkillEffect == eff,
                color = SoulBreakMagenta,
                onClick = { customSkillEffect = eff }
              )
            }
          }
          Text("Element & RPG Origin:", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(vertical = 4.dp)
          ) {
            items(ElementType.entries) { el ->
              SelectableOptionChip(
                label = el.label,
                selected = customSkillElem == el,
                color = el.color,
                onClick = { customSkillElem = el }
              )
            }
          }
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(vertical = 4.dp)
          ) {
            items(RpgSystemOrigin.entries) { sys ->
              SelectableOptionChip(
                label = sys.shortTag,
                selected = customSkillSystem == sys,
                color = sys.accentColor,
                onClick = { customSkillSystem = sys }
              )
            }
          }
          Text(
            text = "Power: ${"%.1f".format(customSkillMult)}x • Hits: $customSkillHits • SP: $customSkillSp • CD: ${customSkillCd}T",
            style = MaterialTheme.typography.labelSmall,
            color = RelicGold
          )
          Slider(
            value = customSkillMult,
            onValueChange = { customSkillMult = (it * 10f).roundToInt() / 10f },
            valueRange = 1.5f..5.5f,
            colors = SliderDefaults.colors(thumbColor = MythrilCyan, activeTrackColor = MythrilCyan)
          )
          Button(
            onClick = {
              onCreateCustomSkill(
                customSkillName,
                customSkillSystem,
                customSkillCategory,
                customSkillElem,
                customSkillAttr,
                customSkillSp,
                customSkillCd,
                customSkillMult,
                customSkillHits,
                customSkillEffect,
                "${customSkillSystem.shortTag} ${customSkillCategory.label}: ${customSkillEffect.label}."
              )
              skillCreatedBanner = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = MythrilCyan, contentColor = VoidNavy),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_create_custom_skill")
          ) {
            Icon(Icons.Default.AddCircle, contentDescription = "Create Skill")
            Spacer(modifier = Modifier.width(6.dp))
            Text("SAVE SKILL TO SKILL GRIMOIRE", fontWeight = FontWeight.Bold)
          }
          if (skillCreatedBanner) {
            Text(
              text = "✓ Custom Skill saved via SkillDao! Equip or socket it in Party → Skill Grimoire.",
              style = MaterialTheme.typography.labelLarge,
              color = HpEmerald,
              modifier = Modifier.padding(top = 6.dp)
            )
          }
        }
      }
    }

    // 3. Custom Boss Encounter Creator
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = DeepCrystalBlue),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, RelicGold),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "CUSTOM BOSS ENCOUNTER ARCHITECT",
            style = MaterialTheme.typography.titleLarge,
            color = RelicGold
          )
          Text(
            text = "Design a custom Boss Stage for RealmStageDao & RealmMetadataDao.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedSilver
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = customBossName,
            onValueChange = { customBossName = it },
            label = { Text("Boss Name") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_custom_boss_name")
          )

          Spacer(modifier = Modifier.height(6.dp))

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = customStageTitle,
              onValueChange = { customStageTitle = it },
              label = { Text("Stage Title") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = customStageRealm,
              onValueChange = { customStageRealm = it },
              label = { Text("Synergy Realm") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text("Boss Sprite Archetype:", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(vertical = 4.dp)
          ) {
            items(BossArchetype.entries) { arch ->
              SelectableOptionChip(
                label = arch.label,
                selected = customBossArchetype == arch,
                color = RelicGold,
                onClick = { customBossArchetype = arch }
              )
            }
          }

          Text("Elemental Weakness:", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(vertical = 4.dp)
          ) {
            items(ElementType.entries) { elem ->
              SelectableOptionChip(
                label = elem.label,
                selected = customBossWeakness == elem,
                color = elem.color,
                onClick = { customBossWeakness = elem }
              )
            }
          }

          Text(
            text = "Boss Max HP: %,d • Difficulty: %d".format(customBossHp, customBossDiff),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
          )
          Slider(
            value = customBossHp.toFloat(),
            onValueChange = { customBossHp = it.roundToInt() },
            valueRange = 80000f..650000f,
            colors = SliderDefaults.colors(thumbColor = RelicGold, activeTrackColor = DangerCrimson)
          )

          Button(
            onClick = {
              onCreateCustomBossStage(
                customStageTitle,
                customStageRealm,
                customBossName,
                customBossArchetype,
                customBossDiff,
                customBossHp,
                customBossWeakness,
                customBossAttr,
                customBossMove
              )
              bossCreatedBanner = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = RelicGold, contentColor = VoidNavy),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_create_custom_boss")
          ) {
            Icon(Icons.Default.AddCircle, contentDescription = "Create Boss")
            Spacer(modifier = Modifier.width(6.dp))
            Text("SAVE BOSS STAGE TO REALM MAP", fontWeight = FontWeight.Bold)
          }

          if (bossCreatedBanner) {
            Text(
              text = "✓ Custom Boss Stage added to Realms tab!",
              style = MaterialTheme.typography.labelLarge,
              color = HpEmerald,
              modifier = Modifier.padding(top = 6.dp)
            )
          }
        }
      }
    }

    // 4. Custom Medal Forge Architect
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = DeepCrystalBlue),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SoulBreakMagenta),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "CUSTOM 7★ MEDAL ARCHITECT",
            style = MaterialTheme.typography.titleLarge,
            color = SoulBreakMagenta
          )
          Text(
            text = "Author custom KHUX / FFRK hybrid Medals persisted via MedalDao.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedSilver
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = customMedalName,
            onValueChange = { customMedalName = it },
            label = { Text("Medal Name") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_custom_medal_name")
          )

          Spacer(modifier = Modifier.height(8.dp))
          Text("Attribute & Alignment:", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(vertical = 4.dp)
          ) {
            MedalAttribute.entries.forEach { attr ->
              SelectableOptionChip(
                label = attr.label,
                selected = customMedalAttr == attr,
                color = attr.color,
                onClick = { customMedalAttr = attr }
              )
            }
            MedalAlignment.entries.forEach { align ->
              SelectableOptionChip(
                label = align.name,
                selected = customMedalAlign == align,
                color = MythrilCyan,
                onClick = { customMedalAlign = align }
              )
            }
          }

          Text("Special Effect:", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(vertical = 4.dp)
          ) {
            items(SpecialEffectType.entries) { eff ->
              SelectableOptionChip(
                label = eff.name,
                selected = customMedalEffect == eff,
                color = SoulBreakMagenta,
                onClick = { customMedalEffect = eff }
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Button(
            onClick = {
              onCreateCustomMedal(
                customMedalName,
                customMedalAttr,
                customMedalAlign,
                customMedalElem,
                customMedalSpCost,
                customMedalMult,
                customMedalGuilt,
                customMedalEffect
              )
              medalCreatedBanner = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = SoulBreakMagenta, contentColor = Color.White),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_create_custom_medal")
          ) {
            Icon(Icons.Default.AddCircle, contentDescription = "Create Medal")
            Spacer(modifier = Modifier.width(6.dp))
            Text("FORGE 7★ MEDAL TO VAULT", fontWeight = FontWeight.Bold)
          }

          if (medalCreatedBanner) {
            Text(
              text = "✓ Custom 7★ Medal added to Medals tab & Party Wheel picker!",
              style = MaterialTheme.typography.labelLarge,
              color = HpEmerald,
              modifier = Modifier.padding(top = 6.dp)
            )
          }
        }
      }
    }

    // 5. Saved Combat Telemetry & Battle History
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "COMBAT TELEMETRY LOGS (${battleLogs.size})",
          style = MaterialTheme.typography.titleMedium,
          color = RelicGold
        )
        if (battleLogs.isNotEmpty()) {
          OutlinedButton(
            onClick = onClearBattleLogs,
            border = BorderStroke(1.dp, MutedSilver),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Logs", tint = MutedSilver)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Clear", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
          }
        }
      }
    }

    if (battleLogs.isEmpty()) {
      item {
        Surface(
          color = VoidNavy,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, AstralBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "No completed encounters recorded yet. Complete a battle in the Battle Arena to inspect full telemetry.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedSilver,
            modifier = Modifier.padding(14.dp)
          )
        }
      }
    } else {
      items(battleLogs, key = { it.id }) { log ->
        Surface(
          color = VoidNavy,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(
            1.dp,
            if (log.outcome == "VICTORY") HpEmerald.copy(alpha = 0.6f) else DangerCrimson.copy(alpha = 0.6f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${log.outcome} • ${log.stageTitle} (${log.bossName})",
                style = MaterialTheme.typography.titleMedium,
                color = if (log.outcome == "VICTORY") HpEmerald else DangerCrimson
              )
              Text(
                text = "Turns: ${log.turnsTaken} • Max Chain: ${log.peakChainCount}x • Total DMG: %,d".format(log.totalDamageDealt),
                style = MaterialTheme.typography.labelSmall,
                color = MutedSilver
              )
            }
            Text(
              text = "★".repeat(log.starsEarned),
              style = MaterialTheme.typography.titleLarge,
              color = RelicGold
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DaoMetricChip(
  daoName: String,
  countText: String,
  color: Color
) {
  Surface(
    color = VoidNavy,
    shape = RoundedCornerShape(8.dp),
    border = BorderStroke(1.dp, color.copy(alpha = 0.6f))
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
      Text(text = daoName, style = MaterialTheme.typography.labelSmall, color = MutedSilver)
      Text(text = countText, style = MaterialTheme.typography.labelLarge, color = color)
    }
  }
}

@Composable
private fun SelectableOptionChip(
  label: String,
  selected: Boolean,
  color: Color,
  onClick: () -> Unit
) {
  Surface(
    color = if (selected) color.copy(alpha = 0.24f) else VoidNavy,
    shape = RoundedCornerShape(8.dp),
    border = BorderStroke(1.dp, if (selected) color else AstralBorder),
    modifier = Modifier.clickable(onClick = onClick)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = if (selected) color else MutedSilver,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
    )
  }
}
