package com.example.data

import com.example.model.ArmorEntity
import com.example.model.ArmorSkillCrossRef
import com.example.model.ArmorWithSkills
import com.example.model.ElementType
import com.example.model.RpgSystemOrigin
import com.example.model.SkillCategory
import com.example.model.SkillCombatEffect
import com.example.model.SkillEntity
import com.example.model.WeaponArmorClass
import com.example.model.WeaponEntity
import com.example.model.WeaponSkillCrossRef
import com.example.model.WeaponWithSkills
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

/**
 * Aggregated reactive snapshot of all normalized Weapons, Armor, and Skills
 * exposed to ViewModels via [ArmoryRepository].
 */
data class ArmoryCatalogSnapshot(
  val weapons: List<WeaponEntity> = emptyList(),
  val armors: List<ArmorEntity> = emptyList(),
  val skills: List<SkillEntity> = emptyList()
) {
  val totalEntries: Int
    get() = weapons.size + armors.size + skills.size
}

/**
 * Repository interface abstracting Room persistence for [WeaponEntity] records
 * and weapon Materia/skill sockets.
 */
interface WeaponRepository {
  val weapons: Flow<List<WeaponEntity>>
  val weaponsWithSkills: Flow<List<WeaponWithSkills>>

  fun observeWeaponById(weaponId: Int): Flow<WeaponEntity?>
  fun observeWeaponsBySystem(rpgSystem: RpgSystemOrigin): Flow<List<WeaponEntity>>
  fun observeWeaponsByClass(weaponClass: WeaponArmorClass): Flow<List<WeaponEntity>>
  fun observeWeaponsByElement(elementBoost: ElementType): Flow<List<WeaponEntity>>
  fun observeWeaponsBySetSeries(setSeriesName: String): Flow<List<WeaponEntity>>
  fun observeWeaponsForHero(heroId: Int): Flow<List<WeaponEntity>>
  fun searchWeapons(query: String): Flow<List<WeaponEntity>>

  suspend fun getAllWeapons(): List<WeaponEntity>
  suspend fun getWeaponById(weaponId: Int): WeaponEntity?
  suspend fun getWeaponWithSkills(weaponId: Int): WeaponWithSkills?
  suspend fun getWeaponCount(): Int

  suspend fun saveWeapon(weapon: WeaponEntity): Long
  suspend fun saveWeapons(weapons: List<WeaponEntity>): List<Long>
  suspend fun updateWeapon(weapon: WeaponEntity): Int
  suspend fun deleteWeapon(weapon: WeaponEntity): Int
  suspend fun deleteWeaponById(weaponId: Int): Int
  suspend fun clearWeapons(): Int

  suspend fun socketSkillInWeapon(weaponId: Int, skillId: Int, slotIndex: Int = 0): Long
  suspend fun unsocketSkillFromWeapon(weaponId: Int, skillId: Int): Int
  suspend fun clearWeaponSockets(weaponId: Int): Int
}

/**
 * Repository interface abstracting Room persistence for [ArmorEntity] records
 * and armor Materia/skill sockets.
 */
interface ArmorRepository {
  val armors: Flow<List<ArmorEntity>>
  val armorsWithSkills: Flow<List<ArmorWithSkills>>

  fun observeArmorById(armorId: Int): Flow<ArmorEntity?>
  fun observeArmorsBySystem(rpgSystem: RpgSystemOrigin): Flow<List<ArmorEntity>>
  fun observeArmorsByClass(armorClass: WeaponArmorClass): Flow<List<ArmorEntity>>
  fun observeArmorsByElementResist(element: ElementType): Flow<List<ArmorEntity>>
  fun observeArmorsBySetSeries(setSeriesName: String): Flow<List<ArmorEntity>>
  fun observeArmorsForHero(heroId: Int): Flow<List<ArmorEntity>>
  fun searchArmors(query: String): Flow<List<ArmorEntity>>

  suspend fun getAllArmors(): List<ArmorEntity>
  suspend fun getArmorById(armorId: Int): ArmorEntity?
  suspend fun getArmorWithSkills(armorId: Int): ArmorWithSkills?
  suspend fun getArmorCount(): Int

  suspend fun saveArmor(armor: ArmorEntity): Long
  suspend fun saveArmors(armors: List<ArmorEntity>): List<Long>
  suspend fun updateArmor(armor: ArmorEntity): Int
  suspend fun deleteArmor(armor: ArmorEntity): Int
  suspend fun deleteArmorById(armorId: Int): Int
  suspend fun clearArmors(): Int

