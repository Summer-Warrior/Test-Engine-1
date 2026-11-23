package com.example

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.assets.ExternalAssetImportService
import com.example.assets.Mesh3DParser
import com.example.data.AppDatabase
import com.example.data.RpgRepository
import com.example.di.AppModule
import com.example.engine.RpgEngineViewModel
import com.example.model.AssetBindTargetType
import com.example.model.ExternalSourceType
import com.example.model.ImportedAssetEntity
import com.example.model.ImportedAssetType
import com.example.model.SfxCombatCue
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExternalAssetImportTest {
  private lateinit var context: Context
  private lateinit var database: AppDatabase
  private lateinit var repository: RpgRepository
  private lateinit var importService: ExternalAssetImportService
  private lateinit var tempDir: File

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    database = AppModule.provideInMemoryDatabase(context)
    repository = RpgRepository(database)
    importService = ExternalAssetImportService(context)
    tempDir = File(context.cacheDir, "test_assets_${System.nanoTime()}").apply { mkdirs() }
  }

  @After
  fun tearDown() {
    tempDir.deleteRecursively()
    database.close()
  }

  @Test
  fun `Mesh3DParser parses OBJ with MTL texture lookup and valid binary GLB`() {
    // 1. Create OBJ + MTL + sibling PNG
    val texFile = File(tempDir, "keyblade_diffuse.png").apply {
      writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))
    }
    File(tempDir, "keyblade.mtl").writeText(
      """
      newmtl KeyMat
      map_Kd keyblade_diffuse.png
      """.trimIndent()
    )
    val objFile = File(tempDir, "keyblade.obj").apply {
      writeText(
        """
        mtllib keyblade.mtl
        v 0.0 1.0 0.0
        v -1.0 -1.0 0.0
        v 1.0 -1.0 0.0
        vt 0.5 1.0
        vt 0.0 0.0
        vt 1.0 0.0
        f 1/1 2/2 3/3
        """.trimIndent()
      )
    }

    val objMesh = Mesh3DParser.parseFile(objFile)
    assertEquals(3, objMesh.vertexCount)
    assertEquals(1, objMesh.triangleCount)
    assertEquals(texFile.absolutePath, objMesh.texturePath)
    assertNotNull(objMesh.uvs)

    // 2. Construct valid minimal GLB binary (single triangle)
    val glbFile = File(tempDir, "crystal.glb")
    val binBytes = ByteArray(3 * 3 * 4 + 3 * 2).also { arr ->
      val bb = ByteBuffer.wrap(arr).order(ByteOrder.LITTLE_ENDIAN)
      // 3 vertices (9 floats)
      bb.putFloat(0f); bb.putFloat(1f); bb.putFloat(0f)
      bb.putFloat(-1f); bb.putFloat(0f); bb.putFloat(0f)
      bb.putFloat(1f); bb.putFloat(0f); bb.putFloat(0f)
      // 3 unsigned short indices
      bb.putShort(0); bb.putShort(1); bb.putShort(2)
    }
    val paddedBinSize = (binBytes.size + 3) and 3.inv()
    val paddedBin = binBytes.copyOf(paddedBinSize)

    val gltfJson = """
      {
        "asset":{"version":"2.0"},
        "buffers":[{"byteLength":${binBytes.size}}],
        "bufferViews":[
          {"buffer":0,"byteOffset":0,"byteLength":36},
          {"buffer":0,"byteOffset":36,"byteLength":6}
        ],
        "accessors":[
          {"bufferView":0,"componentType":5126,"count":3,"type":"VEC3"},
          {"bufferView":1,"componentType":5123,"count":3,"type":"SCALAR"}
        ],
        "meshes":[{"primitives":[{"attributes":{"POSITION":0},"indices":1}]}]
      }
    """.trimIndent().toByteArray(Charsets.UTF_8)
    val paddedJsonSize = (gltfJson.size + 3) and 3.inv()
    val paddedJson = ByteArray(paddedJsonSize) { ' '.code.toByte() }
    System.arraycopy(gltfJson, 0, paddedJson, 0, gltfJson.size)

    val totalLength = 12 + 8 + paddedJsonSize + 8 + paddedBinSize
    val out = ByteArrayOutputStream()
    fun putIntLE(v: Int) {
      val b = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array()
      out.write(b)
    }
    putIntLE(0x46546C67) // glTF
    putIntLE(2)
    putIntLE(totalLength)
    putIntLE(paddedJsonSize)
    putIntLE(0x4E4F534A) // JSON
    out.write(paddedJson)
    putIntLE(paddedBinSize)
    putIntLE(0x004E4942) // BIN
    out.write(paddedBin)
    glbFile.writeBytes(out.toByteArray())

    val glbMesh = Mesh3DParser.parseFile(glbFile)
    assertEquals(3, glbMesh.vertexCount)
    assertEquals(1, glbMesh.triangleCount)

    // 3. Fallback crystal mesh sanity check
    val fallback = Mesh3DParser.createFallbackCrystalMesh("Fallback")
    assertEquals(6, fallback.vertexCount)
    assertEquals(8, fallback.triangleCount)
  }

  @Test
  fun `unpackZipArchive imports allowed assets and blocks executable payloads`() {
    val zipFile = File(tempDir, "nexus_pack.zip")
    ZipOutputStream(zipFile.outputStream()).use { zos ->
      zos.putNextEntry(ZipEntry("hero_blade.obj"))
      zos.write("v 0 1 0\nv -1 0 0\nv 1 0 0\nf 1 2 3\n".toByteArray())
      zos.closeEntry()

      zos.putNextEntry(ZipEntry("hero_blade_albedo.png"))
      zos.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))
      zos.closeEntry()

      zos.putNextEntry(ZipEntry("battle_hit.wav"))
      zos.write(byteArrayOf(0x52, 0x49, 0x46, 0x46))
      zos.closeEntry()

      zos.putNextEntry(ZipEntry("lore_note.json"))
      zos.write("""{"name":"Kingdom Lore","lore":"A chronicle of light and darkness."}""".toByteArray())
      zos.closeEntry()

      // Executable file must be ignored/blocked (Play Integrity / System Integrity compliance)
      zos.putNextEntry(ZipEntry("malicious_payload.dex"))
      zos.write(byteArrayOf(1, 2, 3, 4))
      zos.closeEntry()

      zos.putNextEntry(ZipEntry("native_evil.so"))
      zos.write(byteArrayOf(1, 2, 3, 4))
      zos.closeEntry()
    }

    val entities = importService.unpackZipArchive(
      zipFile = zipFile,
      sourceType = ExternalSourceType.ZIP_PACK,
      sourceUrl = "content://test/nexus_pack.zip"
    )

    assertTrue(entities.any { it.assetTypeEnum == ImportedAssetType.MODEL_3D && it.linkedTexturePath.endsWith(".png") })
    assertTrue(entities.any { it.assetTypeEnum == ImportedAssetType.AUDIO_SFX && it.name == "battle_hit" })
    assertTrue(entities.any { it.assetTypeEnum == ImportedAssetType.WIKI_LORE && it.wikiSummary.contains("chronicle") })
    assertFalse(entities.any { it.localFilePath.endsWith(".dex") || it.localFilePath.endsWith(".so") })
  }

  @Test
  fun `autoBindAsset binds sprite to Hero, backdrop to Realm, and cue to Combat SFX`() = runBlocking {
    repository.ensureSeeded()
    val viewModel = RpgEngineViewModel(repository)
    delay(250)

    val hero = database.heroDao().observeHeroes().first().first()
    val realm = database.realmMetadataDao().observeRealmMetadata().first().first()

    val spriteId = repository.insertImportedAsset(
      ImportedAssetEntity(
        name = "Custom Hero Sheet",
        assetType = ImportedAssetType.SPRITESHEET.name,
        sourceType = ExternalSourceType.LOCAL_SAF.name,
        localFilePath = "/tmp/hero_sheet.png",
        sheetColumns = 4,
        sheetRows = 2,
        sheetFps = 8
      )
    ).toInt()
    val spriteEntity = database.importedAssetDao().getAssetById(spriteId)!!

    val artId = repository.insertImportedAsset(
      ImportedAssetEntity(
        name = "Nebula Backdrop",
        assetType = ImportedAssetType.CONCEPT_ART.name,
        sourceType = ExternalSourceType.OPEN_CC0.name,
        sourceUrl = "https://example.com/nebula.jpg",
        wikiSummary = "Astral realm carved from starlight."
      )
    ).toInt()
    val artEntity = database.importedAssetDao().getAssetById(artId)!!

    val sfxId = repository.insertImportedAsset(
      ImportedAssetEntity(
        name = "Slash Impact",
        assetType = ImportedAssetType.AUDIO_SFX.name,
        sourceType = ExternalSourceType.OPEN_CC0.name,
        localFilePath = "/tmp/slash.wav"
      )
    ).toInt()
    val sfxEntity = database.importedAssetDao().getAssetById(sfxId)!!

    viewModel.autoBindAsset(spriteEntity, AssetBindTargetType.HERO, hero.id, SfxCombatCue.ATTACK)
    viewModel.autoBindAsset(artEntity, AssetBindTargetType.REALM, realm.id, SfxCombatCue.ATTACK)
    viewModel.autoBindAsset(sfxEntity, AssetBindTargetType.COMBAT_SFX, null, SfxCombatCue.SOUL_BREAK)
    repeat(5) {
      shadowOf(Looper.getMainLooper()).idle()
      delay(80)
    }
    shadowOf(Looper.getMainLooper()).idle()

    val updatedHero = database.heroDao().getHeroById(hero.id)!!
    assertEquals(spriteId, updatedHero.spriteAssetId)

    val updatedRealm = database.realmMetadataDao().getRealmById(realm.id)!!
    assertEquals("https://example.com/nebula.jpg", updatedRealm.bannerArtUri)
    assertTrue(updatedRealm.worldLore.contains("starlight"))

    val updatedSfx = database.importedAssetDao().getAssetById(sfxId)!!
    assertEquals(AssetBindTargetType.COMBAT_SFX.name, updatedSfx.boundTargetType)
    assertEquals(SfxCombatCue.SOUL_BREAK.name, updatedSfx.boundSfxCue)
  }
}
