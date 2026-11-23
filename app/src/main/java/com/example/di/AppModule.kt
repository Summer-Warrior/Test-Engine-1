package com.example.di

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.ArmorDao
import com.example.data.ArmorRepository
import com.example.data.ArmoryRepository
import com.example.data.BattleLogDao
import com.example.data.EngineConfigDao
import com.example.data.EquipmentDao
import com.example.data.HeroDao
import com.example.data.ImportedAssetDao
import com.example.data.MedalDao
import com.example.data.RealmMetadataDao
import com.example.data.RealmStageDao
import com.example.data.RoomArmorRepository
import com.example.data.RoomArmoryRepository
import com.example.data.RoomSkillRepository
import com.example.data.RoomWeaponRepository
import com.example.data.RpgDao
import com.example.data.RpgRepository
import com.example.data.SkillDao
import com.example.data.SkillRepository
import com.example.data.WeaponDao
import com.example.data.WeaponRepository

/**
 * Manual Dependency Injection container providing [AppDatabase], domain DAOs,
 * and repository singletons ([RpgRepository], [WeaponRepository], [ArmorRepository],
 * [SkillRepository], [ArmoryRepository]) for the Chronicle Nexus RPG Engine.
 */
interface AppContainer {
  val appDatabase: AppDatabase
  val heroDao: HeroDao
  val equipmentDao: EquipmentDao
  val weaponDao: WeaponDao
  val armorDao: ArmorDao
  val realmMetadataDao: RealmMetadataDao
  val medalDao: MedalDao
  val realmStageDao: RealmStageDao
  val engineConfigDao: EngineConfigDao
  val battleLogDao: BattleLogDao
  val importedAssetDao: ImportedAssetDao
  val skillDao: SkillDao
  val rpgDao: RpgDao
  val weaponRepository: WeaponRepository
  val armorRepository: ArmorRepository
  val skillRepository: SkillRepository
  val armoryRepository: ArmoryRepository
  val rpgRepository: RpgRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
  override val appDatabase: AppDatabase by lazy {
    AppModule.provideAppDatabase(context)
  }

  override val heroDao: HeroDao by lazy {
    AppModule.provideHeroDao(appDatabase)
  }

  override val equipmentDao: EquipmentDao by lazy {
    AppModule.provideEquipmentDao(appDatabase)
  }

  override val weaponDao: WeaponDao by lazy {
    AppModule.provideWeaponDao(appDatabase)
  }

  override val armorDao: ArmorDao by lazy {
    AppModule.provideArmorDao(appDatabase)
  }

  override val realmMetadataDao: RealmMetadataDao by lazy {
    AppModule.provideRealmMetadataDao(appDatabase)
  }

  override val medalDao: MedalDao by lazy {
    AppModule.provideMedalDao(appDatabase)
  }

  override val realmStageDao: RealmStageDao by lazy {
    AppModule.provideRealmStageDao(appDatabase)
  }

  override val engineConfigDao: EngineConfigDao by lazy {
    AppModule.provideEngineConfigDao(appDatabase)
  }

  override val battleLogDao: BattleLogDao by lazy {
    AppModule.provideBattleLogDao(appDatabase)
  }

  override val importedAssetDao: ImportedAssetDao by lazy {
    AppModule.provideImportedAssetDao(appDatabase)
  }

  override val skillDao: SkillDao by lazy {
    AppModule.provideSkillDao(appDatabase)
  }

  override val rpgDao: RpgDao by lazy {
    AppModule.provideRpgDao(appDatabase)
  }

  override val weaponRepository: WeaponRepository by lazy {
    AppModule.provideWeaponRepository(weaponDao)
  }

  override val armorRepository: ArmorRepository by lazy {
    AppModule.provideArmorRepository(armorDao)
  }

  override val skillRepository: SkillRepository by lazy {
    AppModule.provideSkillRepository(skillDao)
  }

  override val armoryRepository: ArmoryRepository by lazy {
    AppModule.provideArmoryRepository(weaponRepository, armorRepository, skillRepository)
  }

  override val rpgRepository: RpgRepository by lazy {
    AppModule.provideRpgRepository(
      heroDao = heroDao,
      equipmentDao = equipmentDao,
      realmMetadataDao = realmMetadataDao,
      medalDao = medalDao,
      realmStageDao = realmStageDao,
      engineConfigDao = engineConfigDao,
      battleLogDao = battleLogDao,
      importedAssetDao = importedAssetDao,
      skillDao = skillDao,
      weaponDao = weaponDao,
      armorDao = armorDao
    )
  }
}

/**
 * Manual Dependency Injection Module for providing database and repository instances.
 */
object AppModule {
  fun provideAppDatabase(context: Context): AppDatabase =
    AppDatabase.getInstance(context)

  fun provideInMemoryDatabase(context: Context): AppDatabase =
    AppDatabase.createInMemory(context)

  fun provideHeroDao(database: AppDatabase): HeroDao = database.heroDao()

  fun provideEquipmentDao(database: AppDatabase): EquipmentDao = database.equipmentDao()

  fun provideWeaponDao(database: AppDatabase): WeaponDao = database.weaponDao()

  fun provideArmorDao(database: AppDatabase): ArmorDao = database.armorDao()

  fun provideRealmMetadataDao(database: AppDatabase): RealmMetadataDao = database.realmMetadataDao()

  fun provideMedalDao(database: AppDatabase): MedalDao = database.medalDao()

  fun provideRealmStageDao(database: AppDatabase): RealmStageDao = database.realmStageDao()

  fun provideEngineConfigDao(database: AppDatabase): EngineConfigDao = database.engineConfigDao()

  fun provideBattleLogDao(database: AppDatabase): BattleLogDao = database.battleLogDao()

  fun provideImportedAssetDao(database: AppDatabase): ImportedAssetDao = database.importedAssetDao()

  fun provideSkillDao(database: AppDatabase): SkillDao = database.skillDao()

  fun provideRpgDao(database: AppDatabase): RpgDao = database.rpgDao()

  fun provideWeaponRepository(weaponDao: WeaponDao): WeaponRepository =
    RoomWeaponRepository(weaponDao)

  fun provideArmorRepository(armorDao: ArmorDao): ArmorRepository =
    RoomArmorRepository(armorDao)

  fun provideSkillRepository(skillDao: SkillDao): SkillRepository =
    RoomSkillRepository(skillDao)

  fun provideArmoryRepository(
    weaponRepository: WeaponRepository,
    armorRepository: ArmorRepository,
    skillRepository: SkillRepository
  ): ArmoryRepository = RoomArmoryRepository(weaponRepository, armorRepository, skillRepository)

  fun provideRpgRepository(
    heroDao: HeroDao,
    equipmentDao: EquipmentDao,
    realmMetadataDao: RealmMetadataDao,
    medalDao: MedalDao,
    realmStageDao: RealmStageDao,
    engineConfigDao: EngineConfigDao,
    battleLogDao: BattleLogDao,
    importedAssetDao: ImportedAssetDao? = null,
    skillDao: SkillDao? = null,
    weaponDao: WeaponDao? = null,
    armorDao: ArmorDao? = null
  ): RpgRepository = RpgRepository(
    heroDao = heroDao,
    equipmentDao = equipmentDao,
    realmMetadataDao = realmMetadataDao,
    medalDao = medalDao,
    realmStageDao = realmStageDao,
    engineConfigDao = engineConfigDao,
    battleLogDao = battleLogDao,
    importedAssetDao = importedAssetDao,
    skillDao = skillDao,
    weaponDao = weaponDao,
    armorDao = armorDao
  )
}