  suspend fun socketSkillInArmor(armorId: Int, skillId: Int, slotIndex: Int = 0): Long
  suspend fun unsocketSkillFromArmor(armorId: Int, skillId: Int): Int
  suspend fun clearArmorSockets(armorId: Int): Int
}

/**
 * Repository interface abstracting Room persistence for [SkillEntity] records
 * across active techniques, magic, tactics arts, and passive traits.
 */
interface SkillRepository {
  val skills: Flow<List<SkillEntity>>

  fun observeSkillById(skillId: Int): Flow<SkillEntity?>
  fun observeSkillsByCategory(category: SkillCategory): Flow<List<SkillEntity>>
  fun observeSkillsBySystem(rpgSystem: RpgSystemOrigin): Flow<List<SkillEntity>>
  fun observeSkillsByElement(element: ElementType): Flow<List<SkillEntity>>
  fun observeSkillsByEffect(effectType: SkillCombatEffect): Flow<List<SkillEntity>>
  fun searchSkills(query: String): Flow<List<SkillEntity>>

  suspend fun getAllSkills(): List<SkillEntity>
  suspend fun getSkillById(skillId: Int): SkillEntity?
  suspend fun getSkillCount(): Int

  suspend fun saveSkill(skill: SkillEntity): Long
  suspend fun saveSkills(skills: List<SkillEntity>): List<Long>
  suspend fun updateSkill(skill: SkillEntity): Int
  suspend fun deleteSkill(skill: SkillEntity): Int
  suspend fun deleteSkillById(skillId: Int): Int
  suspend fun clearSkills(): Int
}

/**
 * Unified Armory Repository facade combining [WeaponRepository], [ArmorRepository],
 * and [SkillRepository] behind a clean API for ViewModels.
 */
interface ArmoryRepository : WeaponRepository, ArmorRepository, SkillRepository {
  fun observeArmoryCatalog(): Flow<ArmoryCatalogSnapshot>
}

// -----------------------------------------------------------------------------
// Room-backed Repository Implementations
// -----------------------------------------------------------------------------

@Singleton
class RoomWeaponRepository @Inject constructor(
  private val weaponDao: WeaponDao
) : WeaponRepository {

  override val weapons: Flow<List<WeaponEntity>> = weaponDao.observeWeapons()
  override val weaponsWithSkills: Flow<List<WeaponWithSkills>> = weaponDao.observeWeaponsWithSkills()

  override fun observeWeaponById(weaponId: Int): Flow<WeaponEntity?> =
    weaponDao.observeWeaponById(weaponId)

  override fun observeWeaponsBySystem(rpgSystem: RpgSystemOrigin): Flow<List<WeaponEntity>> =
    weaponDao.observeWeaponsBySystem(rpgSystem)

  override fun observeWeaponsByClass(weaponClass: WeaponArmorClass): Flow<List<WeaponEntity>> =
    weaponDao.observeWeaponsByClass(weaponClass)

  override fun observeWeaponsByElement(elementBoost: ElementType): Flow<List<WeaponEntity>> =
    weaponDao.observeWeaponsByElement(elementBoost)

  override fun observeWeaponsBySetSeries(setSeriesName: String): Flow<List<WeaponEntity>> =
    weaponDao.observeWeaponsBySetSeries(setSeriesName)

  override fun observeWeaponsForHero(heroId: Int): Flow<List<WeaponEntity>> =
    weaponDao.observeWeaponsForHero(heroId)

  override fun searchWeapons(query: String): Flow<List<WeaponEntity>> =
    weaponDao.searchWeaponsByName(query.trim())

  override suspend fun getAllWeapons(): List<WeaponEntity> = weaponDao.getAllWeapons()
  override suspend fun getWeaponById(weaponId: Int): WeaponEntity? = weaponDao.getWeaponById(weaponId)
  override suspend fun getWeaponWithSkills(weaponId: Int): WeaponWithSkills? =
    weaponDao.getWeaponWithSkillsById(weaponId)

  override suspend fun getWeaponCount(): Int = weaponDao.getWeaponCount()

  override suspend fun saveWeapon(weapon: WeaponEntity): Long = weaponDao.upsertWeapon(weapon)
  override suspend fun saveWeapons(weapons: List<WeaponEntity>): List<Long> =
    weaponDao.upsertWeapons(weapons)

  override suspend fun updateWeapon(weapon: WeaponEntity): Int = weaponDao.updateWeapon(weapon)
  override suspend fun deleteWeapon(weapon: WeaponEntity): Int = weaponDao.deleteWeapon(weapon)
  override suspend fun deleteWeaponById(weaponId: Int): Int = weaponDao.deleteWeaponById(weaponId)
  override suspend fun clearWeapons(): Int = weaponDao.deleteAllWeapons()

  override suspend fun socketSkillInWeapon(weaponId: Int, skillId: Int, slotIndex: Int): Long =
    weaponDao.insertWeaponSkillSocket(WeaponSkillCrossRef(weaponId, skillId, slotIndex))

  override suspend fun unsocketSkillFromWeapon(weaponId: Int, skillId: Int): Int =
    weaponDao.deleteWeaponSkillSocket(weaponId, skillId)

  override suspend fun clearWeaponSockets(weaponId: Int): Int =
    weaponDao.clearWeaponSkillSockets(weaponId)
}

