package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ParsedMesh3D
import com.example.ui.theme.AstralBorder
import com.example.ui.theme.DeepCrystalBlue
import com.example.ui.theme.HpEmerald
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.MythrilCyan
import com.example.ui.theme.RelicGold
import com.example.ui.theme.VoidNavy
import java.io.File
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.delay

/**
 * Interactive Compose 3D Turntable Mesh & Texture Viewport for imported
 * .glb, .gltf, and .obj 3D models. Supports touch-drag orbit, zoom, auto-spin,
 * wireframe mode, and linked texture color sampling.
 */
@Composable
fun Mesh3DTurntableViewer(
  mesh: ParsedMesh3D,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  var yaw by remember(mesh.name) { mutableFloatStateOf(0.6f) }
  var pitch by remember(mesh.name) { mutableFloatStateOf(0.35f) }
  var zoom by remember(mesh.name) { mutableFloatStateOf(1.0f) }
  var autoRotate by remember { mutableStateOf(true) }
  var wireframeOnly by remember { mutableStateOf(false) }

  val textureBitmap: Bitmap? = remember(mesh.texturePath) {
    val p = mesh.texturePath
    if (p.isNotBlank() && File(p).exists()) {
      runCatching { BitmapFactory.decodeFile(p) }.getOrNull()
    } else null
  }

  LaunchedEffect(autoRotate, mesh.name) {
    while (autoRotate) {
      delay(32L)
      yaw += 0.025f
    }
  }

  CardSectionContainer(modifier = modifier.testTag("mesh_3d_turntable_viewer")) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Icon(Icons.Default.ViewInAr, contentDescription = "3D Model Viewer", tint = MythrilCyan)
        Spacer(modifier = Modifier.width(6.dp))
        Column {
          Text(
            text = "3D TURNTABLE: ${mesh.name}",
            style = MaterialTheme.typography.titleMedium,
            color = RelicGold
          )
          Text(
            text = "${mesh.vertexCount} Vertices • ${mesh.triangleCount} Triangles" +
              if (textureBitmap != null) " • Texture Linked (${textureBitmap.width}×${textureBitmap.height})" else " • Procedural Shading",
            style = MaterialTheme.typography.labelSmall,
            color = MutedSilver,
            fontSize = 10.sp
          )
        }
      }
      OutlinedButton(
        onClick = onClose,
        border = BorderStroke(1.dp, MutedSilver),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.testTag("btn_close_3d_viewer")
      ) {
        Text("Close", style = MaterialTheme.typography.labelSmall, color = Color.White)
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(220.dp)
        .clip(RoundedCornerShape(12.dp))
        .border(1.5.dp, MythrilCyan.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
        .background(VoidNavy)
    ) {
      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
              change.consume()
              autoRotate = false
              yaw += dragAmount.x * 0.01f
              pitch = (pitch + dragAmount.y * 0.01f).coerceIn(-1.45f, 1.45f)
            }
          }
      ) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val baseScale = min(w, h) * 0.38f * zoom

        // Subtle studio grid horizon
        for (g in -3..3) {
          val gx = cx + g * (w * 0.12f)
          drawLine(
            color = AstralBorder.copy(alpha = 0.28f),
            start = Offset(gx, h * 0.65f),
            end = Offset(cx + g * (w * 0.18f), h),
            strokeWidth = 1f
          )
        }

        val verts = mesh.vertices
        val indices = mesh.indices
        val uvs = mesh.uvs
        val cosY = cos(yaw)
        val sinY = sin(yaw)
        val cosX = cos(pitch)
        val sinX = sin(pitch)

        val vCount = verts.size / 3
        val rx = FloatArray(vCount)
        val ry = FloatArray(vCount)
        val rz = FloatArray(vCount)
        val px = FloatArray(vCount)
        val py = FloatArray(vCount)

        for (i in 0 until vCount) {
          val x0 = verts[i * 3]
          val y0 = verts[i * 3 + 1]
          val z0 = verts[i * 3 + 2]

          val x1 = x0 * cosY - z0 * sinY
          val z1 = x0 * sinY + z0 * cosY
          val y2 = y0 * cosX - z1 * sinX
          val z2 = y0 * sinX + z1 * cosX

          rx[i] = x1
          ry[i] = y2
          rz[i] = z2

          val persp = 3.2f / (3.2f - z2).coerceAtLeast(0.6f)
          px[i] = cx + x1 * baseScale * persp
          py[i] = cy - y2 * baseScale * persp
        }

        // Cap rendered triangles to keep 60fps smooth on high-poly models
        val maxRenderTris = 2800
        val totalTris = indices.size / 3
        val step = (totalTris / maxRenderTris).coerceAtLeast(1)

        data class TriDraw(val tIdx: Int, val depth: Float)
        val tris = ArrayList<TriDraw>(min(totalTris, maxRenderTris))
        var t = 0
        while (t < totalTris) {
          val i0 = indices[t * 3]
          val i1 = indices[t * 3 + 1]
          val i2 = indices[t * 3 + 2]
          if (i0 in 0 until vCount && i1 in 0 until vCount && i2 in 0 until vCount) {
            val depth = (rz[i0] + rz[i1] + rz[i2]) / 3f
            tris.add(TriDraw(t, depth))
          }
          t += step
        }
        tris.sortBy { it.depth }

        val lightX = 0.45f
        val lightY = 0.72f
        val lightZ = 0.52f

        for (item in tris) {
          val base = item.tIdx * 3
          val i0 = indices[base]
          val i1 = indices[base + 1]
          val i2 = indices[base + 2]

          val ax = rx[i1] - rx[i0]
          val ay = ry[i1] - ry[i0]
          val az = rz[i1] - rz[i0]
          val bx = rx[i2] - rx[i0]
          val by = ry[i2] - ry[i0]
          val bz = rz[i2] - rz[i0]

          val nx = ay * bz - az * by
          val ny = az * bx - ax * bz
          val nz = ax * by - ay * bx
          val nLen = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)
          val ndl = ((nx * lightX + ny * lightY + nz * lightZ) / nLen).coerceIn(0.15f, 1f)

          val path = Path().apply {
            moveTo(px[i0], py[i0])
            lineTo(px[i1], py[i1])
            lineTo(px[i2], py[i2])
            close()
          }

          if (!wireframeOnly) {
            val baseCol = if (textureBitmap != null && uvs != null && i0 * 2 + 1 < uvs.size) {
              val u = ((uvs[i0 * 2] % 1f) + 1f) % 1f
              val v = ((uvs[i0 * 2 + 1] % 1f) + 1f) % 1f
              val tx = (u * (textureBitmap.width - 1)).toInt().coerceIn(0, textureBitmap.width - 1)
              val ty = (v * (textureBitmap.height - 1)).toInt().coerceIn(0, textureBitmap.height - 1)
              val argb = textureBitmap.getPixel(tx, ty)
              Color(argb)
            } else {
              RelicGold
            }
            val shaded = Color(
              red = (baseCol.red * ndl).coerceIn(0f, 1f),
              green = (baseCol.green * ndl).coerceIn(0f, 1f),
              blue = (baseCol.blue * ndl).coerceIn(0f, 1f),
              alpha = 0.96f
            )
            drawPath(path = path, color = shaded)
          }

          drawPath(
            path = path,
            color = if (wireframeOnly) MythrilCyan else Color.White.copy(alpha = 0.14f),
            style = Stroke(width = if (wireframeOnly) 1.2f else 0.7f)
          )
        }
      }

      if (textureBitmap != null) {
        Surface(
          color = DeepCrystalBlue.copy(alpha = 0.9f),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, HpEmerald),
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(4.dp)
          ) {
            Image(
              bitmap = textureBitmap.asImageBitmap(),
              contentDescription = "Linked 3D Texture Map",
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text("TEX", style = MaterialTheme.typography.labelSmall, color = HpEmerald, fontSize = 9.sp)
          }
        }
      }

      Text(
        text = "Drag to Orbit",
        style = MaterialTheme.typography.labelSmall,
        color = MutedSilver,
        fontSize = 10.sp,
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(8.dp)
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      OutlinedButton(
        onClick = { autoRotate = !autoRotate },
        border = BorderStroke(1.dp, if (autoRotate) RelicGold else AstralBorder),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Icon(Icons.Default.RotateRight, contentDescription = "Toggle Auto Spin", tint = if (autoRotate) RelicGold else MutedSilver, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(if (autoRotate) "Spin ON" else "Spin OFF", style = MaterialTheme.typography.labelSmall, color = if (autoRotate) RelicGold else MutedSilver)
      }

      OutlinedButton(
        onClick = { wireframeOnly = !wireframeOnly },
        border = BorderStroke(1.dp, if (wireframeOnly) MythrilCyan else AstralBorder),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(if (wireframeOnly) "Wireframe" else "Shaded", style = MaterialTheme.typography.labelSmall, color = if (wireframeOnly) MythrilCyan else Color.White)
      }

      Slider(
        value = zoom,
        onValueChange = { zoom = it },
        valueRange = 0.5f..2.0f,
        colors = SliderDefaults.colors(thumbColor = RelicGold, activeTrackColor = MythrilCyan),
        modifier = Modifier.weight(1f)
      )
    }
  }
}

@Composable
private fun CardSectionContainer(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  Surface(
    color = DeepCrystalBlue,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.5.dp, MythrilCyan),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      content()
    }
  }
}
