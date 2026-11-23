package com.example.assets

import android.util.Base64
import com.example.model.ParsedMesh3D
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.sqrt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Pure-Kotlin 3D geometry and texture linker supporting:
 * - Binary glTF 2.0 (.glb) with embedded buffers & images
 * - ASCII glTF 2.0 (.gltf) with base64 or sibling .bin / texture files
 * - Wavefront (.obj) with optional .mtl map_Kd texture lookup
 */
object Mesh3DParser {
  private val json = Json { ignoreUnknownKeys = true; isLenient = true }

  fun parseFile(file: File, linkedTexturePath: String = ""): ParsedMesh3D {
    if (!file.exists()) return createFallbackCrystalMesh(file.name, linkedTexturePath)
    val ext = file.extension.lowercase()
    return runCatching {
      when (ext) {
        "glb" -> parseGlb(file, linkedTexturePath)
        "gltf" -> parseGltf(file, linkedTexturePath)
        "obj" -> parseObj(file, linkedTexturePath)
        else -> createFallbackCrystalMesh(file.nameWithoutExtension, linkedTexturePath)
      }
    }.getOrElse {
      createFallbackCrystalMesh(file.nameWithoutExtension, linkedTexturePath)
    }
  }

  private fun parseGlb(file: File, linkedTexturePath: String): ParsedMesh3D {
    val bytes = file.readBytes()
    if (bytes.size < 20) return createFallbackCrystalMesh(file.nameWithoutExtension, linkedTexturePath)

    val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    val magic = buf.int
    val version = buf.int
    val totalLength = buf.int
    if (magic != 0x46546C67 || version != 2 || totalLength > bytes.size) {
      return createFallbackCrystalMesh(file.nameWithoutExtension, linkedTexturePath)
    }

    var jsonChunkStr = ""
    var binChunk: ByteArray? = null

    while (buf.remaining() >= 8) {
      val chunkLen = buf.int
      val chunkType = buf.int
      if (chunkLen < 0 || chunkLen > buf.remaining()) break
      val chunkData = ByteArray(chunkLen)
      buf.get(chunkData)
      when (chunkType) {
        0x4E4F534A -> jsonChunkStr = String(chunkData, Charsets.UTF_8).trimEnd('\u0000', ' ')
        0x004E4942 -> binChunk = chunkData
      }
    }

    if (jsonChunkStr.isBlank()) {
      return createFallbackCrystalMesh(file.nameWithoutExtension, linkedTexturePath)
    }

    val root = json.parseToJsonElement(jsonChunkStr).jsonObject
    val resolvedTexture = linkedTexturePath.ifBlank {
      extractEmbeddedGlbTexture(root, binChunk, file) ?: findSiblingTexture(file)
    }
    return extractMeshFromGltfJson(
      name = file.nameWithoutExtension,
      root = root,
      buffers = listOfNotNull(binChunk),
      texturePath = resolvedTexture
    )
  }

  private fun parseGltf(file: File, linkedTexturePath: String): ParsedMesh3D {
    val root = json.parseToJsonElement(file.readText()).jsonObject
    val buffers = root["buffers"]?.jsonArray?.map { bufEl ->
      val uri = bufEl.jsonObject["uri"]?.jsonPrimitive?.content ?: ""
      when {
        uri.startsWith("data:") -> decodeDataUri(uri)
        uri.isNotBlank() -> {
          val binFile = File(file.parentFile, File(uri).name)
          if (binFile.exists()) binFile.readBytes() else ByteArray(0)
        }
        else -> ByteArray(0)
      }
    } ?: emptyList()

    val resolvedTexture = linkedTexturePath.ifBlank {
      extractGltfImageUri(root, file) ?: findSiblingTexture(file)
    }
    return extractMeshFromGltfJson(
      name = file.nameWithoutExtension,
      root = root,
      buffers = buffers,
      texturePath = resolvedTexture
    )
  }

