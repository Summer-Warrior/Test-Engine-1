package com.example.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room [Entity] representing the `skills` database table for [com.example.data.SkillDao].
 *
 * Stores multi-RPG Active Techniques, Black/White Magic Spells, Tactics Job Arts, and Passive
 * Mastery Traits along with SP cost, cooldown turns, power multipliers, passive bonuses,
 * multi-elemental affinities, secondary combat effects, and stat scaling weights.
 */
@Entity(
  tableName = "skills",
  indices = [
    Index(value = ["category"]),
    Index(value = ["rpgSystem"]),
    Index(value = ["element"]),
    Index(value = ["effectType"])
  ]
)
data class SkillEntity(
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  val id: Int = 0,
  @ColumnInfo(name = "name")
  val name: String,
  @ColumnInfo(name = "rpgSystem")
  val rpgSystem: String = RpgSystemOrigin.IVALICE_TACTICS.name,
  @ColumnInfo(name = "category")
  val category: String = SkillCategory.ACTIVE_TECH.name,
  @ColumnInfo(name = "element")
  val element: String = ElementType.NON_ELEMENTAL.name,
  @ColumnInfo(name = "attribute")
  val attribute: String = MedalAttribute.POWER.name,
  @ColumnInfo(name = "spCost")
  val spCost: Int = 2,
  @ColumnInfo(name = "cooldownTurns")
  val cooldownTurns: Int = 1,
  @ColumnInfo(name = "powerMultiplier")
  val powerMultiplier: Float = 2.6f,
  @ColumnInfo(name = "hitCount")
  val hitCount: Int = 3,
  @ColumnInfo(name = "effectType")
  val effectType: String = SkillCombatEffect.DAMAGE.name,
  @ColumnInfo(name = "rankLevel")
  val rankLevel: Int = 1,
  @ColumnInfo(name = "maxRank")
  val maxRank: Int = 5,
  @ColumnInfo(name = "description")
  val description: String = "Multi-RPG combat ability.",
  @ColumnInfo(name = "passiveAtkPercent")
  val passiveAtkPercent: Int = 0,
  @ColumnInfo(name = "passiveMagPercent")
  val passiveMagPercent: Int = 0,
  @ColumnInfo(name = "passiveDefPercent")
  val passiveDefPercent: Int = 0,
  @ColumnInfo(name = "passiveSpdBonus")
  val passiveSpdBonus: Int = 0,
  @ColumnInfo(name = "passiveCritBonus")
  val passiveCritBonus: Float = 0f,
  @ColumnInfo(name = "passiveLifestealPercent")
  val passiveLifestealPercent: Int = 0,
  @ColumnInfo(name = "passiveCounterPercent")
  val passiveCounterPercent: Int = 0,
  @ColumnInfo(name = "bonusElements")
  val bonusElements: List<ElementType> = emptyList(),
  @ColumnInfo(name = "secondaryEffects")
  val secondaryEffects: List<SkillCombatEffect> = emptyList(),
  @ColumnInfo(name = "requiredJobClasses")
  val requiredJobClasses: List<String> = emptyList(),
  @ColumnInfo(name = "scalingWeights")
  val scalingWeights: Map<String, Float> = emptyMap()
) {
  val rpgSystemEnum: RpgSystemOrigin
    get() = RpgSystemOrigin.entries.find { it.name == rpgSystem } ?: RpgSystemOrigin.IVALICE_TACTICS

  val categoryEnum: SkillCategory
    get() = SkillCategory.entries.find { it.name == category } ?: SkillCategory.ACTIVE_TECH

  val elementEnum: ElementType
    get() = ElementType.entries.find { it.name == element } ?: ElementType.NON_ELEMENTAL

  val attributeEnum: MedalAttribute
    get() = MedalAttribute.entries.find { it.name == attribute } ?: MedalAttribute.POWER

  val effectTypeEnum: SkillCombatEffect
    get() = SkillCombatEffect.entries.find { it.name == effectType } ?: SkillCombatEffect.DAMAGE

  val effectiveMultiplier: Float
    get() = powerMultiplier + (rankLevel - 1) * 0.35f
}

/** Semantic domain alias for the [SkillEntity] Room table data class. */
typealias Skill = SkillEntity
