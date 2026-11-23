package com.example.model

import androidx.compose.ui.graphics.Color
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.ui.theme.MagicSapphire
import com.example.ui.theme.PowerCrimson
import com.example.ui.theme.RelicGold
import com.example.ui.theme.SpeedEmerald

enum class MedalAttribute(val label: String, val shortCode: String, val color: Color) {
  POWER("Power", "PWR", PowerCrimson),
  SPEED("Speed", "SPD", SpeedEmerald),
  MAGIC("Magic", "MAG", MagicSapphire);

  fun hasAdvantageOver(other: MedalAttribute): Boolean {
    return (this == POWER && other == SPEED) ||
      (this == SPEED && other == MAGIC) ||
      (this == MAGIC && other == POWER)
  }
}

enum class MedalAlignment(val label: String) {
  UPRIGHT("Upright (Light)"),
  REVERSED("Reversed (Dark)")
}

enum class ElementType(val label: String, val color: Color) {
  FIRE("Fire", Color(0xFFFF6D00)),
  ICE("Ice", Color(0xFF00E5FF)),
  LIGHTNING("Lightning", Color(0xFFFFEA00)),
  WIND("Wind", Color(0xFF00E676)),
  HOLY("Holy", Color(0xFFFFF59D)),
  DARK("Darkness", Color(0xFFB388FF)),
  NON_ELEMENTAL("Kinetic", Color(0xFFCFD8DC))
}

enum class HeroArchetype(val label: String, val weaponName: String) {
  KEEPER("Record Keeper", "Grimoire Tome"),
  KEYBEARER("Keybearer", "Starlight Keyblade"),
  SOLDIER("Ex-SOLDIER", "Buster Greatsword"),
  ESPER("Esper Magitek", "Enhancer Rune Blade"),
  BLACK_MAGE("Black Mage", "Oak Staff of Vivi"),
  DAWNWALKER("Dawnwalker", "Way to the Dawn")
}

enum class BossArchetype(val label: String) {
  GUARD_ARMOR("Guard Armor X"),
  BEHEMOTH_KING("King Behemoth"),
  DARKSIDE_TITAN("Darkside Heartless"),
  OMEGA_WEAPON("Omega Weapon Mk.II"),
  CHAOS_SERAPH("Chaos Temporal Judge")
}

enum class SpecialEffectType(val description: String) {
  PURE_DAMAGE("High-Multiplier Burst"),
  ATK_BREAKDOWN("ATK Breakdown (-40% Enemy ATK)"),
  DEF_BREAKDOWN("DEF Breakdown (-50% Enemy DEF)"),
  PARTY_HASTEGA("Hastega + ATK Boost (+30% Party ATK & Haste)"),
  PARTY_CURAGA("Curaga Medica (Restore 40% Party HP + Regen)"),
  CHAIN_IGNITER("Elemental Chain Starter (+3 Chain Hits)")
}

enum class EquipmentSlotType(val label: String) {
  WEAPON("Relic Weapon"),
  ARMOR("Relic Armor"),
  ACCESSORY("Relic Accessory")
}

/**
 * Room Entity storing Character stats, growth attributes, Soul Break metadata,
 * 5-slot Medal Wheel deck bindings, and equipped Relic foreign keys.
 */