  private fun extractMeshFromGltfJson(
    name: String,
    root: JsonObject,
    buffers: List<ByteArray>,
    texturePath: String
  ): ParsedMesh3D {
    val accessors = root["accessors"]?.jsonArray ?: return createFallbackCrystalMesh(name, texturePath)
    val bufferViews = root["bufferViews"]?.jsonArray ?: return createFallbackCrystalMesh(name, texturePath)
    val meshes = root["meshes"]?.jsonArray ?: return createFallbackCrystalMesh(name, texturePath)
    if (meshes.isEmpty()) return createFallbackCrystalMesh(name, texturePath)

    val allVerts = ArrayList<Float>()
    val allUvs = ArrayList<Float>()
    val allIndices = ArrayList<Int>()
    var hasAllUvs = true

    for (meshEl in meshes) {
      val primitives = meshEl.jsonObject["primitives"]?.jsonArray ?: continue
      for (primEl in primitives) {
        val prim = primEl.jsonObject
        val attrs = prim["attributes"]?.jsonObject ?: continue
        val posAccessorIdx = attrs["POSITION"]?.jsonPrimitive?.intOrNull ?: continue
        val posFloats = readFloatAccessor(accessors, bufferViews, buffers, posAccessorIdx, 3)
        if (posFloats.isEmpty()) continue

        val vertexOffset = allVerts.size / 3
        for (f in posFloats) allVerts.add(f)

        val uvAccessorIdx = attrs["TEXCOORD_0"]?.jsonPrimitive?.intOrNull
        if (uvAccessorIdx != null) {
          val uvFloats = readFloatAccessor(accessors, bufferViews, buffers, uvAccessorIdx, 2)
          if (uvFloats.size == (posFloats.size / 3) * 2) {
            for (u in uvFloats) allUvs.add(u)
          } else {
            hasAllUvs = false
          }
        } else {
          hasAllUvs = false
        }

        val indicesAccessorIdx = prim["indices"]?.jsonPrimitive?.intOrNull
        if (indicesAccessorIdx != null) {
          val idxList = readIndexAccessor(accessors, bufferViews, buffers, indicesAccessorIdx)
          for (i in idxList) allIndices.add(vertexOffset + i)
        } else {
          val count = posFloats.size / 3
          for (i in 0 until count) allIndices.add(vertexOffset + i)
        }
      }
    }

    if (allVerts.size < 9 || allIndices.size < 3) {
      return createFallbackCrystalMesh(name, texturePath)
    }

    val rawVerts = allVerts.toFloatArray()
    val normalized = normalizeVertices(rawVerts)
    val uvsArray = if (hasAllUvs && allUvs.size == (rawVerts.size / 3) * 2) allUvs.toFloatArray() else null
    val indicesArray = allIndices.toIntArray()
    return ParsedMesh3D(
      name = name,
      vertices = normalized,
      indices = indicesArray,
      uvs = uvsArray,
      vertexCount = normalized.size / 3,
      triangleCount = indicesArray.size / 3,
      boundsRadius = 1f,
      texturePath = texturePath
    )
  }

  private fun readFloatAccessor(
    accessors: kotlinx.serialization.json.JsonArray,
    bufferViews: kotlinx.serialization.json.JsonArray,
    buffers: List<ByteArray>,
    accessorIndex: Int,
    components: Int
  ): FloatArray {
    val acc = accessors.getOrNull(accessorIndex)?.jsonObject ?: return FloatArray(0)
    val bvIndex = acc["bufferView"]?.jsonPrimitive?.intOrNull ?: return FloatArray(0)
    val bv = bufferViews.getOrNull(bvIndex)?.jsonObject ?: return FloatArray(0)
    val bufIndex = bv["buffer"]?.jsonPrimitive?.intOrNull ?: 0
    val raw = buffers.getOrNull(bufIndex) ?: return FloatArray(0)

    val count = acc["count"]?.jsonPrimitive?.int ?: 0
    val bvOffset = bv["byteOffset"]?.jsonPrimitive?.intOrNull ?: 0
    val accOffset = acc["byteOffset"]?.jsonPrimitive?.intOrNull ?: 0
    val stride = bv["byteStride"]?.jsonPrimitive?.intOrNull ?: (components * 4)

    val out = FloatArray(count * components)
    val bb = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
    val start = bvOffset + accOffset
    for (i in 0 until count) {
      val pos = start + i * stride
      if (pos + components * 4 > raw.size) break
      for (c in 0 until components) {
        out[i * components + c] = bb.getFloat(pos + c * 4)
      }
    }
    return out
  }

