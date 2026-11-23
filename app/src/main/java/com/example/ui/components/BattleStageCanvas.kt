package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.engine.BattleArenaState
import java.io.File
import kotlin.math.roundToInt
import com.example.model.BossArchetype
import com.example.model.HeroArchetype
import com.example.model.HeroBattleState
import com.example.ui.theme.AstralBorder
import com.example.ui.theme.AstralPanel
import com.example.ui.theme.AtbAmber
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DeepCrystalBlue
import com.example.ui.theme.HpEmerald
import com.example.ui.theme.MagicSapphire
import com.example.ui.theme.MythrilCyan
import com.example.ui.theme.PowerCrimson
import com.example.ui.theme.RelicGold
import com.example.ui.theme.SoulBreakMagenta
import com.example.ui.theme.SynergyAzure
import com.example.ui.theme.VoidNavy
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BattleStageCanvas(
  battleState: BattleArenaState,
  onSelectHero: (Int) -> Unit,
  onTapBossForAttack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "rpgStageLoop")
  val ambientPhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2 * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3600, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "ambientPhase"
  )

  val boss = battleState.bossState
  val stage = battleState.stage
  val heroFrameMap = battleState.partyStates.associate { hs ->
    hs.hero.id to rememberSlicedSpritesheetFrames(hs.spriteSheetPath, hs.spriteCols, hs.spriteRows)
  }
  val bossFrames = rememberSlicedSpritesheetFrames(
    filePath = boss?.bossSpritePath.orEmpty(),
    columns = boss?.bossSpriteCols ?: 1,
    rows = boss?.bossSpriteRows ?: 1
  )

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(18.dp))
      .border(1.5.dp, AstralBorder, RoundedCornerShape(18.dp))
      .background(VoidNavy)
      .testTag("battle_stage_canvas")
  ) {
    if (!boss?.stageBgArtUri.isNullOrBlank()) {
      val bgModel: Any = if (File(boss!!.stageBgArtUri).exists()) File(boss.stageBgArtUri) else boss.stageBgArtUri
      AsyncImage(
        model = bgModel,
        contentDescription = "Stage Concept Art Backdrop",
        contentScale = ContentScale.Crop,
        alpha = 0.35f,
        modifier = Modifier.fillMaxSize()
      )
    }

    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(battleState.partyStates.size) {
          detectTapGestures { tapOffset ->
            val w = size.width.toFloat().coerceAtLeast(1f)
            val h = size.height.toFloat().coerceAtLeast(1f)
            val xf = tapOffset.x / w
            val yf = tapOffset.y / h
            if (xf < 0.52f) {
              // Tapping on the Boss triggers immediate Basic Attack with active hero
              onTapBossForAttack()
            } else {
              // Tapping on right side selects the hero in that vertical lane
              val idx = ((yf - 0.20f) / 0.17f).toInt().coerceIn(0, (battleState.partyStates.size - 1).coerceAtLeast(0))
              onSelectHero(idx)
            }
          }
        }
    ) {
      val w = size.width
      val h = size.height

      // 1. Parallax Sky & Realm Atmosphere
      val skyAlpha = if (!boss?.stageBgArtUri.isNullOrBlank()) 0.55f else 1.0f
      val skyGradient = Brush.verticalGradient(
        colors = listOf(
          Color(0xFF090D1E).copy(alpha = skyAlpha),
          Color(0xFF162044).copy(alpha = skyAlpha),
          Color(0xFF1F1638).copy(alpha = skyAlpha),
          Color(0xFF0D1326).copy(alpha = skyAlpha)
        )
      )
      drawRect(brush = skyGradient, size = size)

      // Starlight Motes & Crystal Pillars
      for (i in 0 until 18) {
        val sx = ((i * 73 + 19) % 100) / 100f * w
        val sy = (((i * 41 + 11) % 60) / 100f * h) + sin(ambientPhase + i) * 4f
        val alpha = (0.35f + 0.35f * sin(ambientPhase * 1.5f + i)).coerceIn(0.1f, 0.85f)
        drawCircle(
          color = if (i % 3 == 0) MythrilCyan.copy(alpha = alpha) else Color.White.copy(alpha = alpha),
          radius = if (i % 4 == 0) 3.5f else 2.2f,
          center = Offset(sx, sy)
        )
      }

      // Horizon Perspective Grid Floor (Classic RPG Battle Floor)
      val floorTopY = h * 0.36f
      drawRect(
        brush = Brush.verticalGradient(
          colors = listOf(Color(0xFF182449).copy(alpha = 0.65f), Color(0xFF080B16)),
          startY = floorTopY,
          endY = h
        ),
        topLeft = Offset(0f, floorTopY),
        size = Size(w, h - floorTopY)
      )

      // Perspective Grid Lines
      for (i in 0..8) {
        val frac = i / 8f
        val startX = w * (0.1f + frac * 0.8f)
        val endX = w * (-0.15f + frac * 1.3f)
        drawLine(
          color = MythrilCyan.copy(alpha = 0.14f),
          start = Offset(startX, floorTopY),
          end = Offset(endX, h),
          strokeWidth = 1.5f
        )
      }
      for (row in 1..4) {
        val y = floorTopY + (h - floorTopY) * (row / 4.5f)
        drawLine(
          color = AstralBorder.copy(alpha = 0.25f),
          start = Offset(0f, y),
          end = Offset(w, y),
          strokeWidth = 1.2f
        )
      }

      // 2. Render Boss Sprite on Left Side (Imported Sprite/Spritesheet or Procedural)
      if (boss != null) {
        val bossLungeOffset = boss.bossAttackAnimProgress * (w * 0.14f)
        val bossCenterX = w * 0.25f + bossLungeOffset
        val bossCenterY = h * 0.56f + sin(ambientPhase) * 6f
        if (bossFrames.isNotEmpty()) {
          val frameIdx = ((ambientPhase / (2f * PI.toFloat())) * boss.bossSpriteFps).toInt() % bossFrames.size
          val bmp = bossFrames[frameIdx]
          val boxSize = (w.coerceAtMost(h) * 0.42f).roundToInt().coerceAtLeast(64)
          drawImage(
            image = bmp,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(bmp.width, bmp.height),
            dstOffset = IntOffset((bossCenterX - boxSize / 2f).roundToInt(), (bossCenterY - boxSize / 2f).roundToInt()),
            dstSize = IntSize(boxSize, boxSize),
            alpha = if (boss.hitFlashProgress > 0.25f) 0.45f else 1.0f
          )
        } else {
          drawBossSprite(
            archetype = boss.stage.bossArchetypeEnum,
            center = Offset(bossCenterX, bossCenterY),
            scale = (w.coerceAtMost(h) / 340f).coerceIn(0.75f, 1.4f),
            hitFlash = boss.hitFlashProgress,
            weaknessColor = boss.stage.weaknessElementEnum.color,
            ambientPhase = ambientPhase
          )
        }
      }

      // 3. Render Party Heroes in Classic FFRK Staggered Formation on Right Side
      battleState.partyStates.forEachIndexed { index, heroState ->
        val isSelected = index == battleState.selectedHeroIndex
        val lungeX = heroState.attackAnimProgress * (w * 0.26f)
        val baseX = w * (0.74f + (index % 2) * 0.06f) - lungeX
        val baseY = h * (0.34f + index * 0.155f)
        val frames = heroFrameMap[heroState.hero.id].orEmpty()

        if (frames.isNotEmpty()) {
          val frameIdx = ((ambientPhase / (2f * PI.toFloat())) * heroState.spriteFps).toInt() % frames.size
          val bmp = frames[frameIdx]
          val sprSize = (28f * density).roundToInt().coerceAtLeast(40)
          drawImage(
            image = bmp,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(bmp.width, bmp.height),
            dstOffset = IntOffset((baseX - sprSize / 2f).roundToInt(), (baseY - sprSize * 0.72f).roundToInt()),
            dstSize = IntSize(sprSize, sprSize),
            alpha = if (heroState.isAlive) 1f else 0.35f
          )
          drawHeroHudBars(heroState = heroState, center = Offset(baseX, baseY), isSelected = isSelected)
        } else {
          drawHeroSprite(
            heroState = heroState,
            center = Offset(baseX, baseY),
            isSelected = isSelected,
            ambientPhase = ambientPhase
          )
        }
      }

      // 4. Render Active Elemental / Slash Particles
      battleState.particles.forEach { p ->
        val alpha = (1f - p.progress).coerceIn(0f, 1f)
        drawCircle(
          color = p.color.copy(alpha = alpha),
          radius = p.radiusDp * (1f + p.progress * 0.8f) * density,
          center = Offset(p.xFraction * w, p.yFraction * h)
        )
      }
    }

    // Top HUD Overlay: Boss HP Bar, KHUX Countdown Badge, Weakness & FFRK Elemental Chain
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
      if (boss != null && stage != null) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          // KHUX Boss Countdown Orb + Boss Name & HP
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                  if (boss.actionCountdown <= 1) DangerCrimson else DeepCrystalBlue
                )
                .border(2.dp, RelicGold, CircleShape)
            ) {
              Text(
                text = "${boss.actionCountdown}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontSize = 15.sp
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = boss.stage.bossName,
                  style = MaterialTheme.typography.titleMedium,
                  color = Color.White,
                  maxLines = 1
                )
                Text(
                  text = "%,d / %,d".format(boss.currentHp, boss.maxHp),
                  style = MaterialTheme.typography.labelSmall,
                  color = RelicGold
                )
              }
              Spacer(modifier = Modifier.height(3.dp))
              LinearProgressIndicator(
                progress = { boss.hpFraction },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(8.dp)
                  .clip(RoundedCornerShape(4.dp)),
                color = if (boss.hpFraction > 0.3f) PowerCrimson else RelicGold,
                trackColor = Color(0xFF281524)
              )
              Spacer(modifier = Modifier.height(3.dp))
              Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                BadgeChip(
                  text = "WEAK: ${stage.weaknessElementEnum.label.uppercase()}",
                  color = stage.weaknessElementEnum.color
                )
                BadgeChip(
                  text = "ATTR: ${stage.bossAttributeEnum.shortCode}",
                  color = stage.bossAttributeEnum.color
                )
                if (boss.isDefBroken) {
                  BadgeChip(text = "DEF -50%", color = MythrilCyan)
                }
                if (boss.isAtkBroken) {
                  BadgeChip(text = "ATK -40%", color = RelicGold)
                }
              }
            }
          }
        }
      }

      // FFRK Elemental Chain Gauge Overlay
      battleState.activeChain?.let { chain ->
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          color = VoidNavy.copy(alpha = 0.88f),
          shape = RoundedCornerShape(10.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, chain.element.color),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "${chain.element.label.uppercase()} CHAIN ${chain.hitCount} HITS!",
              style = MaterialTheme.typography.labelLarge,
              color = chain.element.color
            )
            Text(
              text = "DMG ${(chain.bonusMultiplier * 100).toInt()}% (${"%.1f".format(chain.timeRemainingSeconds)}s)",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White
            )
          }
        }
      }
    }

    // Floating Combat Text Layer (Damage Numbers & Critical Callouts)
    Box(modifier = Modifier.fillMaxSize()) {
      battleState.floatingTexts.forEach { ft ->
        val alpha = (1f - ft.progress * 0.75f).coerceIn(0f, 1f)
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(
              start = (ft.xFraction * 260).coerceIn(8f, 260f).dp,
              top = (ft.yFraction * 210).coerceIn(28f, 190f).dp
            )
        ) {
          if (ft.subText.isNotEmpty()) {
            Text(
              text = ft.subText,
              style = MaterialTheme.typography.labelSmall,
              color = RelicGold.copy(alpha = alpha),
              fontWeight = FontWeight.Bold
            )
          }
          Text(
            text = ft.text,
            style = if (ft.isCritical) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge,
            color = ft.color.copy(alpha = alpha),
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    // Soul Break Cut-In Cinematic Banner
    battleState.soulBreakBanner?.let { banner ->
      Surface(
        color = VoidNavy.copy(alpha = 0.92f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, SoulBreakMagenta),
        modifier = Modifier
          .align(Alignment.Center)
          .fillMaxWidth(0.88f)
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp)
        ) {
          Text(
            text = "SOUL BREAK OVERDRIVE • ${banner.heroName.uppercase()}",
            style = MaterialTheme.typography.labelSmall,
            color = MythrilCyan
          )
          Text(
            text = banner.soulBreakName,
            style = MaterialTheme.typography.headlineMedium,
            color = RelicGold
          )
        }
      }
    }

    // Bottom Engine Telemetry Bar (Optional Toggle)
    if (battleState.showTelemetryOverlay) {
      Surface(
        color = VoidNavy.copy(alpha = 0.82f),
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
      ) {
        Text(
          text = battleState.lastFormulaBreakdown,
          style = MaterialTheme.typography.labelSmall,
          color = MythrilCyan,
          maxLines = 1,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
      }
    }
  }
}

