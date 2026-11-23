package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.RpgRepository
import com.example.model.ArmorEntity
import com.example.model.ArmorSkillCrossRef
import com.example.model.ElementType
import com.example.model.EquipmentEntity
import com.example.model.EquipmentSlotType
import com.example.model.HeroArchetype
import com.example.model.HeroEntity
import com.example.model.MedalAttribute
import com.example.model.RpgSystemOrigin
import com.example.model.SkillCategory
import com.example.model.SkillCombatEffect
import com.example.model.SkillEntity
import com.example.model.SpecialEffectType
import com.example.model.WeaponArmorClass
import com.example.model.WeaponEntity
import com.example.model.WeaponSkillCrossRef
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
class RpgRoomSchemaTest {

  private lateinit var database: AppDatabase
  private lateinit var repository: RpgRepository

  @Before
  fun setUp() {
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
  fun `weapon armor and skill tables support full CRUD relational sockets and foreign keys`() = runBlocking {
    val heroId = database.heroDao().insertHero(
      HeroEntity(
        name = "Ramza",
        title = "Squire Captain",
        realm = "Ivalice",
        archetype = HeroArchetype.SOLDIER.name,
        soulBreakName = "Shout",
        soulBreakElement = ElementType.HOLY.name,
        soulBreakEffect = SpecialEffectType.PARTY_HASTEGA.name
      )
    ).toInt()

    val grantedSkillId = database.skillDao().insertSkill(
      SkillEntity(
        name = "Ultima Sword",
        rpgSystem = RpgSystemOrigin.IVALICE_TACTICS.name,
        category = SkillCategory.TACTICS_ART.name,
        element = ElementType.HOLY.name,
        attribute = MedalAttribute.POWER.name,
        spCost = 3,
        cooldownTurns = 2,
        powerMultiplier = 3.8f,
        hitCount = 4,
        effectType = SkillCombatEffect.DEF_BREAK.name
      )
    ).toInt()

    val socketSkillId = database.skillDao().insertSkill(
      SkillEntity(
        name = "Counter Mastery",
        rpgSystem = RpgSystemOrigin.IVALICE_TACTICS.name,
        category = SkillCategory.PASSIVE_TRAIT.name,
        element = ElementType.NON_ELEMENTAL.name,
        attribute = MedalAttribute.POWER.name,
        effectType = SkillCombatEffect.PASSIVE_STAT_BOOST.name,
        passiveCounterPercent = 25
      )
    ).toInt()

    val weaponId = database.weaponDao().insertWeapon(
      WeaponEntity(
        name = "Excalibur II",
        weaponClass = WeaponArmorClass.GREATSWORD,
        rpgSystem = RpgSystemOrigin.IVALICE_TACTICS,
        originRealm = "Ivalice",
        setSeriesName = "Ivalice Knighthood",
        rarityStars = 7,
        upgradeLevel = 40,
        reforgeTier = 2,
        attackPower = 265,
        magicPower = 90,
        elementBoost = ElementType.HOLY,
        critRateBonus = 0.14f,
        materiaSocketsMax = 3,
        socketedSkillIds = listOf(socketSkillId),
        grantedSkillId = grantedSkillId,
        equippedByHeroId = heroId
      )
    ).toInt()

    val armorId = database.armorDao().insertArmor(
      ArmorEntity(
        name = "Grand Armor",
        armorClass = WeaponArmorClass.HEAVY_PLATE,
        rpgSystem = RpgSystemOrigin.IVALICE_TACTICS,
        originRealm = "Ivalice",
        setSeriesName = "Ivalice Knighthood",
        rarityStars = 7,
        defenseRating = 230,
        resistanceRating = 195,
        elementResistElement = ElementType.DARK,
        elementResistPercent = 35,
        counterChancePercent = 20,
        materiaSocketsMax = 2,
        socketedSkillIds = listOf(socketSkillId),
        grantedSkillId = grantedSkillId,
        equippedByHeroId = heroId
      )
    ).toInt()

    database.weaponDao().insertWeaponSkillSocket(WeaponSkillCrossRef(weaponId, socketSkillId, 0))
    database.armorDao().insertArmorSkillSocket(ArmorSkillCrossRef(armorId, socketSkillId, 0))

    // Verify @Relation projections
    val weaponWithSkills = database.weaponDao().getWeaponWithSkillsById(weaponId)
    assertNotNull(weaponWithSkills)
    assertEquals("Excalibur II", weaponWithSkills!!.weapon.name)
    assertEquals(WeaponArmorClass.GREATSWORD, weaponWithSkills.weapon.weaponClass)
    assertEquals(RpgSystemOrigin.IVALICE_TACTICS, weaponWithSkills.weapon.rpgSystem)
    assertEquals("Ultima Sword", weaponWithSkills.grantedSkill?.name)
    assertEquals(listOf("Counter Mastery"), weaponWithSkills.socketedSkills.map { it.name })

    val armorWithSkills = database.armorDao().getArmorWithSkillsById(armorId)
    assertNotNull(armorWithSkills)
    assertEquals("Grand Armor", armorWithSkills!!.armor.name)
    assertEquals(WeaponArmorClass.HEAVY_PLATE, armorWithSkills.armor.armorClass)
    assertEquals(ElementType.DARK, armorWithSkills.armor.elementResistElement)
    assertEquals(35, armorWithSkills.armor.elementResistPercent)
    assertEquals("Ultima Sword", armorWithSkills.grantedSkill?.name)
    assertEquals(listOf("Counter Mastery"), armorWithSkills.socketedSkills.map { it.name })

    // Verify Hero -> Weapons & Armors relation
    val heroGear = database.heroDao().getHeroGearRelationById(heroId)
    assertNotNull(heroGear)
    assertEquals(1, heroGear!!.equippedWeapons.size)
    assertEquals(1, heroGear.equippedArmors.size)

    // Verify query filtering by RPG system, class, and set series
    assertEquals(1, database.weaponDao().observeWeaponsBySystem(RpgSystemOrigin.IVALICE_TACTICS).first().size)
    assertEquals(1, database.weaponDao().observeWeaponsByClass(WeaponArmorClass.GREATSWORD).first().size)
    assertEquals(1, database.armorDao().observeArmorsBySetSeries("Ivalice Knighthood").first().size)
    assertEquals(2, database.skillDao().observeSkillsBySystem(RpgSystemOrigin.IVALICE_TACTICS.name).first().size)

    // Verify Foreign Key SET_NULL when granted skill is deleted & CASCADE on socket cross-ref
    database.skillDao().deleteSkillById(grantedSkillId)
    val weaponAfterSkillDelete = database.weaponDao().getWeaponById(weaponId)
    assertNotNull(weaponAfterSkillDelete)
    assertNull("grantedSkillId should become null via FK SET_NULL", weaponAfterSkillDelete!!.grantedSkillId)

    database.skillDao().deleteSkillById(socketSkillId)
    val weaponAfterSocketDelete = database.weaponDao().getWeaponWithSkillsById(weaponId)
    assertTrue("Socketed skill cross-ref should cascade delete", weaponAfterSocketDelete!!.socketedSkills.isEmpty())
  }

  @Test
  fun `repository seeding populates normalized weapons armors skills and syncs new equipment`() = runBlocking {
    repository.ensureSeeded()

    val weapons = repository.weapons.first()
    val armors = repository.armors.first()
    val skills = repository.skills.first()
    val weaponsWithSkills = repository.weaponsWithSkills.first()

    assertTrue("Seeded weapons table should contain at least 6 weapons", weapons.size >= 6)
    assertTrue("Seeded armors table should contain at least 5 armors", armors.size >= 5)
    assertTrue("Seeded skills table should contain at least 16 skills", skills.size >= 16)
    assertTrue(
      "Seeded weaponsWithSkills should resolve granted skills via @Relation",
      weaponsWithSkills.any { it.grantedSkill != null && it.socketedSkills.isNotEmpty() }
    )

    // Inserting a weapon via insertEquipment should automatically sync into the normalized weapons table
    val newEqId = repository.insertEquipment(
      EquipmentEntity(
        name = "Masamune Blade VII",
        slotType = EquipmentSlotType.WEAPON.name,
        originRealm = "Midgar VII",
        rarityStars = 7,
        bonusAtk = 275,
        rpgSystem = RpgSystemOrigin.FFVII.name,
        weaponArmorClass = WeaponArmorClass.KATANA.name,
        setSeriesName = "SOLDIER First Class",
        grantedSkillId = 1,
        socketedSkillIdsCsv = "1,13"
      )
    ).toInt()

    val syncedWeapon = repository.getWeaponWithSkillsById(newEqId)
    assertNotNull("Weapon inserted via repository must sync to weapons table", syncedWeapon)
    assertEquals("Masamune Blade VII", syncedWeapon!!.weapon.name)
    assertEquals(WeaponArmorClass.KATANA, syncedWeapon.weapon.weaponClass)
    assertEquals(2, syncedWeapon.socketedSkills.size)
  }

  @Test
  fun `type converters round-trip all weapon armor and skill custom data types with fallback safety`() {
    val converters = com.example.data.RpgTypeConverters()

    // Enum round-trips
    assertEquals(RpgSystemOrigin.CHRONO_NEXUS, converters.toRpgSystemOrigin(converters.fromRpgSystemOrigin(RpgSystemOrigin.CHRONO_NEXUS)))
    assertEquals(WeaponArmorClass.GRIMOIRE, converters.toWeaponArmorClass(converters.fromWeaponArmorClass(WeaponArmorClass.GRIMOIRE)))
    assertEquals(SkillCategory.TACTICS_ART, converters.toSkillCategory(converters.fromSkillCategory(SkillCategory.TACTICS_ART)))
    assertEquals(SkillCombatEffect.LIFESTEAL, converters.toSkillCombatEffect(converters.fromSkillCombatEffect(SkillCombatEffect.LIFESTEAL)))
    assertEquals(ElementType.LIGHTNING, converters.toElementType(converters.fromElementType(ElementType.LIGHTNING)))

    // Unknown enum values safely fall back without throwing
    assertEquals(RpgSystemOrigin.FFRK, converters.toRpgSystemOrigin("UNKNOWN_SYSTEM"))
    assertEquals(ElementType.NON_ELEMENTAL, converters.toElementType("UNKNOWN_ELEMENT"))

    // List<Int> supports both JSON encoding and legacy CSV fallback
    val intList = listOf(1, 5, 13)
    assertEquals(intList, converters.toIntList(converters.fromIntList(intList)))
    assertEquals(intList, converters.toIntList("1, 5, 13"))
    assertTrue(converters.toIntList("").isEmpty())

    // List<String> preserves entries containing commas or spaces cleanly via JSON
    val jobs = listOf("SOLDIER 1st Class", "Union Keybearer", "Esper, Sage")
    assertEquals(jobs, converters.toStringList(converters.fromStringList(jobs)))

    // List<ElementType> & List<SkillCombatEffect>
    val elements = listOf(ElementType.FIRE, ElementType.HOLY, ElementType.DARK)
    assertEquals(elements, converters.toElementTypeList(converters.fromElementTypeList(elements)))

    val effects = listOf(SkillCombatEffect.DEF_BREAK, SkillCombatEffect.CHAIN_BUILD)
    assertEquals(effects, converters.toSkillCombatEffectList(converters.fromSkillCombatEffectList(effects)))

    // Map<ElementType, Int> & Map<String, Float>
    val resistances = mapOf(ElementType.FIRE to 35, ElementType.ICE to 25, ElementType.DARK to 50)
    assertEquals(resistances, converters.toElementResistanceMap(converters.fromElementResistanceMap(resistances)))

    val scaling = mapOf("ATK" to 1.25f, "MAG" to 0.75f, "SPD" to 0.30f)
    assertEquals(scaling, converters.toStatScalingMap(converters.fromStatScalingMap(scaling)))

    // Malformed JSON inputs gracefully return empty collections
    assertTrue(converters.toElementResistanceMap("{broken-json").isEmpty())
    assertTrue(converters.toStatScalingMap("{broken-json").isEmpty())
  }

  @Test
  fun `room database persists and queries custom typed columns on weapons armors and skills`() = runBlocking {
    val skillId = database.skillDao().insertSkill(
      SkillEntity(
        name = "Tri-Elemental Flare",
        rpgSystem = RpgSystemOrigin.CHRONO_NEXUS.name,
        category = SkillCategory.BLACK_MAGIC.name,
        element = ElementType.FIRE.name,
        attribute = MedalAttribute.MAGIC.name,
        effectType = SkillCombatEffect.CHAIN_BUILD.name,
        bonusElements = listOf(ElementType.ICE, ElementType.LIGHTNING),
        secondaryEffects = listOf(SkillCombatEffect.DEF_BREAK, SkillCombatEffect.HASTE_ATK_UP),
        requiredJobClasses = listOf("Esper Sage", "Black Wizard"),
        scalingWeights = mapOf("MAG" to 1.45f, "SPD" to 0.20f)
      )
    ).toInt()

    val weaponId = database.weaponDao().insertWeapon(
      WeaponEntity(
        name = "Chrono Rainbow Blade",
        weaponClass = WeaponArmorClass.KATANA,
        rpgSystem = RpgSystemOrigin.CHRONO_NEXUS,
        originRealm = "Daybreak Town",
        elementBoost = ElementType.HOLY,
        secondaryElements = listOf(ElementType.WIND, ElementType.LIGHTNING),
        compatibleJobs = listOf("SOLDIER 1st Class", "Union Keybearer"),
        statScaling = mapOf("ATK" to 1.30f, "MAG" to 0.65f),
        socketedSkillIds = listOf(skillId),
        grantedSkillId = skillId
      )
    ).toInt()

    val armorId = database.armorDao().insertArmor(
      ArmorEntity(
        name = "Prism Vestment",
        armorClass = WeaponArmorClass.MYSTIC_ROBE,
        rpgSystem = RpgSystemOrigin.CHRONO_NEXUS,
        originRealm = "Daybreak Town",
        elementResistElement = ElementType.FIRE,
        elementResistPercent = 40,
        elementalResistances = mapOf(ElementType.FIRE to 40, ElementType.ICE to 30, ElementType.DARK to 25),
        statusImmunities = listOf("Silence", "Stop", "Petrify"),
        compatibleJobs = listOf("Esper Sage", "Archivist Keeper"),
        socketedSkillIds = listOf(skillId),
        grantedSkillId = skillId
      )
    ).toInt()

    val loadedSkill = database.skillDao().getSkillById(skillId)
    assertNotNull(loadedSkill)
    assertEquals(listOf(ElementType.ICE, ElementType.LIGHTNING), loadedSkill!!.bonusElements)
    assertEquals(listOf(SkillCombatEffect.DEF_BREAK, SkillCombatEffect.HASTE_ATK_UP), loadedSkill.secondaryEffects)
    assertEquals(listOf("Esper Sage", "Black Wizard"), loadedSkill.requiredJobClasses)
    assertEquals(mapOf("MAG" to 1.45f, "SPD" to 0.20f), loadedSkill.scalingWeights)

    val loadedWeapon = database.weaponDao().getWeaponById(weaponId)
    assertNotNull(loadedWeapon)
    assertEquals(listOf(ElementType.WIND, ElementType.LIGHTNING), loadedWeapon!!.secondaryElements)
    assertEquals(listOf("SOLDIER 1st Class", "Union Keybearer"), loadedWeapon.compatibleJobs)
    assertEquals(mapOf("ATK" to 1.30f, "MAG" to 0.65f), loadedWeapon.statScaling)
    assertEquals(listOf(skillId), loadedWeapon.socketedSkillIds)

    val loadedArmor = database.armorDao().getArmorById(armorId)
    assertNotNull(loadedArmor)
    assertEquals(mapOf(ElementType.FIRE to 40, ElementType.ICE to 30, ElementType.DARK to 25), loadedArmor!!.elementalResistances)
    assertEquals(listOf("Silence", "Stop", "Petrify"), loadedArmor.statusImmunities)
    assertEquals(listOf("Esper Sage", "Archivist Keeper"), loadedArmor.compatibleJobs)

    // Verify strongly-typed enum DAO queries backed by RpgTypeConverters
    assertEquals(1, database.weaponDao().observeWeaponsByElement(ElementType.HOLY).first().size)
    assertEquals(1, database.armorDao().observeArmorsByElementResist(ElementType.FIRE).first().size)
    assertEquals(1, database.skillDao().observeSkillsBySystemEnum(RpgSystemOrigin.CHRONO_NEXUS).first().size)
    assertEquals(1, database.skillDao().observeSkillsByCategoryEnum(SkillCategory.BLACK_MAGIC).first().size)
    assertEquals(1, database.skillDao().observeSkillsByElementEnum(ElementType.FIRE).first().size)
    assertEquals(1, database.skillDao().observeSkillsByEffect(SkillCombatEffect.CHAIN_BUILD).first().size)
  }

  @Test
  fun `weapon armor and skill DAOs support complete CRUD search batch upsert and delete operations`() = runBlocking {
    val weaponDao = database.weaponDao()
    val armorDao = database.armorDao()
    val skillDao = database.skillDao()

    // --- SkillDao CRUD ---
    val skillIds = skillDao.upsertSkills(
      listOf(
        SkillEntity(name = "Omnislash v5", powerMultiplier = 4.2f, rankLevel = 3),
        SkillEntity(name = "Holy Aura", category = SkillCategory.WHITE_MAGIC.name, rankLevel = 2)
      )
    )
    assertEquals(2, skillDao.getSkillCount())
    assertEquals(2, skillDao.getAllSkills().size)
    val firstSkillId = skillIds.first().toInt()
    val loadedSkill = skillDao.observeSkillById(firstSkillId).first()
    assertNotNull(loadedSkill)
    assertEquals("Omnislash v5", loadedSkill!!.name)
    assertEquals(1, skillDao.searchSkillsByName("omni").first().size)

    val updatedSkillRows = skillDao.updateSkill(loadedSkill.copy(rankLevel = 5, powerMultiplier = 5.0f))
    assertEquals(1, updatedSkillRows)
    assertEquals(5, skillDao.getSkillById(firstSkillId)?.rankLevel)
    assertEquals(1, skillDao.deleteSkill(skillDao.getSkillById(firstSkillId)!!))
    assertEquals(1, skillDao.getSkillCount())

    // --- WeaponDao CRUD ---
    val weaponIds = weaponDao.upsertWeapons(
      listOf(
        WeaponEntity(name = "Ultima Weapon", weaponClass = WeaponArmorClass.KEYBLADE, attackPower = 260),
        WeaponEntity(name = "Ragnarok Blade", weaponClass = WeaponArmorClass.GREATSWORD, attackPower = 240)
      )
    )
    assertEquals(2, weaponDao.getWeaponCount())
    assertEquals(2, weaponDao.getAllWeapons().size)
    val firstWeaponId = weaponIds.first().toInt()
    val loadedWeapon = weaponDao.observeWeaponById(firstWeaponId).first()
    assertNotNull(loadedWeapon)
    assertEquals(1, weaponDao.searchWeaponsByName("ultima").first().size)

    val updatedWeaponRows = weaponDao.updateWeapon(loadedWeapon!!.copy(attackPower = 310, reforgeTier = 3))
    assertEquals(1, updatedWeaponRows)
    assertEquals(310, weaponDao.getWeaponById(firstWeaponId)?.attackPower)
    assertEquals(1, weaponDao.deleteWeapon(weaponDao.getWeaponById(firstWeaponId)!!))
    assertEquals(1, weaponDao.getWeaponCount())
    assertEquals(1, weaponDao.deleteAllWeapons())
    assertEquals(0, weaponDao.getWeaponCount())

    // --- ArmorDao CRUD ---
    val armorIds = armorDao.upsertArmors(
      listOf(
        ArmorEntity(name = "Maximilian Plate", armorClass = WeaponArmorClass.HEAVY_PLATE, defenseRating = 245),
        ArmorEntity(name = "Lordly Robes", armorClass = WeaponArmorClass.MYSTIC_ROBE, defenseRating = 195)
      )
    )
    assertEquals(2, armorDao.getArmorCount())
    assertEquals(2, armorDao.getAllArmors().size)
    val firstArmorId = armorIds.first().toInt()
    val loadedArmor = armorDao.observeArmorById(firstArmorId).first()
    assertNotNull(loadedArmor)
    assertEquals(1, armorDao.searchArmorsByName("maximilian").first().size)

    val updatedArmorRows = armorDao.updateArmor(loadedArmor!!.copy(defenseRating = 280, reforgeTier = 2))
    assertEquals(1, updatedArmorRows)
    assertEquals(280, armorDao.getArmorById(firstArmorId)?.defenseRating)
    assertEquals(1, armorDao.deleteArmor(armorDao.getArmorById(firstArmorId)!!))
    assertEquals(1, armorDao.getArmorCount())
    assertEquals(1, armorDao.deleteAllArmors())
    assertEquals(0, armorDao.getArmorCount())

    // Clear remaining skills
    assertEquals(1, skillDao.deleteAllSkills())
    assertEquals(0, skillDao.getSkillCount())
  }

  @Test
  fun `Weapon Armor and Skill Room entity data classes map directly to existing DAOs`() = runBlocking {
    val skill: com.example.model.Skill = com.example.model.Skill(
      name = "Flare Star",
      rpgSystem = RpgSystemOrigin.FFVI.name,
      category = SkillCategory.BLACK_MAGIC.name,
      element = ElementType.FIRE.name,
      effectType = SkillCombatEffect.DAMAGE.name
    )
    val skillId = database.skillDao().insertSkill(skill).toInt()

    val weapon: com.example.model.Weapon = com.example.model.Weapon(
      name = "Oathkeeper Keyblade",
      weaponClass = WeaponArmorClass.KEYBLADE,
      rpgSystem = RpgSystemOrigin.KHUX,
      elementBoost = ElementType.HOLY,
      attackPower = 250,
      grantedSkillId = skillId
    )
    val weaponId = database.weaponDao().insertWeapon(weapon).toInt()

    val armor: com.example.model.Armor = com.example.model.Armor(
      name = "Genji Shield",
      armorClass = WeaponArmorClass.AEGIS_SHIELD,
      rpgSystem = RpgSystemOrigin.FFVI,
      elementResistElement = ElementType.FIRE,
      elementResistPercent = 45,
      defenseRating = 240,
      grantedSkillId = skillId
    )
    val armorId = database.armorDao().insertArmor(armor).toInt()

    assertEquals("Flare Star", database.skillDao().getSkillById(skillId)?.name)
    assertEquals("Oathkeeper Keyblade", database.weaponDao().getWeaponById(weaponId)?.name)
    assertEquals("Genji Shield", database.armorDao().getArmorById(armorId)?.name)
  }
}