@Singleton
class RoomArmorRepository @Inject constructor(
  private val armorDao: ArmorDao
) : ArmorRepository {

  override val armors: Flow<List<ArmorEntity>> = armorDao.observeArmors()
  override val armorsWithSkills: Flow<List<ArmorWithSkills>> = armorDao.observeArmorsWithSkills()

  override fun observeArmorById(armorId: Int): Flow<ArmorEntity?> =
    armorDao.observeArmorById(armorId)

  override fun observeArmorsBySystem(rpgSystem: RpgSystemOrigin): Flow<List<ArmorEntity>> =
    armorDao.observeArmorsBySystem(rpgSystem)

  override fun observeArmorsByClass(armorClass: WeaponArmorClass): Flow<List<ArmorEntity>> =
    armorDao.observeArmorsByClass(armorClass)

  override fun observeArmorsByElementResist(element: ElementType): Flow<List<ArmorEntity>> =
    armorDao.observeArmorsByElementResist(element)

  override fun observeArmorsBySetSeries(setSeriesName: String): Flow<List<ArmorEntity>> =
    armorDao.observeArmorsBySetSeries(setSeriesName)

  override fun observeArmorsForHero(heroId: Int): Flow<List<ArmorEntity>> =
    armorDao.observeArmorsForHero(heroId)

  override fun searchArmors(query: String): Flow<List<ArmorEntity>> =
    armorDao.searchArmorsByName(query.trim())

  override suspend fun getAllArmors(): List<ArmorEntity> = armorDao.getAllArmors()
  override suspend fun getArmorById(armorId: Int): ArmorEntity? = armorDao.getArmorById(armorId)
  override suspend fun getArmorWithSkills(armorId: Int): ArmorWithSkills? =
    armorDao.getArmorWithSkillsById(armorId)

  override suspend fun getArmorCount(): Int = armorDao.getArmorCount()

  override suspend fun saveArmor(armor: ArmorEntity): Long = armorDao.upsertArmor(armor)
  override suspend fun saveArmors(armors: List<ArmorEntity>): List<Long> =
    armorDao.upsertArmors(armors)

  override suspend fun updateArmor(armor: ArmorEntity): Int = armorDao.updateArmor(armor)
  override suspend fun deleteArmor(armor: ArmorEntity): Int = armorDao.deleteArmor(armor)
  override suspend fun deleteArmorById(armorId: Int): Int = armorDao.deleteArmorById(armorId)
  override suspend fun clearArmors(): Int = armorDao.deleteAllArmors()

  override suspend fun socketSkillInArmor(armorId: Int, skillId: Int, slotIndex: Int): Long =
    armorDao.insertArmorSkillSocket(ArmorSkillCrossRef(armorId, skillId, slotIndex))

  override suspend fun unsocketSkillFromArmor(armorId: Int, skillId: Int): Int =
    armorDao.deleteArmorSkillSocket(armorId, skillId)

  override suspend fun clearArmorSockets(armorId: Int): Int =
    armorDao.clearArmorSkillSockets(armorId)
}

