package com.example.engine

import android.app.Application
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ChronicleNexusApplication
import com.example.assets.ExternalAssetImportService
import com.example.assets.Mesh3DParser
import com.example.audio.CombatAudioEngine
import com.example.data.ArmoryCatalogSnapshot
import com.example.data.ArmoryRepository
import com.example.data.RpgRepository
import com.example.di.AppModule
import com.example.model.ActiveChainState
import com.example.model.ArmorEntity
import com.example.model.ArmorWithSkills
import com.example.model.AssetBindTargetType
import com.example.model.BattleLogEntity
import com.example.model.BossArchetype
import com.example.model.BossBattleState
import com.example.model.CombatParticle
import com.example.model.DefaultDeckSlots
import com.example.model.ElementType
import com.example.model.EngineConfigEntity
import com.example.model.EquipmentEntity
import com.example.model.EquipmentSlotType
import com.example.model.ExternalSearchResult
import com.example.model.FloatingCombatText
import com.example.model.HeroBattleState
import com.example.model.HeroEntity
import com.example.model.ImportedAssetEntity
import com.example.model.ImportedAssetType
import com.example.model.MedalAlignment
import com.example.model.MedalAttribute
import com.example.model.MedalEntity
import com.example.model.ParsedMesh3D
import com.example.model.RealmMetadataEntity
import com.example.model.RealmStageEntity
import com.example.model.RpgLoadoutCalculator
import com.example.model.RpgSystemOrigin
import com.example.model.SfxCombatCue
import com.example.model.SkillCategory
import com.example.model.SkillCombatEffect
import com.example.model.SkillEntity
import com.example.model.SoulBreakBannerState
import com.example.model.SpecialEffectType
import com.example.model.WeaponArmorClass
import com.example.model.WeaponEntity
import com.example.model.WeaponWithSkills
import com.example.ui.theme.HpEmerald
import com.example.ui.theme.PowerCrimson
import com.example.ui.theme.RelicGold
import com.example.ui.theme.SoulBreakMagenta
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BattleOutcome {
  ONGOING,
  VICTORY,
  DEFEAT
}

data class BattleArenaState(
  val stage: RealmStageEntity? = null,
  val partyStates: List<HeroBattleState> = emptyList(),
  val bossState: BossBattleState? = null,
  val selectedHeroIndex: Int = 0,
  val currentSp: Int = 10,
  val maxSp: Int = 15,
  val activeChain: ActiveChainState? = null,
  val floatingTexts: List<FloatingCombatText> = emptyList(),
  val particles: List<CombatParticle> = emptyList(),
  val soulBreakBanner: SoulBreakBannerState? = null,
  val isPaused: Boolean = false,
  val isWaitMode: Boolean = true,
  val isAutoBattle: Boolean = false,
  val battleSpeedMultiplier: Int = 1, // 1 or 2
  val showTelemetryOverlay: Boolean = true,
  val outcome: BattleOutcome = BattleOutcome.ONGOING,
  val turnsTaken: Int = 0,
  val peakChainCount: Int = 0,
  val totalDamageDealt: Long = 0L,
  val exploitedWeakness: Boolean = false,
  val usedSoulBreak: Boolean = false,
  val starsEarned: Int = 0,
  val lastFormulaBreakdown: String = "Engine Ready: Waiting for ATB..."
)

