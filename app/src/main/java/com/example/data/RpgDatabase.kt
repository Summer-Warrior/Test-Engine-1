package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.model.ArmorEntity
import com.example.model.ArmorSkillCrossRef
import com.example.model.ArmorWithSkills
import com.example.model.BattleLogEntity
import com.example.model.BossArchetype
import com.example.model.ElementType
import com.example.model.EngineConfigEntity
import com.example.model.EquipmentEntity
import com.example.model.EquipmentSlotType
import com.example.model.HeroArchetype
import com.example.model.HeroEntity
import com.example.model.ImportedAssetEntity
import com.example.model.MedalAlignment
import com.example.model.MedalAttribute
import com.example.model.MedalEntity
import com.example.model.RealmMetadataEntity
import com.example.model.RealmStageEntity
import com.example.model.RpgSystemOrigin
import com.example.model.SkillCategory
import com.example.model.SkillCombatEffect
import com.example.model.SkillEntity
import com.example.model.SpecialEffectType
import com.example.model.WeaponArmorClass
import com.example.model.WeaponEntity
import com.example.model.WeaponSkillCrossRef
import com.example.model.WeaponWithSkills
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf

typealias RpgDatabase = AppDatabase

@Singleton
class RpgRepository @Inject constructor(
  private val heroDao: HeroDao,
  private val equipmentDao: EquipmentDao,
  private val realmMetadataDao: RealmMetadataDao,
  private val medalDao: MedalDao,
  private val realmStageDao: RealmStageDao,
  private val engineConfigDao: EngineConfigDao,
  private val battleLogDao: BattleLogDao,
  private val importedAssetDao: ImportedAssetDao? = null,
  private val skillDao: SkillDao? = null,
  private val weaponDao: WeaponDao? = null,
  private val armorDao: ArmorDao? = null
) {
  constructor(database: AppDatabase) : this(
    heroDao = database.heroDao(),
    equipmentDao = database.equipmentDao(),
    realmMetadataDao = database.realmMetadataDao(),
    medalDao = database.medalDao(),
    realmStageDao = database.realmStageDao(),
    engineConfigDao = database.engineConfigDao(),
    battleLogDao = database.battleLogDao(),
    importedAssetDao = database.importedAssetDao(),
    skillDao = database.skillDao(),
    weaponDao = database.weaponDao(),
    armorDao = database.armorDao()
  )

  /**
   * Returns an [ArmoryRepository] facade backed by this repository's Weapon, Armor, and Skill DAOs.
   */
  fun asArmoryRepository(): ArmoryRepository =
    if (weaponDao != null && armorDao != null && skillDao != null) {
      RoomArmoryRepository(
        weaponRepository = RoomWeaponRepository(weaponDao),
        armorRepository = RoomArmorRepository(armorDao),
        skillRepository = RoomSkillRepository(skillDao)
      )
    } else {
      EmptyArmoryRepository
    }

  val heroes: Flow<List<HeroEntity>> = heroDao.observeHeroes()
  val activeParty: Flow<List<HeroEntity>> = heroDao.observeActiveParty()
  val equipment: Flow<List<EquipmentEntity>> = equipmentDao.observeEquipment()
  val weapons: Flow<List<WeaponEntity>> = weaponDao?.observeWeapons() ?: flowOf(emptyList())
  val weaponsWithSkills: Flow<List<WeaponWithSkills>> =
    weaponDao?.observeWeaponsWithSkills() ?: flowOf(emptyList())
  val armors: Flow<List<ArmorEntity>> = armorDao?.observeArmors() ?: flowOf(emptyList())
  val armorsWithSkills: Flow<List<ArmorWithSkills>> =
    armorDao?.observeArmorsWithSkills() ?: flowOf(emptyList())
  val realmMetadata: Flow<List<RealmMetadataEntity>> = realmMetadataDao.observeRealmMetadata()
  val medals: Flow<List<MedalEntity>> = medalDao.observeMedals()
  val stages: Flow<List<RealmStageEntity>> = realmStageDao.observeRealmStages()
  val engineConfig: Flow<EngineConfigEntity?> = engineConfigDao.observeEngineConfig()
  val battleLogs: Flow<List<BattleLogEntity>> = battleLogDao.observeBattleLogs()
  val importedAssets: Flow<List<ImportedAssetEntity>> =
    importedAssetDao?.observeImportedAssets() ?: flowOf(emptyList())
  val skills: Flow<List<SkillEntity>> =
    skillDao?.observeSkills() ?: flowOf(emptyList())

  suspend fun ensureSeeded() {
    // 1. Seed Skills first so Weapon/Armor foreign keys to skills(id) are satisfied
    val existingSkills = skillDao?.observeSkills()?.firstOrNull()
    if (existingSkills.isNullOrEmpty()) {
      skillDao?.insertSkills(defaultSkills())
    }
    // 2. Seed Heroes and core realm data before normalized weapons/armors referencing heroes(id)
    val existingHeroes = heroDao.observeHeroes().firstOrNull()
    val seededEquipment = defaultEquipment()
    if (existingHeroes.isNullOrEmpty()) {
      medalDao.insertMedals(defaultMedals())
      equipmentDao.insertEquipmentList(seededEquipment)
      realmMetadataDao.insertRealmMetadataList(defaultRealmMetadata())
      heroDao.insertHeroes(defaultHeroes())
      realmStageDao.insertStages(defaultStages())
      engineConfigDao.saveEngineConfig(EngineConfigEntity())
    } else {
      val existingEq = equipmentDao.observeEquipment().firstOrNull()
      if (existingEq.isNullOrEmpty()) {
        equipmentDao.insertEquipmentList(seededEquipment)
      }
      val existingRealms = realmMetadataDao.observeRealmMetadata().firstOrNull()
      if (existingRealms.isNullOrEmpty()) {
        realmMetadataDao.insertRealmMetadataList(defaultRealmMetadata())
      }
    }

    // 3. Seed normalized WeaponEntity & ArmorEntity tables + Materia socket cross-ref tables
    val existingWeapons = weaponDao?.observeWeapons()?.firstOrNull()
    if (existingWeapons.isNullOrEmpty()) {
      val weaponsList = seededEquipment
        .filter { it.slotTypeEnum == EquipmentSlotType.WEAPON }
        .map { WeaponEntity.fromEquipmentEntity(it) }
      weaponDao?.insertWeapons(weaponsList)
      val weaponRefs = weaponsList.flatMap { weapon ->
        weapon.socketedSkillIds.mapIndexed { idx, skillId ->
          WeaponSkillCrossRef(weaponId = weapon.id, skillId = skillId, slotIndex = idx)
        }
      }
      if (weaponRefs.isNotEmpty()) {
        weaponDao?.insertWeaponSkillSockets(weaponRefs)
      }
    }

    val existingArmors = armorDao?.observeArmors()?.firstOrNull()
    if (existingArmors.isNullOrEmpty()) {
      val armorsList = seededEquipment
        .filter { it.slotTypeEnum == EquipmentSlotType.ARMOR }
        .map { ArmorEntity.fromEquipmentEntity(it) }
      armorDao?.insertArmors(armorsList)
      val armorRefs = armorsList.flatMap { armor ->
        armor.socketedSkillIds.mapIndexed { idx, skillId ->
          ArmorSkillCrossRef(armorId = armor.id, skillId = skillId, slotIndex = idx)
        }
      }
      if (armorRefs.isNotEmpty()) {
        armorDao?.insertArmorSkillSockets(armorRefs)
      }
    }
  }

  private suspend fun syncEquipmentToNormalizedTables(item: EquipmentEntity, persistedId: Int) {
    val validGrantedSkillId = item.grantedSkillId?.takeIf { skillDao?.getSkillById(it) != null }
    val validHeroId = item.equippedByHeroId?.takeIf { heroDao.getHeroById(it) != null }
    val validSocketIds = item.socketedSkillIdList().filter { skillDao?.getSkillById(it) != null }

    when (item.slotTypeEnum) {
      EquipmentSlotType.WEAPON -> {
        val weapon = WeaponEntity.fromEquipmentEntity(item).copy(
          id = persistedId,
          grantedSkillId = validGrantedSkillId,
          equippedByHeroId = validHeroId,
          socketedSkillIds = validSocketIds
        )
        weaponDao?.upsertWeapon(weapon)
        weaponDao?.clearWeaponSkillSockets(persistedId)
        if (validSocketIds.isNotEmpty()) {
          weaponDao?.insertWeaponSkillSockets(
            validSocketIds.mapIndexed { idx, sId ->
              WeaponSkillCrossRef(weaponId = persistedId, skillId = sId, slotIndex = idx)
            }
          )
        }
      }

      EquipmentSlotType.ARMOR -> {
        val armor = ArmorEntity.fromEquipmentEntity(item).copy(
          id = persistedId,
          grantedSkillId = validGrantedSkillId,
          equippedByHeroId = validHeroId,
          socketedSkillIds = validSocketIds
        )
        armorDao?.upsertArmor(armor)
        armorDao?.clearArmorSkillSockets(persistedId)
        if (validSocketIds.isNotEmpty()) {
          armorDao?.insertArmorSkillSockets(
            validSocketIds.mapIndexed { idx, sId ->
              ArmorSkillCrossRef(armorId = persistedId, skillId = sId, slotIndex = idx)
            }
          )
        }
      }

      EquipmentSlotType.ACCESSORY -> Unit
    }
  }

  suspend fun getHeroById(heroId: Int): HeroEntity? = heroDao.getHeroById(heroId)
  suspend fun insertHero(hero: HeroEntity): Long = heroDao.insertHero(hero)
  suspend fun updateHero(hero: HeroEntity): Int = heroDao.updateHero(hero)
  suspend fun deleteHero(hero: HeroEntity): Int = heroDao.deleteHero(hero)
  suspend fun deleteHeroById(heroId: Int): Int = heroDao.deleteHeroById(heroId)

  suspend fun getEquipmentById(equipmentId: Int): EquipmentEntity? = equipmentDao.getEquipmentById(equipmentId)
  suspend fun insertEquipment(item: EquipmentEntity): Long {
    val rowId = equipmentDao.insertEquipment(item)
    val resolvedId = if (item.id > 0) item.id else rowId.toInt()
    if (resolvedId > 0) {
      syncEquipmentToNormalizedTables(item, resolvedId)
    }
    return rowId
  }

  suspend fun updateEquipment(item: EquipmentEntity): Int {
    val rows = equipmentDao.updateEquipment(item)
    if (item.id > 0) {
      syncEquipmentToNormalizedTables(item, item.id)
    }
    return rows
  }

  suspend fun deleteEquipment(item: EquipmentEntity): Int {
    if (item.slotTypeEnum == EquipmentSlotType.WEAPON) {
      weaponDao?.deleteWeaponById(item.id)
    } else if (item.slotTypeEnum == EquipmentSlotType.ARMOR) {
      armorDao?.deleteArmorById(item.id)
    }
    return equipmentDao.deleteEquipment(item)
  }

  suspend fun deleteEquipmentById(equipmentId: Int): Int {
    weaponDao?.deleteWeaponById(equipmentId)
    armorDao?.deleteArmorById(equipmentId)
    return equipmentDao.deleteEquipmentById(equipmentId)
  }

  // Dedicated WeaponEntity CRUD & Materia Socket operations
  fun observeWeaponById(weaponId: Int): Flow<WeaponEntity?> =
    weaponDao?.observeWeaponById(weaponId) ?: flowOf(null)

  fun searchWeaponsByName(query: String): Flow<List<WeaponEntity>> =
    weaponDao?.searchWeaponsByName(query) ?: flowOf(emptyList())

  suspend fun getAllWeapons(): List<WeaponEntity> = weaponDao?.getAllWeapons() ?: emptyList()
  suspend fun getWeaponCount(): Int = weaponDao?.getWeaponCount() ?: 0
  suspend fun getWeaponById(weaponId: Int): WeaponEntity? = weaponDao?.getWeaponById(weaponId)
  suspend fun getWeaponWithSkillsById(weaponId: Int): WeaponWithSkills? =
    weaponDao?.getWeaponWithSkillsById(weaponId)

  suspend fun insertWeapon(weapon: WeaponEntity): Long = weaponDao?.insertWeapon(weapon) ?: -1L
  suspend fun upsertWeapon(weapon: WeaponEntity): Long = weaponDao?.upsertWeapon(weapon) ?: -1L
  suspend fun updateWeapon(weapon: WeaponEntity): Int = weaponDao?.updateWeapon(weapon) ?: 0
  suspend fun deleteWeapon(weapon: WeaponEntity): Int = weaponDao?.deleteWeapon(weapon) ?: 0
  suspend fun deleteWeaponById(weaponId: Int): Int = weaponDao?.deleteWeaponById(weaponId) ?: 0
  suspend fun socketSkillInWeapon(weaponId: Int, skillId: Int, slotIndex: Int = 0): Long =
    weaponDao?.insertWeaponSkillSocket(WeaponSkillCrossRef(weaponId, skillId, slotIndex)) ?: -1L

  suspend fun removeSkillFromWeapon(weaponId: Int, skillId: Int): Int =
    weaponDao?.deleteWeaponSkillSocket(weaponId, skillId) ?: 0

  // Dedicated ArmorEntity CRUD & Materia Socket operations
  fun observeArmorById(armorId: Int): Flow<ArmorEntity?> =
    armorDao?.observeArmorById(armorId) ?: flowOf(null)

  fun searchArmorsByName(query: String): Flow<List<ArmorEntity>> =
    armorDao?.searchArmorsByName(query) ?: flowOf(emptyList())

  suspend fun getAllArmors(): List<ArmorEntity> = armorDao?.getAllArmors() ?: emptyList()
  suspend fun getArmorCount(): Int = armorDao?.getArmorCount() ?: 0
  suspend fun getArmorById(armorId: Int): ArmorEntity? = armorDao?.getArmorById(armorId)
  suspend fun getArmorWithSkillsById(armorId: Int): ArmorWithSkills? =
    armorDao?.getArmorWithSkillsById(armorId)

  suspend fun insertArmor(armor: ArmorEntity): Long = armorDao?.insertArmor(armor) ?: -1L
  suspend fun upsertArmor(armor: ArmorEntity): Long = armorDao?.upsertArmor(armor) ?: -1L
  suspend fun updateArmor(armor: ArmorEntity): Int = armorDao?.updateArmor(armor) ?: 0
  suspend fun deleteArmor(armor: ArmorEntity): Int = armorDao?.deleteArmor(armor) ?: 0
  suspend fun deleteArmorById(armorId: Int): Int = armorDao?.deleteArmorById(armorId) ?: 0
  suspend fun socketSkillInArmor(armorId: Int, skillId: Int, slotIndex: Int = 0): Long =
    armorDao?.insertArmorSkillSocket(ArmorSkillCrossRef(armorId, skillId, slotIndex)) ?: -1L

  suspend fun removeSkillFromArmor(armorId: Int, skillId: Int): Int =
    armorDao?.deleteArmorSkillSocket(armorId, skillId) ?: 0

  suspend fun getRealmById(realmId: Int): RealmMetadataEntity? = realmMetadataDao.getRealmById(realmId)
  suspend fun getRealmByName(realmName: String): RealmMetadataEntity? = realmMetadataDao.getRealmByName(realmName)
  suspend fun insertRealmMetadata(realm: RealmMetadataEntity): Long = realmMetadataDao.insertRealmMetadata(realm)
  suspend fun updateRealmMetadata(realm: RealmMetadataEntity): Int = realmMetadataDao.updateRealmMetadata(realm)
  suspend fun deleteRealmMetadata(realm: RealmMetadataEntity): Int = realmMetadataDao.deleteRealmMetadata(realm)
  suspend fun deleteRealmById(realmId: Int): Int = realmMetadataDao.deleteRealmById(realmId)

  suspend fun updateMedal(medal: MedalEntity): Int = medalDao.updateMedal(medal)
  suspend fun insertMedal(medal: MedalEntity): Long = medalDao.insertMedal(medal)
  suspend fun deleteMedalById(medalId: Int): Int = medalDao.deleteMedalById(medalId)

  suspend fun updateStage(stage: RealmStageEntity): Int = realmStageDao.updateStage(stage)
  suspend fun insertStage(stage: RealmStageEntity): Long = realmStageDao.insertStage(stage)
  suspend fun deleteStageById(stageId: Int): Int = realmStageDao.deleteStageById(stageId)

  suspend fun saveEngineConfig(config: EngineConfigEntity): Long = engineConfigDao.saveEngineConfig(config)
  suspend fun insertBattleLog(log: BattleLogEntity): Long = battleLogDao.insertBattleLog(log)
  suspend fun clearBattleLogs(): Int = battleLogDao.clearBattleLogs()

  suspend fun getImportedAssetById(assetId: Int): ImportedAssetEntity? =
    importedAssetDao?.getAssetById(assetId)

  suspend fun insertImportedAsset(asset: ImportedAssetEntity): Long =
    importedAssetDao?.insertAsset(asset) ?: -1L

  suspend fun insertImportedAssets(assets: List<ImportedAssetEntity>): List<Long> =
    importedAssetDao?.insertAssets(assets) ?: emptyList()

  suspend fun updateImportedAsset(asset: ImportedAssetEntity): Int =
    importedAssetDao?.updateAsset(asset) ?: 0

  suspend fun deleteImportedAssetById(assetId: Int): Int =
    importedAssetDao?.deleteAssetById(assetId) ?: 0

  // Dedicated SkillEntity CRUD operations
  fun observeSkillById(skillId: Int): Flow<SkillEntity?> =
    skillDao?.observeSkillById(skillId) ?: flowOf(null)

  fun searchSkillsByName(query: String): Flow<List<SkillEntity>> =
    skillDao?.searchSkillsByName(query) ?: flowOf(emptyList())

  suspend fun getAllSkills(): List<SkillEntity> = skillDao?.getAllSkills() ?: emptyList()
  suspend fun getSkillCount(): Int = skillDao?.getSkillCount() ?: 0
  suspend fun getSkillById(skillId: Int): SkillEntity? = skillDao?.getSkillById(skillId)
  suspend fun insertSkill(skill: SkillEntity): Long = skillDao?.insertSkill(skill) ?: -1L
  suspend fun upsertSkill(skill: SkillEntity): Long = skillDao?.upsertSkill(skill) ?: -1L
  suspend fun updateSkill(skill: SkillEntity): Int = skillDao?.updateSkill(skill) ?: 0
  suspend fun deleteSkill(skill: SkillEntity): Int = skillDao?.deleteSkill(skill) ?: 0
  suspend fun deleteSkillById(skillId: Int): Int = skillDao?.deleteSkillById(skillId) ?: 0

  private fun defaultSkills(): List<SkillEntity> = listOf(
    SkillEntity(
      id = 1,
      name = "Braver Limit Strike",
      rpgSystem = RpgSystemOrigin.FFVII.name,
      category = SkillCategory.ACTIVE_TECH.name,
      element = ElementType.WIND.name,
      attribute = MedalAttribute.POWER.name,
      spCost = 2,
      cooldownTurns = 1,
      powerMultiplier = 3.2f,
      hitCount = 4,
      effectType = SkillCombatEffect.DAMAGE.name,
      rankLevel = 2,
      description = "FFVII Limit Art: Leaping vertical cleave with high critical scaling.",
      bonusElements = listOf(ElementType.NON_ELEMENTAL),
      secondaryEffects = listOf(SkillCombatEffect.CHAIN_BUILD),
      requiredJobClasses = listOf("SOLDIER 1st Class", "Dark Knight"),
      scalingWeights = mapOf("ATK" to 1.35f, "SPD" to 0.25f)
    ),
    SkillEntity(
      id = 2,
      name = "Hallowed Bolt",
      rpgSystem = RpgSystemOrigin.IVALICE_TACTICS.name,
      category = SkillCategory.TACTICS_ART.name,
      element = ElementType.HOLY.name,
      attribute = MedalAttribute.POWER.name,
      spCost = 2,
      cooldownTurns = 2,
      powerMultiplier = 2.9f,
      hitCount = 3,
      effectType = SkillCombatEffect.DEF_BREAK.name,
      rankLevel = 2,
      description = "FFT Sword Skill: Calls down sacred lightning to shatter enemy DEF (-50%).",
      bonusElements = listOf(ElementType.LIGHTNING),
      secondaryEffects = listOf(SkillCombatEffect.DAMAGE),
      requiredJobClasses = listOf("Holy Paladin", "Union Keybearer"),
      scalingWeights = mapOf("ATK" to 1.15f, "MND" to 0.30f)
    ),
    SkillEntity(
      id = 3,
      name = "Aegis Sentinel Wall",
      rpgSystem = RpgSystemOrigin.FFRK.name,
      category = SkillCategory.WHITE_MAGIC.name,
      element = ElementType.HOLY.name,
      attribute = MedalAttribute.MAGIC.name,
      spCost = 2,
      cooldownTurns = 3,
      powerMultiplier = 1.6f,
      hitCount = 1,
      effectType = SkillCombatEffect.SHIELD_WALL.name,
      rankLevel = 2,
      description = "FFRK Protective Grimoire: Grants party -35% incoming damage barrier for 18s.",
      secondaryEffects = listOf(SkillCombatEffect.HEAL_PARTY),
      requiredJobClasses = listOf("Archivist Keeper", "Holy Paladin"),
      scalingWeights = mapOf("MND" to 1.0f, "RES" to 0.45f)
    ),
    SkillEntity(
      id = 4,
      name = "Firaga Chain",
      rpgSystem = RpgSystemOrigin.FFVI.name,
      category = SkillCategory.BLACK_MAGIC.name,
      element = ElementType.FIRE.name,
      attribute = MedalAttribute.MAGIC.name,
      spCost = 2,
      cooldownTurns = 1,
      powerMultiplier = 3.1f,
      hitCount = 5,
      effectType = SkillCombatEffect.CHAIN_BUILD.name,
      rankLevel = 2,
      description = "FFVI Esper Magic: 5-hit crimson inferno that ignites Elemental Chains (+4 hits).",
      bonusElements = listOf(ElementType.WIND),
      secondaryEffects = listOf(SkillCombatEffect.DAMAGE),
      requiredJobClasses = listOf("Esper Sage", "Black Wizard"),
      scalingWeights = mapOf("MAG" to 1.40f)
    ),
    SkillEntity(
      id = 5,
      name = "Thundaga Blitz",
      rpgSystem = RpgSystemOrigin.KHUX.name,
      category = SkillCategory.BLACK_MAGIC.name,
      element = ElementType.LIGHTNING.name,
      attribute = MedalAttribute.MAGIC.name,
      spCost = 2,
      cooldownTurns = 1,
      powerMultiplier = 3.0f,
      hitCount = 6,
      effectType = SkillCombatEffect.CHAIN_BUILD.name,
      rankLevel = 2,
      description = "KH Keyblade Spell: Rapid aerial thunder bursts that extend Lightning Chains."
    ),
    SkillEntity(
      id = 6,
      name = "Curaja Medica",
      rpgSystem = RpgSystemOrigin.FFRK.name,
      category = SkillCategory.WHITE_MAGIC.name,
      element = ElementType.HOLY.name,
      attribute = MedalAttribute.MAGIC.name,
      spCost = 2,
      cooldownTurns = 2,
      powerMultiplier = 1.8f,
      hitCount = 1,
      effectType = SkillCombatEffect.HEAL_PARTY.name,
      rankLevel = 2,
      description = "Restores 40% HP to all living allies and refreshes battle momentum."
    ),
    SkillEntity(
      id = 7,
      name = "Dark Drain Blade",
      rpgSystem = RpgSystemOrigin.IVALICE_TACTICS.name,
      category = SkillCategory.TACTICS_ART.name,
      element = ElementType.DARK.name,
      attribute = MedalAttribute.SPEED.name,
      spCost = 2,
      cooldownTurns = 1,
      powerMultiplier = 2.8f,
      hitCount = 3,
      effectType = SkillCombatEffect.LIFESTEAL.name,
      rankLevel = 2,
      description = "FFT Dark Knight Art: Cleaves foe with dark aura and absorbs 35% of damage as HP."
    ),
    SkillEntity(
      id = 8,
      name = "Hastega & Valor",
      rpgSystem = RpgSystemOrigin.CHRONO_NEXUS.name,
      category = SkillCategory.WHITE_MAGIC.name,
      element = ElementType.WIND.name,
      attribute = MedalAttribute.SPEED.name,
      spCost = 3,
      cooldownTurns = 3,
      powerMultiplier = 1.5f,
      hitCount = 1,
      effectType = SkillCombatEffect.HASTE_ATK_UP.name,
      rankLevel = 1,
      description = "Grants party Haste (+45% ATB fill rate) and +30% ATK/MAG buff."
    ),
    SkillEntity(
      id = 9,
      name = "Crush Weapon Break",
      rpgSystem = RpgSystemOrigin.IVALICE_TACTICS.name,
      category = SkillCategory.TACTICS_ART.name,
      element = ElementType.NON_ELEMENTAL.name,
      attribute = MedalAttribute.POWER.name,
      spCost = 1,
      cooldownTurns = 2,
      powerMultiplier = 2.4f,
      hitCount = 2,
      effectType = SkillCombatEffect.ATK_BREAK.name,
      rankLevel = 1,
      description = "Knight Rend Art: Shatters boss weapon power, lowering Boss ATK by 40%."
    ),
    SkillEntity(
      id = 10,
      name = "Luminare Dual Tech",
      rpgSystem = RpgSystemOrigin.CHRONO_NEXUS.name,
      category = SkillCategory.ACTIVE_TECH.name,
      element = ElementType.HOLY.name,
      attribute = MedalAttribute.MAGIC.name,
      spCost = 4,
      cooldownTurns = 3,
      powerMultiplier = 4.6f,
      hitCount = 8,
      effectType = SkillCombatEffect.DAMAGE.name,
      rankLevel = 1,
      description = "Chrono Pinnacle Tech: High-cap radiant nova breaking standard damage limits."
    ),
    SkillEntity(
      id = 11,
      name = "Blizzaga Freeze",
      rpgSystem = RpgSystemOrigin.KHUX.name,
      category = SkillCategory.BLACK_MAGIC.name,
      element = ElementType.ICE.name,
      attribute = MedalAttribute.MAGIC.name,
      spCost = 2,
      cooldownTurns = 1,
      powerMultiplier = 2.95f,
      hitCount = 4,
      effectType = SkillCombatEffect.DAMAGE.name,
      rankLevel = 1,
      description = "Cryo burst piercing armored targets with crystalline shrapnel."
    ),
    SkillEntity(
      id = 12,
      name = "Esuna Astral Cleanse",
      rpgSystem = RpgSystemOrigin.FFVI.name,
      category = SkillCategory.WHITE_MAGIC.name,
      element = ElementType.HOLY.name,
      attribute = MedalAttribute.MAGIC.name,
      spCost = 1,
      cooldownTurns = 2,
      powerMultiplier = 1.4f,
      hitCount = 1,
      effectType = SkillCombatEffect.DISPEL_CLEANSE.name,
      rankLevel = 1,
      description = "Purifies party ailments, heals 25% HP, and raises Guard stance."
    ),
    // Passive Traits & Materia Orbs
    SkillEntity(
      id = 13,
      name = "SOLDIER Mako Surge",
      rpgSystem = RpgSystemOrigin.FFVII.name,
      category = SkillCategory.PASSIVE_TRAIT.name,
      element = ElementType.WIND.name,
      attribute = MedalAttribute.POWER.name,
      spCost = 0,
      cooldownTurns = 0,
      powerMultiplier = 1.0f,
      hitCount = 1,
      effectType = SkillCombatEffect.PASSIVE_STAT_BOOST.name,
      rankLevel = 2,
      description = "Materia Passive: +12% ATK, +8% Critical Rate & +8 SPD.",
      passiveAtkPercent = 12,
      passiveSpdBonus = 8,
      passiveCritBonus = 0.08f
    ),
    SkillEntity(
      id = 14,
      name = "Doublecast Esper Trance",
      rpgSystem = RpgSystemOrigin.FFVI.name,
      category = SkillCategory.PASSIVE_TRAIT.name,
      element = ElementType.FIRE.name,
      attribute = MedalAttribute.MAGIC.name,
      spCost = 0,
      cooldownTurns = 0,
      powerMultiplier = 1.0f,
      hitCount = 1,
      effectType = SkillCombatEffect.PASSIVE_STAT_BOOST.name,
      rankLevel = 2,
      description = "Esper Passive: +15% MAG & +6% Spell Lifesteal.",
      passiveMagPercent = 15,
      passiveLifestealPercent = 6
    ),
    SkillEntity(
      id = 15,
      name = "Counter-Tackle Stance",
      rpgSystem = RpgSystemOrigin.IVALICE_TACTICS.name,
      category = SkillCategory.PASSIVE_TRAIT.name,
      element = ElementType.NON_ELEMENTAL.name,
      attribute = MedalAttribute.POWER.name,
      spCost = 0,
      cooldownTurns = 0,
      powerMultiplier = 1.0f,
      hitCount = 1,
      effectType = SkillCombatEffect.PASSIVE_STAT_BOOST.name,
      rankLevel = 1,
      description = "FFT Reaction Passive: +10% DEF & +25% Counter-Attack chance when hit.",
      passiveDefPercent = 10,
      passiveCounterPercent = 25
    ),
    SkillEntity(
      id = 16,
      name = "Auto-Haste Chrono Seal",
      rpgSystem = RpgSystemOrigin.CHRONO_NEXUS.name,
      category = SkillCategory.PASSIVE_TRAIT.name,
      element = ElementType.WIND.name,
      attribute = MedalAttribute.SPEED.name,
      spCost = 0,
      cooldownTurns = 0,
      powerMultiplier = 1.0f,
      hitCount = 1,
      effectType = SkillCombatEffect.PASSIVE_STAT_BOOST.name,
      rankLevel = 1,
      description = "Chrono Passive: +18 SPD & +6% Critical Hit Rate.",
      passiveSpdBonus = 18,
      passiveCritBonus = 0.06f
    )
  )

  private fun defaultEquipment(): List<EquipmentEntity> = listOf(
    EquipmentEntity(
      id = 1,
      name = "Fantasy Grimoire Vol. X",
      slotType = EquipmentSlotType.WEAPON.name,
      originRealm = "Record Keep",
      rarityStars = 7,
      upgradeLevel = 35,
      bonusHp = 450,
      bonusAtk = 145,
      bonusDef = 110,
      bonusMag = 165,
      bonusRes = 120,
      bonusSpd = 12,
      elementBoost = ElementType.HOLY.name,
      passiveTrait = "Omni-Synergy & Grants Aegis Sentinel Wall",
      equippedByHeroId = 1,
      rpgSystem = RpgSystemOrigin.FFRK.name,
      weaponArmorClass = WeaponArmorClass.GRIMOIRE.name,
      setSeriesName = "Keeper's Archive",
      spCostReduction = 1,
      socketedSkillIdsCsv = "3",
      grantedSkillId = 3,
      reforgeTier = 1
    ),
    EquipmentEntity(
      id = 2,
      name = "Starlight Keyblade +35",
      slotType = EquipmentSlotType.WEAPON.name,
      originRealm = "Daybreak Town",
      rarityStars = 7,
      upgradeLevel = 35,
      bonusHp = 500,
      bonusAtk = 195,
      bonusDef = 95,
      bonusMag = 160,
      bonusRes = 90,
      bonusSpd = 18,
      elementBoost = ElementType.HOLY.name,
      passiveTrait = "+25% Upright Resonance & Grants Thundaga Blitz",
      equippedByHeroId = 2,
      rpgSystem = RpgSystemOrigin.KHUX.name,
      weaponArmorClass = WeaponArmorClass.KEYBLADE.name,
      setSeriesName = "Daybreak Regalia",
      critRateBonus = 0.08f,
      spCostReduction = 1,
      socketedSkillIdsCsv = "5",
      grantedSkillId = 5,
      reforgeTier = 1
    ),
    EquipmentEntity(
      id = 3,
      name = "Fusion Buster Sword VII",
      slotType = EquipmentSlotType.WEAPON.name,
      originRealm = "Midgar VII",
      rarityStars = 7,
      upgradeLevel = 40,
      bonusHp = 600,
      bonusAtk = 230,
      bonusDef = 105,
      bonusMag = 60,
      bonusRes = 80,
      bonusSpd = 14,
      elementBoost = ElementType.WIND.name,
      passiveTrait = "Dual Materia Slots • +12% Crit & Grants Braver",
      equippedByHeroId = 3,
      rpgSystem = RpgSystemOrigin.FFVII.name,
      weaponArmorClass = WeaponArmorClass.GREATSWORD.name,
      setSeriesName = "SOLDIER First Class",
      critRateBonus = 0.12f,
      critDamageBonus = 0.25f,
      materiaSocketsMax = 3,
      socketedSkillIdsCsv = "1,13",
      grantedSkillId = 1,
      reforgeTier = 1
    ),
    EquipmentEntity(
      id = 4,
      name = "Apocalypse Enhancer VI",
      slotType = EquipmentSlotType.WEAPON.name,
      originRealm = "Vector VI",
      rarityStars = 7,
      upgradeLevel = 35,
      bonusHp = 420,
      bonusAtk = 110,
      bonusDef = 90,
      bonusMag = 235,
      bonusRes = 145,
      bonusSpd = 15,
      elementBoost = ElementType.FIRE.name,
      passiveTrait = "+25% Fire Trance & Grants Firaga Chain",
      equippedByHeroId = 4,
      rpgSystem = RpgSystemOrigin.FFVI.name,
      weaponArmorClass = WeaponArmorClass.GREATSWORD.name,
      setSeriesName = "Gestahlian Magitek",
      critRateBonus = 0.06f,
      socketedSkillIdsCsv = "4,14",
      grantedSkillId = 4
    ),
    EquipmentEntity(
      id = 5,
      name = "Way to the Dawn Keyblade",
      slotType = EquipmentSlotType.WEAPON.name,
      originRealm = "Daybreak Town",
      rarityStars = 6,
      upgradeLevel = 30,
      bonusHp = 440,
      bonusAtk = 185,
      bonusDef = 100,
      bonusMag = 140,
      bonusRes = 95,
      bonusSpd = 16,
      elementBoost = ElementType.DARK.name,
      passiveTrait = "+15% Lifesteal & Grants Dark Drain Blade",
      equippedByHeroId = 5,
      rpgSystem = RpgSystemOrigin.KHUX.name,
      weaponArmorClass = WeaponArmorClass.KEYBLADE.name,
      setSeriesName = "Daybreak Regalia",
      lifestealPercent = 15,
      grantedSkillId = 7
    ),
    EquipmentEntity(
      id = 6,
      name = "Mace of Zeus IX",
      slotType = EquipmentSlotType.WEAPON.name,
      originRealm = "Alexandria IX",
      rarityStars = 6,
      upgradeLevel = 30,
      bonusHp = 380,
      bonusAtk = 75,
      bonusDef = 85,
      bonusMag = 225,
      bonusRes = 150,
      bonusSpd = 12,
      elementBoost = ElementType.FIRE.name,
      passiveTrait = "Doublecast Black Magic +20% & Luminare",
      equippedByHeroId = 6,
      rpgSystem = RpgSystemOrigin.CHRONO_NEXUS.name,
      weaponArmorClass = WeaponArmorClass.STAFF.name,
      setSeriesName = "Alexandria Arcane",
      critDamageBonus = 0.20f,
      grantedSkillId = 10
    ),
    EquipmentEntity(
      id = 7,
      name = "Keeper's Astral Coat",
      slotType = EquipmentSlotType.ARMOR.name,
      originRealm = "Record Keep",
      rarityStars = 6,
      upgradeLevel = 30,
      bonusHp = 850,
      bonusAtk = 45,
      bonusDef = 175,
      bonusMag = 45,
      bonusRes = 180,
      bonusSpd = 8,
      elementBoost = ElementType.HOLY.name,
      passiveTrait = "25% Holy Resist & +10% Counter Guard",
      equippedByHeroId = 1,
      rpgSystem = RpgSystemOrigin.FFRK.name,
      weaponArmorClass = WeaponArmorClass.MYSTIC_ROBE.name,
      setSeriesName = "Keeper's Archive",
      elementResistElement = ElementType.HOLY.name,
      elementResistPercent = 25,
      counterChancePercent = 10,
      grantedSkillId = 6
    ),
    EquipmentEntity(
      id = 8,
      name = "Keybearer Crown Pauldron",
      slotType = EquipmentSlotType.ARMOR.name,
      originRealm = "Daybreak Town",
      rarityStars = 6,
      upgradeLevel = 30,
      bonusHp = 920,
      bonusAtk = 65,
      bonusDef = 165,
      bonusMag = 55,
      bonusRes = 160,
      bonusSpd = 10,
      elementBoost = ElementType.HOLY.name,
      passiveTrait = "25% Dark Resist & Set Synergy (-1 SP)",
      equippedByHeroId = 2,
      rpgSystem = RpgSystemOrigin.KHUX.name,
      weaponArmorClass = WeaponArmorClass.LIGHT_ARMOR.name,
      setSeriesName = "Daybreak Regalia",
      elementResistElement = ElementType.DARK.name,
      elementResistPercent = 25,
      grantedSkillId = 2
    ),
    EquipmentEntity(
      id = 9,
      name = "SOLDIER 1st Class Belt",
      slotType = EquipmentSlotType.ARMOR.name,
      originRealm = "Midgar VII",
      rarityStars = 6,
      upgradeLevel = 30,
      bonusHp = 980,
      bonusAtk = 80,
      bonusDef = 190,
      bonusMag = 30,
      bonusRes = 145,
      bonusSpd = 9,
      elementBoost = ElementType.WIND.name,
      passiveTrait = "25% Lightning Resist & +20% Counter",
      equippedByHeroId = 3,
      rpgSystem = RpgSystemOrigin.FFVII.name,
      weaponArmorClass = WeaponArmorClass.HEAVY_PLATE.name,
      setSeriesName = "SOLDIER First Class",
      elementResistElement = ElementType.LIGHTNING.name,
      elementResistPercent = 25,
      counterChancePercent = 20,
      socketedSkillIdsCsv = "15"
    ),
    EquipmentEntity(
      id = 10,
      name = "Minerva Bustier VI",
      slotType = EquipmentSlotType.ARMOR.name,
      originRealm = "Vector VI",
      rarityStars = 6,
      upgradeLevel = 30,
      bonusHp = 790,
      bonusAtk = 35,
      bonusDef = 155,
      bonusMag = 85,
      bonusRes = 205,
      bonusSpd = 11,
      elementBoost = ElementType.FIRE.name,
      passiveTrait = "30% Fire Resist & Grants Esuna Cleanse",
      equippedByHeroId = 4,
      rpgSystem = RpgSystemOrigin.FFVI.name,
      weaponArmorClass = WeaponArmorClass.MYSTIC_ROBE.name,
      setSeriesName = "Gestahlian Magitek",
      elementResistElement = ElementType.FIRE.name,
      elementResistPercent = 30,
      grantedSkillId = 12
    ),
    EquipmentEntity(
      id = 11,
      name = "Genji Aegis Plate (Tactics)",
      slotType = EquipmentSlotType.ARMOR.name,
      originRealm = "Record Keep",
      rarityStars = 7,
      upgradeLevel = 35,
      bonusHp = 1150,
      bonusAtk = 55,
      bonusDef = 235,
      bonusMag = 40,
      bonusRes = 210,
      bonusSpd = 6,
      elementBoost = ElementType.NON_ELEMENTAL.name,
      passiveTrait = "Heavy Armor: +30% Counter & Crush Break",
      rpgSystem = RpgSystemOrigin.IVALICE_TACTICS.name,
      weaponArmorClass = WeaponArmorClass.HEAVY_PLATE.name,
      setSeriesName = "Ivalice Paladin",
      counterChancePercent = 30,
      grantedSkillId = 9
    ),
    EquipmentEntity(
      id = 12,
      name = "Masamune Murasame VII",
      slotType = EquipmentSlotType.WEAPON.name,
      originRealm = "Midgar VII",
      rarityStars = 7,
      upgradeLevel = 35,
      bonusHp = 410,
      bonusAtk = 245,
      bonusDef = 75,
      bonusMag = 110,
      bonusRes = 80,
      bonusSpd = 22,
      elementBoost = ElementType.DARK.name,
      passiveTrait = "Katana Mastery: +18% Crit & +10% Lifesteal",
      rpgSystem = RpgSystemOrigin.FFVII.name,
      weaponArmorClass = WeaponArmorClass.KATANA.name,
      setSeriesName = "SOLDIER First Class",
      critRateBonus = 0.18f,
      critDamageBonus = 0.30f,
      lifestealPercent = 10,
      materiaSocketsMax = 3
    ),
    // Multi-RPG Accessories
    EquipmentEntity(
      id = 13,
      name = "Chrono Archivist Ring",
      slotType = EquipmentSlotType.ACCESSORY.name,
      originRealm = "Record Keep",
      rarityStars = 6,
      upgradeLevel = 25,
      bonusHp = 350,
      bonusAtk = 35,
      bonusDef = 45,
      bonusMag = 45,
      bonusRes = 55,
      bonusSpd = 18,
      elementBoost = ElementType.WIND.name,
      passiveTrait = "Set Bonus: Auto-Haste & +150 Starting SB",
      equippedByHeroId = 1,
      rpgSystem = RpgSystemOrigin.CHRONO_NEXUS.name,
      weaponArmorClass = WeaponArmorClass.CHRONO_CHARM.name,
      setSeriesName = "Keeper's Archive",
      grantedSkillId = 8
    ),
    EquipmentEntity(
      id = 14,
      name = "Lux Starlight Charm",
      slotType = EquipmentSlotType.ACCESSORY.name,
      originRealm = "Daybreak Town",
      rarityStars = 7,
      upgradeLevel = 30,
      bonusHp = 400,
      bonusAtk = 55,
      bonusDef = 40,
      bonusMag = 55,
      bonusRes = 45,
      bonusSpd = 16,
      elementBoost = ElementType.HOLY.name,
      passiveTrait = "3-Piece Daybreak Regalia • +8% Crit & -1 SP",
      equippedByHeroId = 2,
      rpgSystem = RpgSystemOrigin.KHUX.name,
      weaponArmorClass = WeaponArmorClass.RING.name,
      setSeriesName = "Daybreak Regalia",
      critRateBonus = 0.08f
    ),
    EquipmentEntity(
      id = 15,
      name = "Midgar Champion Belt VII",
      slotType = EquipmentSlotType.ACCESSORY.name,
      originRealm = "Midgar VII",
      rarityStars = 7,
      upgradeLevel = 30,
      bonusHp = 520,
      bonusAtk = 75,
      bonusDef = 65,
      bonusMag = 20,
      bonusRes = 40,
      bonusSpd = 12,
      elementBoost = ElementType.WIND.name,
      passiveTrait = "3-Piece SOLDIER Set • +10% Crit DMG & Lifesteal",
      equippedByHeroId = 3,
      rpgSystem = RpgSystemOrigin.FFVII.name,
      weaponArmorClass = WeaponArmorClass.MATERIA_ORB.name,
      setSeriesName = "SOLDIER First Class",
      critRateBonus = 0.06f,
      critDamageBonus = 0.25f,
      lifestealPercent = 8
    ),
    EquipmentEntity(
      id = 16,
      name = "Ribbon of Alexandria IX",
      slotType = EquipmentSlotType.ACCESSORY.name,
      originRealm = "Alexandria IX",
      rarityStars = 7,
      upgradeLevel = 30,
      bonusHp = 450,
      bonusAtk = 25,
      bonusDef = 60,
      bonusMag = 75,
      bonusRes = 110,
      bonusSpd = 14,
      elementBoost = ElementType.FIRE.name,
      passiveTrait = "25% Fire Ward & +10% Esper Trance",
      equippedByHeroId = 4,
      rpgSystem = RpgSystemOrigin.FFVI.name,
      weaponArmorClass = WeaponArmorClass.RIBBON.name,
      setSeriesName = "Gestahlian Magitek",
      elementResistElement = ElementType.FIRE.name,
      elementResistPercent = 25,
      critRateBonus = 0.05f
    )
  )

  private fun defaultRealmMetadata(): List<RealmMetadataEntity> = listOf(
    RealmMetadataEntity(
      id = 1,
      realmName = "Daybreak Town",
      seriesCode = "KH-UX",
      worldLore = "The ancient bastion of Keyblade Unions where Lux is gathered to preserve the light across worlds.",
      dominantElement = ElementType.HOLY.name,
      realmResonanceLevel = 3,
      staminaShardsCollected = 4,
      totalStagesCount = 2,
      clearedStagesCount = 1,
      masteryPercent = 65,
      isUnlocked = true
    ),
    RealmMetadataEntity(
      id = 2,
      realmName = "Midgar VII",
      seriesCode = "FF-VII",
      worldLore = "The steel-plated metropolis powered by Mako Reactors, where Avalanche and SOLDIER clash.",
      dominantElement = ElementType.LIGHTNING.name,
      realmResonanceLevel = 2,
      staminaShardsCollected = 3,
      totalStagesCount = 1,
      clearedStagesCount = 0,
      masteryPercent = 25,
      isUnlocked = true
    ),
    RealmMetadataEntity(
      id = 3,
      realmName = "Vector VI",
      seriesCode = "FF-VI",
      worldLore = "Industrial capital of the Gestahlian Empire where Magitek armor and ancient Espers collide.",
      dominantElement = ElementType.FIRE.name,
      realmResonanceLevel = 2,
      staminaShardsCollected = 2,
      totalStagesCount = 1,
      clearedStagesCount = 0,
      masteryPercent = 20,
      isUnlocked = true
    ),
    RealmMetadataEntity(
      id = 4,
      realmName = "Alexandria IX",
      seriesCode = "FF-IX",
      worldLore = "A Mist-shrouded kingdom of airships, Black Mages, and ancient crystal memories.",
      dominantElement = ElementType.DARK.name,
      realmResonanceLevel = 1,
      staminaShardsCollected = 2,
      totalStagesCount = 1,
      clearedStagesCount = 0,
      masteryPercent = 15,
      isUnlocked = true
    ),
    RealmMetadataEntity(
      id = 5,
      realmName = "Record Keep",
      seriesCode = "FFRK-CORE",
      worldLore = "The Great Archive of Paintings where Dr. Mog and Tyro safeguard the temporal history of all realms.",
      dominantElement = ElementType.WIND.name,
      realmResonanceLevel = 4,
      staminaShardsCollected = 5,
      totalStagesCount = 1,
      clearedStagesCount = 0,
      masteryPercent = 40,
      isUnlocked = true
    )
  )

  private fun defaultMedals(): List<MedalEntity> = listOf(
    MedalEntity(
      id = 1,
      name = "Illustrated Keybearer EX",
      rarityStars = 7,
      attribute = MedalAttribute.POWER.name,
      alignment = MedalAlignment.UPRIGHT.name,
      element = ElementType.HOLY.name,
      spCost = 2,
      baseMultiplier = 3.4f,
      hitCount = 7,
      guiltTier = 7,
      guiltBonusPercent = 150,
      effectType = SpecialEffectType.PARTY_HASTEGA.name
    ),
    MedalEntity(
      id = 2,
      name = "Omnislash Record VII",
      rarityStars = 7,
      attribute = MedalAttribute.POWER.name,
      alignment = MedalAlignment.UPRIGHT.name,
      element = ElementType.WIND.name,
      spCost = 3,
      baseMultiplier = 4.2f,
      hitCount = 8,
      guiltTier = 7,
      guiltBonusPercent = 145,
      effectType = SpecialEffectType.DEF_BREAKDOWN.name
    ),
    MedalEntity(
      id = 3,
      name = "Trance Esper Terra VI",
      rarityStars = 6,
      attribute = MedalAttribute.MAGIC.name,
      alignment = MedalAlignment.UPRIGHT.name,
      element = ElementType.FIRE.name,
      spCost = 2,
      baseMultiplier = 3.6f,
      hitCount = 6,
      guiltTier = 6,
      guiltBonusPercent = 130,
      effectType = SpecialEffectType.CHAIN_IGNITER.name
    ),
    MedalEntity(
      id = 4,
      name = "Darkness Riku Replica",
      rarityStars = 6,
      attribute = MedalAttribute.SPEED.name,
      alignment = MedalAlignment.REVERSED.name,
      element = ElementType.DARK.name,
      spCost = 2,
      baseMultiplier = 3.5f,
      hitCount = 5,
      guiltTier = 6,
      guiltBonusPercent = 125,
      effectType = SpecialEffectType.ATK_BREAKDOWN.name
    ),
    MedalEntity(
      id = 5,
      name = "Sentinel Grimoire Wall",
      rarityStars = 6,
      attribute = MedalAttribute.MAGIC.name,
      alignment = MedalAlignment.UPRIGHT.name,
      element = ElementType.HOLY.name,
      spCost = 1,
      baseMultiplier = 2.1f,
      hitCount = 3,
      guiltTier = 5,
      guiltBonusPercent = 100,
      effectType = SpecialEffectType.PARTY_CURAGA.name
    ),
    MedalEntity(
      id = 6,
      name = "Sephiroth Supernova EX",
      rarityStars = 7,
      attribute = MedalAttribute.POWER.name,
      alignment = MedalAlignment.REVERSED.name,
      element = ElementType.DARK.name,
      spCost = 4,
      baseMultiplier = 4.8f,
      hitCount = 10,
      guiltTier = 7,
      guiltBonusPercent = 160,
      effectType = SpecialEffectType.PURE_DAMAGE.name
    ),
    MedalEntity(
      id = 7,
      name = "Aqua Mastercrest HD",
      rarityStars = 6,
      attribute = MedalAttribute.MAGIC.name,
      alignment = MedalAlignment.UPRIGHT.name,
      element = ElementType.ICE.name,
      spCost = 2,
      baseMultiplier = 3.3f,
      hitCount = 6,
      guiltTier = 6,
      guiltBonusPercent = 120,
      effectType = SpecialEffectType.CHAIN_IGNITER.name
    ),
    MedalEntity(
      id = 8,
      name = "Lightning Army of One",
      rarityStars = 6,
      attribute = MedalAttribute.SPEED.name,
      alignment = MedalAlignment.UPRIGHT.name,
      element = ElementType.LIGHTNING.name,
      spCost = 2,
      baseMultiplier = 3.7f,
      hitCount = 9,
      guiltTier = 6,
      guiltBonusPercent = 135,
      effectType = SpecialEffectType.CHAIN_IGNITER.name
    ),
    MedalEntity(
      id = 9,
      name = "Vivi Doomsday Matrix",
      rarityStars = 6,
      attribute = MedalAttribute.MAGIC.name,
      alignment = MedalAlignment.REVERSED.name,
      element = ElementType.DARK.name,
      spCost = 3,
      baseMultiplier = 3.9f,
      hitCount = 5,
      guiltTier = 6,
      guiltBonusPercent = 130,
      effectType = SpecialEffectType.PURE_DAMAGE.name
    ),
    MedalEntity(
      id = 10,
      name = "Roxas Dual Oblivion",
      rarityStars = 7,
      attribute = MedalAttribute.SPEED.name,
      alignment = MedalAlignment.REVERSED.name,
      element = ElementType.LIGHTNING.name,
      spCost = 2,
      baseMultiplier = 4.1f,
      hitCount = 13,
      guiltTier = 7,
      guiltBonusPercent = 150,
      effectType = SpecialEffectType.DEF_BREAKDOWN.name
    )
  )

  private fun defaultHeroes(): List<HeroEntity> = listOf(
    HeroEntity(
      id = 1,
      name = "Tyro",
      title = "Archivist Keeper",
      realm = "Record Keep",
      archetype = HeroArchetype.KEEPER.name,
      level = 80,
      experiencePoints = 340000L,
      maxHp = 6400,
      attack = 430,
      defense = 420,
      magic = 440,
      resistance = 450,
      mind = 430,
      speed = 165,
      recordDiveTier = 5,
      soulBreakName = "Divine Veil Grimoire",
      soulBreakElement = ElementType.HOLY.name,
      soulBreakMultiplier = 5.8f,
      soulBreakEffect = SpecialEffectType.PARTY_HASTEGA.name,
      inParty = true,
      equippedMedalIdsCsv = "1,8,3,2,5",
      equippedWeaponId = 1,
      equippedArmorId = 7,
      equippedAccessoryId = 13,
      jobClassName = "Archivist Keeper",
      equippedSkillIdsCsv = "3,6,8",
      equippedPassiveIdsCsv = "16"
    ),
    HeroEntity(
      id = 2,
      name = "Sora",
      title = "Starlight Union Leader",
      realm = "Daybreak Town",
      archetype = HeroArchetype.KEYBEARER.name,
      level = 85,
      experiencePoints = 420000L,
      maxHp = 6850,
      attack = 510,
      defense = 390,
      magic = 410,
      resistance = 380,
      mind = 350,
      speed = 175,
      recordDiveTier = 5,
      soulBreakName = "Ragnarok Infinity Arc",
      soulBreakElement = ElementType.HOLY.name,
      soulBreakMultiplier = 7.4f,
      soulBreakEffect = SpecialEffectType.CHAIN_IGNITER.name,
      inParty = true,
      equippedMedalIdsCsv = "1,10,7,2,6",
      equippedWeaponId = 2,
      equippedArmorId = 8,
      equippedAccessoryId = 14,
      jobClassName = "Union Keybearer",
      equippedSkillIdsCsv = "2,5,11",
      equippedPassiveIdsCsv = "16"
    ),
    HeroEntity(
      id = 3,
      name = "Cloud",
      title = "Mercenary of Sector 7",
      realm = "Midgar VII",
      archetype = HeroArchetype.SOLDIER.name,
      level = 85,
      experiencePoints = 415000L,
      maxHp = 7200,
      attack = 560,
      defense = 410,
      magic = 320,
      resistance = 360,
      mind = 310,
      speed = 162,
      recordDiveTier = 6,
      soulBreakName = "Ultra Cross-Slash",
      soulBreakElement = ElementType.WIND.name,
      soulBreakMultiplier = 8.2f,
      soulBreakEffect = SpecialEffectType.DEF_BREAKDOWN.name,
      inParty = true,
      equippedMedalIdsCsv = "2,8,4,6,1",
      equippedWeaponId = 3,
      equippedArmorId = 9,
      equippedAccessoryId = 15,
      jobClassName = "SOLDIER 1st Class",
      equippedSkillIdsCsv = "1,9,2",
      equippedPassiveIdsCsv = "13,15"
    ),
    HeroEntity(
      id = 4,
      name = "Terra",
      title = "Magitek Half-Esper",
      realm = "Vector VI",
      archetype = HeroArchetype.ESPER.name,
      level = 82,
      experiencePoints = 385000L,
      maxHp = 5900,
      attack = 350,
      defense = 360,
      magic = 565,
      resistance = 460,
      mind = 410,
      speed = 168,
      recordDiveTier = 5,
      soulBreakName = "Riot Blade Inferno",
      soulBreakElement = ElementType.FIRE.name,
      soulBreakMultiplier = 7.8f,
      soulBreakEffect = SpecialEffectType.CHAIN_IGNITER.name,
      inParty = true,
      equippedMedalIdsCsv = "3,7,9,5,6",
      equippedWeaponId = 4,
      equippedArmorId = 10,
      equippedAccessoryId = 16,
      jobClassName = "Esper Sage",
      equippedSkillIdsCsv = "4,10,6",
      equippedPassiveIdsCsv = "14"
    ),
    HeroEntity(
      id = 5,
      name = "Riku",
      title = "Seeker of Dawn",
      realm = "Daybreak Town",
      archetype = HeroArchetype.DAWNWALKER.name,
      level = 80,
      experiencePoints = 330000L,
      maxHp = 6600,
      attack = 525,
      defense = 395,
      magic = 420,
      resistance = 390,
      mind = 340,
      speed = 178,
      recordDiveTier = 4,
      soulBreakName = "Dark Aura Zantetsuken",
      soulBreakElement = ElementType.DARK.name,
      soulBreakMultiplier = 7.6f,
      soulBreakEffect = SpecialEffectType.ATK_BREAKDOWN.name,
      inParty = false,
      equippedMedalIdsCsv = "6,4,9,10,2",
      equippedWeaponId = 5,
      jobClassName = "Dark Knight",
      equippedSkillIdsCsv = "7,9,1",
      equippedPassiveIdsCsv = "13"
    ),
    HeroEntity(
      id = 6,
      name = "Vivi",
      title = "Awakened Black Mage",
      realm = "Alexandria IX",
      archetype = HeroArchetype.BLACK_MAGE.name,
      level = 78,
      experiencePoints = 310000L,
      maxHp = 5400,
      attack = 290,
      defense = 330,
      magic = 590,
      resistance = 475,
      mind = 390,
      speed = 158,
      recordDiveTier = 4,
      soulBreakName = "Doublecast Meteor IX",
      soulBreakElement = ElementType.FIRE.name,
      soulBreakMultiplier = 8.0f,
      soulBreakEffect = SpecialEffectType.PURE_DAMAGE.name,
      inParty = false,
      equippedMedalIdsCsv = "3,9,7,5,8",
      equippedWeaponId = 6,
      jobClassName = "Black Wizard",
      equippedSkillIdsCsv = "4,5,11",
      equippedPassiveIdsCsv = "14"
    )
  )

  private fun defaultStages(): List<RealmStageEntity> = listOf(
    RealmStageEntity(
      id = 1,
      stageCode = "UX-01",
      title = "Clocktower Fountain Plaza",
      realm = "Daybreak Town",
      difficulty = 90,
      isProudMode = false,
      bossName = "Guard Armor X",
      bossArchetype = BossArchetype.GUARD_ARMOR.name,
      bossMaxHp = 115000,
      bossAttack = 360,
      bossDefense = 260,
      bossAttribute = MedalAttribute.SPEED.name,
      weaknessElement = ElementType.HOLY.name,
      bossCountdownTurns = 3,
      signatureAttackName = "Spinning Gauntlet Crush",
      masteryConditionText = "Exploit Holy Weakness & Win within 12 Turns",
      bestStars = 3,
      clearCount = 1
    ),
    RealmStageEntity(
      id = 2,
      stageCode = "RK-07",
      title = "Mako Reactor Core No. 1",
      realm = "Midgar VII",
      difficulty = 140,
      isProudMode = false,
      bossName = "Guard Scorpion & Behemoth",
      bossArchetype = BossArchetype.BEHEMOTH_KING.name,
      bossMaxHp = 185000,
      bossAttack = 440,
      bossDefense = 320,
      bossAttribute = MedalAttribute.POWER.name,
      weaknessElement = ElementType.LIGHTNING.name,
      bossCountdownTurns = 3,
      signatureAttackName = "Tail Laser & Heave",
      masteryConditionText = "Inflict DEF Breakdown & Build a 10+ Chain",
      bestStars = 0,
      clearCount = 0
    ),
    RealmStageEntity(
      id = 3,
      stageCode = "RK-06",
      title = "Floating Continent Summit",
      realm = "Vector VI",
      difficulty = 180,
      isProudMode = false,
      bossName = "Darkside Abyssal Titan",
      bossArchetype = BossArchetype.DARKSIDE_TITAN.name,
      bossMaxHp = 260000,
      bossAttack = 510,
      bossDefense = 370,
      bossAttribute = MedalAttribute.MAGIC.name,
      weaknessElement = ElementType.FIRE.name,
      bossCountdownTurns = 2,
      signatureAttackName = "Heartless Shadow Rain",
      masteryConditionText = "Exploit Fire Weakness & Trigger a Soul Break",
      bestStars = 0,
      clearCount = 0
    ),
    RealmStageEntity(
      id = 4,
      stageCode = "UX-PROUD",
      title = "Keyblade Graveyard Mirage",
      realm = "Daybreak Town",
      difficulty = 240,
      isProudMode = true,
      bossName = "Omega Weapon Mk.II",
      bossArchetype = BossArchetype.OMEGA_WEAPON.name,
      bossMaxHp = 380000,
      bossAttack = 620,
      bossDefense = 450,
      bossAttribute = MedalAttribute.POWER.name,
      weaknessElement = ElementType.DARK.name,
      bossCountdownTurns = 2,
      signatureAttackName = "Wave Cannon Zero",
      masteryConditionText = "Proud Mode: Survive Wave Cannon & Reach 15+ Chain",
      bestStars = 0,
      clearCount = 0
    ),
    RealmStageEntity(
      id = 5,
      stageCode = "RK-NIGHTMARE",
      title = "Hall of the Temporal Records",
      realm = "Record Keep",
      difficulty = 300,
      isProudMode = true,
      bossName = "Chaos Seraph of Eternity",
      bossArchetype = BossArchetype.CHAOS_SERAPH.name,
      bossMaxHp = 520000,
      bossAttack = 720,
      bossDefense = 520,
      bossAttribute = MedalAttribute.MAGIC.name,
      weaknessElement = ElementType.WIND.name,
      bossCountdownTurns = 2,
      signatureAttackName = "Chronicle Erasure Ray",
      masteryConditionText = "Nightmare Boss: Use Realm Synergy & Soul Break Finish",
      bestStars = 0,
      clearCount = 0
    )
  )
}