@Singleton
class RoomSkillRepository @Inject constructor(
  private val skillDao: SkillDao
) : SkillRepository {

  override val skills: Flow<List<SkillEntity>> = skillDao.observeSkills()

  override fun observeSkillById(skillId: Int): Flow<SkillEntity?> =
    skillDao.observeSkillById(skillId)

  override fun observeSkillsByCategory(category: SkillCategory): Flow<List<SkillEntity>> =
    skillDao.observeSkillsByCategoryEnum(category)

  override fun observeSkillsBySystem(rpgSystem: RpgSystemOrigin): Flow<List<SkillEntity>> =
    skillDao.observeSkillsBySystemEnum(rpgSystem)

  override fun observeSkillsByElement(element: ElementType): Flow<List<SkillEntity>> =
    skillDao.observeSkillsByElementEnum(element)

  override fun observeSkillsByEffect(effectType: SkillCombatEffect): Flow<List<SkillEntity>> =
    skillDao.observeSkillsByEffect(effectType)

  override fun searchSkills(query: String): Flow<List<SkillEntity>> =
    skillDao.searchSkillsByName(query.trim())

  override suspend fun getAllSkills(): List<SkillEntity> = skillDao.getAllSkills()
  override suspend fun getSkillById(skillId: Int): SkillEntity? = skillDao.getSkillById(skillId)
  override suspend fun getSkillCount(): Int = skillDao.getSkillCount()

  override suspend fun saveSkill(skill: SkillEntity): Long = skillDao.upsertSkill(skill)
  override suspend fun saveSkills(skills: List<SkillEntity>): List<Long> =
    skillDao.upsertSkills(skills)

  override suspend fun updateSkill(skill: SkillEntity): Int = skillDao.updateSkill(skill)
  override suspend fun deleteSkill(skill: SkillEntity): Int = skillDao.deleteSkill(skill)
  override suspend fun deleteSkillById(skillId: Int): Int = skillDao.deleteSkillById(skillId)
  override suspend fun clearSkills(): Int = skillDao.deleteAllSkills()
}

@Singleton
class RoomArmoryRepository @Inject constructor(
  private val weaponRepository: WeaponRepository,
  private val armorRepository: ArmorRepository,
  private val skillRepository: SkillRepository
) : ArmoryRepository,
  WeaponRepository by weaponRepository,
  ArmorRepository by armorRepository,
  SkillRepository by skillRepository {

  override fun observeArmoryCatalog(): Flow<ArmoryCatalogSnapshot> = combine(
    weaponRepository.weapons,
    armorRepository.armors,
    skillRepository.skills
  ) { weapons, armors, skills ->
    ArmoryCatalogSnapshot(
      weapons = weapons,
      armors = armors,
      skills = skills
    )
  }
}

/**
 * Fallback no-op [ArmoryRepository] used only when legacy callers instantiate [RpgRepository]
 * without optional weapon/armor/skill DAOs.
 */
internal object EmptyArmoryRepository : ArmoryRepository {
  override val weapons: Flow<List<WeaponEntity>> = flowOf(emptyList())
  override val weaponsWithSkills: Flow<List<WeaponWithSkills>> = flowOf(emptyList())
  override val armors: Flow<List<ArmorEntity>> = flowOf(emptyList())
  override val armorsWithSkills: Flow<List<ArmorWithSkills>> = flowOf(emptyList())
  override val skills: Flow<List<SkillEntity>> = flowOf(emptyList())

  override fun observeArmoryCatalog(): Flow<ArmoryCatalogSnapshot> = flowOf(ArmoryCatalogSnapshot())

