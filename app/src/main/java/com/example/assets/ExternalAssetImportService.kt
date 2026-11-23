package com.example.assets

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.ExternalSearchResult
import com.example.model.ExternalSourceType
import com.example.model.ImportedAssetEntity
import com.example.model.ImportedAssetType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Handles external discovery and automatic local caching of sprites, spritesheets,
 * concept art, wiki lore, 3D models (.glb/.gltf/.obj) + textures, and BGM/SFX audio
 * from public MediaWiki/Commons APIs, Open CC0 catalogs, JSON manifests, direct URLs,
 * and zero-permission Android SAF / ZIP archives.
 */
@Singleton
class ExternalAssetImportService @Inject constructor(
  @ApplicationContext private val context: Context
) {
  private val json = Json { ignoreUnknownKeys = true; isLenient = true }
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .followRedirects(true)
    .build()

  private val allowedImageExts = setOf("png", "jpg", "jpeg", "webp", "gif", "bmp")
  private val allowedModelExts = setOf("glb", "gltf", "obj")
  private val allowedAudioExts = setOf("mp3", "wav", "ogg", "m4a", "aac", "flac")
  private val allowedSupportingExts = setOf("mtl", "bin", "json", "txt", "md")
  private val blockedExecutableExts = setOf("dex", "jar", "so", "apk", "exe", "sh", "bat")

  private fun importRootDir(): File {
    return File(context.filesDir, "imported_assets").apply { mkdirs() }
  }

  /**
   * Queries any MediaWiki-compatible API (Wikimedia Commons, Wikipedia, Fandom RPG Wikis)
   * for lore extracts, canonical article URLs, and concept art thumbnails.
   */
  suspend fun searchWikisAndConceptArt(
    query: String,
    wikiDomain: String
  ): Result<List<ExternalSearchResult>> = withContext(Dispatchers.IO) {
    runCatching {
      val cleanQuery = query.trim().ifBlank { "Keyblade" }
      val cleanDomain = wikiDomain.trim()
        .removePrefix("https://")
        .removePrefix("http://")
        .trimEnd('/')
        .ifBlank { "commons.wikimedia.org" }

      val apiPath = if (cleanDomain.contains("wikipedia.org") || cleanDomain.contains("wikimedia.org")) {
        "w/api.php"
      } else {
        "api.php"
      }
      val encodedQ = URLEncoder.encode(cleanQuery, "UTF-8")
      val url = "https://$cleanDomain/$apiPath?action=query&generator=search&gsrsearch=$encodedQ" +
        "&gsrlimit=12&prop=pageimages|extracts|info&piprop=thumbnail|original&pithumbsize=512" +
        "&exintro=1&explaintext=1&exsentences=4&inprop=url&format=json&origin=*"

      val request = Request.Builder()
        .url(url)
        .header("User-Agent", "ChronicleNexusRPG/1.0 (Android Asset Importer)")
        .build()

      client.newCall(request).execute().use { resp ->
        if (!resp.isSuccessful) {
          throw IllegalStateException("Wiki API returned HTTP ${resp.code}")
        }
        val body = resp.body?.string().orEmpty()
        val root = json.parseToJsonElement(body).jsonObject
        val pagesObj = root["query"]?.jsonObject?.get("pages")?.jsonObject ?: return@runCatching emptyList()

        pagesObj.values.mapNotNull { pageEl ->
          val page = pageEl.jsonObject
          val title = page["title"]?.jsonPrimitive?.content ?: return@mapNotNull null
          val extract = page["extract"]?.jsonPrimitive?.content?.trim().orEmpty()
          val pageUrl = page["fullurl"]?.jsonPrimitive?.content.orEmpty()
          val thumbUrl = page["thumbnail"]?.jsonObject?.get("source")?.jsonPrimitive?.content.orEmpty()
          val originalUrl = page["original"]?.jsonObject?.get("source")?.jsonPrimitive?.content.orEmpty()
          val bestImg = originalUrl.ifBlank { thumbUrl }

          ExternalSearchResult(
            id = "wiki_${cleanDomain}_${page["pageid"]?.jsonPrimitive?.content ?: title.hashCode()}",
            title = title.removePrefix("File:"),
            subtitle = cleanDomain,
            description = extract.ifBlank { "MediaWiki entry from $cleanDomain" },
            assetType = if (bestImg.isNotBlank() && extract.isBlank()) {
              ImportedAssetType.CONCEPT_ART
            } else if (bestImg.isNotBlank()) {
              ImportedAssetType.CONCEPT_ART
            } else {
              ImportedAssetType.WIKI_LORE
            },
            sourceType = ExternalSourceType.WIKI_API,
            previewImageUrl = thumbUrl.ifBlank { bestImg },
            assetUrl = bestImg,
            pageUrl = pageUrl,
            attribution = "Source: $cleanDomain"
          )
        }
      }
    }
  }

  /**
   * Curated public open-source / CC0 sample catalog (Khronos glTF 3D models & Wikimedia Commons audio/art)
   * ready for instant 1-tap auto-import testing with zero API keys.
   */
  fun getOpenCc0Catalog(): List<ExternalSearchResult> = listOf(
    ExternalSearchResult(
      id = "cc0_glb_lantern",
      title = "Antique Astral Lantern (3D GLB)",
      subtitle = "Khronos glTF Sample Assets • CC0",
      description = "Binary glTF 2.0 (.glb) prop with embedded mesh and texture data ready for 3D Turntable preview.",
      assetType = ImportedAssetType.MODEL_3D,
      sourceType = ExternalSourceType.OPEN_CC0,
      previewImageUrl = "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/Lantern/screenshot/screenshot.png",
      assetUrl = "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/Lantern/glTF-Binary/Lantern.glb",
      attribution = "KhronosGroup glTF-Sample-Assets (CC0)"
    ),
    ExternalSearchResult(
      id = "cc0_glb_helmet",
      title = "Sci-Fi Relic Helmet (3D GLB)",
      subtitle = "Khronos glTF Sample Assets • CC0",
      description = "High-detail PBR 3D helmet model (.glb) with embedded geometry and diffuse textures.",
      assetType = ImportedAssetType.MODEL_3D,
      sourceType = ExternalSourceType.OPEN_CC0,
      previewImageUrl = "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/DamagedHelmet/screenshot/screenshot.png",
      assetUrl = "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/DamagedHelmet/glTF-Binary/DamagedHelmet.glb",
      attribution = "KhronosGroup glTF-Sample-Assets (CC0)"
    ),
    ExternalSearchResult(
      id = "cc0_glb_duck",
      title = "Golden Companion Duck (3D GLB)",
      subtitle = "Khronos glTF Sample Assets • CC0",
      description = "Lightweight test 3D character prop (.glb) with embedded texture map.",
      assetType = ImportedAssetType.MODEL_3D,
      sourceType = ExternalSourceType.OPEN_CC0,
      previewImageUrl = "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/Duck/screenshot/screenshot.png",
      assetUrl = "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/Duck/glTF-Binary/Duck.glb",
      attribution = "KhronosGroup glTF-Sample-Assets (CC0)"
    ),
    ExternalSearchResult(
      id = "cc0_art_nebula",
      title = "Carina Astral Realm Backdrop",
      subtitle = "Wikimedia Commons / NASA • Public Domain",
      description = "High-resolution cosmic nebula concept backdrop suitable for Realm Map & Battle Stage arenas.",
      assetType = ImportedAssetType.CONCEPT_ART,
      sourceType = ExternalSourceType.OPEN_CC0,
      previewImageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/bf/Webb%27s_First_Deep_Field.jpg/640px-Webb%27s_First_Deep_Field.jpg",
      assetUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/bf/Webb%27s_First_Deep_Field.jpg/1024px-Webb%27s_First_Deep_Field.jpg",
      attribution = "NASA / ESA / CSA (Public Domain via Wikimedia Commons)"
    ),
    ExternalSearchResult(
      id = "cc0_bgm_moonlight",
      title = "Astral Sanctum Piano BGM",
      subtitle = "Wikimedia Commons • Public Domain OGG",
      description = "Loopable classical orchestral/piano theme track ready to bind as Realm or Stage BGM.",
      assetType = ImportedAssetType.AUDIO_BGM,
      sourceType = ExternalSourceType.OPEN_CC0,
      assetUrl = "https://upload.wikimedia.org/wikipedia/commons/e/eb/Beethoven_Moonlight_1st_movement.ogg",
      attribution = "Wikimedia Commons (Public Domain)"
    ),
    ExternalSearchResult(
      id = "cc0_sfx_chime",
      title = "Crystal Chime Strike SFX",
      subtitle = "Wikimedia Commons • CC-BY / Public Domain",
      description = "Crisp metallic/crystal chime audio effect ready to bind to Hero Attacks or Medal Bursts.",
      assetType = ImportedAssetType.AUDIO_SFX,
      sourceType = ExternalSourceType.OPEN_CC0,
      assetUrl = "https://upload.wikimedia.org/wikipedia/commons/3/3c/Glass_harp_C5.ogg",
      attribution = "Wikimedia Commons"
    )
  )

  suspend fun importSearchResult(result: ExternalSearchResult): Result<ImportedAssetEntity> =
    withContext(Dispatchers.IO) {
      runCatching {
        if (result.assetType == ImportedAssetType.WIKI_LORE && result.assetUrl.isBlank()) {
          return@runCatching ImportedAssetEntity(
            name = result.title,
            assetType = ImportedAssetType.WIKI_LORE.name,
            sourceType = result.sourceType.name,
            sourceUrl = result.pageUrl,
            wikiSummary = result.description,
            attribution = result.attribution
          )
        }

        val targetUrl = result.assetUrl.ifBlank { result.previewImageUrl }
        val localFile = if (targetUrl.isNotBlank()) {
          downloadUrlToInternalFile(targetUrl, result.title, guessExtension(targetUrl, result.assetType))
        } else null

        var vertexCount = 0
        var triCount = 0
        var texturePath = ""
        if (result.assetType == ImportedAssetType.MODEL_3D && localFile != null) {
          val parsed = Mesh3DParser.parseFile(localFile)
          vertexCount = parsed.vertexCount
          triCount = parsed.triangleCount
          texturePath = parsed.texturePath
        }

        ImportedAssetEntity(
          name = result.title,
          assetType = result.assetType.name,
          sourceType = result.sourceType.name,
          sourceUrl = targetUrl.ifBlank { result.pageUrl },
          localFilePath = localFile?.absolutePath.orEmpty(),
          linkedTexturePath = texturePath,
          wikiSummary = result.description,
          attribution = result.attribution.ifBlank { "External Public Source" },
          sheetColumns = result.sheetColumns,
          sheetRows = result.sheetRows,
          sheetFps = result.sheetFps,
          vertexCount = vertexCount,
          triangleCount = triCount
        )
      }
    }

  suspend fun importFromDirectUrl(
    url: String,
    overrideType: ImportedAssetType? = null,
    customName: String = "",
    sheetCols: Int = 1,
    sheetRows: Int = 1,
    sheetFps: Int = 6
  ): Result<List<ImportedAssetEntity>> = withContext(Dispatchers.IO) {
    runCatching {
      val cleanUrl = url.trim()
      require(cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
        "URL must begin with http:// or https://"
      }

      val cleanExt = Uri.parse(cleanUrl).lastPathSegment?.substringAfterLast('.', "")?.lowercase().orEmpty()
      require(cleanExt !in blockedExecutableExts) {
        "Executable file type .$cleanExt is blocked for device safety."
      }

      if (cleanExt == "json") {
        return@runCatching importManifestUrlInternal(cleanUrl)
      }

      val downloaded = downloadUrlToInternalFile(
        url = cleanUrl,
        preferredName = customName.ifBlank { Uri.parse(cleanUrl).lastPathSegment?.substringBeforeLast('.') ?: "Imported Asset" },
        fallbackExt = cleanExt.ifBlank { defaultExtensionFor(overrideType ?: ImportedAssetType.SPRITE) }
      )

      if (downloaded.extension.lowercase() == "zip") {
        return@runCatching unpackZipArchive(downloaded, ExternalSourceType.DIRECT_URL, cleanUrl)
      }

      val resolvedType = overrideType ?: classifyExtension(downloaded.extension, sheetCols, sheetRows)
      val entity = buildAssetEntityFromFile(
        file = downloaded,
        assetType = resolvedType,
        sourceType = ExternalSourceType.DIRECT_URL,
        sourceUrl = cleanUrl,
        customName = customName,
        sheetCols = sheetCols,
        sheetRows = sheetRows,
        sheetFps = sheetFps
      )
      listOf(entity)
    }
  }

  suspend fun importFromManifestUrl(manifestUrl: String): Result<List<ImportedAssetEntity>> =
    withContext(Dispatchers.IO) {
      runCatching { importManifestUrlInternal(manifestUrl.trim()) }
    }

  private fun importManifestUrlInternal(manifestUrl: String): List<ImportedAssetEntity> {
    require(manifestUrl.startsWith("http://") || manifestUrl.startsWith("https://")) {
      "Manifest URL must start with http:// or https://"
    }
    val req = Request.Builder().url(manifestUrl).build()
    val rawJson = client.newCall(req).execute().use { resp ->
      if (!resp.isSuccessful) throw IllegalStateException("Manifest HTTP ${resp.code}")
      resp.body?.string() ?: throw IllegalStateException("Empty JSON manifest")
    }
    val root = json.parseToJsonElement(rawJson).jsonObject
    val items = root["assets"]?.jsonArray ?: root["items"]?.jsonArray
      ?: throw IllegalStateException("JSON manifest must contain an 'assets' array")

    val packAttribution = root["attribution"]?.jsonPrimitive?.content ?: "Custom JSON Manifest"
    val imported = ArrayList<ImportedAssetEntity>()

    for (itemEl in items) {
      val obj = itemEl.jsonObject
      val name = obj["name"]?.jsonPrimitive?.content ?: "Pack Asset"
      val assetUrl = obj["url"]?.jsonPrimitive?.content.orEmpty()
      val typeStr = obj["type"]?.jsonPrimitive?.content?.uppercase().orEmpty()
      val assetType = ImportedAssetType.entries.find { it.name == typeStr }
        ?: classifyExtension(assetUrl.substringAfterLast('.', ""), 1, 1)
      val cols = obj["cols"]?.jsonPrimitive?.intOrNull ?: 1
      val rows = obj["rows"]?.jsonPrimitive?.intOrNull ?: 1
      val fps = obj["fps"]?.jsonPrimitive?.intOrNull ?: 6
      val wiki = obj["lore"]?.jsonPrimitive?.content ?: obj["summary"]?.jsonPrimitive?.content.orEmpty()
      val texUrl = obj["textureUrl"]?.jsonPrimitive?.content.orEmpty()

      val localFile = if (assetUrl.startsWith("http")) {
        runCatching {
          downloadUrlToInternalFile(assetUrl, name, guessExtension(assetUrl, assetType))
        }.getOrNull()
      } else null

      val localTex = if (texUrl.startsWith("http")) {
        runCatching {
          downloadUrlToInternalFile(texUrl, "${name}_tex", "png").absolutePath
        }.getOrElse { "" }
      } else ""

      val mesh = if (assetType == ImportedAssetType.MODEL_3D && localFile != null) {
        Mesh3DParser.parseFile(localFile, localTex)
      } else null

      imported.add(
        ImportedAssetEntity(
          name = name,
          assetType = assetType.name,
          sourceType = ExternalSourceType.MANIFEST_JSON.name,
          sourceUrl = assetUrl.ifBlank { manifestUrl },
          localFilePath = localFile?.absolutePath.orEmpty(),
          linkedTexturePath = mesh?.texturePath ?: localTex,
          wikiSummary = wiki,
          attribution = obj["attribution"]?.jsonPrimitive?.content ?: packAttribution,
          sheetColumns = cols.coerceIn(1, 32),
          sheetRows = rows.coerceIn(1, 32),
          sheetFps = fps.coerceIn(1, 30),
          vertexCount = mesh?.vertexCount ?: 0,
          triangleCount = mesh?.triangleCount ?: 0
        )
      )
    }
    return imported
  }

  /**
   * Imports a user-selected file or .zip asset bundle via zero-permission Android SAF / Photo Picker.
   */
  suspend fun importFromLocalUri(
    uri: Uri,
    overrideType: ImportedAssetType? = null,
    sheetCols: Int = 1,
    sheetRows: Int = 1,
    sheetFps: Int = 6
  ): Result<List<ImportedAssetEntity>> = withContext(Dispatchers.IO) {
    runCatching {
      val displayName = resolveDisplayName(uri)
      val ext = displayName.substringAfterLast('.', "").lowercase()
      require(ext !in blockedExecutableExts) {
        "File type .$ext is blocked for Play Integrity compliance."
      }

      val safeExt = ext.ifBlank {
        mimeToExt(context.contentResolver.getType(uri))
          ?: defaultExtensionFor(overrideType ?: ImportedAssetType.SPRITE)
      }
      val destFile = createUniqueFile(displayName.substringBeforeLast('.'), safeExt)
      context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(destFile).use { output -> input.copyTo(output) }
      } ?: throw IllegalStateException("Unable to read selected file.")

      if (destFile.extension.lowercase() == "zip") {
        val entities = unpackZipArchive(destFile, ExternalSourceType.ZIP_PACK, uri.toString())
        destFile.delete()
        return@runCatching entities
      }

      val resolvedType = overrideType ?: classifyExtension(destFile.extension, sheetCols, sheetRows)
      listOf(
        buildAssetEntityFromFile(
          file = destFile,
          assetType = resolvedType,
          sourceType = ExternalSourceType.LOCAL_SAF,
          sourceUrl = uri.toString(),
          customName = displayName.substringBeforeLast('.'),
          sheetCols = sheetCols,
          sheetRows = sheetRows,
          sheetFps = sheetFps
        )
      )
    }
  }

  fun unpackZipArchive(
    zipFile: File,
    sourceType: ExternalSourceType,
    sourceUrl: String
  ): List<ImportedAssetEntity> {
    val packDir = File(importRootDir(), "pack_${System.currentTimeMillis()}").apply { mkdirs() }
    val canonicalDir = packDir.canonicalFile
    val extractedFiles = ArrayList<File>()

    ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zis ->
      var entry = zis.nextEntry
      while (entry != null) {
        if (!entry.isDirectory) {
          val leafName = File(entry.name).name
          val ext = leafName.substringAfterLast('.', "").lowercase()
          val isAllowed = ext in allowedImageExts ||
            ext in allowedModelExts ||
            ext in allowedAudioExts ||
            ext in allowedSupportingExts

          if (isAllowed && leafName.isNotBlank()) {
            val target = File(canonicalDir, leafName).canonicalFile
            if (!target.path.startsWith(canonicalDir.path + File.separator)) {
              throw SecurityException("Blocked unsafe ZIP entry path: ${entry.name}")
            }
            FileOutputStream(target).use { out -> zis.copyTo(out) }
            extractedFiles.add(target)
          }
        }
        zis.closeEntry()
        entry = zis.nextEntry
      }
    }

    val results = ArrayList<ImportedAssetEntity>()
    val textures = extractedFiles.filter { it.extension.lowercase() in allowedImageExts }

    for (file in extractedFiles) {
      val ext = file.extension.lowercase()
      if (ext in setOf("mtl", "bin")) continue

      if (ext == "json") {
        runCatching {
          val text = file.readText()
          val obj = json.parseToJsonElement(text).jsonObject
          val lore = obj["lore"]?.jsonPrimitive?.content ?: obj["description"]?.jsonPrimitive?.content
          if (!lore.isNullOrBlank()) {
            results.add(
              ImportedAssetEntity(
                name = obj["name"]?.jsonPrimitive?.content ?: file.nameWithoutExtension,
                assetType = ImportedAssetType.WIKI_LORE.name,
                sourceType = sourceType.name,
                sourceUrl = sourceUrl,
                localFilePath = file.absolutePath,
                wikiSummary = lore,
                attribution = "Unpacked from ZIP Archive"
              )
            )
          }
        }
        continue
      }

      val matchedTex = if (ext in allowedModelExts) {
        textures.firstOrNull {
          it.nameWithoutExtension.startsWith(file.nameWithoutExtension, ignoreCase = true)
        }?.absolutePath ?: textures.firstOrNull()?.absolutePath.orEmpty()
      } else ""

      val inferredType = classifyExtension(ext, 1, 1)
      results.add(
        buildAssetEntityFromFile(
          file = file,
          assetType = inferredType,
          sourceType = sourceType,
          sourceUrl = sourceUrl,
          customName = file.nameWithoutExtension,
          linkedTextureOverride = matchedTex
        )
      )
    }
    return results
  }

  private fun buildAssetEntityFromFile(
    file: File,
    assetType: ImportedAssetType,
    sourceType: ExternalSourceType,
    sourceUrl: String,
    customName: String,
    sheetCols: Int = 1,
    sheetRows: Int = 1,
    sheetFps: Int = 6,
    linkedTextureOverride: String = ""
  ): ImportedAssetEntity {
    val mesh = if (assetType == ImportedAssetType.MODEL_3D) {
      Mesh3DParser.parseFile(file, linkedTextureOverride)
    } else null

    return ImportedAssetEntity(
      name = customName.ifBlank { file.nameWithoutExtension },
      assetType = assetType.name,
      sourceType = sourceType.name,
      sourceUrl = sourceUrl,
      localFilePath = file.absolutePath,
      linkedTexturePath = mesh?.texturePath ?: linkedTextureOverride,
      attribution = sourceType.label,
      sheetColumns = sheetCols.coerceIn(1, 32),
      sheetRows = sheetRows.coerceIn(1, 32),
      sheetFps = sheetFps.coerceIn(1, 30),
      vertexCount = mesh?.vertexCount ?: 0,
      triangleCount = mesh?.triangleCount ?: 0
    )
  }

  private fun downloadUrlToInternalFile(url: String, preferredName: String, fallbackExt: String): File {
    val req = Request.Builder()
      .url(url)
      .header("User-Agent", "ChronicleNexusRPG/1.0")
      .build()
    client.newCall(req).execute().use { resp ->
      if (!resp.isSuccessful) throw IllegalStateException("Download failed: HTTP ${resp.code}")
      val body = resp.body ?: throw IllegalStateException("Empty response body")
      val contentTypeExt = mimeToExt(resp.header("Content-Type"))
      val urlExt = Uri.parse(url).lastPathSegment?.substringAfterLast('.', "")?.lowercase().orEmpty()
      val ext = urlExt.ifBlank { contentTypeExt ?: fallbackExt }.ifBlank { "bin" }
      require(ext !in blockedExecutableExts) { "Blocked executable extension .$ext" }

      val dest = createUniqueFile(preferredName, ext)
      body.byteStream().use { input ->
        FileOutputStream(dest).use { out -> input.copyTo(out) }
      }
      return dest
    }
  }

  private fun createUniqueFile(baseName: String, ext: String): File {
    val sanitized = baseName.replace(Regex("[^A-Za-z0-9_\\-]"), "_").take(40).ifBlank { "asset" }
    val cleanExt = ext.lowercase().replace(Regex("[^a-z0-9]"), "").ifBlank { "dat" }
    return File(importRootDir(), "${sanitized}_${System.currentTimeMillis()}.$cleanExt")
  }

  private fun resolveDisplayName(uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
      if (cursor.moveToFirst()) {
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0) return cursor.getString(idx) ?: "imported_asset"
      }
    }
    return uri.lastPathSegment?.substringAfterLast('/') ?: "imported_asset"
  }

  private fun classifyExtension(ext: String, cols: Int, rows: Int): ImportedAssetType {
    return when (ext.lowercase()) {
      in allowedModelExts -> ImportedAssetType.MODEL_3D
      in allowedAudioExts -> if (ext.lowercase() == "wav") ImportedAssetType.AUDIO_SFX else ImportedAssetType.AUDIO_BGM
      in allowedImageExts -> if (cols > 1 || rows > 1) ImportedAssetType.SPRITESHEET else ImportedAssetType.SPRITE
      else -> ImportedAssetType.WIKI_LORE
    }
  }

  private fun guessExtension(url: String, type: ImportedAssetType): String {
    val ext = Uri.parse(url).lastPathSegment?.substringAfterLast('.', "")?.lowercase().orEmpty()
    if (ext.isNotBlank() && ext.length <= 5) return ext
    return defaultExtensionFor(type)
  }

  private fun defaultExtensionFor(type: ImportedAssetType): String = when (type) {
    ImportedAssetType.SPRITE, ImportedAssetType.SPRITESHEET, ImportedAssetType.CONCEPT_ART, ImportedAssetType.TEXTURE -> "png"
    ImportedAssetType.MODEL_3D -> "glb"
    ImportedAssetType.AUDIO_BGM -> "ogg"
    ImportedAssetType.AUDIO_SFX -> "wav"
    ImportedAssetType.WIKI_LORE -> "txt"
  }

  private fun mimeToExt(mime: String?): String? {
    val clean = mime?.substringBefore(';')?.trim()?.lowercase() ?: return null
    return when (clean) {
      "image/png" -> "png"
      "image/jpeg" -> "jpg"
      "image/webp" -> "webp"
      "image/gif" -> "gif"
      "model/gltf-binary" -> "glb"
      "model/gltf+json" -> "gltf"
      "audio/mpeg" -> "mp3"
      "audio/ogg", "application/ogg" -> "ogg"
      "audio/wav", "audio/x-wav" -> "wav"
      "audio/mp4" -> "m4a"
      "application/zip" -> "zip"
      "application/json" -> "json"
      else -> null
    }
  }
}