@Composable
private fun BadgeChip(text: String, color: Color) {
  Surface(
    color = color.copy(alpha = 0.18f),
    shape = RoundedCornerShape(6.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.7f))
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = color,
      fontSize = 10.sp,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
    )
  }
}

private fun DrawScope.drawBossSprite(
  archetype: BossArchetype,
  center: Offset,
  scale: Float,
  hitFlash: Float,
  weaknessColor: Color,
  ambientPhase: Float
) {
  val r = 46f * scale
  val flashColor = if (hitFlash > 0.1f) Color.White else weaknessColor

  // Boss Aura Ring on Floor
  drawOval(
    color = flashColor.copy(alpha = 0.25f),
    topLeft = Offset(center.x - r * 1.35f, center.y + r * 0.75f),
    size = Size(r * 2.7f, r * 0.65f)
  )
  drawOval(
    color = flashColor.copy(alpha = 0.65f),
    topLeft = Offset(center.x - r * 1.35f, center.y + r * 0.75f),
    size = Size(r * 2.7f, r * 0.65f),
    style = Stroke(width = 2.5f)
  )

  val bodyColor = when (archetype) {
    BossArchetype.GUARD_ARMOR -> Color(0xFF7E57C2)
    BossArchetype.BEHEMOTH_KING -> Color(0xFFD84315)
    BossArchetype.DARKSIDE_TITAN -> Color(0xFF1A1230)
    BossArchetype.OMEGA_WEAPON -> Color(0xFF00ACC1)
    BossArchetype.CHAOS_SERAPH -> Color(0xFFFFB300)
  }
  val finalBody = if (hitFlash > 0.25f) Color.White else bodyColor

  // Floating Armor Shoulders / Wings (KHUX Guard Armor / FFRK Boss style)
  val wingBob = sin(ambientPhase * 2f) * 6f
  drawRoundRect(
    color = finalBody,
    topLeft = Offset(center.x - r * 1.25f, center.y - r * 0.8f + wingBob),
    size = Size(r * 0.55f, r * 0.9f),
    cornerRadius = CornerRadius(10f, 10f)
  )
  drawRoundRect(
    color = finalBody,
    topLeft = Offset(center.x + r * 0.70f, center.y - r * 0.8f - wingBob),
    size = Size(r * 0.55f, r * 0.9f),
    cornerRadius = CornerRadius(10f, 10f)
  )

  // Main Boss Torso / Core
  val torsoPath = Path().apply {
    moveTo(center.x, center.y - r * 1.15f)
    lineTo(center.x + r * 0.85f, center.y - r * 0.35f)
    lineTo(center.x + r * 0.55f, center.y + r * 0.85f)
    lineTo(center.x - r * 0.55f, center.y + r * 0.85f)
    lineTo(center.x - r * 0.85f, center.y - r * 0.35f)
    close()
  }
  drawPath(torsoPath, color = finalBody)
  drawPath(torsoPath, color = RelicGold, style = Stroke(width = 3f))

  // Glowing Heartless / Mechanical Core Eye
  drawCircle(
    color = RelicGold,
    radius = r * 0.24f,
    center = Offset(center.x, center.y - r * 0.22f)
  )
  drawCircle(
    color = PowerCrimson,
    radius = r * 0.12f,
    center = Offset(center.x, center.y - r * 0.22f)
  )
}