@Entity(
  tableName = "heroes",
  indices = [
    Index(value = ["realm"]),
    Index(value = ["inParty"])
  ]
)
data class HeroEntity(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val name: String,
  val title: String,
  val realm: String,
  val archetype: String, // HeroArchetype.name
  val level: Int = 65,
  val experiencePoints: Long = 125000L,
  val maxHp: Int = 5200,
  val attack: Int = 420,
  val defense: Int = 340,
  val magic: Int = 380,
  val resistance: Int = 360,
  val mind: Int = 330,
  val speed: Int = 155,
  val recordDiveTier: Int = 3, // FFRK Record Spheres / Board tier (1..6)
  val soulBreakName: String,
  val soulBreakElement: String, // ElementType.name
  val soulBreakMultiplier: Float = 6.5f,
  val soulBreakEffect: String, // SpecialEffectType.name
  val inParty: Boolean = true,
  val equippedMedalIdsCsv: String = "1,2,3,4,5",
  val equippedWeaponId: Int? = null,
  val equippedArmorId: Int? = null,
  val equippedAccessoryId: Int? = null,
  val portraitUri: String = "",
  val loreBio: String = "",
  val spriteAssetId: Int? = null,
  val model3dAssetId: Int? = null,
  val attackSfxAssetId: Int? = null,
  val jobClassName: String = "Freelancer",
  val equippedSkillIdsCsv: String = "1,2,3",
  val equippedPassiveIdsCsv: String = "13"
) {
  val archetypeEnum: HeroArchetype
    get() = HeroArchetype.entries.find { it.name == archetype } ?: HeroArchetype.KEEPER

  val soulBreakElementEnum: ElementType
    get() = ElementType.entries.find { it.name == soulBreakElement } ?: ElementType.HOLY

  val soulBreakEffectEnum: SpecialEffectType
    get() = SpecialEffectType.entries.find { it.name == soulBreakEffect } ?: SpecialEffectType.PURE_DAMAGE

  fun equippedMedalIdList(): List<Int> =
    equippedMedalIdsCsv.split(",").mapNotNull { it.trim().toIntOrNull() }

  fun equippedSkillIdList(): List<Int> =
    equippedSkillIdsCsv.split(",").mapNotNull { it.trim().toIntOrNull() }

  fun equippedPassiveIdList(): List<Int> =
    equippedPassiveIdsCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
}

typealias CharacterEntity = HeroEntity

/**
 * Room Entity storing Relic Equipment (Weapons, Armor, Accessories),
 * stat bonuses, elemental boosts, and Realm Synergy origin.
 */
@Entity(
  tableName = "equipment",
  indices = [
    Index(value = ["originRealm"]),
    Index(value = ["slotType"]),
    Index(value = ["equippedByHeroId"])
  ]
)
data class EquipmentEntity(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val name: String,
  val slotType: String, // EquipmentSlotType.name
  val originRealm: String, // Matches RealmMetadataEntity.realmName for FFRK Equipment Synergy
  val rarityStars: Int = 6, // 5 to 7 stars
  val upgradeLevel: Int = 25, // 1..50
  val bonusHp: Int = 0,
  val bonusAtk: Int = 0,
  val bonusDef: Int = 0,
  val bonusMag: Int = 0,
  val bonusRes: Int = 0,
  val bonusSpd: Int = 0,
  val elementBoost: String = ElementType.NON_ELEMENTAL.name,
  val passiveTrait: String = "+15% Elemental Damage",
  val equippedByHeroId: Int? = null,
  val rpgSystem: String = RpgSystemOrigin.FFRK.name,
  val weaponArmorClass: String = WeaponArmorClass.KEYBLADE.name,
  val setSeriesName: String = "",
  val critRateBonus: Float = 0f,
  val critDamageBonus: Float = 0f,
  val elementResistElement: String = ElementType.NON_ELEMENTAL.name,
  val elementResistPercent: Int = 0,
  val lifestealPercent: Int = 0,
  val counterChancePercent: Int = 0,
  val spCostReduction: Int = 0,
  val materiaSocketsMax: Int = 2,
  val socketedSkillIdsCsv: String = "",
  val grantedSkillId: Int? = null,
  val reforgeTier: Int = 0
) {
  val slotTypeEnum: EquipmentSlotType
    get() = EquipmentSlotType.entries.find { it.name == slotType } ?: EquipmentSlotType.WEAPON

  val elementBoostEnum: ElementType
    get() = ElementType.entries.find { it.name == elementBoost } ?: ElementType.NON_ELEMENTAL

  val rpgSystemEnum: RpgSystemOrigin
    get() = RpgSystemOrigin.entries.find { it.name == rpgSystem } ?: RpgSystemOrigin.FFRK

  val weaponArmorClassEnum: WeaponArmorClass
    get() = WeaponArmorClass.entries.find { it.name == weaponArmorClass }
      ?: when (slotTypeEnum) {
        EquipmentSlotType.WEAPON -> WeaponArmorClass.KEYBLADE
        EquipmentSlotType.ARMOR -> WeaponArmorClass.HEAVY_PLATE
        EquipmentSlotType.ACCESSORY -> WeaponArmorClass.RING
      }

  val elementResistEnum: ElementType
    get() = ElementType.entries.find { it.name == elementResistElement } ?: ElementType.NON_ELEMENTAL

  fun socketedSkillIdList(): List<Int> =
    socketedSkillIdsCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
}

