package com.example.data

import androidx.room.TypeConverter
import com.example.model.AssetBindTargetType
import com.example.model.BossArchetype
import com.example.model.ElementType
import com.example.model.EquipmentSlotType
import com.example.model.ExternalSourceType
import com.example.model.HeroArchetype
import com.example.model.ImportedAssetType
import com.example.model.MedalAlignment
import com.example.model.MedalAttribute
import com.example.model.RpgSystemOrigin
import com.example.model.SfxCombatCue
import com.example.model.SkillCategory
import com.example.model.SkillCombatEffect
import com.example.model.SpecialEffectType
import com.example.model.WeaponArmorClass
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Room [TypeConverter] registry handling custom RPG domain types persisted across
 * [com.example.model.WeaponEntity], [com.example.model.ArmorEntity],
 * [com.example.model.SkillEntity], and supporting engine tables.
 *
 * Covers:
 * - RPG domain enums ([RpgSystemOrigin], [WeaponArmorClass], [SkillCategory],
 *   [SkillCombatEffect], [ElementType], [MedalAttribute], [MedalAlignment], etc.)
 * - Typed collections ([List]<[Int]>, [List]<[String]>, [List]<[ElementType]>,
 *   [List]<[SkillCombatEffect]>)
 * - Typed stat & resistance maps ([Map]<[ElementType], [Int]>, [Map]<[String], [Float]>)
 */
class RpgTypeConverters {

  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  private val intListSerializer = ListSerializer(Int.serializer())
  private val stringListSerializer = ListSerializer(String.serializer())
  private val stringIntMapSerializer = MapSerializer(String.serializer(), Int.serializer())
  private val stringFloatMapSerializer = MapSerializer(String.serializer(), Float.serializer())

  // ---------------------------------------------------------------------------
  // Core RPG & Weapon / Armor / Skill Enum Converters
  // ---------------------------------------------------------------------------

  @TypeConverter
  fun fromMedalAttribute(value: MedalAttribute): String = value.name

  @TypeConverter
  fun toMedalAttribute(value: String): MedalAttribute =
    MedalAttribute.entries.find { it.name == value } ?: MedalAttribute.POWER

  @TypeConverter
  fun fromMedalAlignment(value: MedalAlignment): String = value.name

  @TypeConverter
  fun toMedalAlignment(value: String): MedalAlignment =
    MedalAlignment.entries.find { it.name == value } ?: MedalAlignment.UPRIGHT

  @TypeConverter
  fun fromElementType(value: ElementType): String = value.name

  @TypeConverter
  fun toElementType(value: String): ElementType =
    ElementType.entries.find { it.name == value } ?: ElementType.NON_ELEMENTAL

  @TypeConverter
  fun fromHeroArchetype(value: HeroArchetype): String = value.name

  @TypeConverter
  fun toHeroArchetype(value: String): HeroArchetype =
    HeroArchetype.entries.find { it.name == value } ?: HeroArchetype.KEEPER

  @TypeConverter
  fun fromBossArchetype(value: BossArchetype): String = value.name

  @TypeConverter
  fun toBossArchetype(value: String): BossArchetype =
    BossArchetype.entries.find { it.name == value } ?: BossArchetype.GUARD_ARMOR

  @TypeConverter
  fun fromSpecialEffectType(value: SpecialEffectType): String = value.name

  @TypeConverter
  fun toSpecialEffectType(value: String): SpecialEffectType =
    SpecialEffectType.entries.find { it.name == value } ?: SpecialEffectType.PURE_DAMAGE

  @TypeConverter
  fun fromEquipmentSlotType(value: EquipmentSlotType): String = value.name

  @TypeConverter
  fun toEquipmentSlotType(value: String): EquipmentSlotType =
    EquipmentSlotType.entries.find { it.name == value } ?: EquipmentSlotType.WEAPON

  @TypeConverter
  fun fromRpgSystemOrigin(value: RpgSystemOrigin): String = value.name

  @TypeConverter
  fun toRpgSystemOrigin(value: String): RpgSystemOrigin =
    RpgSystemOrigin.entries.find { it.name == value } ?: RpgSystemOrigin.FFRK

  @TypeConverter
  fun fromWeaponArmorClass(value: WeaponArmorClass): String = value.name

  @TypeConverter
  fun toWeaponArmorClass(value: String): WeaponArmorClass =
    WeaponArmorClass.entries.find { it.name == value } ?: WeaponArmorClass.KEYBLADE

  @TypeConverter
  fun fromSkillCategory(value: SkillCategory): String = value.name

  @TypeConverter
  fun toSkillCategory(value: String): SkillCategory =
    SkillCategory.entries.find { it.name == value } ?: SkillCategory.ACTIVE_TECH