private fun DrawScope.drawHeroSprite(
  heroState: HeroBattleState,
  center: Offset,
  isSelected: Boolean,
  ambientPhase: Float
) {
  val r = 17f * density * 0.55f
  val aliveAlpha = if (heroState.isAlive) 1f else 0.3f

  // Selection & Realm Synergy Floor Ring
  if (isSelected) {
    drawOval(
      color = RelicGold.copy(alpha = 0.85f),
      topLeft = Offset(center.x - r * 1.7f, center.y + r * 0.8f),
      size = Size(r * 3.4f, r * 0.8f),
      style = Stroke(width = 3f)
    )
  } else if (heroState.hasRealmSynergy && heroState.isAlive) {
    drawOval(
      color = SynergyAzure.copy(alpha = 0.55f),
      topLeft = Offset(center.x - r * 1.5f, center.y + r * 0.8f),
      size = Size(r * 3.0f, r * 0.7f),
      style = Stroke(width = 2f)
    )
  }

  // Hero Outfit Color by Archetype
  val heroColor = when (heroState.hero.archetypeEnum) {
    HeroArchetype.KEEPER -> Color(0xFF42A5F5)
    HeroArchetype.KEYBEARER -> Color(0xFFEF5350)
    HeroArchetype.SOLDIER -> Color(0xFF5C6BC0)
    HeroArchetype.ESPER -> Color(0xFFAB47BC)
    HeroArchetype.BLACK_MAGE -> Color(0xFFFFCA28)
    HeroArchetype.DAWNWALKER -> Color(0xFF26C6DA)
  }.copy(alpha = aliveAlpha)

  // Cape / Aura
  val bob = if (heroState.isAlive) sin(ambientPhase * 2f + center.y) * 2.5f else 0f
  drawRoundRect(
    color = heroColor,
    topLeft = Offset(center.x - r * 0.65f, center.y - r * 0.6f + bob),
    size = Size(r * 1.3f, r * 1.45f),
    cornerRadius = CornerRadius(8f, 8f)
  )

  // Hero Head
  drawCircle(
    color = Color(0xFFFFE0B2).copy(alpha = aliveAlpha),
    radius = r * 0.52f,
    center = Offset(center.x, center.y - r * 0.95f + bob)
  )

  // Iconic Weapon (Keyblade / Buster Sword / Staff) pointing left toward Boss
  val weaponLength = r * 1.85f
  val swingAngle = heroState.attackAnimProgress * 0.9f
  val wx = center.x - cos(swingAngle) * weaponLength
  val wy = (center.y - r * 0.2f) - sin(swingAngle) * weaponLength
  drawLine(
    color = if (heroState.soulBreakBars >= 1) RelicGold else MythrilCyan,
    start = Offset(center.x - r * 0.4f, center.y - r * 0.1f + bob),
    end = Offset(wx, wy),
    strokeWidth = 4.5f
  )

  // Sentinel Guard Shield Dome
  if (heroState.isDefending && heroState.isAlive) {
    drawCircle(
      color = MythrilCyan.copy(alpha = 0.28f),
      radius = r * 1.7f,
      center = center
    )
    drawCircle(
      color = MythrilCyan.copy(alpha = 0.85f),
      radius = r * 1.7f,
      center = center,
      style = Stroke(width = 2.2f)
    )
  }

  // Mini HP & ATB Bars under Hero Sprite
  val barW = r * 2.6f
  val barLeft = center.x - barW / 2f
  val hpTop = center.y + r * 1.15f
  val hpFrac = (heroState.currentHp.toFloat() / heroState.effectiveMaxHp.coerceAtLeast(1)).coerceIn(0f, 1f)

  drawRect(
    color = Color(0xFF1A2238),
    topLeft = Offset(barLeft, hpTop),
    size = Size(barW, 4.5f)
  )
  drawRect(
    color = if (hpFrac > 0.35f) HpEmerald else DangerCrimson,
    topLeft = Offset(barLeft, hpTop),
    size = Size(barW * hpFrac, 4.5f)
  )

  // ATB Bar directly below HP bar
  val atbTop = hpTop + 6f
  val atbFrac = (heroState.atbPercent / 100f).coerceIn(0f, 1f)
  drawRect(
    color = Color(0xFF1A2238),
    topLeft = Offset(barLeft, atbTop),
    size = Size(barW, 4f)
  )
  drawRect(
    color = if (heroState.isAtbReady) RelicGold else AtbAmber,
    topLeft = Offset(barLeft, atbTop),
    size = Size(barW * atbFrac, 4f)
  )
}

