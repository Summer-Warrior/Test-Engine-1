package com.example.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room [Entity] representing the `armors` database table for [com.example.data.ArmorDao].
 *
 * Stores defense and resistance ratings, multi-element resistance profiles, status immunities,
 * counter-attack rates, compatible job classes, Materia/skill sockets, and foreign key links to
 * [SkillEntity] (`grantedSkillId`) and [HeroEntity] (`equippedByHeroId`).
 */
@Entity(
  tableName = "armors",
  foreignKeys = [
    ForeignKey(
      entity = SkillEntity::class,
      parentColumns = ["id"],
      childColumns = ["grantedSkillId"],
      onDelete = ForeignKey.SET_NULL
    ),
    ForeignKey(
      entity = HeroEntity::class,
      parentColumns = ["id"],
      childColumns = ["equippedByHeroId"],
      onDelete = ForeignKey.SET_NULL
    )
  ],
  indices = [
    Index(value = ["originRealm"]),
    Index(value = ["rpgSystem"]),
    Index(value = ["armorClass"]),
    Index(value = ["elementResistElement"]),
    Index(value = ["setSeriesName"]),
    Index(value = ["grantedSkillId"]),
    Index(value = ["equippedByHeroId"])
  ]
)
data class ArmorEntity(
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  val id: Int = 0,
  @ColumnInfo(name = "name")
  val name: String,
  @ColumnInfo(name = "armorClass")
  val armorClass: WeaponArmorClass = WeaponArmorClass.HEAVY_PLATE,
  @ColumnInfo(name = "rpgSystem")
  val rpgSystem: RpgSystemOrigin = RpgSystemOrigin.FFRK,
  @ColumnInfo(name = "originRealm")
  val originRealm: String = "Record Keep",
  @ColumnInfo(name = "setSeriesName")
  val setSeriesName: String = "",
  @ColumnInfo(name = "rarityStars")
  val rarityStars: Int = 6,
  @ColumnInfo(name = "upgradeLevel")
  val upgradeLevel: Int = 25,
  @ColumnInfo(name = "reforgeTier")
  val reforgeTier: Int = 0,
  @ColumnInfo(name = "defenseRating")
  val defenseRating: Int = 165,
  @ColumnInfo(name = "resistanceRating")
  val resistanceRating: Int = 160,
  @ColumnInfo(name = "bonusHp")
  val bonusHp: Int = 850,
  @ColumnInfo(name = "bonusAtk")
  val bonusAtk: Int = 45,
  @ColumnInfo(name = "bonusMag")
  val bonusMag: Int = 45,
  @ColumnInfo(name = "bonusSpd")
  val bonusSpd: Int = 8,
  @ColumnInfo(name = "elementResistElement")
  val elementResistElement: ElementType = ElementType.NON_ELEMENTAL,
  @ColumnInfo(name = "elementResistPercent")
  val elementResistPercent: Int = 0,
  @ColumnInfo(name = "elementalResistances")
  val elementalResistances: Map<ElementType, Int> = emptyMap(),
  @ColumnInfo(name = "statusImmunities")
  val statusImmunities: List<String> = emptyList(),
  @ColumnInfo(name = "compatibleJobs")
  val compatibleJobs: List<String> = emptyList(),
  @ColumnInfo(name = "counterChancePercent")
  val counterChancePercent: Int = 0,
  @ColumnInfo(name = "spCostReduction")
  val spCostReduction: Int = 0,
  @ColumnInfo(name = "materiaSocketsMax")
  val materiaSocketsMax: Int = 2,
  @ColumnInfo(name = "socketedSkillIds")
  val socketedSkillIds: List<Int> = emptyList(),
  @ColumnInfo(name = "grantedSkillId")
  val grantedSkillId: Int? = null,
  @ColumnInfo(name = "passiveTrait")
  val passiveTrait: String = "",
  @ColumnInfo(name = "equippedByHeroId")
  val equippedByHeroId: Int? = null
) {
  fun toEquipmentEntity(): EquipmentEntity = EquipmentEntity(
    id = id,
    name = name,
    slotType = EquipmentSlotType.ARMOR.name,
    originRealm = originRealm,
    rarityStars = rarityStars,
    upgradeLevel = upgradeLevel,
    bonusHp = bonusHp,
    bonusAtk = bonusAtk,
    bonusDef = defenseRating,
    bonusMag = bonusMag,
    bonusRes = resistanceRating,
    bonusSpd = bonusSpd,
    elementBoost = elementResistElement.name,
    passiveTrait = passiveTrait,
    equippedByHeroId = equippedByHeroId,
    rpgSystem = rpgSystem.name,
    weaponArmorClass = armorClass.name,
    setSeriesName = setSeriesName,
    elementResistElement = elementResistElement.name,
    elementResistPercent = elementResistPercent,
    counterChancePercent = counterChancePercent,
    spCostReduction = spCostReduction,
    materiaSocketsMax = materiaSocketsMax,
    socketedSkillIdsCsv = socketedSkillIds.joinToString(","),
    grantedSkillId = grantedSkillId,
    reforgeTier = reforgeTier
  )

  companion object {
    fun fromEquipmentEntity(equipment: EquipmentEntity): ArmorEntity {
      val resolvedClass = equipment.weaponArmorClassEnum
      val resistMap = buildMap {
        if (equipment.elementResistPercent > 0 && equipment.elementResistEnum != ElementType.NON_ELEMENTAL) {
          put(equipment.elementResistEnum, equipment.elementResistPercent)
        }
      }
      val immunities = when (resolvedClass) {
        WeaponArmorClass.HEAVY_PLATE, WeaponArmorClass.AEGIS_SHIELD -> listOf("Stun", "Knockback")
        WeaponArmorClass.MYSTIC_ROBE -> listOf("Silence", "Confuse")
        WeaponArmorClass.LIGHT_ARMOR -> listOf("Slow", "Blind")
        else -> emptyList()
      }
      val defaultJobs = when (resolvedClass) {
        WeaponArmorClass.HEAVY_PLATE, WeaponArmorClass.AEGIS_SHIELD ->
          listOf("SOLDIER 1st Class", "Holy Paladin", "Dark Knight", "Dragoon")
        WeaponArmorClass.MYSTIC_ROBE ->
          listOf("Esper Sage", "Black Wizard", "Time Mage", "Archivist Keeper")
        WeaponArmorClass.LIGHT_ARMOR ->
          listOf("Union Keybearer", "Freelancer", "Dragoon")
        else -> listOf("Freelancer")
      }
      return ArmorEntity(
        id = equipment.id,
        name = equipment.name,
        armorClass = resolvedClass,
        rpgSystem = equipment.rpgSystemEnum,
        originRealm = equipment.originRealm,
        setSeriesName = equipment.setSeriesName,
        rarityStars = equipment.rarityStars,
        upgradeLevel = equipment.upgradeLevel,
        reforgeTier = equipment.reforgeTier,
        defenseRating = equipment.bonusDef,
        resistanceRating = equipment.bonusRes,
        bonusHp = equipment.bonusHp,
        bonusAtk = equipment.bonusAtk,
        bonusMag = equipment.bonusMag,
        bonusSpd = equipment.bonusSpd,
        elementResistElement = equipment.elementResistEnum,
        elementResistPercent = equipment.elementResistPercent,
        elementalResistances = resistMap,
        statusImmunities = immunities,
        compatibleJobs = defaultJobs,
        counterChancePercent = equipment.counterChancePercent,
        spCostReduction = equipment.spCostReduction,
        materiaSocketsMax = equipment.materiaSocketsMax,
        socketedSkillIds = equipment.socketedSkillIdList(),
        grantedSkillId = equipment.grantedSkillId,
        passiveTrait = equipment.passiveTrait,
        equippedByHeroId = equipment.equippedByHeroId
      )
    }
  }
}

/** Semantic domain alias for the [ArmorEntity] Room table data class. */
typealias Armor = ArmorEntity
