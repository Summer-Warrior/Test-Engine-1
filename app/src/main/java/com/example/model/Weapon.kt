package com.example.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room [Entity] representing the `weapons` database table for [com.example.data.WeaponDao].
 *
 * Stores weapon stats, RPG system origin, weapon class, elemental affinities, compatible job
 * classes, stat scaling multipliers, Materia/skill sockets, and foreign key references to
 * [SkillEntity] (`grantedSkillId`) and [HeroEntity] (`equippedByHeroId`).
 */
@Entity(
  tableName = "weapons",
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
    Index(value = ["weaponClass"]),
    Index(value = ["elementBoost"]),
    Index(value = ["setSeriesName"]),
    Index(value = ["grantedSkillId"]),
    Index(value = ["equippedByHeroId"])
  ]
)
data class WeaponEntity(
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  val id: Int = 0,
  @ColumnInfo(name = "name")
  val name: String,
  @ColumnInfo(name = "weaponClass")
  val weaponClass: WeaponArmorClass = WeaponArmorClass.KEYBLADE,
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
  @ColumnInfo(name = "attackPower")
  val attackPower: Int = 160,
  @ColumnInfo(name = "magicPower")
  val magicPower: Int = 120,
  @ColumnInfo(name = "bonusHp")
  val bonusHp: Int = 400,
  @ColumnInfo(name = "bonusDef")
  val bonusDef: Int = 80,
  @ColumnInfo(name = "bonusRes")
  val bonusRes: Int = 80,
  @ColumnInfo(name = "bonusSpd")
  val bonusSpd: Int = 12,
  @ColumnInfo(name = "elementBoost")
  val elementBoost: ElementType = ElementType.NON_ELEMENTAL,
  @ColumnInfo(name = "critRateBonus")
  val critRateBonus: Float = 0f,
  @ColumnInfo(name = "critDamageBonus")
  val critDamageBonus: Float = 0f,
  @ColumnInfo(name = "lifestealPercent")
  val lifestealPercent: Int = 0,
  @ColumnInfo(name = "spCostReduction")
  val spCostReduction: Int = 0,
  @ColumnInfo(name = "materiaSocketsMax")
  val materiaSocketsMax: Int = 2,
  @ColumnInfo(name = "socketedSkillIds")
  val socketedSkillIds: List<Int> = emptyList(),
  @ColumnInfo(name = "secondaryElements")
  val secondaryElements: List<ElementType> = emptyList(),
  @ColumnInfo(name = "compatibleJobs")
  val compatibleJobs: List<String> = emptyList(),
  @ColumnInfo(name = "statScaling")
  val statScaling: Map<String, Float> = emptyMap(),
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
    slotType = EquipmentSlotType.WEAPON.name,
    originRealm = originRealm,
    rarityStars = rarityStars,
    upgradeLevel = upgradeLevel,
    bonusHp = bonusHp,
    bonusAtk = attackPower,
    bonusDef = bonusDef,
    bonusMag = magicPower,
    bonusRes = bonusRes,
    bonusSpd = bonusSpd,
    elementBoost = elementBoost.name,
    passiveTrait = passiveTrait,
    equippedByHeroId = equippedByHeroId,
    rpgSystem = rpgSystem.name,
    weaponArmorClass = weaponClass.name,
    setSeriesName = setSeriesName,
    critRateBonus = critRateBonus,
    critDamageBonus = critDamageBonus,
    lifestealPercent = lifestealPercent,
    spCostReduction = spCostReduction,
    materiaSocketsMax = materiaSocketsMax,
    socketedSkillIdsCsv = socketedSkillIds.joinToString(","),
    grantedSkillId = grantedSkillId,
    reforgeTier = reforgeTier
  )

  companion object {
    fun fromEquipmentEntity(equipment: EquipmentEntity): WeaponEntity {
      val resolvedClass = equipment.weaponArmorClassEnum
      val defaultJobs = when (resolvedClass) {
        WeaponArmorClass.KEYBLADE -> listOf("Union Keybearer", "Freelancer")
        WeaponArmorClass.GREATSWORD -> listOf("SOLDIER 1st Class", "Dark Knight", "Holy Paladin")
        WeaponArmorClass.KATANA -> listOf("SOLDIER 1st Class", "Dark Knight")
        WeaponArmorClass.STAFF -> listOf("Black Wizard", "Esper Sage", "Time Mage")
        WeaponArmorClass.GRIMOIRE -> listOf("Archivist Keeper", "Esper Sage")
        WeaponArmorClass.DAGGER -> listOf("Freelancer", "Time Mage")
        WeaponArmorClass.SPEAR -> listOf("Dragoon", "Holy Paladin")
        else -> listOf("Freelancer")
      }
      val defaultScaling = when (resolvedClass) {
        WeaponArmorClass.STAFF, WeaponArmorClass.GRIMOIRE -> mapOf("MAG" to 1.25f, "MND" to 0.35f)
        WeaponArmorClass.KEYBLADE -> mapOf("ATK" to 0.90f, "MAG" to 0.75f, "SPD" to 0.25f)
        WeaponArmorClass.DAGGER -> mapOf("ATK" to 0.85f, "SPD" to 0.55f)
        else -> mapOf("ATK" to 1.20f, "SPD" to 0.20f)
      }
      val secondaryElems = when (equipment.elementBoostEnum) {
        ElementType.HOLY -> listOf(ElementType.LIGHTNING)
        ElementType.FIRE -> listOf(ElementType.WIND)
        ElementType.DARK -> listOf(ElementType.ICE)
        else -> emptyList()
      }
      return WeaponEntity(
        id = equipment.id,
        name = equipment.name,
        weaponClass = resolvedClass,
        rpgSystem = equipment.rpgSystemEnum,
        originRealm = equipment.originRealm,
        setSeriesName = equipment.setSeriesName,
        rarityStars = equipment.rarityStars,
        upgradeLevel = equipment.upgradeLevel,
        reforgeTier = equipment.reforgeTier,
        attackPower = equipment.bonusAtk,
        magicPower = equipment.bonusMag,
        bonusHp = equipment.bonusHp,
        bonusDef = equipment.bonusDef,
        bonusRes = equipment.bonusRes,
        bonusSpd = equipment.bonusSpd,
        elementBoost = equipment.elementBoostEnum,
        critRateBonus = equipment.critRateBonus,
        critDamageBonus = equipment.critDamageBonus,
        lifestealPercent = equipment.lifestealPercent,
        spCostReduction = equipment.spCostReduction,
        materiaSocketsMax = equipment.materiaSocketsMax,
        socketedSkillIds = equipment.socketedSkillIdList(),
        secondaryElements = secondaryElems,
        compatibleJobs = defaultJobs,
        statScaling = defaultScaling,
        grantedSkillId = equipment.grantedSkillId,
        passiveTrait = equipment.passiveTrait,
        equippedByHeroId = equipment.equippedByHeroId
      )
    }
  }
}

/** Semantic domain alias for the [WeaponEntity] Room table data class. */
typealias Weapon = WeaponEntity