private fun DrawScope.drawHeroHudBars(
  heroState: HeroBattleState,
  center: Offset,
  isSelected: Boolean
) {
  val r = 17f * density * 0.55f
  if (isSelected) {
    drawOval(
      color = RelicGold.copy(alpha = 0.85f),
      topLeft = Offset(center.x - r * 1.7f, center.y + r * 0.8f),
      size = Size(r * 3.4f, r * 0.8f),
      style = Stroke(width = 3f)
    )
  }
  val barW = r * 2.6f
  val barLeft = center.x - barW / 2f
  val hpTop = center.y + r * 1.15f
  val hpFrac = (heroState.currentHp.toFloat() / heroState.effectiveMaxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
  drawRect(color = Color(0xFF1A2238), topLeft = Offset(barLeft, hpTop), size = Size(barW, 4.5f))
  drawRect(
    color = if (hpFrac > 0.35f) HpEmerald else DangerCrimson,
    topLeft = Offset(barLeft, hpTop),
    size = Size(barW * hpFrac, 4.5f)
  )
  val atbTop = hpTop + 6f
  val atbFrac = (heroState.atbPercent / 100f).coerceIn(0f, 1f)
  drawRect(color = Color(0xFF1A2238), topLeft = Offset(barLeft, atbTop), size = Size(barW, 4f))
  drawRect(
    color = if (heroState.isAtbReady) RelicGold else AtbAmber,
    topLeft = Offset(barLeft, atbTop),
    size = Size(barW * atbFrac, 4f)
  )
}
