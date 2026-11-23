package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.HpEmerald
import com.example.ui.theme.MagicSapphire
import com.example.ui.theme.MythrilCyan
import com.example.ui.theme.PowerCrimson
import com.example.ui.theme.RelicGold
import com.example.ui.theme.SoulBreakMagenta
import com.example.ui.theme.SynergyAzure
import kotlin.math.roundToInt

/**
 * Multi-RPG systemic traditions integrated into Chronicle Nexus.
 */
enum class RpgSystemOrigin(
  val label: String,
  val shortTag: String,
  val accentColor: Color
) {
  FFRK("Record Keep (Synergy & Dive)", "FFRK", SynergyAzure),
  FFVII("Midgar VII (Materia Sockets)", "FFVII", HpEmerald),
  KHUX("Daybreak (Keyblade & Lux)", "KHUX", RelicGold),
  FFVI("Vector VI (Esper & Trance)", "FFVI", SoulBreakMagenta),
  IVALICE_TACTICS("Ivalice Tactics (Jobs & Arts)", "FFT", PowerCrimson),
  CHRONO_NEXUS("Chrono Raid (Sets & Dual Tech)", "CHRONO", MythrilCyan)
}

enum class WeaponArmorClass(
  val label: String,
  val slotType: EquipmentSlotType
) {
  // Weapons
  KEYBLADE("Keyblade", EquipmentSlotType.WEAPON),
  GREATSWORD("Greatsword", EquipmentSlotType.WEAPON),
  KATANA("Katana", EquipmentSlotType.WEAPON),
  STAFF("Arcane Staff", EquipmentSlotType.WEAPON),
  GRIMOIRE("Record Grimoire", EquipmentSlotType.WEAPON),
  DAGGER("Twin Dagger", EquipmentSlotType.WEAPON),
  SPEAR("Dragoon Spear", EquipmentSlotType.WEAPON),

  // Armor
  HEAVY_PLATE("Heavy Plate", EquipmentSlotType.ARMOR),
  LIGHT_ARMOR("Brigandine Coat", EquipmentSlotType.ARMOR),
  MYSTIC_ROBE("Mystic Vestment", EquipmentSlotType.ARMOR),
  AEGIS_SHIELD("Aegis Shield", EquipmentSlotType.ARMOR),

  // Accessories
  RING("Relic Ring", EquipmentSlotType.ACCESSORY),
  RIBBON("Ward Ribbon", EquipmentSlotType.ACCESSORY),
  MATERIA_ORB("Materia Charm", EquipmentSlotType.ACCESSORY),
  CHRONO_CHARM("Chrono Emblem", EquipmentSlotType.ACCESSORY)
}

enum class SkillCategory(val label: String, val isPassive: Boolean = false) {
  ACTIVE_TECH("Combat Tech"),
  BLACK_MAGIC("Black Magic"),
  WHITE_MAGIC("White Magic"),
  TACTICS_ART("Tactics Job Art"),
  PASSIVE_TRAIT("Passive Trait", isPassive = true)
}

enum class SkillCombatEffect(val label: String) {
  DAMAGE("Direct Strike"),
  HEAL_PARTY("Party Medica Heal"),
  DEF_BREAK("Armor Crush (-50% DEF)"),
  ATK_BREAK("Power Break (-40% ATK)"),
  HASTE_ATK_UP("Haste + Valor (+30% ATK)"),
  SHIELD_WALL("Aegis Wall (-35% DMG Taken)"),
  LIFESTEAL("Drain Blade (35% Lifesteal)"),
  CHAIN_BUILD("Elemental Chain Surge (+4 Hits)"),
  PASSIVE_STAT_BOOST("Passive Mastery Aura"),
  DISPEL_CLEANSE("Esuna Cleanse + Heal")
}