  override fun observeWeaponById(weaponId: Int): Flow<WeaponEntity?> = flowOf(null)
  override fun observeWeaponsBySystem(rpgSystem: RpgSystemOrigin): Flow<List<WeaponEntity>> = flowOf(emptyList())
  override fun observeWeaponsByClass(weaponClass: WeaponArmorClass): Flow<List<WeaponEntity>> = flowOf(emptyList())
  override fun observeWeaponsByElement(elementBoost: ElementType): Flow<List<WeaponEntity>> = flowOf(emptyList())
  override fun observeWeaponsBySetSeries(setSeriesName: String): Flow<List<WeaponEntity>> = flowOf(emptyList())
  override fun observeWeaponsForHero(heroId: Int): Flow<List<WeaponEntity>> = flowOf(emptyList())
  override fun searchWeapons(query: String): Flow<List<WeaponEntity>> = flowOf(emptyList())
  override suspend fun getAllWeapons(): List<WeaponEntity> = emptyList()
  override suspend fun getWeaponById(weaponId: Int): WeaponEntity? = null
  override suspend fun getWeaponWithSkills(weaponId: Int): WeaponWithSkills? = null
  override suspend fun getWeaponCount(): Int = 0
  override suspend fun saveWeapon(weapon: WeaponEntity): Long = -1L
  override suspend fun saveWeapons(weapons: List<WeaponEntity>): List<Long> = emptyList()
  override suspend fun updateWeapon(weapon: WeaponEntity): Int = 0
  override suspend fun deleteWeapon(weapon: WeaponEntity): Int = 0
  override suspend fun deleteWeaponById(weaponId: Int): Int = 0
  override suspend fun clearWeapons(): Int = 0
  override suspend fun socketSkillInWeapon(weaponId: Int, skillId: Int, slotIndex: Int): Long = -1L
  override suspend fun unsocketSkillFromWeapon(weaponId: Int, skillId: Int): Int = 0
  override suspend fun clearWeaponSockets(weaponId: Int): Int = 0

  override fun observeArmorById(armorId: Int): Flow<ArmorEntity?> = flowOf(null)
  override fun observeArmorsBySystem(rpgSystem: RpgSystemOrigin): Flow<List<ArmorEntity>> = flowOf(emptyList())
  override fun observeArmorsByClass(armorClass: WeaponArmorClass): Flow<List<ArmorEntity>> = flowOf(emptyList())
  override fun observeArmorsByElementResist(element: ElementType): Flow<List<ArmorEntity>> = flowOf(emptyList())
  override fun observeArmorsBySetSeries(setSeriesName: String): Flow<List<ArmorEntity>> = flowOf(emptyList())
  override fun observeArmorsForHero(heroId: Int): Flow<List<ArmorEntity>> = flowOf(emptyList())
  override fun searchArmors(query: String): Flow<List<ArmorEntity>> = flowOf(emptyList())
  override suspend fun getAllArmors(): List<ArmorEntity> = emptyList()
  override suspend fun getArmorById(armorId: Int): ArmorEntity? = null
  override suspend fun getArmorWithSkills(armorId: Int): ArmorWithSkills? = null
  override suspend fun getArmorCount(): Int = 0
  override suspend fun saveArmor(armor: ArmorEntity): Long = -1L
  override suspend fun saveArmors(armors: List<ArmorEntity>): List<Long> = emptyList()
  override suspend fun updateArmor(armor: ArmorEntity): Int = 0
  override suspend fun deleteArmor(armor: ArmorEntity): Int = 0
  override suspend fun deleteArmorById(armorId: Int): Int = 0
  override suspend fun clearArmors(): Int = 0
  override suspend fun socketSkillInArmor(armorId: Int, skillId: Int, slotIndex: Int): Long = -1L
  override suspend fun unsocketSkillFromArmor(armorId: Int, skillId: Int): Int = 0
  override suspend fun clearArmorSockets(armorId: Int): Int = 0

  override fun observeSkillById(skillId: Int): Flow<SkillEntity?> = flowOf(null)
  override fun observeSkillsByCategory(category: SkillCategory): Flow<List<SkillEntity>> = flowOf(emptyList())
  override fun observeSkillsBySystem(rpgSystem: RpgSystemOrigin): Flow<List<SkillEntity>> = flowOf(emptyList())
  override fun observeSkillsByElement(element: ElementType): Flow<List<SkillEntity>> = flowOf(emptyList())
  override fun observeSkillsByEffect(effectType: SkillCombatEffect): Flow<List<SkillEntity>> = flowOf(emptyList())
  override fun searchSkills(query: String): Flow<List<SkillEntity>> = flowOf(emptyList())
  override suspend fun getAllSkills(): List<SkillEntity> = emptyList()
  override suspend fun getSkillById(skillId: Int): SkillEntity? = null
  override suspend fun getSkillCount(): Int = 0
  override suspend fun saveSkill(skill: SkillEntity): Long = -1L
  override suspend fun saveSkills(skills: List<SkillEntity>): List<Long> = emptyList()
  override suspend fun updateSkill(skill: SkillEntity): Int = 0
  override suspend fun deleteSkill(skill: SkillEntity): Int = 0
  override suspend fun deleteSkillById(skillId: Int): Int = 0
  override suspend fun clearSkills(): Int = 0
}