@HiltViewModel
class RpgEngineViewModel @Inject constructor(
  private val repository: RpgRepository,
  private val armoryRepository: ArmoryRepository,
  private val assetImportService: ExternalAssetImportService?,
  private val audioEngine: CombatAudioEngine?
) : ViewModel() {
  constructor(repository: RpgRepository) : this(
    repository = repository,
    armoryRepository = repository.asArmoryRepository(),
    assetImportService = null,
    audioEngine = null
  )

  val heroesFlow: StateFlow<List<HeroEntity>>
  val equipmentFlow: StateFlow<List<EquipmentEntity>>
  val realmMetadataFlow: StateFlow<List<RealmMetadataEntity>>
  val medalsFlow: StateFlow<List<MedalEntity>>
  val stagesFlow: StateFlow<List<RealmStageEntity>>
  val engineConfigFlow: StateFlow<EngineConfigEntity>
  val battleLogsFlow: StateFlow<List<BattleLogEntity>>
  val importedAssetsFlow: StateFlow<List<ImportedAssetEntity>>
  val skillsFlow: StateFlow<List<SkillEntity>>
  val weaponsFlow: StateFlow<List<WeaponEntity>>
  val armorsFlow: StateFlow<List<ArmorEntity>>
  val weaponsWithSkillsFlow: StateFlow<List<WeaponWithSkills>>
  val armorsWithSkillsFlow: StateFlow<List<ArmorWithSkills>>
  val armoryCatalogFlow: StateFlow<ArmoryCatalogSnapshot>

  private val _battleState = MutableStateFlow(BattleArenaState())
  val battleState: StateFlow<BattleArenaState> = _battleState.asStateFlow()

  private val _lastSummonedMedal = MutableStateFlow<MedalEntity?>(null)
  val lastSummonedMedal: StateFlow<MedalEntity?> = _lastSummonedMedal.asStateFlow()

  private val _assetSearchResults = MutableStateFlow<List<ExternalSearchResult>>(emptyList())
  val assetSearchResults: StateFlow<List<ExternalSearchResult>> = _assetSearchResults.asStateFlow()

  private val _importerLoading = MutableStateFlow(false)
  val importerLoading: StateFlow<Boolean> = _importerLoading.asStateFlow()

  private val _importerStatusMessage = MutableStateFlow<String?>(null)
  val importerStatusMessage: StateFlow<String?> = _importerStatusMessage.asStateFlow()

  private val _inspectedMesh3D = MutableStateFlow<ParsedMesh3D?>(null)
  val inspectedMesh3D: StateFlow<ParsedMesh3D?> = _inspectedMesh3D.asStateFlow()

  val isAudioMutedFlow: StateFlow<Boolean> =
    audioEngine?.isMuted ?: MutableStateFlow(false)
  val activeBgmTitleFlow: StateFlow<String?> =
    audioEngine?.activeBgmTitle ?: MutableStateFlow(null)

  val openCc0Catalog: List<ExternalSearchResult>
    get() = assetImportService?.getOpenCc0Catalog().orEmpty()

  private var nextEntityId = 1000L

  companion object {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val app = this[APPLICATION_KEY]
        val repo: RpgRepository
        val armoryRepo: ArmoryRepository
        if (app is ChronicleNexusApplication) {
          repo = app.container.rpgRepository
          armoryRepo = app.container.armoryRepository
        } else if (app != null) {
          val db = AppModule.provideAppDatabase(app)
          repo = RpgRepository(db)
          armoryRepo = repo.asArmoryRepository()
        } else {
          throw IllegalStateException("Application context required to provide AppDatabase")
        }
        val importer = if (app != null) ExternalAssetImportService(app) else null
        val audio = if (app != null) CombatAudioEngine(app) else null
        RpgEngineViewModel(repo, armoryRepo, importer, audio)
      }
    }
  }

  init {
    heroesFlow = repository.heroes.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      emptyList()
    )
    equipmentFlow = repository.equipment.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      emptyList()
    )
    realmMetadataFlow = repository.realmMetadata.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      emptyList()
    )
    medalsFlow = repository.medals.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      emptyList()
    )
    stagesFlow = repository.stages.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      emptyList()
    )
    engineConfigFlow = repository.engineConfig.combine(MutableStateFlow(EngineConfigEntity())) { cfg, def ->
      cfg ?: def
    }.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      EngineConfigEntity()
    )
    battleLogsFlow = repository.battleLogs.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      emptyList()
    )
    importedAssetsFlow = repository.importedAssets.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      emptyList()
    )
    skillsFlow = armoryRepository.skills.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      emptyList()
    )
    weaponsFlow = armoryRepository.weapons.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      emptyList()
    )
    armorsFlow = armoryRepository.armors.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      emptyList()
    )
    weaponsWithSkillsFlow = armoryRepository.weaponsWithSkills.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      emptyList()
    )
    armorsWithSkillsFlow = armoryRepository.armorsWithSkills.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      emptyList()
    )
    armoryCatalogFlow = armoryRepository.observeArmoryCatalog().stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      ArmoryCatalogSnapshot()
    )

    viewModelScope.launch {
      repository.ensureSeeded()
    }

    // Preload imported SFX assets into SoundPool as they arrive
    viewModelScope.launch {
      importedAssetsFlow.collect { assets ->
        assets.filter { it.assetTypeEnum == ImportedAssetType.AUDIO_SFX }.forEach { sfx ->
          audioEngine?.preloadSfx(sfx)
        }
      }
    }

    // Automatically initialize battle when stages & heroes load
    viewModelScope.launch {
      combine(heroesFlow, stagesFlow, engineConfigFlow, equipmentFlow, skillsFlow) { heroes, stages, config, eq, skills ->
        EncounterInputs(heroes, stages, config, eq, skills)
      }.collect { (heroes, stages, config, eq, skills) ->
        if (_battleState.value.stage == null && heroes.isNotEmpty() && stages.isNotEmpty()) {
          startEncounter(stages.first(), heroes, config, eq, skills)
        }
      }
    }

    // Start 20Hz Engine Loop (50ms delta ticks)
    viewModelScope.launch {
      while (true) {
        delay(50L)
        tickEngine(0.05f)
      }
    }
  }

  private data class EncounterInputs(
    val heroes: List<HeroEntity>,
    val stages: List<RealmStageEntity>,
    val config: EngineConfigEntity,
    val equipment: List<EquipmentEntity>,
    val skills: List<SkillEntity>
  )

  fun startEncounter(
    stage: RealmStageEntity,
    allHeroes: List<HeroEntity> = heroesFlow.value,
    config: EngineConfigEntity = engineConfigFlow.value,
    allEquipment: List<EquipmentEntity> = equipmentFlow.value,
    allSkills: List<SkillEntity> = skillsFlow.value
  ) {
    val activeParty = allHeroes.filter { it.inParty }.take(4).ifEmpty { allHeroes.take(4) }
    val allAssets = importedAssetsFlow.value
    val partyBattleStates = activeParty.mapIndexed { idx, hero ->
      val weapon = allEquipment.find { it.id == hero.equippedWeaponId }
      val armor = allEquipment.find { it.id == hero.equippedArmorId }
      val accessory = allEquipment.find { it.id == hero.equippedAccessoryId }
      val loadout = RpgLoadoutCalculator.computeHeroLoadout(
        hero = hero,
        weapon = weapon,
        armor = armor,
        accessory = accessory,
        allSkills = allSkills,
        activeStageRealm = stage.realm,
        config = config
      )
      val boundSprite = hero.spriteAssetId?.let { sId -> allAssets.find { it.id == sId } }

      HeroBattleState(
        hero = hero,
        hasRealmSynergy = loadout.hasRealmSynergy,
        equippedWeaponName = weapon?.name ?: hero.archetypeEnum.weaponName,
        equippedArmorName = armor?.name ?: "Standard Mail",
        equippedAccessoryName = accessory?.name ?: "None",
        effectiveMaxHp = loadout.effectiveMaxHp,
        currentHp = loadout.effectiveMaxHp,
        effectiveAtk = loadout.effectiveAtk,
        effectiveDef = loadout.effectiveDef,
        effectiveMag = loadout.effectiveMag,
        effectiveRes = loadout.effectiveRes,
        effectiveSpd = loadout.effectiveSpd,
        atbPercent = (35f + idx * 18f).coerceAtMost(95f),
        soulBreakPoints = (500 + loadout.startingSbBonus).coerceAtMost(1500),
        critRateBonus = loadout.critRateBonus,
        critDamageBonus = loadout.critDamageBonus,
        lifestealPercent = loadout.lifestealPercent,
        counterChancePercent = loadout.counterChancePercent,
        spCostReduction = loadout.spCostReduction,
        elementResistElement = loadout.dominantResistElement,
        elementResistPercent = loadout.dominantResistPercent,
        activeSetBonuses = loadout.activeSetBonuses.map { "${it.setName} (${it.piecesEquipped}pc): ${it.description}" },
        availableSkills = loadout.activeSkills,
        spriteSheetPath = boundSprite?.localFilePath?.ifBlank { boundSprite.sourceUrl }.orEmpty(),
        spriteCols = boundSprite?.sheetColumns ?: 1,
        spriteRows = boundSprite?.sheetRows ?: 1,
        spriteFps = boundSprite?.sheetFps ?: 6
      )
    }

    val boundBossSprite = stage.bossSpriteAssetId?.let { bId -> allAssets.find { it.id == bId } }
    val matchingRealm = realmMetadataFlow.value.find { it.realmName.equals(stage.realm, ignoreCase = true) }
    val stageBgArt = stage.stageConceptArtUri.ifBlank { matchingRealm?.bannerArtUri.orEmpty() }
    val bgmAssetId = stage.stageBgmAssetId ?: matchingRealm?.bgmAssetId
    val bgmAsset = bgmAssetId?.let { id -> allAssets.find { it.id == id } }
    audioEngine?.playStageBgm(bgmAsset)

    val bossState = BossBattleState(
      stage = stage,
      maxHp = stage.bossMaxHp,
      currentHp = stage.bossMaxHp,
      atbPercent = 15f,
      actionCountdown = stage.bossCountdownTurns,
      bossSpritePath = boundBossSprite?.localFilePath?.ifBlank { boundBossSprite.sourceUrl }.orEmpty(),
      bossSpriteCols = boundBossSprite?.sheetColumns ?: 1,
      bossSpriteRows = boundBossSprite?.sheetRows ?: 1,
      bossSpriteFps = boundBossSprite?.sheetFps ?: 6,
      stageBgArtUri = stageBgArt
    )

    _battleState.update { prev ->
      prev.copy(
        stage = stage,
        partyStates = partyBattleStates,
        bossState = bossState,
        selectedHeroIndex = 0,
        currentSp = (config.maxSpGauges * 0.75f).roundToInt(),
        maxSp = config.maxSpGauges,
        activeChain = null,
        floatingTexts = emptyList(),
        particles = emptyList(),
        soulBreakBanner = null,
        isPaused = false,
        outcome = BattleOutcome.ONGOING,
        turnsTaken = 0,
        peakChainCount = 0,
        totalDamageDealt = 0L,
        exploitedWeakness = false,
        usedSoulBreak = false,
        starsEarned = 0,
        lastFormulaBreakdown = "Stage ${stage.stageCode}: ${stage.bossName} engaged! Weakness: ${stage.weaknessElementEnum.label}"
      )
    }
  }

  fun restartCurrentEncounter() {
    val currentStage = _battleState.value.stage ?: stagesFlow.value.firstOrNull() ?: return
    startEncounter(currentStage)
  }

  private fun tickEngine(rawDtSeconds: Float) {
    val state = _battleState.value
    if (state.isPaused || state.outcome != BattleOutcome.ONGOING) return
    val boss = state.bossState ?: return
    val config = engineConfigFlow.value

    val dt = rawDtSeconds * state.battleSpeedMultiplier

    val updatedTexts = state.floatingTexts.mapNotNull { ft ->
      val nextProg = ft.progress + rawDtSeconds * 0.95f
      if (nextProg >= 1f) null else ft.copy(progress = nextProg, yFraction = ft.yFraction - rawDtSeconds * 0.06f)
    }
    val updatedParticles = state.particles.mapNotNull { p ->
      val nextProg = p.progress + rawDtSeconds * 1.6f
      if (nextProg >= 1f) null else p.copy(
        progress = nextProg,
        xFraction = p.xFraction + p.vx * rawDtSeconds,
        yFraction = p.yFraction + p.vy * rawDtSeconds
      )
    }
    val updatedBanner = state.soulBreakBanner?.let { banner ->
      val nextProg = banner.progress + rawDtSeconds * 0.7f
      if (nextProg >= 1f) null else banner.copy(progress = nextProg)
    }

    val anyHeroReady = state.partyStates.any { it.isAtbReady }
    val pauseAtbForWait = state.isWaitMode && anyHeroReady && !state.isAutoBattle

    val updatedChain = state.activeChain?.let { chain ->
      val decayDt = if (pauseAtbForWait) dt * 0.25f else dt
      val rem = chain.timeRemainingSeconds - decayDt
      if (rem <= 0f) null else chain.copy(timeRemainingSeconds = rem)
    }

    val updatedParty = state.partyStates.map { h ->
      val nextAnim = (h.attackAnimProgress - rawDtSeconds * 2.5f).coerceAtLeast(0f)
      if (!h.isAlive) {
        h.copy(atbPercent = 0f, attackAnimProgress = 0f)
      } else {
        val hasteRem = (h.hasteSecondsRemaining - dt).coerceAtLeast(0f)
        val shieldWallRem = (h.shieldWallSecondsRemaining - dt).coerceAtLeast(0f)
        val hasteMult = if (hasteRem > 0f) 1.45f else 1.0f
        val fillDelta = if (pauseAtbForWait && h.atbPercent < 100f) {
          0f
        } else {
          config.atbBaseFillRate * (h.effectiveSpd / 150f) * hasteMult * dt
        }
        h.copy(
          atbPercent = (h.atbPercent + fillDelta).coerceAtMost(100f),
          hasteSecondsRemaining = hasteRem,
          shieldWallSecondsRemaining = shieldWallRem,
          attackAnimProgress = nextAnim
        )
      }
    }

    val currentSel = state.selectedHeroIndex.coerceIn(0, (updatedParty.size - 1).coerceAtLeast(0))
    val nextSelectedHeroIndex = if (updatedParty.getOrNull(currentSel)?.isAtbReady == true) {
      currentSel
    } else {
      val readyIdx = updatedParty.indexOfFirst { it.isAtbReady }
      if (readyIdx >= 0) readyIdx else currentSel
    }

    val nextBossHitFlash = (boss.hitFlashProgress - rawDtSeconds * 3.0f).coerceAtLeast(0f)
    val nextBossAttackAnim = (boss.bossAttackAnimProgress - rawDtSeconds * 2.2f).coerceAtLeast(0f)
    val bossFillDelta = if (pauseAtbForWait) 0f else (config.atbBaseFillRate * 0.72f * dt)
    val nextBossAtb = boss.atbPercent + bossFillDelta

    val updatedBoss = boss.copy(
      atbPercent = nextBossAtb.coerceAtMost(100f),
      atkBreakSeconds = (boss.atkBreakSeconds - dt).coerceAtLeast(0f),
      defBreakSeconds = (boss.defBreakSeconds - dt).coerceAtLeast(0f),
      hitFlashProgress = nextBossHitFlash,
      bossAttackAnimProgress = nextBossAttackAnim
    )

    _battleState.update {
      it.copy(
        partyStates = updatedParty,
        bossState = updatedBoss,
        selectedHeroIndex = nextSelectedHeroIndex,
        activeChain = updatedChain,
        floatingTexts = updatedTexts,
        particles = updatedParticles,
        soulBreakBanner = updatedBanner
      )
    }

    if (updatedBoss.atbPercent >= 100f && updatedBoss.currentHp > 0) {
      executeBossTurn()
      return
    }

    if (state.isAutoBattle) {
      val readyHeroIdx = updatedParty.indexOfFirst { it.isAtbReady }
      if (readyHeroIdx >= 0) {
        val heroState = updatedParty[readyHeroIdx]
        val readySkill = heroState.availableSkills.firstOrNull { skill ->
          val cd = heroState.skillCooldowns[skill.id] ?: 0
          val cost = (skill.spCost - heroState.spCostReduction).coerceAtLeast(0)
          cd == 0 && _battleState.value.currentSp >= cost &&
            skill.effectTypeEnum != SkillCombatEffect.HEAL_PARTY &&
            skill.effectTypeEnum != SkillCombatEffect.DISPEL_CLEANSE
        }
        if (heroState.soulBreakBars >= 1) {
          executeSoulBreak(readyHeroIdx)
        } else if (readySkill != null && Random.nextFloat() < 0.45f) {
          executeHeroSkill(readySkill, readyHeroIdx)
        } else {
          val medal = getEquippedMedalForHero(heroState.hero, heroState.activeWheelIndex)
          val effectiveCost = medal?.let { (it.spCost - heroState.spCostReduction).coerceAtLeast(0) } ?: 0
          if (medal != null && _battleState.value.currentSp >= effectiveCost) {
            executeMedalBurst(readyHeroIdx)
          } else {
            executeBasicAttack(readyHeroIdx)
          }
        }
      }
    }
  }

  fun selectHero(index: Int) {
    _battleState.update { it.copy(selectedHeroIndex = index) }
  }

  fun rotateMedalWheel(heroIndex: Int = _battleState.value.selectedHeroIndex) {
    _battleState.update { state ->
      val updated = state.partyStates.mapIndexed { idx, h ->
        if (idx == heroIndex) h.copy(activeWheelIndex = (h.activeWheelIndex + 1) % 5) else h
      }
      state.copy(partyStates = updated)
    }
  }

  fun getEquippedMedalForHero(hero: HeroEntity, slotIndex: Int): MedalEntity? {
    val allMedals = medalsFlow.value
    if (allMedals.isEmpty()) return null
    val ids = hero.equippedMedalIdList()
    val targetId = ids.getOrNull(slotIndex % ids.size.coerceAtLeast(1))
    return allMedals.find { it.id == targetId } ?: allMedals.first()
  }

  fun executeBasicAttack(heroIndex: Int = _battleState.value.selectedHeroIndex) {
    val state = _battleState.value
    val heroState = state.partyStates.getOrNull(heroIndex) ?: return
    if (!heroState.isAtbReady || state.outcome != BattleOutcome.ONGOING) return
    val medal = getEquippedMedalForHero(heroState.hero, heroState.activeWheelIndex) ?: return

    performHeroOffensiveAction(
      heroIndex = heroIndex,
      medal = medal,
      spCost = 0,
      spGain = 2,
      sbPointsGain = 140,
      skillMultiplier = 1.35f,
      hitCount = 2,
      actionName = "Strike (${medal.attributeEnum.shortCode})",
      applySpecialEffect = false,
      isSoulBreak = false
    )
  }

  fun executeMedalBurst(heroIndex: Int = _battleState.value.selectedHeroIndex) {
    val state = _battleState.value
    val heroState = state.partyStates.getOrNull(heroIndex) ?: return
    if (!heroState.isAtbReady || state.outcome != BattleOutcome.ONGOING) return
    val medal = getEquippedMedalForHero(heroState.hero, heroState.activeWheelIndex) ?: return
    val effectiveCost = (medal.spCost - heroState.spCostReduction).coerceAtLeast(0)
    if (state.currentSp < effectiveCost) {
      executeBasicAttack(heroIndex)
      return
    }

    val guiltMult = 1.0f + (medal.guiltBonusPercent / 100f)
    performHeroOffensiveAction(
      heroIndex = heroIndex,
      medal = medal,
      spCost = effectiveCost,
      spGain = 0,
      sbPointsGain = 220,
      skillMultiplier = medal.baseMultiplier * guiltMult,
      hitCount = medal.hitCount,
      actionName = medal.name,
      applySpecialEffect = true,
      isSoulBreak = false
    )
  }

  fun executeHeroSkill(
    skill: SkillEntity,
    heroIndex: Int = _battleState.value.selectedHeroIndex
  ) {
    val state = _battleState.value
    val heroState = state.partyStates.getOrNull(heroIndex) ?: return
    if (!heroState.isAtbReady || state.outcome != BattleOutcome.ONGOING) return
    val cooldown = heroState.skillCooldowns[skill.id] ?: 0
    if (cooldown > 0) return

    val effectiveSpCost = (skill.spCost - heroState.spCostReduction).coerceAtLeast(0)
    if (state.currentSp < effectiveSpCost) return

    val baseMedal = getEquippedMedalForHero(heroState.hero, heroState.activeWheelIndex)
      ?: MedalEntity(
        name = skill.name,
        attribute = skill.attribute,
        alignment = MedalAlignment.UPRIGHT.name,
        element = skill.element
      )

    val skillProxyMedal = baseMedal.copy(
      name = skill.name,
      rarityStars = (5 + (skill.rankLevel / 2)).coerceIn(6, 7),
      attribute = skill.attribute,
      element = skill.element,
      hitCount = skill.hitCount
    )

    val effect = skill.effectTypeEnum
    performHeroOffensiveAction(
      heroIndex = heroIndex,
      medal = skillProxyMedal,
      spCost = effectiveSpCost,
      spGain = if (effectiveSpCost == 0) 1 else 0,
      sbPointsGain = 240,
      skillMultiplier = skill.effectiveMultiplier,
      hitCount = skill.hitCount,
      actionName = "${skill.name} (R${skill.rankLevel})",
      applySpecialEffect = false,
      isSoulBreak = false,
      skillIdUsed = skill.id,
      cooldownForSkill = skill.cooldownTurns,
      skillEffectOverride = effect
    )
  }

  fun executeDefend(heroIndex: Int = _battleState.value.selectedHeroIndex) {
    val state = _battleState.value
    val heroState = state.partyStates.getOrNull(heroIndex) ?: return
    if (!heroState.isAtbReady || state.outcome != BattleOutcome.ONGOING) return

    val healAmount = (heroState.effectiveMaxHp * 0.12f).roundToInt()
    val updatedParty = state.partyStates.mapIndexed { idx, h ->
      if (idx == heroIndex) {
        h.copy(
          atbPercent = 0f,
          isDefending = true,
          currentHp = (h.currentHp + healAmount).coerceAtMost(h.effectiveMaxHp),
          soulBreakPoints = (h.soulBreakPoints + 100).coerceAtMost(1500),
          skillCooldowns = decrementCooldowns(h.skillCooldowns),
          lastActionLabel = "Guard +SP"
        )
      } else h
    }

    val heroY = 0.28f + heroIndex * 0.15f
    val healText = FloatingCombatText(
      id = ++nextEntityId,
      text = "+$healAmount HP",
      subText = "GUARD & +2 SP",
      xFraction = 0.76f,
      yFraction = heroY,
      color = HpEmerald
    )

    _battleState.update {
      it.copy(
        partyStates = updatedParty,
        currentSp = (it.currentSp + 2).coerceAtMost(it.maxSp),
        floatingTexts = it.floatingTexts + healText,
        turnsTaken = it.turnsTaken + 1,
        lastFormulaBreakdown = "${heroState.hero.name} enters Sentinel Guard (-50% DMG taken, +$healAmount HP, +2 SP)"
      )
    }
  }

  fun executeSoulBreak(heroIndex: Int = _battleState.value.selectedHeroIndex) {
    val state = _battleState.value
    val heroState = state.partyStates.getOrNull(heroIndex) ?: return
    if (!heroState.isAlive || heroState.soulBreakBars < 1 || state.outcome != BattleOutcome.ONGOING) return
    val medal = getEquippedMedalForHero(heroState.hero, heroState.activeWheelIndex) ?: return

    val sbMedal = medal.copy(
      name = heroState.hero.soulBreakName,
      element = heroState.hero.soulBreakElement,
      effectType = heroState.hero.soulBreakEffect,
      hitCount = 10
    )

    performHeroOffensiveAction(
      heroIndex = heroIndex,
      medal = sbMedal,
      spCost = 0,
      spGain = 3,
      sbPointsGain = -500,
      skillMultiplier = heroState.hero.soulBreakMultiplier * 2.2f,
      hitCount = 10,
      actionName = heroState.hero.soulBreakName,
      applySpecialEffect = true,
      isSoulBreak = true
    )
  }

  private fun decrementCooldowns(
    current: Map<Int, Int>,
    triggeredSkillId: Int? = null,
    triggeredCooldown: Int = 0
  ): Map<Int, Int> {
    val next = current.mapValues { (_, cd) -> (cd - 1).coerceAtLeast(0) }
      .filterValues { it > 0 }
      .toMutableMap()
    if (triggeredSkillId != null && triggeredCooldown > 0) {
      next[triggeredSkillId] = triggeredCooldown
    }
    return next
  }

  private fun performHeroOffensiveAction(
    heroIndex: Int,
    medal: MedalEntity,
    spCost: Int,
    spGain: Int,
    sbPointsGain: Int,
    skillMultiplier: Float,
    hitCount: Int,
    actionName: String,
    applySpecialEffect: Boolean,
    isSoulBreak: Boolean,
    skillIdUsed: Int? = null,
    cooldownForSkill: Int = 0,
    skillEffectOverride: SkillCombatEffect? = null
  ) {
    val state = _battleState.value
    val boss = state.bossState ?: return
    val heroState = state.partyStates.getOrNull(heroIndex) ?: return
    val config = engineConfigFlow.value

    val slotCfg = DefaultDeckSlots[heroState.activeWheelIndex % DefaultDeckSlots.size]
    val isSlotMatched = medal.attributeEnum == slotCfg.requiredAttribute &&
      medal.alignmentEnum == slotCfg.requiredAlignment
    val slotMult = if (isSlotMatched) slotCfg.matchedSlotMultiplier else slotCfg.baseSlotMultiplier

    val baseOffense = if (medal.attributeEnum == MedalAttribute.MAGIC) {
      heroState.effectiveMag
    } else {
      heroState.effectiveAtk
    }
    val buffedOffense = baseOffense * (1.0f + heroState.atkBuffPercent / 100f)

    val hasAttrAdvantage = medal.attributeEnum.hasAdvantageOver(boss.stage.bossAttributeEnum)
    val attrMult = if (hasAttrAdvantage) config.attributeAdvantageMultiplier else 1.0f

    val element = medal.elementEnum
    val isWeakness = element == boss.stage.weaknessElementEnum
    val elemMult = if (isWeakness) config.weaknessMultiplier else 1.0f

    val currentChain = state.activeChain
    val chainBonusHits = when {
      skillEffectOverride == SkillCombatEffect.CHAIN_BUILD -> 4
      applySpecialEffect && medal.effectTypeEnum == SpecialEffectType.CHAIN_IGNITER -> 4
      else -> 0
    }
    val updatedChain = if (currentChain == null || currentChain.element != element) {
      ActiveChainState(
        element = element,
        hitCount = hitCount + chainBonusHits,
        timeRemainingSeconds = config.chainDurationSeconds,
        maxSeconds = config.chainDurationSeconds
      )
    } else {
      currentChain.copy(
        hitCount = currentChain.hitCount + hitCount + chainBonusHits,
        timeRemainingSeconds = config.chainDurationSeconds
      )
    }
    val chainMult = updatedChain.bonusMultiplier

    val effectiveBossDef = (boss.stage.bossDefense * (if (boss.isDefBroken) 0.55f else 1.0f)).coerceAtLeast(100f)

    val totalCritChance = (config.criticalHitRate + heroState.critRateBonus).coerceIn(0f, 0.90f)
    val isCrit = isSoulBreak || Random.nextFloat() < totalCritChance
    val critMult = if (isCrit) (config.criticalDamageMultiplier + heroState.critDamageBonus) else 1.0f

    val rawTotal = (buffedOffense * 26f / (effectiveBossDef / 160f)) *
      skillMultiplier * slotMult * attrMult * elemMult * chainMult * critMult
    val capPerHit = if (isSoulBreak || medal.rarityStars >= 7) {
      config.damageCapBreakLimit
    } else {
      config.damageCapStandard
    }
    val totalCap = capPerHit * hitCount.coerceAtMost(4)
    val finalDamage = rawTotal.roundToInt().coerceIn(450, totalCap)

    var newBossAtkBreak = boss.atkBreakSeconds
    var newBossDefBreak = boss.defBreakSeconds
    var partyHealFraction = 0f
    var grantPartyHaste = false
    var grantPartyShieldWall = false
    var grantCleanseGuard = false
    var extraLifestealPct = 0
    var effectSubLabel = ""

    if (skillEffectOverride != null) {
      when (skillEffectOverride) {
        SkillCombatEffect.ATK_BREAK -> {
          newBossAtkBreak = 18f
          effectSubLabel = "ATK BREAK -40%!"
        }
        SkillCombatEffect.DEF_BREAK -> {
          newBossDefBreak = 18f
          effectSubLabel = "DEF BREAK -50%!"
        }
        SkillCombatEffect.HASTE_ATK_UP -> {
          grantPartyHaste = true
          effectSubLabel = "HASTEGA +30% ATK!"
        }
        SkillCombatEffect.HEAL_PARTY -> {
          partyHealFraction = 0.42f
          effectSubLabel = "CURA MEDICA +42% HP!"
        }
        SkillCombatEffect.SHIELD_WALL -> {
          grantPartyShieldWall = true
          effectSubLabel = "AEGIS WALL -35% DMG!"
        }
        SkillCombatEffect.LIFESTEAL -> {
          extraLifestealPct = 35
          effectSubLabel = "DRAIN +35% LIFESTEAL!"
        }
        SkillCombatEffect.CHAIN_BUILD -> {
          effectSubLabel = "CHAIN SURGE +4 HITS!"
        }
        SkillCombatEffect.DISPEL_CLEANSE -> {
          partyHealFraction = 0.25f
          grantCleanseGuard = true
          effectSubLabel = "ESUNA CLEANSE + GUARD!"
        }
        SkillCombatEffect.DAMAGE, SkillCombatEffect.PASSIVE_STAT_BOOST -> {
          if (heroState.lifestealPercent > 0) {
            effectSubLabel = "LIFESTEAL ${heroState.lifestealPercent}%!"
          }
        }
      }
    } else if (applySpecialEffect) {
      when (medal.effectTypeEnum) {
        SpecialEffectType.ATK_BREAKDOWN -> {
          newBossAtkBreak = 18f
          effectSubLabel = "ATK BREAKDOWN -40%!"
        }
        SpecialEffectType.DEF_BREAKDOWN -> {
          newBossDefBreak = 18f
          effectSubLabel = "DEF BREAKDOWN -50%!"
        }
        SpecialEffectType.PARTY_HASTEGA -> {
          grantPartyHaste = true
          effectSubLabel = "HASTEGA + ATK UP!"
        }
        SpecialEffectType.PARTY_CURAGA -> {
          partyHealFraction = 0.40f
          effectSubLabel = "MEDICA CURAGA +40% HP!"
        }
        SpecialEffectType.CHAIN_IGNITER -> {
          effectSubLabel = "CHAIN IGNITER +4 HITS!"
        }
        SpecialEffectType.PURE_DAMAGE -> {
          effectSubLabel = if (isSlotMatched) "SLOT RESONANCE ${slotMult}x!" else ""
        }
      }
    }

    val totalLifestealPct = (heroState.lifestealPercent + extraLifestealPct).coerceIn(0, 75)
    val selfDrainHeal = if (totalLifestealPct > 0) {
      (finalDamage * (totalLifestealPct / 100f)).roundToInt().coerceAtLeast(80)
    } else 0

    val newBossHp = (boss.currentHp - finalDamage).coerceAtLeast(0)
    val nextCountdown = (boss.actionCountdown - 1).coerceAtLeast(0)

    val updatedParty = state.partyStates.mapIndexed { idx, h ->
      val partyHeal = if (partyHealFraction > 0f && h.isAlive) {
        (h.effectiveMaxHp * partyHealFraction).roundToInt()
      } else 0
      val selfHeal = if (idx == heroIndex && h.isAlive) selfDrainHeal else 0
      val healedHp = (h.currentHp + partyHeal + selfHeal).coerceAtMost(h.effectiveMaxHp)

      val hasteSec = if (grantPartyHaste && h.isAlive) 20f else h.hasteSecondsRemaining
      val wallSec = if (grantPartyShieldWall && h.isAlive) 18f else h.shieldWallSecondsRemaining
      val atkBuff = if (grantPartyHaste && h.isAlive) 30 else h.atkBuffPercent

      if (idx == heroIndex) {
        h.copy(
          atbPercent = 0f,
          currentHp = healedHp,
          soulBreakPoints = (h.soulBreakPoints + sbPointsGain).coerceIn(0, 1500),
          activeWheelIndex = if (skillIdUsed == null) (h.activeWheelIndex + 1) % 5 else h.activeWheelIndex,
          isDefending = grantCleanseGuard,
          hasteSecondsRemaining = hasteSec,
          shieldWallSecondsRemaining = wallSec,
          atkBuffPercent = atkBuff,
          skillCooldowns = decrementCooldowns(h.skillCooldowns, skillIdUsed, cooldownForSkill),
          attackAnimProgress = 1.0f,
          lastActionLabel = actionName
        )
      } else {
        h.copy(
          currentHp = healedHp,
          hasteSecondsRemaining = hasteSec,
          shieldWallSecondsRemaining = wallSec,
          atkBuffPercent = atkBuff
        )
      }
    }

    val damageBadges = buildList {
      if (isWeakness) add("WEAKNESS!")
      if (hasAttrAdvantage) add("${medal.attributeEnum.shortCode} ADV!")
      if (isCrit) add("CRITICAL!")
      if (selfDrainHeal > 0) add("+$selfDrainHeal DRAIN")
      if (effectSubLabel.isNotEmpty()) add(effectSubLabel)
    }.joinToString(" • ")

    val mainCombatText = FloatingCombatText(
      id = ++nextEntityId,
      text = "%,d".format(finalDamage) + " (${hitCount}x)",
      subText = damageBadges,
      xFraction = 0.24f + Random.nextFloat() * 0.08f,
      yFraction = 0.34f + Random.nextFloat() * 0.10f,
      color = when {
        isSoulBreak -> SoulBreakMagenta
        isWeakness -> element.color
        isCrit -> RelicGold
        else -> Color.White
      },
      isCritical = isCrit || isSoulBreak
    )

    val burstParticles = (0 until 14).map { i ->
      val angle = (i / 14f) * (Math.PI * 2.0) + Random.nextFloat() * 0.3
      val speed = 0.14f + Random.nextFloat() * 0.22f
      CombatParticle(
        id = ++nextEntityId,
        xFraction = 0.26f,
        yFraction = 0.44f,
        vx = (cos(angle) * speed).toFloat(),
        vy = (sin(angle) * speed).toFloat(),
        color = element.color,
        radiusDp = if (isSoulBreak) 7f else 4.5f
      )
    }

    val formulaSummary = buildString {
      append("${heroState.hero.name} → $actionName: ")
      append("%,d DMG ".format(finalDamage))
      append("[Skill ${"%.2f".format(skillMultiplier)}x")
      append(" × Slot ${"%.2f".format(slotMult)}x")
      append(" × Chain ${"%.2f".format(chainMult)}x")
      if (isWeakness) append(" × Weak ${"%.2f".format(elemMult)}x")
      if (isCrit) append(" × Crit ${"%.2f".format(critMult)}x")
      append("]")
    }

    val peakChain = maxOf(state.peakChainCount, updatedChain.hitCount)
    val totalDmg = state.totalDamageDealt + finalDamage
    val turns = state.turnsTaken + 1
    val expWeak = state.exploitedWeakness || isWeakness
    val usedSb = state.usedSoulBreak || isSoulBreak

    val updatedBoss = boss.copy(
      currentHp = newBossHp,
      actionCountdown = if (nextCountdown == 0) boss.stage.bossCountdownTurns else nextCountdown,
      atbPercent = if (nextCountdown == 0) 100f else boss.atbPercent,
      atkBreakSeconds = newBossAtkBreak,
      defBreakSeconds = newBossDefBreak,
      hitFlashProgress = 1.0f
    )

    val sbBanner = if (isSoulBreak) {
      SoulBreakBannerState(
        heroName = heroState.hero.name,
        soulBreakName = heroState.hero.soulBreakName,
        element = element
      )
    } else state.soulBreakBanner

    val targetCue = when {
      isSoulBreak -> SfxCombatCue.SOUL_BREAK
      applySpecialEffect -> SfxCombatCue.MEDAL_BURST
      else -> SfxCombatCue.ATTACK
    }
    triggerCombatAudioCue(heroState.hero.attackSfxAssetId, targetCue)

    if (newBossHp <= 0) {
      var stars = 1
      if (turns <= 15) stars++
      if (expWeak || peakChain >= 10 || usedSb) stars++

      _battleState.update {
        it.copy(
          partyStates = updatedParty,
          bossState = updatedBoss,
          currentSp = (it.currentSp - spCost + spGain).coerceIn(0, it.maxSp),
          activeChain = updatedChain,
          floatingTexts = it.floatingTexts + mainCombatText,
          particles = it.particles + burstParticles,
          soulBreakBanner = sbBanner,
          outcome = BattleOutcome.VICTORY,
          turnsTaken = turns,
          peakChainCount = peakChain,
          totalDamageDealt = totalDmg,
          exploitedWeakness = expWeak,
          usedSoulBreak = usedSb,
          starsEarned = stars,
          lastFormulaBreakdown = "VICTORY! $formulaSummary"
        )
      }
      recordBattleOutcome(
        stage = boss.stage,
        outcome = "VICTORY",
        turns = turns,
        peakChain = peakChain,
        totalDamage = totalDmg,
        stars = stars
      )
    } else {
      _battleState.update {
        it.copy(
          partyStates = updatedParty,
          bossState = updatedBoss,
          currentSp = (it.currentSp - spCost + spGain).coerceIn(0, it.maxSp),
          activeChain = updatedChain,
          floatingTexts = it.floatingTexts + mainCombatText,
          particles = it.particles + burstParticles,
          soulBreakBanner = sbBanner,
          turnsTaken = turns,
          peakChainCount = peakChain,
          totalDamageDealt = totalDmg,
          exploitedWeakness = expWeak,
          usedSoulBreak = usedSb,
          lastFormulaBreakdown = formulaSummary
        )
      }

      if (nextCountdown == 0) {
        executeBossTurn()
      }
    }
  }

  private fun executeBossTurn() {
    val state = _battleState.value
    val boss = state.bossState ?: return
    if (boss.currentHp <= 0 || state.outcome != BattleOutcome.ONGOING) return

    val atkMult = if (boss.isAtkBroken) 0.60f else 1.0f
    val effectiveBossAtk = boss.stage.bossAttack * atkMult
    val realmDominantElement = realmMetadataFlow.value
      .find { it.realmName.equals(boss.stage.realm, ignoreCase = true) }
      ?.dominantElementEnum ?: boss.stage.weaknessElementEnum

    val newFloatingTexts = mutableListOf<FloatingCombatText>()
    var counterDamageTotal = 0

    val updatedParty = state.partyStates.mapIndexed { idx, h ->
      if (!h.isAlive) {
        h
      } else {
        val guardMult = if (h.isDefending) 0.50f else 1.0f
        val shieldWallMult = if (h.shieldWallSecondsRemaining > 0f) 0.65f else 1.0f
        val hasElemResist = h.elementResistPercent > 0 && (
          h.elementResistElement == realmDominantElement ||
            h.elementResistElement == boss.stage.weaknessElementEnum ||
            h.elementResistElement == ElementType.NON_ELEMENTAL
          )
        val resistMult = if (hasElemResist) (1f - (h.elementResistPercent.coerceIn(0, 80) / 100f)) else 1.0f

        val rawDmg = (effectiveBossAtk * 3.1f * (240f / h.effectiveDef.coerceAtLeast(150)) *
          guardMult * shieldWallMult * resistMult)
        val dmg = (rawDmg * (0.9f + Random.nextFloat() * 0.2f)).roundToInt().coerceAtLeast(120)
        val nextHp = (h.currentHp - dmg).coerceAtLeast(0)

        val tags = buildList {
          if (h.isDefending) add("GUARD")
          if (h.shieldWallSecondsRemaining > 0f) add("WALL")
          if (hasElemResist) add("RES -${h.elementResistPercent}%")
        }.joinToString(" • ")

        newFloatingTexts.add(
          FloatingCombatText(
            id = ++nextEntityId,
            text = "-$dmg",
            subText = tags,
            xFraction = 0.74f,
            yFraction = 0.26f + idx * 0.15f,
            color = PowerCrimson
          )
        )

        if (nextHp > 0 && h.counterChancePercent > 0 && Random.nextInt(100) < h.counterChancePercent) {
          val counterDmg = (h.effectiveAtk * 1.45f).roundToInt().coerceAtLeast(350)
          counterDamageTotal += counterDmg
          newFloatingTexts.add(
            FloatingCombatText(
              id = ++nextEntityId,
              text = "COUNTER %,d!".format(counterDmg),
              subText = "${h.hero.name} Strike-Back",
              xFraction = 0.28f + idx * 0.04f,
              yFraction = 0.36f + idx * 0.05f,
              color = RelicGold,
              isCritical = true
            )
          )
        }

        h.copy(
          currentHp = nextHp,
          soulBreakPoints = (h.soulBreakPoints + 90).coerceAtMost(1500)
        )
      }
    }

    val allDefeated = updatedParty.none { it.isAlive }
    val bossHpAfterCounter = (boss.currentHp - counterDamageTotal).coerceAtLeast(0)
    val nextBoss = boss.copy(
      currentHp = bossHpAfterCounter,
      atbPercent = 0f,
      actionCountdown = boss.stage.bossCountdownTurns,
      bossAttackAnimProgress = 1.0f,
      hitFlashProgress = if (counterDamageTotal > 0) 1.0f else boss.hitFlashProgress
    )

    triggerCombatAudioCue(null, SfxCombatCue.BOSS_HIT)

    val nextOutcome = when {
      bossHpAfterCounter <= 0 && !allDefeated -> BattleOutcome.VICTORY
      allDefeated -> BattleOutcome.DEFEAT
      else -> BattleOutcome.ONGOING
    }
    val nextTotalDmg = state.totalDamageDealt + counterDamageTotal

    _battleState.update {
      it.copy(
        partyStates = updatedParty,
        bossState = nextBoss,
        floatingTexts = it.floatingTexts + newFloatingTexts,
        outcome = nextOutcome,
        totalDamageDealt = nextTotalDmg,
        starsEarned = if (nextOutcome == BattleOutcome.VICTORY) 3 else it.starsEarned,
        lastFormulaBreakdown = "${boss.stage.bossName} unleashes ${boss.stage.signatureAttackName}!" +
          (if (boss.isAtkBroken) " (ATK Break -40%)" else "") +
          (if (counterDamageTotal > 0) " • Party Countered for %,d DMG!".format(counterDamageTotal) else "")
      )
    }

    if (nextOutcome != BattleOutcome.ONGOING) {
      recordBattleOutcome(
        stage = boss.stage,
        outcome = if (nextOutcome == BattleOutcome.VICTORY) "VICTORY" else "DEFEATED",
        turns = state.turnsTaken,
        peakChain = state.peakChainCount,
        totalDamage = nextTotalDmg,
        stars = if (nextOutcome == BattleOutcome.VICTORY) 3 else 0
      )
    }
  }

  private fun recordBattleOutcome(
    stage: RealmStageEntity,
    outcome: String,
    turns: Int,
    peakChain: Int,
    totalDamage: Long,
    stars: Int
  ) {
    viewModelScope.launch {
      repository.insertBattleLog(
        BattleLogEntity(
          stageTitle = stage.title,
          bossName = stage.bossName,
          outcome = outcome,
          turnsTaken = turns,
          peakChainCount = peakChain,
          totalDamageDealt = totalDamage,
          starsEarned = stars
        )
      )
      if (outcome == "VICTORY") {
        repository.updateStage(
          stage.copy(
            bestStars = maxOf(stage.bestStars, stars),
            clearCount = stage.clearCount + 1
          )
        )
        // Update World/Realm Metadata Progression in Room
        val matchingRealm = realmMetadataFlow.value.find {
          it.realmName.equals(stage.realm, ignoreCase = true)
        }
        if (matchingRealm != null) {
          repository.updateRealmMetadata(
            matchingRealm.copy(
              clearedStagesCount = (matchingRealm.clearedStagesCount + 1).coerceAtMost(matchingRealm.totalStagesCount.coerceAtLeast(1)),
              masteryPercent = (matchingRealm.masteryPercent + 20).coerceAtMost(100),
              staminaShardsCollected = (matchingRealm.staminaShardsCollected + 1).coerceAtMost(5),
              realmResonanceLevel = (matchingRealm.realmResonanceLevel + 1).coerceAtMost(10)
            )
          )
        }
        // Grant Experience Points to Active Party Heroes in Room
        heroesFlow.value.filter { it.inParty }.forEach { hero ->
          repository.updateHero(
            hero.copy(experiencePoints = hero.experiencePoints + 15000L)
          )
        }
        val cfg = engineConfigFlow.value
        repository.saveEngineConfig(cfg.copy(mythrilShards = cfg.mythrilShards + 25))
      }
    }
  }

  // Engine HUD Toggles
  fun toggleWaitMode() {
    _battleState.update { it.copy(isWaitMode = !it.isWaitMode) }
  }

  fun toggleAutoBattle() {
    _battleState.update { it.copy(isAutoBattle = !it.isAutoBattle) }
  }

  fun toggleBattleSpeed() {
    _battleState.update { it.copy(battleSpeedMultiplier = if (it.battleSpeedMultiplier == 1) 2 else 1) }
  }

  // Party, Character Stats & Relic Equipment Management
  fun toggleHeroInParty(hero: HeroEntity) {
    viewModelScope.launch {
      val currentPartyCount = heroesFlow.value.count { it.inParty }
      if (hero.inParty && currentPartyCount <= 1) return@launch
      if (!hero.inParty && currentPartyCount >= 4) {
        val lastActive = heroesFlow.value.lastOrNull { it.inParty && it.id != hero.id }
        if (lastActive != null) {
          repository.updateHero(lastActive.copy(inParty = false))
        }
      }
      repository.updateHero(hero.copy(inParty = !hero.inParty))
    }
  }

  fun levelUpHero(hero: HeroEntity) {
    viewModelScope.launch {
      if (hero.level >= 99) return@launch
      val updated = hero.copy(
        level = (hero.level + 5).coerceAtMost(99),
        experiencePoints = hero.experiencePoints + 50000L,
        maxHp = hero.maxHp + 380,
        attack = hero.attack + 28,
        defense = hero.defense + 24,
        magic = hero.magic + 28,
        resistance = hero.resistance + 25,
        mind = hero.mind + 22,
        speed = (hero.speed + 3).coerceAtMost(210)
      )
      repository.updateHero(updated)
    }
  }

  fun diveHeroRecordBoard(hero: HeroEntity) {
    viewModelScope.launch {
      if (hero.recordDiveTier >= 6) return@launch
      repository.updateHero(
        hero.copy(
          recordDiveTier = hero.recordDiveTier + 1,
          maxHp = hero.maxHp + 450,
          attack = hero.attack + 32,
          magic = hero.magic + 32,
          defense = hero.defense + 26,
          resistance = hero.resistance + 26,
          soulBreakMultiplier = ((hero.soulBreakMultiplier + 0.4f) * 10f).roundToInt() / 10f
        )
      )
    }
  }

  fun equipRelicToHero(hero: HeroEntity, equipment: EquipmentEntity) {
    viewModelScope.launch {
      val previousItemId = when (equipment.slotTypeEnum) {
        EquipmentSlotType.WEAPON -> hero.equippedWeaponId
        EquipmentSlotType.ARMOR -> hero.equippedArmorId
        EquipmentSlotType.ACCESSORY -> hero.equippedAccessoryId
      }
      if (previousItemId != null && previousItemId != equipment.id) {
        val prevEq = equipmentFlow.value.find { it.id == previousItemId }
          ?: repository.getEquipmentById(previousItemId)
        if (prevEq != null && prevEq.equippedByHeroId == hero.id) {
          repository.updateEquipment(prevEq.copy(equippedByHeroId = null))
        }
      }
      val updatedHero = when (equipment.slotTypeEnum) {
        EquipmentSlotType.WEAPON -> hero.copy(equippedWeaponId = equipment.id)
        EquipmentSlotType.ARMOR -> hero.copy(equippedArmorId = equipment.id)
        EquipmentSlotType.ACCESSORY -> hero.copy(equippedAccessoryId = equipment.id)
      }
      repository.updateHero(updatedHero)
      repository.updateEquipment(equipment.copy(equippedByHeroId = hero.id))
    }
  }

  fun upgradeEquipment(equipment: EquipmentEntity) {
    viewModelScope.launch {
      if (equipment.upgradeLevel >= 50) return@launch
      repository.updateEquipment(
        equipment.copy(
          upgradeLevel = (equipment.upgradeLevel + 5).coerceAtMost(50),
          bonusHp = equipment.bonusHp + 80,
          bonusAtk = if (equipment.bonusAtk > 0) equipment.bonusAtk + 18 else 0,
          bonusDef = if (equipment.bonusDef > 0) equipment.bonusDef + 16 else 0,
          bonusMag = if (equipment.bonusMag > 0) equipment.bonusMag + 18 else 0,
          bonusRes = if (equipment.bonusRes > 0) equipment.bonusRes + 16 else 0
        )
      )
    }
  }

  fun reforgeEquipment(equipment: EquipmentEntity) {
    viewModelScope.launch {
      if (equipment.reforgeTier >= 3) return@launch
      val cfg = engineConfigFlow.value
      if (cfg.mythrilShards < 25) return@launch
      repository.saveEngineConfig(cfg.copy(mythrilShards = cfg.mythrilShards - 25))
      repository.updateEquipment(
        equipment.copy(
          reforgeTier = equipment.reforgeTier + 1,
          rarityStars = (equipment.rarityStars + 1).coerceAtMost(7),
          bonusHp = equipment.bonusHp + 120,
          bonusAtk = if (equipment.bonusAtk > 0) equipment.bonusAtk + 24 else 0,
          bonusMag = if (equipment.bonusMag > 0) equipment.bonusMag + 24 else 0,
          bonusDef = if (equipment.bonusDef > 0) equipment.bonusDef + 20 else 0,
          bonusRes = if (equipment.bonusRes > 0) equipment.bonusRes + 20 else 0
        )
      )
    }
  }

  fun socketSkillIntoEquipment(equipment: EquipmentEntity, skillId: Int) {
    viewModelScope.launch {
      val current = equipment.socketedSkillIdList().toMutableList()
      if (current.contains(skillId)) return@launch
      if (current.size >= equipment.materiaSocketsMax.coerceAtLeast(1)) {
        current[current.lastIndex] = skillId
      } else {
        current.add(skillId)
      }
      repository.updateEquipment(
        equipment.copy(socketedSkillIdsCsv = current.joinToString(","))
      )
    }
  }

  fun removeSocketFromEquipment(equipment: EquipmentEntity, slotIndex: Int) {
    viewModelScope.launch {
      val current = equipment.socketedSkillIdList().toMutableList()
      if (slotIndex in current.indices) {
        current.removeAt(slotIndex)
        repository.updateEquipment(
          equipment.copy(socketedSkillIdsCsv = current.joinToString(","))
        )
      }
    }
  }

  fun equipSkillToHeroSlot(hero: HeroEntity, slotIndex: Int, skillId: Int) {
    viewModelScope.launch {
      val current = hero.equippedSkillIdList().toMutableList()
      while (current.size < 3) current.add(1)
      current[slotIndex.coerceIn(0, 2)] = skillId
      repository.updateHero(hero.copy(equippedSkillIdsCsv = current.joinToString(",")))
    }
  }

  fun equipPassiveToHeroSlot(hero: HeroEntity, slotIndex: Int, skillId: Int) {
    viewModelScope.launch {
      val current = hero.equippedPassiveIdList().toMutableList()
      while (current.size < 2) current.add(13)
      current[slotIndex.coerceIn(0, 1)] = skillId
      repository.updateHero(hero.copy(equippedPassiveIdsCsv = current.joinToString(",")))
    }
  }

  fun changeHeroJobClass(hero: HeroEntity, jobClassName: String) {
    viewModelScope.launch {
      repository.updateHero(hero.copy(jobClassName = jobClassName.ifBlank { "Freelancer" }))
    }
  }

  fun honeSkillRank(skill: SkillEntity) {
    viewModelScope.launch {
      if (skill.rankLevel >= skill.maxRank) return@launch
      val cfg = engineConfigFlow.value
      if (cfg.mythrilShards < 20) return@launch
      repository.saveEngineConfig(cfg.copy(mythrilShards = cfg.mythrilShards - 20))
      repository.updateSkill(skill.copy(rankLevel = skill.rankLevel + 1))
    }
  }

  fun equipMedalToHeroSlot(hero: HeroEntity, slotIndex: Int, medalId: Int) {
    viewModelScope.launch {
      val currentIds = hero.equippedMedalIdList().toMutableList()
      while (currentIds.size < 5) currentIds.add(1)
      currentIds[slotIndex.coerceIn(0, 4)] = medalId
      repository.updateHero(hero.copy(equippedMedalIdsCsv = currentIds.joinToString(",")))
    }
  }

  fun upgradeRealmResonance(realm: RealmMetadataEntity) {
    viewModelScope.launch {
      repository.updateRealmMetadata(
        realm.copy(
          realmResonanceLevel = (realm.realmResonanceLevel + 1).coerceAtMost(10),
          staminaShardsCollected = (realm.staminaShardsCollected + 1).coerceAtMost(5),
          masteryPercent = (realm.masteryPercent + 15).coerceAtMost(100)
        )
      )
    }
  }

  // Medal Forge, Guilt Awakening & Summon Simulator
  fun upgradeMedalGuilt(medal: MedalEntity) {
    viewModelScope.launch {
      val newStars = (medal.rarityStars + 1).coerceAtMost(7)
      val newGuilt = (medal.guiltBonusPercent + Random.nextInt(10, 26)).coerceAtMost(200)
      val newMult = ((medal.baseMultiplier + 0.25f) * 100).roundToInt() / 100f
      repository.updateMedal(
        medal.copy(
          rarityStars = newStars,
          guiltBonusPercent = newGuilt,
          baseMultiplier = newMult
        )
      )
    }
  }

  fun summonRelicMedal() {
    viewModelScope.launch {
      val cfg = engineConfigFlow.value
      val nextMythril = if (cfg.mythrilShards >= 50) cfg.mythrilShards - 50 else 200
      repository.saveEngineConfig(cfg.copy(mythrilShards = nextMythril))

      val prefixes = listOf("Illustrated", "Awakened", "Key Art", "Arcane", "Sync", "Ultima")
      val heroes = listOf("Sora", "Cloud", "Terra", "Riku", "Vivi", "Tyro", "Aqua", "Noctis", "Celes")
      val suffixes = listOf("EX+", "Prime", "Record", "Ver. B", "Infinity", "Overdrive")

      val attr = MedalAttribute.entries.random()
      val align = MedalAlignment.entries.random()
      val elem = ElementType.entries.filter { it != ElementType.NON_ELEMENTAL }.random()
      val effect = SpecialEffectType.entries.random()
      val stars = if (Random.nextFloat() < 0.55f) 7 else 6

      val forged = MedalEntity(
        name = "${prefixes.random()} ${heroes.random()} ${suffixes.random()}",
        rarityStars = stars,
        attribute = attr.name,
        alignment = align.name,
        element = elem.name,
        spCost = Random.nextInt(1, 4),
        baseMultiplier = if (stars == 7) 4.1f else 3.4f,
        hitCount = Random.nextInt(5, 13),
        guiltTier = stars,
        guiltBonusPercent = if (stars == 7) Random.nextInt(135, 185) else Random.nextInt(100, 140),
        effectType = effect.name
      )
      repository.insertMedal(forged)
      _lastSummonedMedal.value = forged
    }
  }

  // Engine Studio: Custom Entity Creators & Config Tuning
  fun createCustomCharacter(
    name: String,
    title: String,
    realm: String,
    archetype: com.example.model.HeroArchetype,
    soulBreakName: String,
    soulBreakElement: ElementType
  ) {
    viewModelScope.launch {
      repository.insertHero(
        HeroEntity(
          name = name.ifBlank { "Roxas" },
          title = title.ifBlank { "Twilight Keybearer" },
          realm = realm.ifBlank { "Daybreak Town" },
          archetype = archetype.name,
          level = 85,
          experiencePoints = 400000L,
          maxHp = 7100,
          attack = 545,
          defense = 405,
          magic = 480,
          resistance = 420,
          mind = 380,
          speed = 176,
          recordDiveTier = 5,
          soulBreakName = soulBreakName.ifBlank { "Magic Hour Dual Arc" },
          soulBreakElement = soulBreakElement.name,
          soulBreakMultiplier = 8.0f,
          soulBreakEffect = SpecialEffectType.CHAIN_IGNITER.name,
          inParty = false,
          equippedMedalIdsCsv = "10,1,2,8,6",
          equippedWeaponId = 2,
          equippedArmorId = 8
        )
      )
    }
  }

  fun createCustomEquipment(
    name: String,
    slotType: EquipmentSlotType,
    originRealm: String,
    elementBoost: ElementType,
    bonusAtk: Int,
    bonusMag: Int,
    bonusDef: Int,
    rpgSystem: RpgSystemOrigin = RpgSystemOrigin.CHRONO_NEXUS,
    weaponArmorClass: WeaponArmorClass = when (slotType) {
      EquipmentSlotType.WEAPON -> WeaponArmorClass.KEYBLADE
      EquipmentSlotType.ARMOR -> WeaponArmorClass.HEAVY_PLATE
      EquipmentSlotType.ACCESSORY -> WeaponArmorClass.CHRONO_CHARM
    },
    setSeriesName: String = "",
    critRateBonus: Float = 0.08f,
    elementResistElement: ElementType = ElementType.NON_ELEMENTAL,
    elementResistPercent: Int = 0,
    lifestealPercent: Int = 0,
    counterChancePercent: Int = 0,
    grantedSkillId: Int? = null
  ) {
    viewModelScope.launch {
      repository.insertEquipment(
        EquipmentEntity(
          name = name.ifBlank { "Ultima Weapon Relic VII" },
          slotType = slotType.name,
          originRealm = originRealm.ifBlank { "Record Keep" },
          rarityStars = 7,
          upgradeLevel = 35,
          bonusHp = 650,
          bonusAtk = bonusAtk,
          bonusDef = bonusDef,
          bonusMag = bonusMag,
          bonusRes = bonusDef,
          bonusSpd = 15,
          elementBoost = elementBoost.name,
          passiveTrait = "+25% ${elementBoost.label} • ${weaponArmorClass.label}",
          rpgSystem = rpgSystem.name,
          weaponArmorClass = weaponArmorClass.name,
          setSeriesName = setSeriesName.trim(),
          critRateBonus = critRateBonus,
          elementResistElement = elementResistElement.name,
          elementResistPercent = elementResistPercent,
          lifestealPercent = lifestealPercent,
          counterChancePercent = counterChancePercent,
          materiaSocketsMax = 3,
          grantedSkillId = grantedSkillId
        )
      )
    }
  }

  fun createCustomSkill(
    name: String,
    rpgSystem: RpgSystemOrigin,
    category: SkillCategory,
    element: ElementType,
    attribute: MedalAttribute,
    spCost: Int,
    cooldownTurns: Int,
    powerMultiplier: Float,
    hitCount: Int,
    effectType: SkillCombatEffect,
    description: String
  ) {
    viewModelScope.launch {
      val isPassive = category.isPassive
      repository.insertSkill(
        SkillEntity(
          name = name.ifBlank { "Custom Chrono Flare" },
          rpgSystem = rpgSystem.name,
          category = category.name,
          element = element.name,
          attribute = attribute.name,
          spCost = if (isPassive) 0 else spCost.coerceIn(0, 5),
          cooldownTurns = if (isPassive) 0 else cooldownTurns.coerceIn(0, 4),
          powerMultiplier = powerMultiplier.coerceIn(1.2f, 6.0f),
          hitCount = hitCount.coerceIn(1, 10),
          effectType = if (isPassive) SkillCombatEffect.PASSIVE_STAT_BOOST.name else effectType.name,
          rankLevel = 1,
          maxRank = 5,
          description = description.ifBlank { "${rpgSystem.shortTag} ${category.label}: ${effectType.label}." },
          passiveAtkPercent = if (isPassive && attribute == MedalAttribute.POWER) 12 else 0,
          passiveMagPercent = if (isPassive && attribute == MedalAttribute.MAGIC) 12 else 0,
          passiveSpdBonus = if (isPassive && attribute == MedalAttribute.SPEED) 15 else 0,
          passiveCritBonus = if (isPassive) 0.06f else 0f
        )
      )
    }
  }

  fun createCustomMedal(
    name: String,
    attribute: MedalAttribute,
    alignment: MedalAlignment,
    element: ElementType,
    spCost: Int,
    multiplier: Float,
    guiltPercent: Int,
    effect: SpecialEffectType
  ) {
    viewModelScope.launch {
      repository.insertMedal(
        MedalEntity(
          name = name.ifBlank { "Custom Key Art EX" },
          rarityStars = 7,
          attribute = attribute.name,
          alignment = alignment.name,
          element = element.name,
          spCost = spCost.coerceIn(0, 5),
          baseMultiplier = multiplier.coerceIn(1.5f, 6.5f),
          hitCount = 8,
          guiltTier = 7,
          guiltBonusPercent = guiltPercent.coerceIn(50, 200),
          effectType = effect.name
        )
      )
    }
  }

  fun createCustomBossStage(
    title: String,
    realm: String,
    bossName: String,
    archetype: BossArchetype,
    difficulty: Int,
    maxHp: Int,
    weakness: ElementType,
    bossAttribute: MedalAttribute,
    signatureMove: String
  ) {
    viewModelScope.launch {
      val cleanRealm = realm.ifBlank { "Record Keep" }
      val stage = RealmStageEntity(
        stageCode = "ENG-${Random.nextInt(10, 99)}",
        title = title.ifBlank { "Custom Astral Rift" },
        realm = cleanRealm,
        difficulty = difficulty.coerceIn(50, 400),
        isProudMode = difficulty >= 220,
        bossName = bossName.ifBlank { "Custom Aeon Construct" },
        bossArchetype = archetype.name,
        bossMaxHp = maxHp.coerceIn(50000, 999999),
        bossAttack = (280 + difficulty * 1.4f).roundToInt(),
        bossDefense = (220 + difficulty * 1.1f).roundToInt(),
        bossAttribute = bossAttribute.name,
        weaknessElement = weakness.name,
        bossCountdownTurns = 3,
        signatureAttackName = signatureMove.ifBlank { "Omega Astral Ray" },
        masteryConditionText = "Custom Engine Stage: Exploit ${weakness.label} Weakness"
      )
      repository.insertStage(stage)

      // Also ensure RealmMetadataEntity exists for this realm
      val existingRealm = realmMetadataFlow.value.find { it.realmName.equals(cleanRealm, ignoreCase = true) }
      if (existingRealm != null) {
        repository.updateRealmMetadata(
          existingRealm.copy(totalStagesCount = existingRealm.totalStagesCount + 1)
        )
      } else {
        repository.insertRealmMetadata(
          RealmMetadataEntity(
            realmName = cleanRealm,
            seriesCode = "ENG-CUSTOM",
            worldLore = "Custom world realm forged inside the Chronicle Nexus Engine Studio.",
            dominantElement = weakness.name,
            realmResonanceLevel = 1,
            staminaShardsCollected = 1,
            totalStagesCount = 1,
            clearedStagesCount = 0,
            masteryPercent = 0,
            isUnlocked = true
          )
        )
      }
    }
  }

  fun updateEngineConfig(updated: EngineConfigEntity) {
    viewModelScope.launch {
      repository.saveEngineConfig(updated)
    }
  }

  fun clearBattleHistory() {
    viewModelScope.launch {
      repository.clearBattleLogs()
    }
  }

  // External Asset Importer & Live Audio Controls
  fun toggleAudioMute() {
    audioEngine?.toggleMute()
  }

  fun previewAudioAsset(asset: ImportedAssetEntity) {
    if (asset.assetTypeEnum == ImportedAssetType.AUDIO_SFX) {
      audioEngine?.preloadSfx(asset)
      audioEngine?.previewAudio(asset)
    } else {
      audioEngine?.previewAudio(asset)
    }
  }

  private fun triggerCombatAudioCue(heroSfxAssetId: Int?, cue: SfxCombatCue) {
    val allAssets = importedAssetsFlow.value
    val heroBound = heroSfxAssetId?.let { id -> allAssets.find { it.id == id } }
    val cueBound = allAssets.firstOrNull {
      it.assetTypeEnum == ImportedAssetType.AUDIO_SFX &&
        it.boundTargetTypeEnum == AssetBindTargetType.COMBAT_SFX &&
        it.boundSfxCueEnum == cue
    }
    audioEngine?.triggerSfx(heroBound ?: cueBound)
  }

  fun searchExternalWikis(query: String, wikiDomain: String) {
    val importer = assetImportService ?: return
    viewModelScope.launch {
      _importerLoading.value = true
      _importerStatusMessage.value = "Searching $wikiDomain..."
      importer.searchWikisAndConceptArt(query, wikiDomain)
        .onSuccess { results ->
          _assetSearchResults.value = results
          _importerStatusMessage.value = if (results.isEmpty()) {
            "No wiki matches found on $wikiDomain for \"$query\"."
          } else {
            "Found ${results.size} entries from $wikiDomain."
          }
        }
        .onFailure { err ->
          _importerStatusMessage.value = "Wiki lookup failed: ${err.message ?: "Network error"}"
        }
      _importerLoading.value = false
    }
  }

  fun importSearchResult(result: ExternalSearchResult) {
    val importer = assetImportService ?: return
    viewModelScope.launch {
      _importerLoading.value = true
      _importerStatusMessage.value = "Importing ${result.title}..."
      importer.importSearchResult(result)
        .onSuccess { entity ->
          repository.insertImportedAsset(entity)
          if (entity.assetTypeEnum == ImportedAssetType.MODEL_3D && entity.localFilePath.isNotBlank()) {
            _inspectedMesh3D.value = Mesh3DParser.parseFile(File(entity.localFilePath), entity.linkedTexturePath)
          }
          _importerStatusMessage.value = "Imported \"${entity.name}\" (${entity.assetTypeEnum.label})."
        }
        .onFailure { err ->
          _importerStatusMessage.value = "Import failed: ${err.message}"
        }
      _importerLoading.value = false
    }
  }

  fun importFromDirectUrl(
    url: String,
    overrideType: ImportedAssetType?,
    customName: String,
    cols: Int,
    rows: Int,
    fps: Int
  ) {
    val importer = assetImportService ?: return
    viewModelScope.launch {
      _importerLoading.value = true
      _importerStatusMessage.value = "Fetching from URL..."
      importer.importFromDirectUrl(url, overrideType, customName, cols, rows, fps)
        .onSuccess { entities ->
          repository.insertImportedAssets(entities)
          val first3d = entities.firstOrNull { it.assetTypeEnum == ImportedAssetType.MODEL_3D }
          if (first3d != null && first3d.localFilePath.isNotBlank()) {
            _inspectedMesh3D.value = Mesh3DParser.parseFile(File(first3d.localFilePath), first3d.linkedTexturePath)
          }
          _importerStatusMessage.value = "Auto-imported ${entities.size} asset(s) from URL."
        }
        .onFailure { err ->
          _importerStatusMessage.value = "URL import failed: ${err.message}"
        }
      _importerLoading.value = false
    }
  }

  fun importFromManifestUrl(manifestUrl: String) {
    val importer = assetImportService ?: return
    viewModelScope.launch {
      _importerLoading.value = true
      _importerStatusMessage.value = "Downloading JSON asset pack..."
      importer.importFromManifestUrl(manifestUrl)
        .onSuccess { entities ->
          repository.insertImportedAssets(entities)
          _importerStatusMessage.value = "Imported ${entities.size} asset(s) from JSON manifest."
        }
        .onFailure { err ->
          _importerStatusMessage.value = "Manifest import failed: ${err.message}"
        }
      _importerLoading.value = false
    }
  }

  fun importFromLocalUri(
    uri: Uri,
    overrideType: ImportedAssetType?,
    cols: Int,
    rows: Int,
    fps: Int
  ) {
    val importer = assetImportService ?: return
    viewModelScope.launch {
      _importerLoading.value = true
      _importerStatusMessage.value = "Importing local file / ZIP pack..."
      importer.importFromLocalUri(uri, overrideType, cols, rows, fps)
        .onSuccess { entities ->
          repository.insertImportedAssets(entities)
          val first3d = entities.firstOrNull { it.assetTypeEnum == ImportedAssetType.MODEL_3D }
          if (first3d != null && first3d.localFilePath.isNotBlank()) {
            _inspectedMesh3D.value = Mesh3DParser.parseFile(File(first3d.localFilePath), first3d.linkedTexturePath)
          }
          _importerStatusMessage.value = "Imported ${entities.size} local asset(s) into Vault."
        }
        .onFailure { err ->
          _importerStatusMessage.value = "Local import failed: ${err.message}"
        }
      _importerLoading.value = false
    }
  }

  fun inspect3DModelAsset(asset: ImportedAssetEntity) {
    val file = File(asset.localFilePath)
    _inspectedMesh3D.value = Mesh3DParser.parseFile(file, asset.linkedTexturePath)
  }

  fun close3DInspector() {
    _inspectedMesh3D.value = null
  }

  fun autoBindAsset(
    asset: ImportedAssetEntity,
    targetType: AssetBindTargetType,
    targetId: Int?,
    sfxCue: SfxCombatCue
  ) {
    viewModelScope.launch {
      val updatedAsset = asset.copy(
        boundTargetType = targetType.name,
        boundTargetId = targetId,
        boundSfxCue = sfxCue.name
      )
      repository.updateImportedAsset(updatedAsset)

      val mediaPathOrUrl = asset.localFilePath.ifBlank { asset.sourceUrl }

      when (targetType) {
        AssetBindTargetType.HERO -> {
          val hero = targetId?.let { id ->
            heroesFlow.value.find { it.id == id } ?: repository.getHeroById(id)
          } ?: return@launch
          val updatedHero = when (asset.assetTypeEnum) {
            ImportedAssetType.SPRITE, ImportedAssetType.SPRITESHEET -> hero.copy(spriteAssetId = asset.id)
            ImportedAssetType.CONCEPT_ART -> hero.copy(
              portraitUri = mediaPathOrUrl,
              loreBio = asset.wikiSummary.ifBlank { hero.loreBio }
            )
            ImportedAssetType.WIKI_LORE -> hero.copy(
              loreBio = asset.wikiSummary.ifBlank { hero.loreBio },
              portraitUri = hero.portraitUri.ifBlank { asset.sourceUrl }
            )
            ImportedAssetType.MODEL_3D -> hero.copy(model3dAssetId = asset.id)
            ImportedAssetType.AUDIO_SFX -> hero.copy(attackSfxAssetId = asset.id)
            else -> hero
          }
          repository.updateHero(updatedHero)
          if (asset.assetTypeEnum == ImportedAssetType.SPRITE || asset.assetTypeEnum == ImportedAssetType.SPRITESHEET) {
            _battleState.update { state ->
              state.copy(
                partyStates = state.partyStates.map { hs ->
                  if (hs.hero.id == hero.id) {
                    hs.copy(
                      hero = updatedHero,
                      spriteSheetPath = mediaPathOrUrl,
                      spriteCols = asset.sheetColumns,
                      spriteRows = asset.sheetRows,
                      spriteFps = asset.sheetFps
                    )
                  } else hs
                }
              )
            }
          }
        }
        AssetBindTargetType.BOSS_STAGE -> {
          val stage = targetId?.let { id ->
            stagesFlow.value.find { it.id == id }
              ?: repository.stages.firstOrNull()?.find { it.id == id }
          } ?: return@launch
          val updatedStage = when (asset.assetTypeEnum) {
            ImportedAssetType.SPRITE, ImportedAssetType.SPRITESHEET -> stage.copy(bossSpriteAssetId = asset.id)
            ImportedAssetType.CONCEPT_ART -> stage.copy(stageConceptArtUri = mediaPathOrUrl)
            ImportedAssetType.MODEL_3D -> stage.copy(boss3dAssetId = asset.id)
            ImportedAssetType.AUDIO_BGM -> stage.copy(stageBgmAssetId = asset.id)
            else -> stage
          }
          repository.updateStage(updatedStage)
          if (_battleState.value.stage?.id == stage.id) {
            _battleState.update { state ->
              state.copy(
                stage = updatedStage,
                bossState = state.bossState?.copy(
                  stage = updatedStage,
                  bossSpritePath = if (asset.assetTypeEnum == ImportedAssetType.SPRITE || asset.assetTypeEnum == ImportedAssetType.SPRITESHEET) mediaPathOrUrl else state.bossState.bossSpritePath,
                  bossSpriteCols = if (asset.assetTypeEnum == ImportedAssetType.SPRITESHEET) asset.sheetColumns else state.bossState.bossSpriteCols,
                  bossSpriteRows = if (asset.assetTypeEnum == ImportedAssetType.SPRITESHEET) asset.sheetRows else state.bossState.bossSpriteRows,
                  bossSpriteFps = if (asset.assetTypeEnum == ImportedAssetType.SPRITESHEET) asset.sheetFps else state.bossState.bossSpriteFps,
                  stageBgArtUri = if (asset.assetTypeEnum == ImportedAssetType.CONCEPT_ART) mediaPathOrUrl else state.bossState.stageBgArtUri
                )
              )
            }
            if (asset.assetTypeEnum == ImportedAssetType.AUDIO_BGM) {
              audioEngine?.playStageBgm(updatedAsset)
            }
          }
        }
        AssetBindTargetType.REALM -> {
          val realm = targetId?.let { id ->
            realmMetadataFlow.value.find { it.id == id } ?: repository.getRealmById(id)
          } ?: return@launch
          val updatedRealm = when (asset.assetTypeEnum) {
            ImportedAssetType.CONCEPT_ART -> realm.copy(
              bannerArtUri = mediaPathOrUrl,
              worldLore = asset.wikiSummary.ifBlank { realm.worldLore }
            )
            ImportedAssetType.WIKI_LORE -> realm.copy(
              worldLore = asset.wikiSummary.ifBlank { realm.worldLore },
              wikiUrl = asset.sourceUrl.ifBlank { realm.wikiUrl }
            )
            ImportedAssetType.AUDIO_BGM -> realm.copy(bgmAssetId = asset.id)
            else -> realm
          }
          repository.updateRealmMetadata(updatedRealm)
          if (_battleState.value.stage?.realm.equals(realm.realmName, ignoreCase = true) &&
            asset.assetTypeEnum == ImportedAssetType.AUDIO_BGM
          ) {
            audioEngine?.playStageBgm(updatedAsset)
          }
        }
        AssetBindTargetType.COMBAT_SFX -> {
          audioEngine?.preloadSfx(updatedAsset)
        }
        else -> Unit
      }
      _importerStatusMessage.value = "Bound \"${asset.name}\" to ${targetType.label}!"
    }
  }

  fun deleteImportedAsset(assetId: Int) {
    viewModelScope.launch {
      repository.deleteImportedAssetById(assetId)
      _importerStatusMessage.value = "Removed imported asset #$assetId."
    }
  }

  // ---------------------------------------------------------------------------
  // Clean ArmoryRepository API for UI / ViewModel Consumers (Weapons, Armor, Skills)
  // ---------------------------------------------------------------------------

  fun observeWeaponsBySystem(rpgSystem: RpgSystemOrigin): Flow<List<WeaponEntity>> =
    armoryRepository.observeWeaponsBySystem(rpgSystem)

  fun observeArmorsBySystem(rpgSystem: RpgSystemOrigin): Flow<List<ArmorEntity>> =
    armoryRepository.observeArmorsBySystem(rpgSystem)

  fun observeSkillsByCategory(category: SkillCategory): Flow<List<SkillEntity>> =
    armoryRepository.observeSkillsByCategory(category)

  fun searchArmoryWeapons(query: String): Flow<List<WeaponEntity>> =
    armoryRepository.searchWeapons(query)

  fun searchArmoryArmors(query: String): Flow<List<ArmorEntity>> =
    armoryRepository.searchArmors(query)

  fun searchArmorySkills(query: String): Flow<List<SkillEntity>> =
    armoryRepository.searchSkills(query)

  fun saveWeapon(weapon: WeaponEntity) {
    viewModelScope.launch { armoryRepository.saveWeapon(weapon) }
  }

  fun deleteWeapon(weaponId: Int) {
    viewModelScope.launch { armoryRepository.deleteWeaponById(weaponId) }
  }

  fun saveArmor(armor: ArmorEntity) {
    viewModelScope.launch { armoryRepository.saveArmor(armor) }
  }

  fun deleteArmor(armorId: Int) {
    viewModelScope.launch { armoryRepository.deleteArmorById(armorId) }
  }

  fun saveSkill(skill: SkillEntity) {
    viewModelScope.launch { armoryRepository.saveSkill(skill) }
  }

  fun deleteSkill(skillId: Int) {
    viewModelScope.launch { armoryRepository.deleteSkillById(skillId) }
  }
}
