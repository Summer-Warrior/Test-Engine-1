package com.example

import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.RpgRepository
import com.example.engine.RpgEngineViewModel
import com.example.model.ElementType
import com.example.model.EquipmentEntity
import com.example.model.EquipmentSlotType
import com.example.model.HeroArchetype
import com.example.model.HeroEntity
import com.example.model.MedalAttribute
import com.example.model.RpgLoadoutCalculator
import com.example.model.RpgSystemOrigin
import com.example.model.SkillCategory
import com.example.model.SkillCombatEffect
import com.example.model.SkillEntity
import com.example.model.SpecialEffectType
import com.example.model.WeaponArmorClass
import java.time.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MultiRpgSystemTest {

  private lateinit var database: AppDatabase
  private lateinit var repository: RpgRepository

  @Before
  fun setup() {
    database = Room.inMemoryDatabaseBuilder(
      ApplicationProvider.getApplicationContext(),
      AppDatabase::class.java
    ).allowMainThreadQueries().build()
    repository = RpgRepository(database)
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `RpgLoadoutCalculator evaluates set bonuses, reforge scaling, materia sockets, and passive traits`() {
    val hero = HeroEntity(
      id = 10,
      name = "Cloud",
      title = "1st Class SOLDIER",
      realm = "Midgar VII",
      archetype = HeroArchetype.SOLDIER.name,
      maxHp = 5000,
      attack = 500,
      defense = 350,
      magic = 250,
      resistance = 300,
      speed = 160,
      soulBreakName = "Omnislash",
      soulBreakElement = ElementType.WIND.name,
      soulBreakEffect = SpecialEffectType.PURE_DAMAGE.name,
      equippedSkillIdsCsv = "1",
      equippedPassiveIdsCsv = "13"
    )
    val weapon = EquipmentEntity(
      id = 101,
      name = "Fusion Buster Sword",
      slotType = EquipmentSlotType.WEAPON.name,
      originRealm = "Midgar VII",
      bonusAtk = 200,
      setSeriesName = "SOLDIER First Class",
      critRateBonus = 0.12f,
      reforgeTier = 2, // +20% gear stats
      socketedSkillIdsCsv = "2,13", // 2 materia orbs
      grantedSkillId = 4
    )
    val armor = EquipmentEntity(
      id = 102,
      name = "SOLDIER Pauldron",
      slotType = EquipmentSlotType.ARMOR.name,
      originRealm = "Midgar VII",
      bonusDef = 150,
      setSeriesName = "SOLDIER First Class",
      counterChancePercent = 15
    )
    val accessory = EquipmentEntity(
      id = 103,
      name = "Mako Emblem",
      slotType = EquipmentSlotType.ACCESSORY.name,
      originRealm = "Midgar VII",
      bonusAtk = 50,
      setSeriesName = "SOLDIER First Class",
      lifestealPercent = 10,
      elementResistElement = ElementType.LIGHTNING.name,
      elementResistPercent = 30
    )
    val skills = listOf(
      SkillEntity(id = 1, name = "Braver", category = SkillCategory.ACTIVE_TECH.name),
      SkillEntity(id = 2, name = "Hallowed Bolt", category = SkillCategory.TACTICS_ART.name),
      SkillEntity(id = 4, name = "Firaga Chain", category = SkillCategory.BLACK_MAGIC.name),
      SkillEntity(
        id = 13,
        name = "Mako Surge",
        category = SkillCategory.PASSIVE_TRAIT.name,
        effectType = SkillCombatEffect.PASSIVE_STAT_BOOST.name,
        passiveAtkPercent = 12,
        passiveSpdBonus = 8,
        passiveCritBonus = 0.08f
      )
    )

    val loadout = RpgLoadoutCalculator.computeHeroLoadout(
      hero = hero,
      weapon = weapon,
      armor = armor,
      accessory = accessory,
      allSkills = skills,
      activeStageRealm = "Midgar VII"
    )

    assertTrue(loadout.hasRealmSynergy)
    assertEquals(1, loadout.activeSetBonuses.size)
    assertEquals(3, loadout.activeSetBonuses.first().piecesEquipped)
    assertTrue("Effective ATK should reflect synergy, reforge, set bonus, and passive", loadout.effectiveAtk > 1200)
    assertTrue("Crit rate should stack gear + 3pc set + passive", loadout.critRateBonus >= 0.34f)
    assertEquals(25, loadout.counterChancePercent) // 15 gear + 10 3pc set
    assertEquals(10, loadout.lifestealPercent)
    assertEquals(ElementType.LIGHTNING, loadout.dominantResistElement)
    assertEquals(30, loadout.dominantResistPercent)
    assertEquals(listOf(1, 4, 2), loadout.activeSkills.map { it.id })
  }

  @Test
  fun `ViewModel executes multi-RPG skills with Shield Wall, DEF Break, cooldowns, and SP cost reduction`() = runBlocking {
    repository.ensureSeeded()
    val vm = RpgEngineViewModel(repository)

    val heroes = repository.heroes.first()
    val stages = repository.stages.first()
    val eq = repository.equipment.first()
    val skills = repository.skills.first()
    val cfg = repository.engineConfig.first()!!

    vm.startEncounter(stages.first(), heroes, cfg, eq, skills)
    // Advance Robolectric MainLooper clock so the 20Hz engine loop ticks ATB to 100%
    var attempts = 0
    while (attempts < 30 && vm.battleState.value.partyStates.none { it.isAtbReady }) {
      shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
      attempts++
    }
    val readyIdx = vm.battleState.value.partyStates.indexOfFirst { it.isAtbReady }
    assertTrue("At least one hero should become ATB ready", readyIdx >= 0)

    val aegisWall = skills.first { it.effectTypeEnum == SkillCombatEffect.SHIELD_WALL }
    val spBefore = vm.battleState.value.currentSp
    vm.executeHeroSkill(aegisWall, readyIdx)

    val afterWall = vm.battleState.value
    assertTrue("Party should receive Shield Wall barrier", afterWall.partyStates.any { it.shieldWallSecondsRemaining > 0f })
    assertTrue("Cooldown should be set for cast skill", (afterWall.partyStates[readyIdx].skillCooldowns[aegisWall.id] ?: 0) > 0)
    assertTrue("SP should decrease or stay within max", afterWall.currentSp <= spBefore)

    // Test SkillDao creation & honing persistence
    vm.createCustomSkill(
      name = "Chrono Flare X",
      rpgSystem = RpgSystemOrigin.CHRONO_NEXUS,
      category = SkillCategory.BLACK_MAGIC,
      element = ElementType.FIRE,
      attribute = MedalAttribute.MAGIC,
      spCost = 2,
      cooldownTurns = 1,
      powerMultiplier = 3.8f,
      hitCount = 6,
      effectType = SkillCombatEffect.DEF_BREAK,
      description = "Dual-era fire burst"
    )
    shadowOf(Looper.getMainLooper()).idle()
    delay(60L)
    shadowOf(Looper.getMainLooper()).idle()

    val updatedSkills = repository.skills.first()
    val created = updatedSkills.find { it.name == "Chrono Flare X" }
    assertTrue("Custom skill should persist in SkillDao", created != null)

    vm.honeSkillRank(created!!)
    shadowOf(Looper.getMainLooper()).idle()
    delay(60L)
    shadowOf(Looper.getMainLooper()).idle()

    val honed = repository.getSkillById(created.id)
    assertEquals(2, honed?.rankLevel)
  }
}