  @TypeConverter
  fun fromSkillCombatEffect(value: SkillCombatEffect): String = value.name

  @TypeConverter
  fun toSkillCombatEffect(value: String): SkillCombatEffect =
    SkillCombatEffect.entries.find { it.name == value } ?: SkillCombatEffect.DAMAGE

  @TypeConverter
  fun fromImportedAssetType(value: ImportedAssetType): String = value.name

  @TypeConverter
  fun toImportedAssetType(value: String): ImportedAssetType =
    ImportedAssetType.entries.find { it.name == value } ?: ImportedAssetType.SPRITE

  @TypeConverter
  fun fromExternalSourceType(value: ExternalSourceType): String = value.name

  @TypeConverter
  fun toExternalSourceType(value: String): ExternalSourceType =
    ExternalSourceType.entries.find { it.name == value } ?: ExternalSourceType.DIRECT_URL

  @TypeConverter
  fun fromAssetBindTargetType(value: AssetBindTargetType): String = value.name

  @TypeConverter
  fun toAssetBindTargetType(value: String): AssetBindTargetType =
    AssetBindTargetType.entries.find { it.name == value } ?: AssetBindTargetType.NONE

  @TypeConverter
  fun fromSfxCombatCue(value: SfxCombatCue): String = value.name

  @TypeConverter
  fun toSfxCombatCue(value: String): SfxCombatCue =
    SfxCombatCue.entries.find { it.name == value } ?: SfxCombatCue.ATTACK

  // ---------------------------------------------------------------------------
  // Collection & Map Converters for Weapon, Armor, and Skill Custom Columns
  // ---------------------------------------------------------------------------

  @TypeConverter
  fun fromIntList(list: List<Int>): String =
    json.encodeToString(intListSerializer, list)

  @TypeConverter
  fun toIntList(raw: String): List<Int> {
    if (raw.isBlank()) return emptyList()
    val trimmed = raw.trim()
    if (trimmed.startsWith("[")) {
      return runCatching { json.decodeFromString(intListSerializer, trimmed) }
        .getOrElse { emptyList() }
    }
    // Backward-compatible fallback for legacy comma-separated IDs
    return trimmed.split(",").mapNotNull { it.trim().toIntOrNull() }
  }

  @TypeConverter
  fun fromStringList(list: List<String>): String =
    json.encodeToString(stringListSerializer, list)

  @TypeConverter
  fun toStringList(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    val trimmed = raw.trim()
    if (trimmed.startsWith("[")) {
      return runCatching { json.decodeFromString(stringListSerializer, trimmed) }
        .getOrElse { emptyList() }
    }
    return trimmed.split(",").map { it.trim() }.filter { it.isNotEmpty() }
  }

  @TypeConverter
  fun fromElementTypeList(elements: List<ElementType>): String =
    json.encodeToString(stringListSerializer, elements.map { it.name })

  @TypeConverter
  fun toElementTypeList(raw: String): List<ElementType> {
    val names = toStringList(raw)
    return names.mapNotNull { name -> ElementType.entries.find { it.name == name } }
  }

  @TypeConverter
  fun fromSkillCombatEffectList(effects: List<SkillCombatEffect>): String =
    json.encodeToString(stringListSerializer, effects.map { it.name })

  @TypeConverter
  fun toSkillCombatEffectList(raw: String): List<SkillCombatEffect> {
    val names = toStringList(raw)
    return names.mapNotNull { name -> SkillCombatEffect.entries.find { it.name == name } }
  }

  @TypeConverter
  fun fromElementResistanceMap(resistances: Map<ElementType, Int>): String {
    val stringKeyMap = resistances.entries.associate { (element, pct) -> element.name to pct }
    return json.encodeToString(stringIntMapSerializer, stringKeyMap)
  }

  @TypeConverter
  fun toElementResistanceMap(raw: String): Map<ElementType, Int> {
    if (raw.isBlank()) return emptyMap()
    val decoded = runCatching { json.decodeFromString(stringIntMapSerializer, raw.trim()) }
      .getOrElse { emptyMap() }
    return decoded.entries.mapNotNull { (key, pct) ->
      ElementType.entries.find { it.name == key }?.let { it to pct }
    }.toMap()
  }

  @TypeConverter
  fun fromStatScalingMap(scaling: Map<String, Float>): String =
    json.encodeToString(stringFloatMapSerializer, scaling)

  @TypeConverter
  fun toStatScalingMap(raw: String): Map<String, Float> {
    if (raw.isBlank()) return emptyMap()
    return runCatching { json.decodeFromString(stringFloatMapSerializer, raw.trim()) }
      .getOrElse { emptyMap() }
  }
}
