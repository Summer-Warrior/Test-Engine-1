package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.RpgRepository
import com.example.di.AppModule
import com.example.di.DefaultAppContainer
import com.example.engine.RpgEngineViewModel
import com.example.model.ElementType
import com.example.model.EquipmentEntity
import com.example.model.EquipmentSlotType
import com.example.model.HeroArchetype
import com.example.model.HeroEntity
import com.example.model.RealmMetadataEntity
import com.example.model.SpecialEffectType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {
  private lateinit var database: AppDatabase
  private lateinit var repository: RpgRepository

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = AppModule.provideInMemoryDatabase(context)
    repository = AppModule.provideRpgRepository(
      heroDao = AppModule.provideHeroDao(database),
      equipmentDao = AppModule.provideEquipmentDao(database),
      realmMetadataDao = AppModule.provideRealmMetadataDao(database),
      medalDao = AppModule.provideMedalDao(database),
      realmStageDao = AppModule.provideRealmStageDao(database),
      engineConfigDao = AppModule.provideEngineConfigDao(database),
      battleLogDao = AppModule.provideBattleLogDao(database)
    )
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Chronicle Nexus", appName)
  }

  @Test
  fun `appDatabase and AppModule DI provide all domain DAOs and seed data`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = DefaultAppContainer(context)
    assertNotNull(container.appDatabase)
    assertNotNull(container.heroDao)
    assertNotNull(container.equipmentDao)
    assertNotNull(container.realmMetadataDao)
    assertNotNull(container.medalDao)
    assertNotNull(container.realmStageDao)
    assertNotNull(container.engineConfigDao)
    assertNotNull(container.battleLogDao)
    assertNotNull(container.rpgRepository)

    repository.ensureSeeded()

    val heroes = database.heroDao().observeHeroes().first()
    val loadouts = database.heroDao().observeCharacterLoadouts().first()
    val equipment = database.equipmentDao().observeEquipment().first()
    val realms = database.realmMetadataDao().observeRealmMetadata().first()
    val medals = database.medalDao().observeMedals().first()
    val stages = database.realmStageDao().observeRealmStages().first()
    val config = database.engineConfigDao().observeEngineConfig().first()

    assertTrue("Expected default heroes to be seeded", heroes.size >= 6)
    assertTrue("Expected character loadout relations to be resolved", loadouts.isNotEmpty() && loadouts.first().equippedItems.isNotEmpty())
    assertNotNull("Expected character home realm metadata relation", loadouts.first().homeRealmMetadata)
    assertTrue("Expected default equipment to be seeded", equipment.size >= 10)
    assertTrue("Expected default realm metadata to be seeded", realms.size >= 5)
    assertTrue("Expected default medals to be seeded", medals.size >= 10)
    assertTrue("Expected default stages to be seeded", stages.size >= 5)
    assertNotNull("Expected engine config to be seeded", config)

    val viewModel = RpgEngineViewModel(repository)
    assertNotNull(viewModel.battleState.value)
  }

  @Test
  fun `character equipment and realm DAOs support full CRUD operations`() = runBlocking {
    val charDao = database.characterDao()
    val equipDao = database.equipmentDao()
    val realmDao = database.realmDao()

    // 1. Character CRUD
    val createdHeroId = charDao.insertHero(
      HeroEntity(
        name = "Celes",
        title = "Rune Knight",
        realm = "Vector VI",
        archetype = HeroArchetype.ESPER.name,
        level = 70,
        soulBreakName = "Runic Maria Aria",
        soulBreakElement = ElementType.ICE.name,
        soulBreakEffect = SpecialEffectType.PARTY_CURAGA.name
      )
    ).toInt()
    val fetchedHero = charDao.getHeroById(createdHeroId)
    assertNotNull(fetchedHero)
    assertEquals("Celes", fetchedHero?.name)

    charDao.updateHero(fetchedHero!!.copy(level = 85, attack = 510))
    val updatedHero = charDao.getHeroById(createdHeroId)
    assertEquals(85, updatedHero?.level)
    assertEquals(510, updatedHero?.attack)

    charDao.deleteHeroById(createdHeroId)
    assertNull(charDao.getHeroById(createdHeroId))

    // 2. Equipment CRUD
    val createdEquipId = equipDao.insertEquipment(
      EquipmentEntity(
        name = "Save the Queen VII",
        slotType = EquipmentSlotType.WEAPON.name,
        originRealm = "Vector VI",
        rarityStars = 7,
        upgradeLevel = 30,
        bonusAtk = 215
      )
    ).toInt()
    val fetchedEquip = equipDao.getEquipmentById(createdEquipId)
    assertNotNull(fetchedEquip)
    assertEquals("Save the Queen VII", fetchedEquip?.name)

    equipDao.updateEquipment(fetchedEquip!!.copy(upgradeLevel = 45, bonusAtk = 260))
    val updatedEquip = equipDao.getEquipmentById(createdEquipId)
    assertEquals(45, updatedEquip?.upgradeLevel)
    assertEquals(260, updatedEquip?.bonusAtk)

    equipDao.deleteEquipment(updatedEquip!!)
    assertNull(equipDao.getEquipmentById(createdEquipId))

    // 3. Realm Metadata CRUD
    val createdRealmId = realmDao.insertRealmMetadata(
      RealmMetadataEntity(
        realmName = "Twilight Town",
        seriesCode = "KH-II",
        worldLore = "A quiet town bathed in perpetual sunset.",
        dominantElement = ElementType.LIGHTNING.name,
        realmResonanceLevel = 2,
        staminaShardsCollected = 3,
        totalStagesCount = 4,
        clearedStagesCount = 1,
        masteryPercent = 25
      )
    ).toInt()
    val fetchedRealm = realmDao.getRealmById(createdRealmId)
    assertNotNull(fetchedRealm)
    assertEquals("Twilight Town", fetchedRealm?.realmName)

    realmDao.updateRealmMetadata(fetchedRealm!!.copy(masteryPercent = 100, clearedStagesCount = 4))
    val updatedRealm = realmDao.getRealmByName("Twilight Town")
    assertEquals(100, updatedRealm?.masteryPercent)

    realmDao.deleteRealmById(createdRealmId)
    assertNull(realmDao.getRealmById(createdRealmId))
  }
}
