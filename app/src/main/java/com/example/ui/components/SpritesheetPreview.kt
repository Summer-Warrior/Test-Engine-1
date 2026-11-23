package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MythrilCyan
import com.example.ui.theme.VoidNavy
import java.io.File
import kotlinx.coroutines.delay

/**
 * Slices a spritesheet file on disk into individual grid frames for live battle
 * and importer animation preview.
 */
@Composable
fun rememberSlicedSpritesheetFrames(
  filePath: String,
  columns: Int,
  rows: Int
): List<ImageBitmap> {
  return remember(filePath, columns, rows) {
    loadSlicedSpritesheetFrames(filePath, columns, rows)
  }
}

fun loadSlicedSpritesheetFrames(
  filePath: String,
  columns: Int,
  rows: Int
): List<ImageBitmap> {
  if (filePath.isBlank()) return emptyList()
  val file = File(filePath)
  if (!file.exists()) return emptyList()
  return runCatching {
    val fullBitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return emptyList()
    val cols = columns.coerceIn(1, 32)
    val rowCount = rows.coerceIn(1, 32)
    if (cols == 1 && rowCount == 1) {
      return listOf(fullBitmap.asImageBitmap())
    }
    val cellW = (fullBitmap.width / cols).coerceAtLeast(1)
    val cellH = (fullBitmap.height / rowCount).coerceAtLeast(1)
    val frames = ArrayList<ImageBitmap>(cols * rowCount)
    for (r in 0 until rowCount) {
      for (c in 0 until cols) {
        val x = c * cellW
        val y = r * cellH
        if (x + cellW <= fullBitmap.width && y + cellH <= fullBitmap.height) {
          val cell: Bitmap = Bitmap.createBitmap(fullBitmap, x, y, cellW, cellH)
          frames.add(cell.asImageBitmap())
        }
      }
    }
    if (frames.isEmpty()) listOf(fullBitmap.asImageBitmap()) else frames
  }.getOrElse { emptyList() }
}

@Composable
fun AnimatedSpritesheetPreview(
  filePath: String,
  columns: Int,
  rows: Int,
  fps: Int,
  contentDescription: String,
  modifier: Modifier = Modifier
) {
  val frames = rememberSlicedSpritesheetFrames(filePath, columns, rows)
  var frameIdx by remember(filePath, columns, rows) { mutableIntStateOf(0) }

  LaunchedEffect(frames.size, fps) {
    if (frames.size > 1) {
      val delayMs = (1000L / fps.coerceIn(1, 30)).coerceAtLeast(33L)
      while (true) {
        delay(delayMs)
        frameIdx = (frameIdx + 1) % frames.size
      }
    }
  }

  Surface(
    color = VoidNavy,
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, MythrilCyan.copy(alpha = 0.6f)),
    modifier = modifier
  ) {
    Box(modifier = Modifier.fillMaxSize().background(VoidNavy), contentAlignment = Alignment.Center) {
      if (frames.isNotEmpty()) {
        Image(
          bitmap = frames[frameIdx % frames.size],
          contentDescription = contentDescription,
          contentScale = ContentScale.Fit,
          filterQuality = FilterQuality.None,
          modifier = Modifier.fillMaxSize().padding(4.dp)
        )
        if (frames.size > 1) {
          Text(
            text = "${frameIdx + 1}/${frames.size}",
            style = MaterialTheme.typography.labelSmall,
            color = MythrilCyan,
            fontSize = 9.sp,
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(3.dp)
          )
        }
      }
    }
  }
}
