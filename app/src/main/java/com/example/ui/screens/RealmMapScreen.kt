package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import com.example.model.HeroEntity
import com.example.model.RealmMetadataEntity
import com.example.model.RealmStageEntity
import com.example.ui.theme.AstralBorder
import com.example.ui.theme.DeepCrystalBlue
import com.example.ui.theme.HpEmerald
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.MythrilCyan
import com.example.ui.theme.RelicGold
import com.example.ui.theme.SoulBreakMagenta
import com.example.ui.theme.SynergyAzure
import com.example.ui.theme.VoidNavy

@Composable
fun RealmMapScreen(
  stages: List<RealmStageEntity>,
  realmMetadataList: List<RealmMetadataEntity>,
  heroes: List<HeroEntity>,
  activeStageId: Int?,
  onLaunchStage: (RealmStageEntity) -> Unit,
  onUpgradeRealmResonance: (RealmMetadataEntity) -> Unit,
  modifier: Modifier = Modifier
) {
  val activeParty = heroes.filter { it.inParty }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 12.dp)
      .testTag("realm_map_screen"),
    contentPadding = PaddingValues(vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // 1. World & Realm Progression Atlas (Room RealmMetadataEntity)
    item {
      Surface(
        color = DeepCrystalBlue,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AstralBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = "WORLD & REALM PROGRESSION ATLAS",
            style = MaterialTheme.typography.titleLarge,
            color = RelicGold
          )
          Text(
            text = "Persistent Realm Metadata stored in Room: Stamina Shards (5/5 = Stamina Refresh), Realm Resonance Levels, and World Mastery.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedSilver
          )
          Spacer(modifier = Modifier.height(8.dp))
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            items(realmMetadataList, key = { it.id }) { realmMeta ->
              RealmMetadataCard(
                realm = realmMeta,
                onResonate = { onUpgradeRealmResonance(realmMeta) }
              )
            }
          }
        }
      }
    }

    // 2. Record Paintings & Stage List
    items(stages, key = { it.id }) { stage ->
      val synergyHeroes = activeParty.filter {
        it.realm.equals(stage.realm, ignoreCase = true) || it.realm == "Record Keep"
      }
      val isCurrentStage = stage.id == activeStageId
      val realmMeta = realmMetadataList.find { it.realmName.equals(stage.realm, ignoreCase = true) }

      Card(
        colors = CardDefaults.cardColors(containerColor = DeepCrystalBlue),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
          width = if (isCurrentStage) 2.dp else 1.dp,
          color = when {
            isCurrentStage -> RelicGold
            stage.isProudMode -> SoulBreakMagenta
            else -> AstralBorder
          }
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("stage_card_${stage.id}")
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                color = if (stage.isProudMode) SoulBreakMagenta.copy(alpha = 0.22f) else VoidNavy,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, if (stage.isProudMode) SoulBreakMagenta else RelicGold)
              ) {
                Text(
                  text = "${stage.stageCode} • DIFF ${stage.difficulty}",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (stage.isProudMode) SoulBreakMagenta else RelicGold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "${stage.realm} (${realmMeta?.seriesCode ?: "CORE"})",
                style = MaterialTheme.typography.labelLarge,
                color = SynergyAzure
              )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              for (s in 1..3) {
                Icon(
                  imageVector = Icons.Default.Star,
                  contentDescription = "Star",
                  tint = if (s <= stage.bestStars) RelicGold else MutedSilver.copy(alpha = 0.35f),
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = stage.title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
          )
          Text(
            text = "Boss: ${stage.bossName} (HP %,d • Countdown ${stage.bossCountdownTurns} Turns)".format(stage.bossMaxHp),
            style = MaterialTheme.typography.bodyMedium,
            color = MutedSilver
          )

          Spacer(modifier = Modifier.height(6.dp))

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              color = stage.weaknessElementEnum.color.copy(alpha = 0.18f),
              shape = RoundedCornerShape(6.dp),
              border = BorderStroke(1.dp, stage.weaknessElementEnum.color)
            ) {
              Text(
                text = "WEAK: ${stage.weaknessElementEnum.label.uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = stage.weaknessElementEnum.color,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
              )
            }
            Surface(
              color = stage.bossAttributeEnum.color.copy(alpha = 0.18f),
              shape = RoundedCornerShape(6.dp),
              border = BorderStroke(1.dp, stage.bossAttributeEnum.color)
            ) {
              Text(
                text = "BOSS ATTR: ${stage.bossAttributeEnum.label.uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = stage.bossAttributeEnum.color,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "Target Score: ${stage.masteryConditionText}",
            style = MaterialTheme.typography.labelSmall,
            color = HpEmerald
          )
          Text(
            text = if (synergyHeroes.isNotEmpty()) {
              "Active Party Synergy: ${synergyHeroes.joinToString { it.name }} (+25% All Stats!)"
            } else {
              "No matching Realm Synergy hero in current party."
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (synergyHeroes.isNotEmpty()) SynergyAzure else MutedSilver
          )

          Spacer(modifier = Modifier.height(10.dp))

          Button(
            onClick = { onLaunchStage(stage) },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (stage.isProudMode) SoulBreakMagenta else RelicGold,
              contentColor = if (stage.isProudMode) Color.White else VoidNavy
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_launch_stage_${stage.id}")
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Enter Record")
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isCurrentStage) "RESTART / ENTER RECORD DUNGEON" else "ENTER RECORD DUNGEON",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

@Composable
private fun RealmMetadataCard(
  realm: RealmMetadataEntity,
  onResonate: () -> Unit
) {
  Surface(
    color = VoidNavy,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, realm.dominantElementEnum.color.copy(alpha = 0.7f)),
    modifier = Modifier
      .width(235.dp)
      .testTag("realm_meta_card_${realm.id}")
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      if (realm.bannerArtUri.isNotBlank()) {
        val artModel: Any = if (File(realm.bannerArtUri).exists()) File(realm.bannerArtUri) else realm.bannerArtUri
        AsyncImage(
          model = artModel,
          contentDescription = "${realm.realmName} Concept Art",
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(6.dp))
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = realm.realmName,
          style = MaterialTheme.typography.titleMedium,
          color = Color.White
        )
        Text(
          text = "${realm.seriesCode} • Lv.${realm.realmResonanceLevel}" + if (realm.bgmAssetId != null) " • ♪" else "",
          style = MaterialTheme.typography.labelSmall,
          color = RelicGold,
          fontSize = 10.sp
        )
      }
      Text(
        text = realm.worldLore,
        style = MaterialTheme.typography.labelSmall,
        color = MutedSilver,
        maxLines = 2,
        fontSize = 10.sp,
        modifier = Modifier.padding(vertical = 3.dp)
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Shards: " + "♦".repeat(realm.staminaShardsCollected) + "◊".repeat((5 - realm.staminaShardsCollected).coerceAtLeast(0)),
          style = MaterialTheme.typography.labelSmall,
          color = HpEmerald
        )
        Text(
          text = "${realm.masteryPercent}% Mastery",
          style = MaterialTheme.typography.labelSmall,
          color = MythrilCyan
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      LinearProgressIndicator(
        progress = { (realm.masteryPercent / 100f).coerceIn(0f, 1f) },
        modifier = Modifier
          .fillMaxWidth()
          .height(5.dp)
          .clip(RoundedCornerShape(3.dp)),
        color = realm.dominantElementEnum.color,
        trackColor = DeepCrystalBlue
      )
      Spacer(modifier = Modifier.height(6.dp))
      OutlinedButton(
        onClick = onResonate,
        border = BorderStroke(1.dp, MythrilCyan),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(28.dp)
      ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = "Attune Realm", tint = MythrilCyan, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Attune Realm Resonance", style = MaterialTheme.typography.labelSmall, color = MythrilCyan, fontSize = 10.sp)
      }
    }
  }
}
