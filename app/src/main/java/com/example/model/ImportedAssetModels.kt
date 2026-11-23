package com.example.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ImportedAssetType(val label: String) {
  SPRITE("Sprite"),
  SPRITESHEET("Spritesheet"),
  CONCEPT_ART("Concept Art"),
  WIKI_LORE("Wiki Lore"),
  MODEL_3D("3D Model"),
  TEXTURE("Texture"),
  AUDIO_BGM("Music (BGM)"),
  AUDIO_SFX("Sound Effect")
}

enum class ExternalSourceType(val label: String) {
  WIKI_API("Wiki / Commons API"),
  OPEN_CC0("Open CC0 Catalog"),
  MANIFEST_JSON("JSON Manifest"),
  DIRECT_URL("Direct URL"),
  LOCAL_SAF("Local Device File"),
  ZIP_PACK("ZIP Asset Pack")
}

enum class AssetBindTargetType(val label: String) {
  NONE("Unbound"),
  HERO("Hero"),
  BOSS_STAGE("Boss Stage"),
  REALM("Realm"),
  EQUIPMENT("Relic / 3D"),
  COMBAT_SFX("Combat SFX")
}

enum class SfxCombatCue(val label: String) {
  ATTACK("Basic Attack"),
  MEDAL_BURST("Medal Burst"),
  SOUL_BREAK("Soul Break"),
  BOSS_HIT("Boss Attack")
}

/**
 * Room Entity storing externally imported sprites, spritesheets, concept art,
 * wiki lore entries, 3D models (.glb/.gltf/.obj) + linked textures, and BGM/SFX audio.
 */
@Entity(
  tableName = "imported_assets",
  indices = [
    Index(value = ["assetType"]),
    Index(value = ["boundTargetType", "boundTargetId"])
  ]
)
data class ImportedAssetEntity(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val name: String,
  val assetType: String, // ImportedAssetType.name
  val sourceType: String, // ExternalSourceType.name
  val sourceUrl: String = "",
  val localFilePath: String = "",
  val linkedTexturePath: String = "",
  val wikiSummary: String = "",
  val attribution: String = "User / Public Source",
  val sheetColumns: Int = 1,
  val sheetRows: Int = 1,
  val sheetFps: Int = 6,
  val vertexCount: Int = 0,
  val triangleCount: Int = 0,
  val boundTargetType: String = AssetBindTargetType.NONE.name,
  val boundTargetId: Int? = null,
  val boundSfxCue: String = SfxCombatCue.ATTACK.name,
  val createdAtMillis: Long = System.currentTimeMillis()
) {
  val assetTypeEnum: ImportedAssetType
    get() = ImportedAssetType.entries.find { it.name == assetType } ?: ImportedAssetType.SPRITE

  val sourceTypeEnum: ExternalSourceType
    get() = ExternalSourceType.entries.find { it.name == sourceType } ?: ExternalSourceType.DIRECT_URL

  val boundTargetTypeEnum: AssetBindTargetType
    get() = AssetBindTargetType.entries.find { it.name == boundTargetType } ?: AssetBindTargetType.NONE

  val boundSfxCueEnum: SfxCombatCue
    get() = SfxCombatCue.entries.find { it.name == boundSfxCue } ?: SfxCombatCue.ATTACK
}

data class ExternalSearchResult(
  val id: String,
  val title: String,
  val subtitle: String,
  val description: String,
  val assetType: ImportedAssetType,
  val sourceType: ExternalSourceType,
  val previewImageUrl: String = "",
  val assetUrl: String = "",
  val pageUrl: String = "",
  val attribution: String = "",
  val sheetColumns: Int = 1,
  val sheetRows: Int = 1,
  val sheetFps: Int = 6
)

class ParsedMesh3D(
  val name: String,
  val vertices: FloatArray, // x,y,z triplets normalized to unit sphere
  val indices: IntArray,    // triangle index triplets
  val uvs: FloatArray?,     // optional u,v pairs per vertex
  val vertexCount: Int,
  val triangleCount: Int,
  val boundsRadius: Float = 1f,
  val texturePath: String = ""
)
