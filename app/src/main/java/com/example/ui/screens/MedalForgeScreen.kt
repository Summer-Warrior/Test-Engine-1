package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MedalAttribute
import com.example.model.MedalEntity
import com.example.ui.theme.AstralBorder
import com.example.ui.theme.AstralPanel
import com.example.ui.theme.DeepCrystalBlue
import com.example.ui.theme.HpEmerald
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.MythrilCyan
import com.example.ui.theme.RelicGold
import com.example.ui.theme.SoulBreakMagenta
import com.example.ui.theme.VoidNavy

@Composable
fun MedalForgeScreen(
  medals: List<MedalEntity>,
  mythrilShards: Int,
  lastSummonedMedal: MedalEntity?,
  onSummonRelicMedal: () -> Unit,
  onUpgradeMedalGuilt: (MedalEntity) -> Unit,
  modifier: Modifier = Modifier
) {
  var filterAttribute by remember { mutableStateOf<MedalAttribute?>(null) }
  val filteredMedals = remember(medals, filterAttribute) {
    if (filterAttribute == null) medals else medals.filter { it.attributeEnum == filterAttribute }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 12.dp)
      .testTag("medal_forge_screen"),
    contentPadding = PaddingValues(vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // 1. Relic & Medal Summon Altar (In-Game Mythril Simulator)
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = DeepCrystalBlue),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, RelicGold),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "ASTRAL RELIC & MEDAL ALTAR",
                style = MaterialTheme.typography.titleLarge,
                color = RelicGold
              )
              Text(
                text = "Channel earned Mythril Shards to manifest 6★–7★ Upright & Reversed Medals with Tier 6–7 Guilt.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSilver
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              color = VoidNavy,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, MythrilCyan)
            ) {
              Text(
                text = "$mythrilShards Mythril",
                style = MaterialTheme.typography.labelLarge,
                color = MythrilCyan,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Button(
            onClick = onSummonRelicMedal,
            colors = ButtonDefaults.buttonColors(
              containerColor = RelicGold,
              contentColor = VoidNavy
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_summon_medal")
          ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = "Summon Medal")
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "MANIFEST RELIC MEDAL (50 MYTHRIL)",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold
            )
          }

          if (lastSummonedMedal != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
              color = VoidNavy,
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.5.dp, SoulBreakMagenta),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(
                  text = "NEWLY MANIFESTED MEDAL! ${"★".repeat(lastSummonedMedal.rarityStars)}",
                  style = MaterialTheme.typography.labelSmall,
                  color = SoulBreakMagenta
                )
                Text(
                  text = lastSummonedMedal.name,
                  style = MaterialTheme.typography.titleMedium,
                  color = Color.White
                )
                Text(
                  text = "${lastSummonedMedal.alignmentEnum.label} • ${lastSummonedMedal.attributeEnum.label} • ${lastSummonedMedal.elementEnum.label} • Guilt +${lastSummonedMedal.guiltBonusPercent}%",
                  style = MaterialTheme.typography.labelSmall,
                  color = RelicGold
                )
              }
            }
          }
        }
      }
    }

    // 2. Attribute Filter Row (All, Power, Speed, Magic)
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterPill(
          label = "ALL (${medals.size})",
          selected = filterAttribute == null,
          color = RelicGold,
          onClick = { filterAttribute = null }
        )
        MedalAttribute.entries.forEach { attr ->
          FilterPill(
            label = attr.label.uppercase(),
            selected = filterAttribute == attr,
            color = attr.color,
            onClick = { filterAttribute = attr }
          )
        }
      }
    }

    // 3. Medal Vault & Guilt Awakening Cards
    items(filteredMedals, key = { it.id }) { medal ->
      MedalVaultCard(
        medal = medal,
        onUpgradeGuilt = { onUpgradeMedalGuilt(medal) }
      )
    }
  }
}

@Composable
private fun FilterPill(
  label: String,
  selected: Boolean,
  color: Color,
  onClick: () -> Unit
) {
  Surface(
    color = if (selected) color.copy(alpha = 0.25f) else VoidNavy,
    shape = RoundedCornerShape(20.dp),
    border = BorderStroke(1.dp, if (selected) color else AstralBorder),
    modifier = Modifier.clickable(onClick = onClick)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = if (selected) color else MutedSilver,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
    )
  }
}

@Composable
private fun MedalVaultCard(
  medal: MedalEntity,
  onUpgradeGuilt: () -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = DeepCrystalBlue),
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, medal.attributeEnum.color.copy(alpha = 0.65f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("medal_card_${medal.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Medal Emblem Badge
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(medal.attributeEnum.color.copy(alpha = 0.2f))
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = medal.attributeEnum.shortCode,
            style = MaterialTheme.typography.labelLarge,
            color = medal.attributeEnum.color
          )
          Text(
            text = "${medal.rarityStars}★",
            style = MaterialTheme.typography.labelSmall,
            color = RelicGold,
            fontSize = 10.sp
          )
        }
      }

      Spacer(modifier = Modifier.width(10.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = medal.name,
          style = MaterialTheme.typography.titleMedium,
          color = Color.White
        )
        Text(
          text = "${medal.alignmentEnum.label} • ${medal.elementEnum.label} • ${medal.spCost} SP • ${medal.hitCount} Hits",
          style = MaterialTheme.typography.labelSmall,
          color = MutedSilver
        )
        Text(
          text = "Base ${"%.2f".format(medal.baseMultiplier)}x • Guilt Bonus +${medal.guiltBonusPercent}% • ${medal.effectTypeEnum.description}",
          style = MaterialTheme.typography.labelSmall,
          color = HpEmerald
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      OutlinedButton(
        onClick = onUpgradeGuilt,
        enabled = medal.guiltBonusPercent < 200 || medal.rarityStars < 7,
        border = BorderStroke(1.dp, RelicGold),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        modifier = Modifier.testTag("btn_upgrade_medal_${medal.id}")
      ) {
        Icon(Icons.Default.Upgrade, contentDescription = "Awaken Guilt", tint = RelicGold, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text("AWAKEN", style = MaterialTheme.typography.labelSmall, color = RelicGold)
      }
    }
  }
}
