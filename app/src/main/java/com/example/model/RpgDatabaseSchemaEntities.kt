package com.example.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.Relation

/**
 * Many-to-many junction table linking socketed [SkillEntity] Materia orbs to a [WeaponEntity].
 */
@Entity(
  tableName = "weapon_skill_sockets",
  primaryKeys = ["weaponId", "skillId"],
  foreignKeys = [
    ForeignKey(
      entity = WeaponEntity::class,
      parentColumns = ["id"],
      childColumns = ["weaponId"],
      onDelete = ForeignKey.CASCADE
    ),
    ForeignKey(
      entity = SkillEntity::class,
      parentColumns = ["id"],
      childColumns = ["skillId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index(value = ["weaponId"]),
    Index(value = ["skillId"])
  ]
)
data class WeaponSkillCrossRef(
  @ColumnInfo(name = "weaponId") val weaponId: Int,
  @ColumnInfo(name = "skillId") val skillId: Int,
  @ColumnInfo(name = "slotIndex") val slotIndex: Int = 0
)

/**
 * Many-to-many junction table linking socketed [SkillEntity] Materia orbs to an [ArmorEntity].
 */
@Entity(
  tableName = "armor_skill_sockets",
  primaryKeys = ["armorId", "skillId"],
  foreignKeys = [
    ForeignKey(
      entity = ArmorEntity::class,
      parentColumns = ["id"],
      childColumns = ["armorId"],
      onDelete = ForeignKey.CASCADE
    ),
    ForeignKey(
      entity = SkillEntity::class,
      parentColumns = ["id"],
      childColumns = ["skillId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index(value = ["armorId"]),
    Index(value = ["skillId"])
  ]
)
data class ArmorSkillCrossRef(
  @ColumnInfo(name = "armorId") val armorId: Int,
  @ColumnInfo(name = "skillId") val skillId: Int,
  @ColumnInfo(name = "slotIndex") val slotIndex: Int = 0
)

/**
 * Relational Room projection joining a [WeaponEntity] with its granted [SkillEntity]
 * and all Materia-socketed [SkillEntity] rows via [WeaponSkillCrossRef].
 */
data class WeaponWithSkills(
  @Embedded val weapon: WeaponEntity,
  @Relation(
    parentColumn = "grantedSkillId",
    entityColumn = "id"
  )
  val grantedSkill: SkillEntity?,
  @Relation(
    parentColumn = "id",
    entityColumn = "id",
    associateBy = Junction(
      value = WeaponSkillCrossRef::class,
      parentColumn = "weaponId",
      entityColumn = "skillId"
    )
  )
  val socketedSkills: List<SkillEntity>
)

/**
 * Relational Room projection joining an [ArmorEntity] with its granted [SkillEntity]
 * and all Materia-socketed [SkillEntity] rows via [ArmorSkillCrossRef].
 */
data class ArmorWithSkills(
  @Embedded val armor: ArmorEntity,
  @Relation(
    parentColumn = "grantedSkillId",
    entityColumn = "id"
  )
  val grantedSkill: SkillEntity?,
  @Relation(
    parentColumn = "id",
    entityColumn = "id",
    associateBy = Junction(
      value = ArmorSkillCrossRef::class,
      parentColumn = "armorId",
      entityColumn = "skillId"
    )
  )
  val socketedSkills: List<SkillEntity>
)

/**
 * Relational Room projection joining a [HeroEntity] with its equipped [WeaponEntity]
 * and [ArmorEntity] records.
 */
data class HeroGearRelation(
  @Embedded val hero: HeroEntity,
  @Relation(
    parentColumn = "id",
    entityColumn = "equippedByHeroId"
  )
  val equippedWeapons: List<WeaponEntity>,
  @Relation(
    parentColumn = "id",
    entityColumn = "equippedByHeroId"
  )
  val equippedArmors: List<ArmorEntity>
)