/**
 * Room Entity storing World/Realm progression metadata, Stamina Shards,
 * Realm Resonance Level, and stage completion counts.
 */
@Entity(
  tableName = "realm_metadata",
  indices = [
    Index(value = ["realmName"], unique = true),
    Index(value = ["seriesCode"])
  ]
)
data class RealmMetadataEntity(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val realmName: String,
  val seriesCode: String, // e.g., "KH-UX", "FF-VII", "FF-VI", "FF-IX", "FFRK-CORE"
  val worldLore: String,
  val dominantElement: String, // ElementType.name
  val realmResonanceLevel: Int = 1, // Boosts synergy bonus in this realm
  val staminaShardsCollected: Int = 2, // 0..5 FFRK Stamina Shards
  val totalStagesCount: Int = 2,
  val clearedStagesCount: Int = 0,
  val masteryPercent: Int = 0,
  val isUnlocked: Boolean = true,
  val bannerArtUri: String = "",
  val bgmAssetId: Int? = null,
  val wikiUrl: String = ""
) {
  val dominantElementEnum: ElementType
    get() = ElementType.entries.find { it.name == dominantElement } ?: ElementType.HOLY
}

/**
 * Room Relational POJO joining a [CharacterEntity] with their equipped [EquipmentEntity] items
 * and home [RealmMetadataEntity].
 */
data class CharacterLoadoutRelation(
  @Embedded val character: HeroEntity,
  @Relation(
    parentColumn = "id",
    entityColumn = "equippedByHeroId"
  )
  val equippedItems: List<EquipmentEntity>,
  @Relation(
    parentColumn = "realm",
    entityColumn = "realmName"
  )
  val homeRealmMetadata: RealmMetadataEntity?
)

@Entity(tableName = "medals")
data class MedalEntity(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val name: String,
  val rarityStars: Int = 6, // 5 to 7 stars
  val attribute: String, // MedalAttribute.name
  val alignment: String, // MedalAlignment.name
  val element: String, // ElementType.name
  val spCost: Int = 2, // 0 to 5 SP gauges
  val baseMultiplier: Float = 2.8f,
  val hitCount: Int = 4,
  val guiltTier: Int = 5, // Tier 1-7
  val guiltBonusPercent: Int = 120, // e.g., +120% Guilt bonus
  val effectType: String = SpecialEffectType.PURE_DAMAGE.name
) {
  val attributeEnum: MedalAttribute
    get() = MedalAttribute.entries.find { it.name == attribute } ?: MedalAttribute.POWER

  val alignmentEnum: MedalAlignment
    get() = MedalAlignment.entries.find { it.name == alignment } ?: MedalAlignment.UPRIGHT

  val elementEnum: ElementType
    get() = ElementType.entries.find { it.name == element } ?: ElementType.FIRE

  val effectTypeEnum: SpecialEffectType
    get() = SpecialEffectType.entries.find { it.name == effectType } ?: SpecialEffectType.PURE_DAMAGE
}