val JobClassPresets = listOf(
  "Freelancer",
  "Union Keybearer",
  "SOLDIER 1st Class",
  "Esper Sage",
  "Black Wizard",
  "Holy Paladin",
  "Dragoon",
  "Dark Knight",
  "Time Mage",
  "Archivist Keeper"
)

data class ActiveSetBonus(
  val setName: String,
  val piecesEquipped: Int,
  val description: String,
  val atkPercentBonus: Int = 0,
  val magPercentBonus: Int = 0,
  val defResPercentBonus: Int = 0,
  val critRateBonus: Float = 0f,
  val spCostReduction: Int = 0,
  val lifestealPercent: Int = 0,
  val counterChancePercent: Int = 0,
  val startingSbBonus: Int = 0
)

data class HeroComputedLoadout(
  val hero: HeroEntity,
  val weapon: EquipmentEntity?,
  val armor: EquipmentEntity?,
  val accessory: EquipmentEntity?,
  val hasRealmSynergy: Boolean,
  val effectiveMaxHp: Int,
  val effectiveAtk: Int,
  val effectiveDef: Int,
  val effectiveMag: Int,
  val effectiveRes: Int,
  val effectiveMind: Int,
  val effectiveSpd: Int,
  val critRateBonus: Float,
  val critDamageBonus: Float,
  val lifestealPercent: Int,
  val counterChancePercent: Int,
  val spCostReduction: Int,
  val dominantResistElement: ElementType,
  val dominantResistPercent: Int,
  val startingSbBonus: Int,
  val activeSetBonuses: List<ActiveSetBonus>,
  val activeSkills: List<SkillEntity>,
  val passiveSkills: List<SkillEntity>
)

/**
 * Shared multi-RPG stat, equipment set-bonus, Materia socket, and passive trait calculator
 * used consistently by both Party Armory previews and the 60fps Battle Engine.
 */
object RpgLoadoutCalculator {

  fun evaluateSetBonuses(
    weapon: EquipmentEntity?,
    armor: EquipmentEntity?,
    accessory: EquipmentEntity?
  ): List<ActiveSetBonus> {
    val equipped = listOfNotNull(weapon, armor, accessory)
      .filter { it.setSeriesName.isNotBlank() }
    val grouped = equipped.groupBy { it.setSeriesName }
    val bonuses = mutableListOf<ActiveSetBonus>()

    for ((setName, pieces) in grouped) {
      val count = pieces.size
      if (count < 2) continue
      val bonus = when {
        setName.contains("SOLDIER", ignoreCase = true) || setName.contains("Avalanche", ignoreCase = true) -> ActiveSetBonus(
          setName = setName,
          piecesEquipped = count,
          description = if (count >= 3) "3pc: +22% ATK, +15% Crit, +10% Counter" else "2pc: +14% ATK, +10% Crit",
          atkPercentBonus = if (count >= 3) 22 else 14,
          critRateBonus = if (count >= 3) 0.15f else 0.10f,
          counterChancePercent = if (count >= 3) 10 else 0
        )
        setName.contains("Daybreak", ignoreCase = true) || setName.contains("Union", ignoreCase = true) -> ActiveSetBonus(
          setName = setName,
          piecesEquipped = count,
          description = if (count >= 3) "3pc: -1 SP Cost, +15% ATK/MAG, +200 Start SB" else "2pc: -1 SP Cost, +10% ATK/MAG",
          atkPercentBonus = if (count >= 3) 15 else 10,
          magPercentBonus = if (count >= 3) 15 else 10,
          spCostReduction = 1,
          startingSbBonus = if (count >= 3) 200 else 0
        )
        setName.contains("Magitek", ignoreCase = true) || setName.contains("Esper", ignoreCase = true) || setName.contains("Alexandria", ignoreCase = true) -> ActiveSetBonus(
          setName = setName,
          piecesEquipped = count,
          description = if (count >= 3) "3pc: +22% MAG, +15% DEF/RES, +8% Lifesteal" else "2pc: +15% MAG, +10% DEF/RES",
          magPercentBonus = if (count >= 3) 22 else 15,
          defResPercentBonus = if (count >= 3) 15 else 10,
          lifestealPercent = if (count >= 3) 8 else 0
        )
        setName.contains("Keeper", ignoreCase = true) || setName.contains("Ivalice", ignoreCase = true) -> ActiveSetBonus(
          setName = setName,
          piecesEquipped = count,
          description = if (count >= 3) "3pc: +18% DEF/RES, +250 Start SB, +12% Counter" else "2pc: +12% DEF/RES, +150 Start SB",
          defResPercentBonus = if (count >= 3) 18 else 12,
          counterChancePercent = if (count >= 3) 12 else 0,
          startingSbBonus = if (count >= 3) 250 else 150
        )
        else -> ActiveSetBonus(
          setName = setName,
          piecesEquipped = count,
          description = if (count >= 3) "3pc: +15% All Primary Stats & +10% Crit" else "2pc: +10% All Primary Stats",
          atkPercentBonus = if (count >= 3) 15 else 10,
          magPercentBonus = if (count >= 3) 15 else 10,
          defResPercentBonus = if (count >= 3) 15 else 10,
          critRateBonus = if (count >= 3) 0.10f else 0f
        )
      }
      bonuses.add(bonus)
    }
    return bonuses
  }

