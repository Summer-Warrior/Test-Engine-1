package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.RpgEngineViewModel
import com.example.ui.screens.BattleScreen
import com.example.ui.screens.EngineStudioScreen
import com.example.ui.screens.MedalForgeScreen
import com.example.ui.screens.PartyScreen
import com.example.ui.screens.RealmMapScreen
import com.example.ui.theme.DeepCrystalBlue
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RelicGold
import com.example.ui.theme.VoidNavy
import dagger.hilt.android.AndroidEntryPoint

enum class RpgDestination(
  val route: String,
  val label: String,
  val icon: ImageVector,
  val testTag: String
) {
  BATTLE("battle", "Battle", Icons.Default.SportsMartialArts, "nav_tab_battle"),
  PARTY("party", "Party", Icons.Default.Groups, "nav_tab_party"),
  MEDALS("medals", "Medals", Icons.Default.AutoAwesome, "nav_tab_medals"),
  REALMS("realms", "Realms", Icons.Default.Public, "nav_tab_realms"),
  ENGINE("engine", "Engine", Icons.Default.Build, "nav_tab_engine")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        ChronicleNexusApp()
      }
    }
  }
}

@Composable
fun ChronicleNexusApp(
  viewModel: RpgEngineViewModel = hiltViewModel()
) {
  var currentTab by rememberSaveable { mutableStateOf(RpgDestination.BATTLE) }

  val battleState by viewModel.battleState.collectAsStateWithLifecycle()
  val heroes by viewModel.heroesFlow.collectAsStateWithLifecycle()
  val equipmentList by viewModel.equipmentFlow.collectAsStateWithLifecycle()
  val realmMetadataList by viewModel.realmMetadataFlow.collectAsStateWithLifecycle()
  val medals by viewModel.medalsFlow.collectAsStateWithLifecycle()
  val stages by viewModel.stagesFlow.collectAsStateWithLifecycle()
  val engineConfig by viewModel.engineConfigFlow.collectAsStateWithLifecycle()
  val battleLogs by viewModel.battleLogsFlow.collectAsStateWithLifecycle()
  val lastSummonedMedal by viewModel.lastSummonedMedal.collectAsStateWithLifecycle()
  val importedAssets by viewModel.importedAssetsFlow.collectAsStateWithLifecycle()
  val skills by viewModel.skillsFlow.collectAsStateWithLifecycle()
  val weapons by viewModel.weaponsFlow.collectAsStateWithLifecycle()
  val armors by viewModel.armorsFlow.collectAsStateWithLifecycle()
  val assetSearchResults by viewModel.assetSearchResults.collectAsStateWithLifecycle()
  val importerLoading by viewModel.importerLoading.collectAsStateWithLifecycle()
  val importerStatusMessage by viewModel.importerStatusMessage.collectAsStateWithLifecycle()
  val inspectedMesh3D by viewModel.inspectedMesh3D.collectAsStateWithLifecycle()
  val isAudioMuted by viewModel.isAudioMutedFlow.collectAsStateWithLifecycle()
  val activeBgmTitle by viewModel.activeBgmTitleFlow.collectAsStateWithLifecycle()

  if (currentTab != RpgDestination.BATTLE) {
    BackHandler {
      currentTab = RpgDestination.BATTLE
    }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = VoidNavy,
    bottomBar = {
      NavigationBar(
        containerColor = DeepCrystalBlue,
        tonalElevation = 8.dp
      ) {
        RpgDestination.entries.forEach { dest ->
          val selected = currentTab == dest
          NavigationBarItem(
            selected = selected,
            onClick = { currentTab = dest },
            icon = {
              Icon(
                imageVector = dest.icon,
                contentDescription = dest.label
              )
            },
            label = { Text(dest.label) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = VoidNavy,
              selectedTextColor = RelicGold,
              indicatorColor = RelicGold,
              unselectedIconColor = MutedSilver,
              unselectedTextColor = MutedSilver
            ),
            modifier = Modifier.testTag(dest.testTag)
          )
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter
    ) {
      val contentModifier = Modifier.widthIn(max = 840.dp)
      when (currentTab) {
        RpgDestination.BATTLE -> {
          BattleScreen(
            battleState = battleState,
            allMedals = medals,
            allStages = stages,
            onSelectHero = viewModel::selectHero,
            onBasicAttack = { viewModel.executeBasicAttack() },
            onMedalBurst = { viewModel.executeMedalBurst() },
            onDefend = { viewModel.executeDefend() },
            onRotateWheel = { viewModel.rotateMedalWheel() },
            onSoulBreak = { viewModel.executeSoulBreak() },
            onExecuteSkill = { skill -> viewModel.executeHeroSkill(skill) },
            onToggleWaitMode = viewModel::toggleWaitMode,
            onToggleAutoBattle = viewModel::toggleAutoBattle,
            onToggleSpeed = viewModel::toggleBattleSpeed,
            onRestartEncounter = viewModel::restartCurrentEncounter,
            onSelectStage = { nextStage ->
              viewModel.startEncounter(nextStage)
            },
            isAudioMuted = isAudioMuted,
            activeBgmTitle = activeBgmTitle,
            onToggleAudioMute = viewModel::toggleAudioMute,
            modifier = contentModifier
          )
        }

        RpgDestination.PARTY -> {
          PartyScreen(
            heroes = heroes,
            equipmentList = equipmentList,
            medals = medals,
            activeStageRealm = battleState.stage?.realm ?: "Daybreak Town",
            onToggleHeroInParty = viewModel::toggleHeroInParty,
            onLevelUpHero = viewModel::levelUpHero,
            onDiveRecordBoard = viewModel::diveHeroRecordBoard,
            onEquipRelicToHero = viewModel::equipRelicToHero,
            onUpgradeEquipment = viewModel::upgradeEquipment,
            onEquipMedalToSlot = viewModel::equipMedalToHeroSlot,
            skills = skills,
            mythrilShards = engineConfig.mythrilShards,
            onReforgeEquipment = viewModel::reforgeEquipment,
            onSocketSkillIntoEquipment = viewModel::socketSkillIntoEquipment,
            onRemoveSocketFromEquipment = viewModel::removeSocketFromEquipment,
            onEquipSkillToSlot = viewModel::equipSkillToHeroSlot,
            onEquipPassiveToSlot = viewModel::equipPassiveToHeroSlot,
            onChangeHeroJobClass = viewModel::changeHeroJobClass,
            onHoneSkill = viewModel::honeSkillRank,
            modifier = contentModifier
          )
        }

        RpgDestination.MEDALS -> {
          MedalForgeScreen(
            medals = medals,
            mythrilShards = engineConfig.mythrilShards,
            lastSummonedMedal = lastSummonedMedal,
            onSummonRelicMedal = viewModel::summonRelicMedal,
            onUpgradeMedalGuilt = viewModel::upgradeMedalGuilt,
            modifier = contentModifier
          )
        }

        RpgDestination.REALMS -> {
          RealmMapScreen(
            stages = stages,
            realmMetadataList = realmMetadataList,
            heroes = heroes,
            activeStageId = battleState.stage?.id,
            onLaunchStage = { stage ->
              viewModel.startEncounter(stage)
              currentTab = RpgDestination.BATTLE
            },
            onUpgradeRealmResonance = viewModel::upgradeRealmResonance,
            modifier = contentModifier
          )
        }

        RpgDestination.ENGINE -> {
          EngineStudioScreen(
            engineConfig = engineConfig,
            heroesCount = heroes.size,
            equipmentCount = equipmentList.size,
            realmsCount = realmMetadataList.size,
            medalsCount = medals.size,
            stagesCount = stages.size,
            battleLogs = battleLogs,
            heroes = heroes,
            stages = stages,
            realms = realmMetadataList,
            skills = skills,
            weaponsCount = weapons.size,
            armorsCount = armors.size,
            importedAssets = importedAssets,
            assetSearchResults = assetSearchResults,
            openCc0Catalog = viewModel.openCc0Catalog,
            importerLoading = importerLoading,
            importerStatusMessage = importerStatusMessage,
            inspectedMesh3D = inspectedMesh3D,
            onSearchWiki = viewModel::searchExternalWikis,
            onImportSearchResult = viewModel::importSearchResult,
            onImportDirectUrl = viewModel::importFromDirectUrl,
            onImportManifestUrl = viewModel::importFromManifestUrl,
            onImportLocalUri = viewModel::importFromLocalUri,
            onInspect3DModel = viewModel::inspect3DModelAsset,
            onClose3DViewer = viewModel::close3DInspector,
            onPreviewAudio = viewModel::previewAudioAsset,
            onAutoBindAsset = viewModel::autoBindAsset,
            onDeleteImportedAsset = viewModel::deleteImportedAsset,
            onUpdateEngineConfig = viewModel::updateEngineConfig,
            onCreateCustomEquipment = viewModel::createCustomEquipment,
            onCreateCustomSkill = viewModel::createCustomSkill,
            onCreateCustomMedal = viewModel::createCustomMedal,
            onCreateCustomBossStage = viewModel::createCustomBossStage,
            onClearBattleLogs = viewModel::clearBattleHistory,
            modifier = contentModifier
          )
        }
      }
    }
  }
}
