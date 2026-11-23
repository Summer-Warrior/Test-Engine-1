package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AssetBindTargetType
import com.example.model.ExternalSearchResult
import com.example.model.HeroEntity
import com.example.model.ImportedAssetEntity
import com.example.model.ImportedAssetType
import com.example.model.ParsedMesh3D
import com.example.model.RealmMetadataEntity
import com.example.model.RealmStageEntity
import com.example.model.SfxCombatCue
import com.example.ui.theme.AstralBorder
import com.example.ui.theme.AstralPanel
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DeepCrystalBlue
import com.example.ui.theme.HpEmerald
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.MythrilCyan
import com.example.ui.theme.RelicGold
import com.example.ui.theme.SoulBreakMagenta
import com.example.ui.theme.SynergyAzure
import com.example.ui.theme.VoidNavy
import java.io.File

private enum class ImporterSourceTab(val label: String) {
  WIKI_SEARCH("Wiki & Art"),
  OPEN_CC0("Open CC0 / Packs"),
  DIRECT_URL("Direct URL"),
  LOCAL_SAF("Local / ZIP")
}

@Composable
fun AssetImporterSection(
  importedAssets: List<ImportedAssetEntity>,
  heroes: List<HeroEntity>,
  stages: List<RealmStageEntity>,
  realms: List<RealmMetadataEntity>,
  searchResults: List<ExternalSearchResult>,
  openCc0Catalog: List<ExternalSearchResult>,
  isLoading: Boolean,
  statusMessage: String?,
  inspectedMesh3D: ParsedMesh3D?,
  onSearchWiki: (query: String, wikiDomain: String) -> Unit,
  onImportSearchResult: (ExternalSearchResult) -> Unit,
  onImportDirectUrl: (url: String, type: ImportedAssetType?, name: String, cols: Int, rows: Int, fps: Int) -> Unit,
  onImportManifestUrl: (url: String) -> Unit,
  onImportLocalUri: (uri: Uri, type: ImportedAssetType?, cols: Int, rows: Int, fps: Int) -> Unit,
  onInspect3DModel: (ImportedAssetEntity) -> Unit,
  onClose3DViewer: () -> Unit,
  onPreviewAudio: (ImportedAssetEntity) -> Unit,
  onAutoBindAsset: (ImportedAssetEntity, AssetBindTargetType, Int?, SfxCombatCue) -> Unit,
  onDeleteAsset: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  var activeTab by remember { mutableStateOf(ImporterSourceTab.WIKI_SEARCH) }
  var wikiQuery by remember { mutableStateOf("Sora") }
  var wikiDomain by remember { mutableStateOf("kingdomhearts.fandom.com") }
  var manifestUrl by remember { mutableStateOf("") }
  var directUrl by remember { mutableStateOf("") }
  var directName by remember { mutableStateOf("") }
  var selectedType by remember { mutableStateOf<ImportedAssetType?>(null) }
  var sheetCols by remember { mutableIntStateOf(4) }
  var sheetRows by remember { mutableIntStateOf(1) }
  var sheetFps by remember { mutableIntStateOf(8) }
  var bindingAssetId by remember { mutableStateOf<Int?>(null) }

  val openDocLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
    onResult = { uri ->
      if (uri != null) onImportLocalUri(uri, selectedType, sheetCols, sheetRows, sheetFps)
    }
  )

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
    onResult = { uri ->
      if (uri != null) {
        val type = selectedType ?: if (sheetCols > 1 || sheetRows > 1) ImportedAssetType.SPRITESHEET else ImportedAssetType.SPRITE
        onImportLocalUri(uri, type, sheetCols, sheetRows, sheetFps)
      }
    }
  )

  Card(
    colors = CardDefaults.cardColors(containerColor = DeepCrystalBlue),
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.5.dp, MythrilCyan),
    modifier = modifier
      .fillMaxWidth()
      .testTag("external_asset_importer_card")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(Icons.Default.CloudDownload, contentDescription = "External Asset Importer", tint = MythrilCyan)
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "EXTERNAL ASSET IMPORTER",
              style = MaterialTheme.typography.titleLarge,
              color = MythrilCyan
            )
            Text(
              text = "Auto-import Sprites, Sheets, Concept Art, Wikis, 3D Models + Textures & Audio",
              style = MaterialTheme.typography.labelSmall,
              color = MutedSilver
            )
          }
        }
        if (isLoading) {
          CircularProgressIndicator(
            color = RelicGold,
            strokeWidth = 2.dp,
            modifier = Modifier.size(22.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(ImporterSourceTab.entries) { tab ->
          val selected = activeTab == tab
          Surface(
            color = if (selected) MythrilCyan.copy(alpha = 0.22f) else VoidNavy,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, if (selected) MythrilCyan else AstralBorder),
            modifier = Modifier
              .clickable { activeTab = tab }
              .testTag("importer_tab_${tab.name.lowercase()}")
          ) {
            Text(
              text = tab.label,
              style = MaterialTheme.typography.labelSmall,
              color = if (selected) MythrilCyan else MutedSilver,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
          }
        }
      }

      if (!statusMessage.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          color = VoidNavy,
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, RelicGold.copy(alpha = 0.7f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = statusMessage,
            style = MaterialTheme.typography.labelSmall,
            color = RelicGold,
            modifier = Modifier.padding(8.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      when (activeTab) {
        ImporterSourceTab.WIKI_SEARCH -> {
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
              "kingdomhearts.fandom.com" to "KH Wiki",
              "finalfantasy.fandom.com" to "FF Wiki",
              "commons.wikimedia.org" to "Commons",
              "en.wikipedia.org" to "Wikipedia"
            ).forEach { (domain, label) ->
              val active = wikiDomain == domain
              Surface(
                color = if (active) SynergyAzure.copy(alpha = 0.2f) else VoidNavy,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, if (active) SynergyAzure else AstralBorder),
                modifier = Modifier.clickable { wikiDomain = domain }
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelSmall,
                  color = if (active) SynergyAzure else MutedSilver,
                  fontSize = 10.sp,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = wikiQuery,
              onValueChange = { wikiQuery = it },
              label = { Text("Search Lore / Art") },
              singleLine = true,
              modifier = Modifier
                .weight(1f)
                .testTag("input_wiki_query")
            )
            OutlinedTextField(
              value = wikiDomain,
              onValueChange = { wikiDomain = it },
              label = { Text("Wiki Domain") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Button(
            onClick = { onSearchWiki(wikiQuery, wikiDomain) },
            colors = ButtonDefaults.buttonColors(containerColor = MythrilCyan, contentColor = VoidNavy),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_search_wiki")
          ) {
            Icon(Icons.Default.Search, contentDescription = "Search External Wiki")
            Spacer(modifier = Modifier.width(6.dp))
            Text("SEARCH WIKI LORE & CONCEPT ART", fontWeight = FontWeight.Bold)
          }

          if (searchResults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(searchResults, key = { it.id }) { res ->
                ExternalCatalogCard(
                  item = res,
                  onImport = { onImportSearchResult(res) }
                )
              }
            }
          }
        }

        ImporterSourceTab.OPEN_CC0 -> {
          Text(
            text = "1-Tap Open CC0 3D Models (.glb + textures), Concept Art & Audio:",
            style = MaterialTheme.typography.labelSmall,
            color = RelicGold
          )
          Spacer(modifier = Modifier.height(6.dp))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(openCc0Catalog, key = { it.id }) { item ->
              ExternalCatalogCard(
                item = item,
                onImport = { onImportSearchResult(item) }
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = manifestUrl,
            onValueChange = { manifestUrl = it },
            label = { Text("Custom JSON Asset Manifest URL") },
            placeholder = { Text("https://.../chronicle-pack.json") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_manifest_url")
          )
          Spacer(modifier = Modifier.height(6.dp))
          Button(
            onClick = { onImportManifestUrl(manifestUrl) },
            enabled = manifestUrl.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = RelicGold, contentColor = VoidNavy),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_import_manifest")
          ) {
            Icon(Icons.Default.Public, contentDescription = "Import JSON Manifest")
            Spacer(modifier = Modifier.width(6.dp))
            Text("AUTO-IMPORT JSON ASSET PACK", fontWeight = FontWeight.Bold)
          }
        }

        ImporterSourceTab.DIRECT_URL -> {
          OutlinedTextField(
            value = directUrl,
            onValueChange = { directUrl = it },
            label = { Text("Asset URL (.png, .glb, .gltf, .obj, .mp3, .ogg, .wav, .zip)") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_direct_url")
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = directName,
            onValueChange = { directName = it },
            label = { Text("Display Name (Optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(6.dp))
          AssetTypeSelectorRow(selectedType = selectedType, onSelect = { selectedType = it })
          if (selectedType == ImportedAssetType.SPRITESHEET || selectedType == ImportedAssetType.SPRITE) {
            SpritesheetGridControls(
              cols = sheetCols,
              rows = sheetRows,
              fps = sheetFps,
              onColsChange = { sheetCols = it },
              onRowsChange = { sheetRows = it },
              onFpsChange = { sheetFps = it }
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Button(
            onClick = { onImportDirectUrl(directUrl, selectedType, directName, sheetCols, sheetRows, sheetFps) },
            enabled = directUrl.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = HpEmerald, contentColor = VoidNavy),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_import_direct_url")
          ) {
            Icon(Icons.Default.Link, contentDescription = "Import from URL")
            Spacer(modifier = Modifier.width(6.dp))
            Text("FETCH & AUTO-IMPORT ASSET", fontWeight = FontWeight.Bold)
          }
        }

        ImporterSourceTab.LOCAL_SAF -> {
          Text(
            text = "Zero-Permission Device Import (Downloads, Drive, ZIP packs, Photo Picker):",
            style = MaterialTheme.typography.labelSmall,
            color = MutedSilver
          )
          Spacer(modifier = Modifier.height(6.dp))
          AssetTypeSelectorRow(selectedType = selectedType, onSelect = { selectedType = it })
          SpritesheetGridControls(
            cols = sheetCols,
            rows = sheetRows,
            fps = sheetFps,
            onColsChange = { sheetCols = it },
            onRowsChange = { sheetRows = it },
            onFpsChange = { sheetFps = it }
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = {
                openDocLauncher.launch(
                  arrayOf("application/zip", "image/*", "audio/*", "model/*", "application/json", "*/*")
                )
              },
              colors = ButtonDefaults.buttonColors(containerColor = MythrilCyan, contentColor = VoidNavy),
              modifier = Modifier
                .weight(1f)
                .testTag("btn_pick_local_zip_or_file")
            ) {
              Icon(Icons.Default.FolderOpen, contentDescription = "Pick File or ZIP")
              Spacer(modifier = Modifier.width(4.dp))
              Text("FILE / 3D / ZIP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
              onClick = {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              border = BorderStroke(1.dp, RelicGold),
              modifier = Modifier
                .weight(1f)
                .testTag("btn_pick_visual_media")
            ) {
              Icon(Icons.Default.Image, contentDescription = "Pick Sprite or Concept Art", tint = RelicGold)
              Spacer(modifier = Modifier.width(4.dp))
              Text("SPRITE / ART", color = RelicGold, fontSize = 11.sp)
            }
          }
        }
      }

      if (inspectedMesh3D != null) {
        Spacer(modifier = Modifier.height(10.dp))
        Mesh3DTurntableViewer(mesh = inspectedMesh3D, onClose = onClose3DViewer)
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "IMPORTED ASSET VAULT (${importedAssets.size}) — AUTO-BIND TO ENGINE",
        style = MaterialTheme.typography.titleMedium,
        color = RelicGold
      )

      if (importedAssets.isEmpty()) {
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          color = VoidNavy,
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, AstralBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "No external assets imported yet. Search a Wiki, tap an Open CC0 sample, paste a URL, or pick a local ZIP/GLB/Sprite above.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedSilver,
            modifier = Modifier.padding(10.dp)
          )
        }
      } else {
        Spacer(modifier = Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          importedAssets.forEach { asset ->
            ImportedAssetVaultRow(
              asset = asset,
              isBindPanelOpen = bindingAssetId == asset.id,
              heroes = heroes,
              stages = stages,
              realms = realms,
              onToggleBindPanel = {
                bindingAssetId = if (bindingAssetId == asset.id) null else asset.id
              },
              onInspect3D = { onInspect3DModel(asset) },
              onPlayAudio = { onPreviewAudio(asset) },
              onBind = { targetType, targetId, sfxCue ->
                onAutoBindAsset(asset, targetType, targetId, sfxCue)
                bindingAssetId = null
              },
              onDelete = { onDeleteAsset(asset.id) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ExternalCatalogCard(
  item: ExternalSearchResult,
  onImport: () -> Unit
) {
  Surface(
    color = VoidNavy,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, AstralBorder),
    modifier = Modifier.width(210.dp)
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      if (item.previewImageUrl.isNotBlank()) {
        AsyncImage(
          model = item.previewImageUrl,
          contentDescription = item.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(6.dp))
      }
      Text(
        text = item.title,
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = "${item.assetType.label} • ${item.subtitle}",
        style = MaterialTheme.typography.labelSmall,
        color = MythrilCyan,
        maxLines = 1
      )
      Text(
        text = item.description,
        style = MaterialTheme.typography.labelSmall,
        color = MutedSilver,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        fontSize = 10.sp,
        modifier = Modifier.padding(vertical = 3.dp)
      )
      Button(
        onClick = onImport,
        colors = ButtonDefaults.buttonColors(containerColor = RelicGold, contentColor = VoidNavy),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("IMPORT ASSET", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun AssetTypeSelectorRow(
  selectedType: ImportedAssetType?,
  onSelect: (ImportedAssetType?) -> Unit
) {
  LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
    item {
      val autoSelected = selectedType == null
      Surface(
        color = if (autoSelected) RelicGold.copy(alpha = 0.2f) else VoidNavy,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, if (autoSelected) RelicGold else AstralBorder),
        modifier = Modifier.clickable { onSelect(null) }
      ) {
        Text(
          text = "Auto-Detect",
          style = MaterialTheme.typography.labelSmall,
          color = if (autoSelected) RelicGold else MutedSilver,
          fontSize = 10.sp,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
      }
    }
    items(ImportedAssetType.entries) { t ->
      val active = selectedType == t
      Surface(
        color = if (active) MythrilCyan.copy(alpha = 0.2f) else VoidNavy,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, if (active) MythrilCyan else AstralBorder),
        modifier = Modifier.clickable { onSelect(t) }
      ) {
        Text(
          text = t.label,
          style = MaterialTheme.typography.labelSmall,
          color = if (active) MythrilCyan else MutedSilver,
          fontSize = 10.sp,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
      }
    }
  }
}

@Composable
private fun SpritesheetGridControls(
  cols: Int,
  rows: Int,
  fps: Int,
  onColsChange: (Int) -> Unit,
  onRowsChange: (Int) -> Unit,
  onFpsChange: (Int) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    StepperChip("Cols: $cols", onDec = { onColsChange((cols - 1).coerceAtLeast(1)) }, onInc = { onColsChange((cols + 1).coerceAtMost(16)) })
    StepperChip("Rows: $rows", onDec = { onRowsChange((rows - 1).coerceAtLeast(1)) }, onInc = { onRowsChange((rows + 1).coerceAtMost(16)) })
    StepperChip("FPS: $fps", onDec = { onFpsChange((fps - 2).coerceAtLeast(1)) }, onInc = { onFpsChange((fps + 2).coerceAtMost(24)) })
  }
}

@Composable
private fun StepperChip(label: String, onDec: () -> Unit, onInc: () -> Unit) {
  Surface(
    color = VoidNavy,
    shape = RoundedCornerShape(8.dp),
    border = BorderStroke(1.dp, AstralBorder)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
      Text("-", color = MythrilCyan, modifier = Modifier.clickable(onClick = onDec).padding(horizontal = 4.dp))
      Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 10.sp)
      Text("+", color = MythrilCyan, modifier = Modifier.clickable(onClick = onInc).padding(horizontal = 4.dp))
    }
  }
}

@Composable
private fun ImportedAssetVaultRow(
  asset: ImportedAssetEntity,
  isBindPanelOpen: Boolean,
  heroes: List<HeroEntity>,
  stages: List<RealmStageEntity>,
  realms: List<RealmMetadataEntity>,
  onToggleBindPanel: () -> Unit,
  onInspect3D: () -> Unit,
  onPlayAudio: () -> Unit,
  onBind: (AssetBindTargetType, Int?, SfxCombatCue) -> Unit,
  onDelete: () -> Unit
) {
  Surface(
    color = VoidNavy,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(
      1.dp,
      if (asset.boundTargetTypeEnum != AssetBindTargetType.NONE) HpEmerald else AstralBorder
    ),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("imported_asset_row_${asset.id}")
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        when (asset.assetTypeEnum) {
          ImportedAssetType.SPRITE, ImportedAssetType.SPRITESHEET -> {
            if (asset.localFilePath.isNotBlank() && File(asset.localFilePath).exists()) {
              AnimatedSpritesheetPreview(
                filePath = asset.localFilePath,
                columns = asset.sheetColumns,
                rows = asset.sheetRows,
                fps = asset.sheetFps,
                contentDescription = asset.name,
                modifier = Modifier.size(48.dp)
              )
            }
          }
          ImportedAssetType.CONCEPT_ART, ImportedAssetType.TEXTURE -> {
            val imgModel = if (asset.localFilePath.isNotBlank() && File(asset.localFilePath).exists()) {
              File(asset.localFilePath)
            } else asset.sourceUrl
            AsyncImage(
              model = imgModel,
              contentDescription = asset.name,
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
            )
          }
          ImportedAssetType.MODEL_3D -> {
            Icon(Icons.Default.ViewInAr, contentDescription = "3D Model", tint = MythrilCyan, modifier = Modifier.size(36.dp))
          }
          ImportedAssetType.AUDIO_BGM, ImportedAssetType.AUDIO_SFX -> {
            Icon(Icons.Default.MusicNote, contentDescription = "Audio Track", tint = SoulBreakMagenta, modifier = Modifier.size(36.dp))
          }
          ImportedAssetType.WIKI_LORE -> {
            Icon(Icons.Default.Public, contentDescription = "Wiki Lore", tint = RelicGold, modifier = Modifier.size(36.dp))
          }
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(asset.name, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
          val metaText = buildString {
            append("${asset.assetTypeEnum.label} • ${asset.sourceTypeEnum.label}")
            if (asset.assetTypeEnum == ImportedAssetType.MODEL_3D) append(" • ${asset.vertexCount}v/${asset.triangleCount}t")
            if (asset.assetTypeEnum == ImportedAssetType.SPRITESHEET) append(" • ${asset.sheetColumns}×${asset.sheetRows} @${asset.sheetFps}fps")
          }
          Text(metaText, style = MaterialTheme.typography.labelSmall, color = MutedSilver, fontSize = 10.sp)
          if (asset.boundTargetTypeEnum != AssetBindTargetType.NONE) {
            Text(
              text = "Bound → ${asset.boundTargetTypeEnum.label} #${asset.boundTargetId ?: asset.boundSfxCue}",
              style = MaterialTheme.typography.labelSmall,
              color = HpEmerald,
              fontSize = 10.sp
            )
          }
        }

        if (asset.assetTypeEnum == ImportedAssetType.MODEL_3D) {
          IconButton(onClick = onInspect3D) {
            Icon(Icons.Default.ViewInAr, contentDescription = "Open 3D Turntable", tint = MythrilCyan)
          }
        }
        if (asset.assetTypeEnum == ImportedAssetType.AUDIO_BGM || asset.assetTypeEnum == ImportedAssetType.AUDIO_SFX) {
          IconButton(onClick = onPlayAudio) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Audition Audio", tint = HpEmerald)
          }
        }
        OutlinedButton(
          onClick = onToggleBindPanel,
          border = BorderStroke(1.dp, RelicGold),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(if (isBindPanelOpen) "Done" else "Bind", style = MaterialTheme.typography.labelSmall, color = RelicGold)
        }
        IconButton(onClick = onDelete) {
          Icon(Icons.Default.Delete, contentDescription = "Delete Asset", tint = DangerCrimson)
        }
      }

      if (isBindPanelOpen) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          color = AstralPanel,
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, RelicGold.copy(alpha = 0.6f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Auto-Bind to Live Game Engine:", style = MaterialTheme.typography.labelSmall, color = RelicGold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(heroes, key = { "h_${it.id}" }) { hero ->
                OutlinedButton(
                  onClick = { onBind(AssetBindTargetType.HERO, hero.id, SfxCombatCue.ATTACK) },
                  border = BorderStroke(1.dp, MythrilCyan),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                  Text("Hero: ${hero.name}", style = MaterialTheme.typography.labelSmall, color = MythrilCyan, fontSize = 10.sp)
                }
              }
              items(stages, key = { "s_${it.id}" }) { st ->
                OutlinedButton(
                  onClick = { onBind(AssetBindTargetType.BOSS_STAGE, st.id, SfxCombatCue.BOSS_HIT) },
                  border = BorderStroke(1.dp, SoulBreakMagenta),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                  Text("Stage: ${st.stageCode}", style = MaterialTheme.typography.labelSmall, color = SoulBreakMagenta, fontSize = 10.sp)
                }
              }
              items(realms, key = { "r_${it.id}" }) { r ->
                OutlinedButton(
                  onClick = { onBind(AssetBindTargetType.REALM, r.id, SfxCombatCue.ATTACK) },
                  border = BorderStroke(1.dp, SynergyAzure),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                  Text("Realm: ${r.realmName}", style = MaterialTheme.typography.labelSmall, color = SynergyAzure, fontSize = 10.sp)
                }
              }
              if (asset.assetTypeEnum == ImportedAssetType.AUDIO_SFX) {
                items(SfxCombatCue.entries) { cue ->
                  OutlinedButton(
                    onClick = { onBind(AssetBindTargetType.COMBAT_SFX, null, cue) },
                    border = BorderStroke(1.dp, HpEmerald),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                  ) {
                    Text("SFX: ${cue.label}", style = MaterialTheme.typography.labelSmall, color = HpEmerald, fontSize = 10.sp)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
