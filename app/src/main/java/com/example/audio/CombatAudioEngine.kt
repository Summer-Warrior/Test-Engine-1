package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.model.ImportedAssetEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Dual audio engine for Chronicle Nexus:
 * - Jetpack Media3 ExoPlayer for seamless looping Realm/Stage BGM & audio preview
 * - Android SoundPool for low-latency polyphonic combat hit/attack/Soul Break SFX
 */
@Singleton
class CombatAudioEngine @Inject constructor(
  @ApplicationContext private val context: Context
) {
  private val mainHandler = Handler(Looper.getMainLooper())
  private var exoPlayer: ExoPlayer? = null
  private var soundPool: SoundPool? = null
  private val loadedSoundIds = ConcurrentHashMap<Int, Int>()
  private var currentBgmPathOrUrl: String? = null

  private val _isMuted = MutableStateFlow(false)
  val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

  private val _activeBgmTitle = MutableStateFlow<String?>(null)
  val activeBgmTitle: StateFlow<String?> = _activeBgmTitle.asStateFlow()

  init {
    runCatching {
      val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
      soundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(attrs)
        .build()
    }
  }

  private fun ensureExoPlayer(): ExoPlayer? {
    if (exoPlayer != null) return exoPlayer
    return runCatching {
      ExoPlayer.Builder(context).build().apply {
        repeatMode = Player.REPEAT_MODE_ONE
        volume = if (_isMuted.value) 0f else 0.7f
        exoPlayer = this
      }
    }.getOrNull()
  }

  fun playStageBgm(asset: ImportedAssetEntity?) {
    if (asset == null) {
      stopBgm()
      return
    }
    val uriStr = resolvePlayableUri(asset) ?: return
    if (currentBgmPathOrUrl == uriStr && exoPlayer?.isPlaying == true) return
    currentBgmPathOrUrl = uriStr
    _activeBgmTitle.value = asset.name

    mainHandler.post {
      runCatching {
        val player = ensureExoPlayer() ?: return@runCatching
        player.setMediaItem(MediaItem.fromUri(Uri.parse(uriStr)))
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.volume = if (_isMuted.value) 0f else 0.7f
        player.prepare()
        player.playWhenReady = !_isMuted.value
      }
    }
  }

  fun previewAudio(asset: ImportedAssetEntity) {
    val uriStr = resolvePlayableUri(asset) ?: return
    currentBgmPathOrUrl = uriStr
    _activeBgmTitle.value = "Preview: ${asset.name}"
    mainHandler.post {
      runCatching {
        val player = ensureExoPlayer() ?: return@runCatching
        player.setMediaItem(MediaItem.fromUri(Uri.parse(uriStr)))
        player.repeatMode = Player.REPEAT_MODE_OFF
        player.volume = if (_isMuted.value) 0f else 0.85f
        player.prepare()
        player.playWhenReady = true
      }
    }
  }

  fun stopBgm() {
    currentBgmPathOrUrl = null
    _activeBgmTitle.value = null
    mainHandler.post {
      runCatching { exoPlayer?.pause() }
    }
  }

  fun preloadSfx(asset: ImportedAssetEntity) {
    val local = asset.localFilePath
    if (local.isBlank() || !File(local).exists()) return
    if (loadedSoundIds.containsKey(asset.id)) return
    runCatching {
      val id = soundPool?.load(local, 1) ?: return
      if (id > 0) loadedSoundIds[asset.id] = id
    }
  }

  fun triggerSfx(asset: ImportedAssetEntity?) {
    if (_isMuted.value || asset == null) return
    val pool = soundPool ?: return
    val local = asset.localFilePath
    if (local.isBlank() || !File(local).exists()) return

    val soundId = loadedSoundIds[asset.id]
    if (soundId != null) {
      runCatching { pool.play(soundId, 0.9f, 0.9f, 1, 0, 1.0f) }
    } else {
      runCatching {
        pool.setOnLoadCompleteListener { sp, sampleId, status ->
          if (status == 0) {
            sp.play(sampleId, 0.9f, 0.9f, 1, 0, 1.0f)
          }
        }
        val id = pool.load(local, 1)
        if (id > 0) loadedSoundIds[asset.id] = id
      }
    }
  }

  fun toggleMute() {
    val nextMuted = !_isMuted.value
    _isMuted.value = nextMuted
    mainHandler.post {
      runCatching {
        exoPlayer?.volume = if (nextMuted) 0f else 0.7f
        if (nextMuted) exoPlayer?.pause() else if (currentBgmPathOrUrl != null) exoPlayer?.play()
      }
    }
  }

  fun release() {
    mainHandler.post {
      runCatching { exoPlayer?.release() }
      exoPlayer = null
    }
    runCatching { soundPool?.release() }
    soundPool = null
    loadedSoundIds.clear()
  }

  private fun resolvePlayableUri(asset: ImportedAssetEntity): String? {
    val local = asset.localFilePath
    if (local.isNotBlank() && File(local).exists()) {
      return Uri.fromFile(File(local)).toString()
    }
    return asset.sourceUrl.takeIf { it.isNotBlank() }
  }
}