  private fun readIndexAccessor(
    accessors: kotlinx.serialization.json.JsonArray,
    bufferViews: kotlinx.serialization.json.JsonArray,
    buffers: List<ByteArray>,
    accessorIndex: Int
  ): IntArray {
    val acc = accessors.getOrNull(accessorIndex)?.jsonObject ?: return IntArray(0)
    val bvIndex = acc["bufferView"]?.jsonPrimitive?.intOrNull ?: return IntArray(0)
    val bv = bufferViews.getOrNull(bvIndex)?.jsonObject ?: return IntArray(0)
    val bufIndex = bv["buffer"]?.jsonPrimitive?.intOrNull ?: 0
    val raw = buffers.getOrNull(bufIndex) ?: return IntArray(0)

    val count = acc["count"]?.jsonPrimitive?.int ?: 0
    val compType = acc["componentType"]?.jsonPrimitive?.int ?: 5123
    val bvOffset = bv["byteOffset"]?.jsonPrimitive?.intOrNull ?: 0
    val accOffset = acc["byteOffset"]?.jsonPrimitive?.intOrNull ?: 0
    val start = bvOffset + accOffset
    val bb = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)

    val out = IntArray(count)
    when (compType) {
      5121 -> { // UNSIGNED_BYTE
        for (i in 0 until count) {
          if (start + i >= raw.size) break
          out[i] = raw[start + i].toInt() and 0xFF
        }
      }
      5123 -> { // UNSIGNED_SHORT
        for (i in 0 until count) {
          val p = start + i * 2
          if (p + 2 > raw.size) break
          out[i] = bb.getShort(p).toInt() and 0xFFFF
        }
      }
      5125 -> { // UNSIGNED_INT
        for (i in 0 until count) {
          val p = start + i * 4
          if (p + 4 > raw.size) break
          out[i] = bb.getInt(p)
        }
      }
    }
    return out
  }

  private fun extractEmbeddedGlbTexture(root: JsonObject, binChunk: ByteArray?, modelFile: File): String? {
    if (binChunk == null) return null
    val images = root["images"]?.jsonArray ?: return null
    val bufferViews = root["bufferViews"]?.jsonArray ?: return null
    val firstImg = images.firstOrNull()?.jsonObject ?: return null
    val bvIdx = firstImg["bufferView"]?.jsonPrimitive?.intOrNull ?: return null
    val bv = bufferViews.getOrNull(bvIdx)?.jsonObject ?: return null
    val offset = bv["byteOffset"]?.jsonPrimitive?.intOrNull ?: 0
    val length = bv["byteLength"]?.jsonPrimitive?.intOrNull ?: return null
    if (offset < 0 || length <= 0 || offset + length > binChunk.size) return null

    val mime = firstImg["mimeType"]?.jsonPrimitive?.contentOrNull ?: "image/png"
    val ext = if (mime.contains("jpeg") || mime.contains("jpg")) "jpg" else "png"
    val texFile = File(modelFile.parentFile, "${modelFile.nameWithoutExtension}_tex.$ext")
    if (!texFile.exists()) {
      texFile.writeBytes(binChunk.copyOfRange(offset, offset + length))
    }
    return texFile.absolutePath
  }

  private fun extractGltfImageUri(root: JsonObject, modelFile: File): String? {
    val uri = root["images"]?.jsonArray?.firstOrNull()?.jsonObject?.get("uri")?.jsonPrimitive?.contentOrNull
      ?: return null
    if (uri.startsWith("data:")) {
      val ext = if (uri.contains("jpeg") || uri.contains("jpg")) "jpg" else "png"
      val texFile = File(modelFile.parentFile, "${modelFile.nameWithoutExtension}_tex.$ext")
      if (!texFile.exists()) texFile.writeBytes(decodeDataUri(uri))
      return texFile.absolutePath
    }
    val candidate = File(modelFile.parentFile, File(uri).name)
    return if (candidate.exists()) candidate.absolutePath else null
  }

  private fun parseObj(file: File, linkedTexturePath: String): ParsedMesh3D {
    val positions = ArrayList<Float>()
    val texCoords = ArrayList<Float>()
    val finalVerts = ArrayList<Float>()
    val finalUvs = ArrayList<Float>()
    val finalIndices = ArrayList<Int>()
    var mtlLibName = ""
    var hasUvs = false

    file.forEachLine { rawLine ->
      val line = rawLine.trim()
      when {
        line.startsWith("mtllib ") -> mtlLibName = line.removePrefix("mtllib ").trim()
        line.startsWith("v ") -> {
          val parts = line.split(Regex("\\s+")).drop(1)
          if (parts.size >= 3) {
            positions.add(parts[0].toFloatOrNull() ?: 0f)
            positions.add(parts[1].toFloatOrNull() ?: 0f)
            positions.add(parts[2].toFloatOrNull() ?: 0f)
          }
        }
        line.startsWith("vt ") -> {
          val parts = line.split(Regex("\\s+")).drop(1)
          if (parts.size >= 2) {
            texCoords.add(parts[0].toFloatOrNull() ?: 0f)
            texCoords.add(1f - (parts[1].toFloatOrNull() ?: 0f))
          }
        }
        line.startsWith("f ") -> {
          val tokens = line.split(Regex("\\s+")).drop(1)
          if (tokens.size >= 3) {
            val faceVerts = ArrayList<Int>(tokens.size)
            val totalVerts = positions.size / 3
            val totalUvs = texCoords.size / 2
            for (tok in tokens) {
              val segs = tok.split("/")
              val rawIdx = segs[0].toIntOrNull() ?: continue
              val vIdx = if (rawIdx < 0) totalVerts + rawIdx else rawIdx - 1
              if (vIdx !in 0 until totalVerts) continue

              val outIdx = finalVerts.size / 3
              finalVerts.add(positions[vIdx * 3])
              finalVerts.add(positions[vIdx * 3 + 1])
              finalVerts.add(positions[vIdx * 3 + 2])

              val rawUvIdx = segs.getOrNull(1)?.toIntOrNull()
              if (rawUvIdx != null && totalUvs > 0) {
                val uvIdx = if (rawUvIdx < 0) totalUvs + rawUvIdx else rawUvIdx - 1
                if (uvIdx in 0 until totalUvs) {
                  hasUvs = true
                  finalUvs.add(texCoords[uvIdx * 2])
                  finalUvs.add(texCoords[uvIdx * 2 + 1])
                } else {
                  finalUvs.add(0.5f); finalUvs.add(0.5f)
                }
              } else {
                finalUvs.add(0.5f); finalUvs.add(0.5f)
              }
              faceVerts.add(outIdx)
            }
            for (k in 1 until faceVerts.size - 1) {
              finalIndices.add(faceVerts[0])
              finalIndices.add(faceVerts[k])
              finalIndices.add(faceVerts[k + 1])
            }
          }
        }
      }
    }

    val resolvedTexture = linkedTexturePath.ifBlank {
      findMtlTexture(file.parentFile, mtlLibName) ?: findSiblingTexture(file)
    }

    if (finalVerts.size < 9 || finalIndices.size < 3) {
      return createFallbackCrystalMesh(file.nameWithoutExtension, resolvedTexture)
    }

    val normalized = normalizeVertices(finalVerts.toFloatArray())
    val indicesArr = finalIndices.toIntArray()
    return ParsedMesh3D(
      name = file.nameWithoutExtension,
      vertices = normalized,
      indices = indicesArr,
      uvs = if (hasUvs) finalUvs.toFloatArray() else null,
      vertexCount = normalized.size / 3,
      triangleCount = indicesArr.size / 3,
      boundsRadius = 1f,
      texturePath = resolvedTexture
    )
  }

  private fun findMtlTexture(parentDir: File?, mtlName: String): String? {
    if (parentDir == null || mtlName.isBlank()) return null
    val mtlFile = File(parentDir, File(mtlName).name)
    if (!mtlFile.exists()) return null
    for (raw in mtlFile.readLines()) {
      val t = raw.trim()
      if (t.startsWith("map_Kd ", ignoreCase = true)) {
        val texName = t.substringAfter(" ").trim()
        val candidate = File(parentDir, File(texName).name)
        if (candidate.exists()) return candidate.absolutePath
      }
    }
    return null
  }

  private fun findSiblingTexture(modelFile: File): String {
    val dir = modelFile.parentFile ?: return ""
    val imageExts = setOf("png", "jpg", "jpeg", "webp")
    val files = dir.listFiles()?.filter { it.isFile && it.extension.lowercase() in imageExts } ?: return ""
    val prefixMatch = files.firstOrNull {
      it.nameWithoutExtension.startsWith(modelFile.nameWithoutExtension, ignoreCase = true)
    }
    return (prefixMatch ?: files.firstOrNull())?.absolutePath.orEmpty()
  }

  private fun decodeDataUri(uri: String): ByteArray {
    val comma = uri.indexOf(',')
    if (comma < 0) return ByteArray(0)
    return runCatching { Base64.decode(uri.substring(comma + 1), Base64.DEFAULT) }
      .getOrElse { ByteArray(0) }
  }

  private fun normalizeVertices(verts: FloatArray): FloatArray {
    if (verts.isEmpty()) return verts
    var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
    var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
    var minZ = Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
    for (i in verts.indices step 3) {
      val x = verts[i]; val y = verts[i + 1]; val z = verts[i + 2]
      if (x < minX) minX = x; if (x > maxX) maxX = x
      if (y < minY) minY = y; if (y > maxY) maxY = y
      if (z < minZ) minZ = z; if (z > maxZ) maxZ = z
    }
    val cx = (minX + maxX) * 0.5f
    val cy = (minY + maxY) * 0.5f
    val cz = (minZ + maxZ) * 0.5f
    var maxDist = 0.0001f
    for (i in verts.indices step 3) {
      val dx = verts[i] - cx
      val dy = verts[i + 1] - cy
      val dz = verts[i + 2] - cz
      val d = sqrt(dx * dx + dy * dy + dz * dz)
      if (d > maxDist) maxDist = d
    }
    val invScale = 1f / max(maxDist, 0.0001f)
    val out = FloatArray(verts.size)
    for (i in verts.indices step 3) {
      out[i] = (verts[i] - cx) * invScale
      out[i + 1] = (verts[i + 1] - cy) * invScale
      out[i + 2] = (verts[i + 2] - cz) * invScale
    }
    return out
  }

  fun createFallbackCrystalMesh(name: String = "Relic Crystal", texturePath: String = ""): ParsedMesh3D {
    val verts = floatArrayOf(
      0f, 1.0f, 0f,     // 0: top apex
      0.7f, 0.1f, 0f,   // 1: right
      0f, 0.1f, 0.7f,   // 2: front
      -0.7f, 0.1f, 0f,  // 3: left
      0f, 0.1f, -0.7f,  // 4: back
      0f, -1.0f, 0f     // 5: bottom apex
    )
    val indices = intArrayOf(
      0, 1, 2,
      0, 2, 3,
      0, 3, 4,
      0, 4, 1,
      5, 2, 1,
      5, 3, 2,
      5, 4, 3,
      5, 1, 4
    )
    return ParsedMesh3D(
      name = name,
      vertices = verts,
      indices = indices,
      uvs = null,
      vertexCount = 6,
      triangleCount = 8,
      boundsRadius = 1f,
      texturePath = texturePath
    )
  }
}