  fun computeHeroLoadout(
    hero: HeroEntity,
    weapon: EquipmentEntity?,
    armor: EquipmentEntity?,
    accessory: EquipmentEntity?,
    allSkills: List<SkillEntity>,
    activeStageRealm: String,
    config: EngineConfigEntity = EngineConfigEntity()
  ): HeroComputedLoadout {
    val heroSynergy = hero.realm.equals(activeStageRealm, ignoreCase = true) || hero.realm == "Record Keep"
    val heroMult = if (heroSynergy) config.realmSynergyStatMultiplier else 1.0f

    val gearList = listOfNotNull(weapon, armor, accessory)
    var gearHp = 0
    var gearAtk = 0
    var gearDef = 0
    var gearMag = 0
    var gearRes = 0
    var gearSpd = 0
    var anyGearSynergy = false

    for (eq in gearList) {
      val syn = eq.originRealm.equals(activeStageRealm, ignoreCase = true) || eq.originRealm == "Record Keep"
      if (syn) anyGearSynergy = true
      val synMult = if (syn) 1.30f else 1.0f
      val reforgeMult = 1.0f + eq.reforgeTier * 0.10f
      val totalMult = synMult * reforgeMult

      gearHp += (eq.bonusHp * totalMult).roundToInt()
      gearAtk += (eq.bonusAtk * totalMult).roundToInt()
      gearDef += (eq.bonusDef * totalMult).roundToInt()
      gearMag += (eq.bonusMag * totalMult).roundToInt()
      gearRes += (eq.bonusRes * totalMult).roundToInt()
      gearSpd += (eq.bonusSpd * totalMult).roundToInt()
    }

    val setBonuses = evaluateSetBonuses(weapon, armor, accessory)

    // Gather equipped active skills + equipment-granted skills + materia-socketed skills
    val equippedActiveIds = hero.equippedSkillIdList()
    val equippedPassiveIds = hero.equippedPassiveIdList()
    val gearGrantedIds = gearList.mapNotNull { it.grantedSkillId }
    val socketedIds = gearList.flatMap { it.socketedSkillIdList() }

    val allCandidateIds = (equippedActiveIds + gearGrantedIds + socketedIds).distinct()
    val resolvedActiveSkills = allCandidateIds.mapNotNull { id ->
      allSkills.find { it.id == id && !it.categoryEnum.isPassive }
    }.take(5)

    val resolvedPassives = (equippedPassiveIds + gearGrantedIds + socketedIds).distinct()
      .mapNotNull { id -> allSkills.find { it.id == id && it.categoryEnum.isPassive } }

    // Materia socket synergy gives small flat resonance per socketed orb
    val socketCount = socketedIds.size

    val atkPct = setBonuses.sumOf { it.atkPercentBonus } +
      resolvedPassives.sumOf { it.passiveAtkPercent } +
      socketCount * 2
    val magPct = setBonuses.sumOf { it.magPercentBonus } +
      resolvedPassives.sumOf { it.passiveMagPercent } +
      socketCount * 2
    val defPct = setBonuses.sumOf { it.defResPercentBonus } +
      resolvedPassives.sumOf { it.passiveDefPercent }
    val spdFlat = resolvedPassives.sumOf { it.passiveSpdBonus } + socketCount * 2

    val baseHp = (hero.maxHp * heroMult).roundToInt() + gearHp
    val baseAtk = (hero.attack * heroMult).roundToInt() + gearAtk
    val baseDef = (hero.defense * heroMult).roundToInt() + gearDef
    val baseMag = (hero.magic * heroMult).roundToInt() + gearMag
    val baseRes = (hero.resistance * heroMult).roundToInt() + gearRes
    val baseSpd = (hero.speed * heroMult).roundToInt() + gearSpd + spdFlat

    val finalAtk = (baseAtk * (1f + atkPct / 100f)).roundToInt()
    val finalMag = (baseMag * (1f + magPct / 100f)).roundToInt()
    val finalDef = (baseDef * (1f + defPct / 100f)).roundToInt()
    val finalRes = (baseRes * (1f + defPct / 100f)).roundToInt()

    val critRate = (
      gearList.sumOf { it.critRateBonus.toDouble() }.toFloat() +
        setBonuses.sumOf { it.critRateBonus.toDouble() }.toFloat() +
        resolvedPassives.sumOf { it.passiveCritBonus.toDouble() }.toFloat()
      ).coerceIn(0f, 0.75f)
    val critDmg = gearList.sumOf { it.critDamageBonus.toDouble() }.toFloat().coerceIn(0f, 1.5f)
    val lifesteal = (
      gearList.sumOf { it.lifestealPercent } +
        setBonuses.sumOf { it.lifestealPercent } +
        resolvedPassives.sumOf { it.passiveLifestealPercent }
      ).coerceIn(0, 60)
    val counter = (
      gearList.sumOf { it.counterChancePercent } +
        setBonuses.sumOf { it.counterChancePercent } +
        resolvedPassives.sumOf { it.passiveCounterPercent }
      ).coerceIn(0, 80)
    val spRed = (
      gearList.sumOf { it.spCostReduction } +
        setBonuses.sumOf { it.spCostReduction }
      ).coerceIn(0, 3)

    val strongestResistGear = gearList
      .filter { it.elementResistPercent > 0 }
      .maxByOrNull { it.elementResistPercent }

    return HeroComputedLoadout(
      hero = hero,
      weapon = weapon,
      armor = armor,
      accessory = accessory,
      hasRealmSynergy = heroSynergy || anyGearSynergy,
      effectiveMaxHp = baseHp,
      effectiveAtk = finalAtk,
      effectiveDef = finalDef,
      effectiveMag = finalMag,
      effectiveRes = finalRes,
      effectiveMind = (hero.mind * heroMult).roundToInt(),
      effectiveSpd = baseSpd,
      critRateBonus = critRate,
      critDamageBonus = critDmg,
      lifestealPercent = lifesteal,
      counterChancePercent = counter,
      spCostReduction = spRed,
      dominantResistElement = strongestResistGear?.elementResistEnum ?: ElementType.NON_ELEMENTAL,
      dominantResistPercent = strongestResistGear?.elementResistPercent ?: 0,
      startingSbBonus = setBonuses.sumOf { it.startingSbBonus },
      activeSetBonuses = setBonuses,
      activeSkills = resolvedActiveSkills,
      passiveSkills = resolvedPassives
    )
  }
}