@Entity(tableName = "realm_stages")
data class RealmStageEntity(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val stageCode: String,
  val title: String,
  val realm: String,
  val difficulty: Int, // e.g. 99, 140, 220, 300
  val isProudMode: Boolean = false,
  val bossName: String,
  val bossArchetype: String, // BossArchetype.name
  val bossMaxHp: Int,
  val bossAttack: Int,
  val bossDefense: Int,
  val bossAttribute: String, // MedalAttribute.name
  val weaknessElement: String, // ElementType.name
  val bossCountdownTurns: Int = 3,
  val signatureAttackName: String,
  val masteryConditionText: String,
  val bestStars: Int = 0,
  val clearCount: Int = 0,
  val bossSpriteAssetId: Int? = null,
  val boss3dAssetId: Int? = null,
  val stageBgmAssetId: Int? = null,
  val stageConceptArtUri: String = ""
) {
  val bossArchetypeEnum: BossArchetype
    get() = BossArchetype.entries.find { it.name == bossArchetype } ?: BossArchetype.GUARD_ARMOR

  val bossAttributeEnum: MedalAttribute
    get() = MedalAttribute.entries.find { it.name == bossAttribute } ?: MedalAttribute.POWER

  val weaknessElementEnum: ElementType
    get() = ElementType.entries.find { it.name == weaknessElement } ?: ElementType.FIRE
}

@Entity(tableName = "engine_configs")
data class EngineConfigEntity(
  @PrimaryKey val id: Int = 1,
  val atbBaseFillRate: Float = 34.0f, // Fill units per second at 150 SPD
  val realmSynergyStatMultiplier: Float = 1.25f, // +25% all stats in matching realm
  val weaknessMultiplier: Float = 1.60f, // +60% elemental weakness damage
  val attributeAdvantageMultiplier: Float = 1.35f, // PWR > SPD > MAG
  val criticalHitRate: Float = 0.22f,
  val criticalDamageMultiplier: Float = 1.55f,
  val chainDurationSeconds: Float = 14.0f,
  val damageCapStandard: Int = 9999,
  val damageCapBreakLimit: Int = 29999,
  val maxSpGauges: Int = 15,
  val mythrilShards: Int = 250
)

@Entity(tableName = "battle_logs")
data class BattleLogEntity(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val stageTitle: String,
  val bossName: String,
  val outcome: String, // "VICTORY" or "DEFEATED"
  val turnsTaken: Int,
  val peakChainCount: Int,
  val totalDamageDealt: Long,
  val starsEarned: Int,
  val timestampMillis: Long = System.currentTimeMillis()
)

// Keyblade / Deck Slot Multiplier Definition (KH Union X style 5-slot wheel)
data class DeckSlotConfig(
  val slotIndex: Int,
  val requiredAttribute: MedalAttribute,
  val requiredAlignment: MedalAlignment,
  val baseSlotMultiplier: Float,
  val matchedSlotMultiplier: Float
)

val DefaultDeckSlots = listOf(
  DeckSlotConfig(0, MedalAttribute.POWER, MedalAlignment.UPRIGHT, 1.30f, 1.95f),
  DeckSlotConfig(1, MedalAttribute.SPEED, MedalAlignment.UPRIGHT, 1.40f, 2.15f),
  DeckSlotConfig(2, MedalAttribute.MAGIC, MedalAlignment.REVERSED, 1.50f, 2.35f),
  DeckSlotConfig(3, MedalAttribute.POWER, MedalAlignment.UPRIGHT, 1.60f, 2.55f),
  DeckSlotConfig(4, MedalAttribute.MAGIC, MedalAlignment.REVERSED, 1.75f, 2.90f)
)

