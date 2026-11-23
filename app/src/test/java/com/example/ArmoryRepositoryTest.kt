package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.ArmoryRepository
import com.example.data.RoomArmorRepository
import com.example.data.RoomArmoryRepository
import com.example.data.RoomSkillRepository
import com.example.data.RoomWeaponRepository
import com.example.data.RpgRepository
import com.example.engine.RpgEngineViewModel
import com.example.model.ArmorEntity
import com.example.model.ElementType
import com.example.model.RpgSystemOrigin
import com.example.model.SkillCategory
import com.example.model.SkillCombatEffect
import com.example.model.SkillEntity
import com.example.model.WeaponArmorClass
import com.example.model.WeaponEntity
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
class ArmoryRepositoryTest {

  private lateinit var database: AppDatabase
  private lateinit var armoryRepository: ArmoryRepository
  private lateinit var rpgRepository: RpgRepository

  @Before
  fun setUp() {
    database = Room.inMemoryDatabaseBuilder(
      ApplicationProvider.getApplicationContext(),
      AppDatabase::class.java
    ).allowMainThreadQueries().build()

    val weaponRepo = RoomWeaponRepository(database.weaponDao())
    val armorRepo = RoomArmorRepository(database.armorDao())
    val skillRepo = RoomSkillRepository(database.skillDao())
    armoryRepository = RoomArmoryRepository(weaponRepo, armorRepo, skillRepo)
    rpgRepository = RpgRepository(database)
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `armory repository abstracts weapon armor and skill DAOs with clean reactive catalog API`() = runBlocking {
    val skillId = armoryRepository.saveSkill(
      SkillEntity(
        name = "Meteor Strike",
        rpgSystem = RpgSystemOrigin.FFVII.name,
        category = SkillCategory.BLACK_MAGIC.name,
        element = ElementType.FIRE.name,
        effectType = SkillCombatEffect.CHAIN_BUILD.name,
        powerMultiplier = 4.5f,
        bonusElements = listOf(ElementType.NON_ELEMENTAL),
        scalingWeights = mapOf("MAG" to 1.5f)
      )
    ).toInt()

    val weaponId = armoryRepository.saveWeapon(
      WeaponEntity(
        name = "Apocalypse Blade",
        weaponClass = WeaponArmorClass.GREATSWORD,
        rpgSystem = RpgSystemOrigin.FFVII,
        originRealm = "Midgar VII",
        setSeriesName = "SOLDIER First Class",
        elementBoost = ElementType.FIRE,
        attackPower = 290,
        grantedSkillId = skillId,
        socketedSkillIds = listOf(skillId)
      )
    ).toInt()

    val armorId = armoryRepository.saveArmor(
      ArmorEntity(
        name = "Mystile Bangle",
        armorClass = WeaponArmorClass.LIGHT_ARMOR,
        rpgSystem = RpgSystemOrigin.FFVII,
        originRealm = "Midgar VII",
        setSeriesName = "SOLDIER First Class",
        elementResistElement = ElementType.FIRE,
        elementResistPercent = 35,
        elementalResistances = mapOf(ElementType.FIRE to 35, ElementType.ICE to 20),
        defenseRating = 215,
        grantedSkillId = skillId,
        socketedSkillIds = listOf(skillId)
      )
    ).toInt()

    armoryRepository.socketSkillInWeapon(weaponId, skillId, slotIndex = 0)
    armoryRepository.socketSkillInArmor(armorId, skillId, slotIndex = 0)

    // Verify combined reactive catalog snapshot
    val catalog = armoryRepository.observeArmoryCatalog().first()
    assertEquals(1, catalog.weapons.size)
    assertEquals(1, catalog.armors.size)
    assertEquals(1, catalog.skills.size)
    assertEquals(3, catalog.totalEntries)

    // Verify search & typed filters through repository interfaces
    assertEquals(1, armoryRepository.searchWeapons("apocalypse").first().size)
    assertEquals(1, armoryRepository.observeWeaponsBySystem(RpgSystemOrigin.FFVII).first().size)
    assertEquals(1, armoryRepository.observeWeaponsByElement(ElementType.FIRE).first().size)

    assertEquals(1, armoryRepository.searchArmors("mystile").first().size)
    assertEquals(1, armoryRepository.observeArmorsByClass(WeaponArmorClass.LIGHT_ARMOR).first().size)
    assertEquals(1, armoryRepository.observeArmorsByElementResist(ElementType.FIRE).first().size)

    assertEquals(1, armoryRepository.searchSkills("meteor").first().size)
    assertEquals(1, armoryRepository.observeSkillsByCategory(SkillCategory.BLACK_MAGIC).first().size)
    assertEquals(1, armoryRepository.observeSkillsByEffect(SkillCombatEffect.CHAIN_BUILD).first().size)

    // Verify relational joins via repository
    val weaponWithSkills = armoryRepository.getWeaponWithSkills(weaponId)
    assertNotNull(weaponWithSkills)
    assertEquals("Meteor Strike", weaponWithSkills!!.grantedSkill?.name)
    assertEquals(1, weaponWithSkills.socketedSkills.size)

    val armorWithSkills = armoryRepository.getArmorWithSkills(armorId)
    assertNotNull(armorWithSkills)
    assertEquals("Meteor Strike", armorWithSkills!!.grantedSkill?.name)
    assertEquals(1, armorWithSkills.socketedSkills.size)

    // Verify update & delete through repository
    val updated = armoryRepository.updateWeapon(weaponWithSkills.weapon.copy(attackPower = 330))
    assertEquals(1, updated)
    assertEquals(330, armoryRepository.getWeaponById(weaponId)?.attackPower)

    armoryRepository.unsocketSkillFromWeapon(weaponId, skillId)
    assertTrue(armoryRepository.getWeaponWithSkills(weaponId)!!.socketedSkills.isEmpty())

    armoryRepository.deleteWeaponById(weaponId)
    assertNull(armoryRepository.getWeaponById(weaponId))
    assertEquals(0, armoryRepository.getWeaponCount())

    armoryRepository.deleteArmorById(armorId)
    assertNull(armoryRepository.getArmorById(armorId))
    assertEquals(0, armoryRepository.getArmorCount())

    armoryRepository.deleteSkillById(skillId)
    assertNull(armoryRepository.getSkillById(skillId))
    assertEquals(0, armoryRepository.getSkillCount())
  }

  @Test
  fun `view model consumes weapons armors and skills cleanly through ArmoryRepository`() = runBlocking {
    rpgRepository.ensureSeeded()
    val viewModel = RpgEngineViewModel(
      repository = rpgRepository,
      armoryRepository = armoryRepository,
      assetImportService = null,
      audioEngine = null
    )

    val catalog = armoryRepository.observeArmoryCatalog().first()
    assertTrue("Seeded weapons should flow through ArmoryRepository", catalog.weapons.size >= 6)
    assertTrue("Seeded armors should flow through ArmoryRepository", catalog.armors.size >= 5)
    assertTrue("Seeded skills should flow through ArmoryRepository", catalog.skills.size >= 16)
    assertNotNull(viewModel.armoryCatalogFlow)
    assertNotNull(viewModel.weaponsWithSkillsFlow)
    assertNotNull(viewModel.armorsWithSkillsFlow)
  }
}
