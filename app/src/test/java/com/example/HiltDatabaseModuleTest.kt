package com.example

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
import com.example.data.RpgDao
import com.example.data.RpgRepository
import com.example.data.SkillDao
import com.example.data.SkillRepository
import com.example.data.WeaponDao
import com.example.data.WeaponRepository
import com.example.model.ExternalSourceType
import com.example.model.ImportedAssetEntity
import com.example.model.ImportedAssetType
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [36])
class HiltDatabaseModuleTest {

  @get:Rule
  val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var appDatabase: AppDatabase
  @Inject lateinit var secondDatabaseRef: AppDatabase
  @Inject lateinit var heroDao: HeroDao
  @Inject lateinit var equipmentDao: EquipmentDao
  @Inject lateinit var weaponDao: WeaponDao
  @Inject lateinit var armorDao: ArmorDao
  @Inject lateinit var realmMetadataDao: RealmMetadataDao
  @Inject lateinit var medalDao: MedalDao
  @Inject lateinit var realmStageDao: RealmStageDao
  @Inject lateinit var engineConfigDao: EngineConfigDao
  @Inject lateinit var battleLogDao: BattleLogDao
  @Inject lateinit var importedAssetDao: ImportedAssetDao
  @Inject lateinit var skillDao: SkillDao
  @Inject lateinit var rpgDao: RpgDao
  @Inject lateinit var weaponRepository: WeaponRepository
  @Inject lateinit var armorRepository: ArmorRepository
  @Inject lateinit var skillRepository: SkillRepository
  @Inject lateinit var armoryRepository: ArmoryRepository
  @Inject lateinit var secondArmoryRepositoryRef: ArmoryRepository
  @Inject lateinit var repository: RpgRepository

  @Before
  fun init() {
    hiltRule.inject()
  }

  @Test
  fun `hilt DatabaseModule provides singleton AppDatabase and all DAOs`() = runBlocking {
    assertNotNull(appDatabase)
    assertSame("AppDatabase must be a Hilt singleton", appDatabase, secondDatabaseRef)
    assertSame(appDatabase.heroDao(), heroDao)
    assertSame(appDatabase.equipmentDao(), equipmentDao)
    assertSame(appDatabase.weaponDao(), weaponDao)
    assertSame(appDatabase.armorDao(), armorDao)
    assertSame(appDatabase.realmMetadataDao(), realmMetadataDao)
    assertSame(appDatabase.medalDao(), medalDao)
    assertSame(appDatabase.realmStageDao(), realmStageDao)
    assertSame(appDatabase.engineConfigDao(), engineConfigDao)
    assertSame(appDatabase.battleLogDao(), battleLogDao)
    assertSame(appDatabase.importedAssetDao(), importedAssetDao)
    assertSame(appDatabase.skillDao(), skillDao)
    assertSame(appDatabase.rpgDao(), rpgDao)

    repository.ensureSeeded()
    assertSame("ArmoryRepository must be a Hilt singleton", armoryRepository, secondArmoryRepositoryRef)
    assertTrue(heroDao.observeHeroes().first().isNotEmpty())
    assertTrue(equipmentDao.observeEquipment().first().isNotEmpty())
    assertTrue(weaponRepository.weapons.first().isNotEmpty())
    assertTrue(armorRepository.armors.first().isNotEmpty())
    assertTrue(realmMetadataDao.observeRealmMetadata().first().isNotEmpty())
    assertTrue(skillRepository.skills.first().size >= 16)
    assertTrue(armoryRepository.observeArmoryCatalog().first().totalEntries >= 27)

    val insertedAssetId = repository.insertImportedAsset(
      ImportedAssetEntity(
        name = "Test Sprite",
        assetType = ImportedAssetType.SPRITE.name,
        sourceType = ExternalSourceType.DIRECT_URL.name,
        sourceUrl = "https://example.com/sprite.png"
      )
    ).toInt()
    assertTrue(insertedAssetId > 0)
    val fetched = importedAssetDao.getAssetById(insertedAssetId)
    assertEquals("Test Sprite", fetched?.name)
    assertTrue(repository.importedAssets.first().any { it.id == insertedAssetId })
  }
}