// Runtime Combat Models (60 FPS Engine State)
data class HeroBattleState(
  val hero: HeroEntity,
  val hasRealmSynergy: Boolean,
  val equippedWeaponName: String = "",
  val equippedArmorName: String = "",
  val equippedAccessoryName: String = "",
  val effectiveMaxHp: Int,
  val currentHp: Int,
  val effectiveAtk: Int,
  val effectiveDef: Int,
  val effectiveMag: Int,
  val effectiveRes: Int = 360,
  val effectiveSpd: Int,
  val atbPercent: Float = 0f, // 0f..100f
  val soulBreakPoints: Int = 250, // 0..1500 (500 per bar, max 3 bars)
  val activeWheelIndex: Int = 0, // 0..4 rotating medal wheel
  val isDefending: Boolean = false,
  val hasteSecondsRemaining: Float = 0f,
  val shieldWallSecondsRemaining: Float = 0f,
  val atkBuffPercent: Int = 0, // e.g. +30%
  val critRateBonus: Float = 0f,
  val critDamageBonus: Float = 0f,
  val lifestealPercent: Int = 0,
  val counterChancePercent: Int = 0,
  val spCostReduction: Int = 0,
  val elementResistElement: ElementType = ElementType.NON_ELEMENTAL,
  val elementResistPercent: Int = 0,
  val activeSetBonuses: List<String> = emptyList(),
  val availableSkills: List<SkillEntity> = emptyList(),
  val skillCooldowns: Map<Int, Int> = emptyMap(),
  val attackAnimProgress: Float = 0f, // 0f..1f for sprite lunge/swing
  val lastActionLabel: String = "Ready",
  val spriteSheetPath: String = "",
  val spriteCols: Int = 1,
  val spriteRows: Int = 1,
  val spriteFps: Int = 6
) {
  val isAlive: Boolean get() = currentHp > 0
  val isAtbReady: Boolean get() = isAlive && atbPercent >= 100f
  val soulBreakBars: Int get() = soulBreakPoints / 500
}

data class BossBattleState(
  val stage: RealmStageEntity,
  val maxHp: Int,
  val currentHp: Int,
  val atbPercent: Float = 0f, // Enemy real-time ATB bar
  val actionCountdown: Int = 3, // KHUX turn counter above head
  val atkBreakSeconds: Float = 0f, // FFRK ATK Breakdown
  val defBreakSeconds: Float = 0f, // FFRK DEF Breakdown
  val hitFlashProgress: Float = 0f,
  val bossAttackAnimProgress: Float = 0f,
  val bossSpritePath: String = "",
  val bossSpriteCols: Int = 1,
  val bossSpriteRows: Int = 1,
  val bossSpriteFps: Int = 6,
  val stageBgArtUri: String = ""
) {
  val hpFraction: Float get() = (currentHp.toFloat() / maxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
  val isDefBroken: Boolean get() = defBreakSeconds > 0f
  val isAtkBroken: Boolean get() = atkBreakSeconds > 0f
}

data class ActiveChainState(
  val element: ElementType,
  val hitCount: Int = 0,
  val timeRemainingSeconds: Float = 0f,
  val maxSeconds: Float = 14f
) {
  val bonusMultiplier: Float
    get() = 1.0f + (hitCount * 0.04f).coerceAtMost(1.50f)
}

data class FloatingCombatText(
  val id: Long,
  val text: String,
  val subText: String = "",
  val xFraction: Float, // 0f..1f on canvas
  val yFraction: Float, // 0f..1f on canvas
  val color: Color = RelicGold,
  val isCritical: Boolean = false,
  val progress: Float = 0f // 0f..1f lifetime
)

data class CombatParticle(
  val id: Long,
  val xFraction: Float,
  val yFraction: Float,
  val vx: Float,
  val vy: Float,
  val color: Color,
  val radiusDp: Float,
  val progress: Float = 0f
)

data class SoulBreakBannerState(
  val heroName: String,
  val soulBreakName: String,
  val element: ElementType,
  val progress: Float = 0f // 0f..1f
)
