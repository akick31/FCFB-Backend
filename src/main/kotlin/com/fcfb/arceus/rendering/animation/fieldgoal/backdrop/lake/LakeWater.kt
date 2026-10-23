package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.lake

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Path2D
import java.util.Random

object LakeWater {
    const val SHORE_HEIGHT = 0.12f
    private const val REFLECTION_DEPTH = 0.06f
    private const val GRASS_HEIGHT = 0.06f
    private const val SHORE_WAVE_STEP = 24
    private const val SHORE_WAVE_DEPTH = 4
    private const val FOAM_THICKNESS = 2
    private const val SHIMMER_ROWS = 5
    private const val SHIMMER_MIN_LENGTH = 12
    private const val SHIMMER_MAX_LENGTH = 40
    private const val SHIMMER_MIN_GAP = 40
    private const val SHIMMER_MAX_GAP = 120
    private const val NO_BOAT_CHANCE = 0.35f
    private const val MAX_BOATS = 2
    private const val MIN_BOAT_DEPTH = 0.2f
    private const val MAX_BOAT_DEPTH = 0.7f
    private const val BOAT_WIDTH = 16
    private const val BOAT_DRIFT = 0.025f

    private val waterFar = Color(96, 160, 196)
    private val waterMid = Color(64, 132, 176)
    private val waterNear = Color(42, 104, 150)
    private val shimmerColor = Color(170, 210, 230)
    private val foamColor = Color(232, 240, 244)
    private val sandColor = Color(222, 204, 160)
    private val shoreGrass = Color(96, 146, 72)
    private val hullColor = Color(104, 110, 120)
    private val sailColor = Color(212, 220, 226)

    val PALETTE: List<Color> =
        listOf(waterFar, waterMid, waterNear, shimmerColor, foamColor, sandColor, shoreGrass, hullColor, sailColor)

    fun paint(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
        horizonY: Int,
        reflection: Color?,
    ) {
        val shoreY = area.yAt(SHORE_HEIGHT)
        paintWater(g, random, area, horizonY, shoreY, reflection)
        paintSailboats(g, area, horizonY, shoreY)
        paintNearShore(g, area, shoreY)
    }

    private fun paintWater(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
        horizonY: Int,
        shoreY: Int,
        reflection: Color?,
    ) {
        val depth = shoreY - horizonY
        listOf(waterFar, waterMid, waterNear).forEachIndexed { index, shade ->
            val top = horizonY + depth * index / 3
            g.color = shade
            g.fillRect(0, top, area.width, horizonY + depth * (index + 1) / 3 - top + 1)
        }
        reflection?.let {
            g.color = it
            g.fillRect(0, horizonY, area.width, (area.height * REFLECTION_DEPTH).toInt().coerceAtLeast(2))
        }

        g.color = shimmerColor
        repeat(SHIMMER_ROWS) { row ->
            val y = horizonY + depth * (row + 1) / (SHIMMER_ROWS + 1)
            var x = random.nextInt(SHIMMER_MAX_GAP)
            while (x < area.width) {
                val length = area.scaled(SHIMMER_MIN_LENGTH + random.nextInt(SHIMMER_MAX_LENGTH - SHIMMER_MIN_LENGTH))
                g.fillRect(x, y, length, area.scaled(2))
                x += length + area.scaled(SHIMMER_MIN_GAP + random.nextInt(SHIMMER_MAX_GAP - SHIMMER_MIN_GAP))
            }
        }
    }

    private fun paintSailboats(
        g: Graphics2D,
        area: BackdropArea,
        horizonY: Int,
        shoreY: Int,
    ) {
        val random = Random(area.playSeed)
        if (random.nextFloat() < NO_BOAT_CHANCE) return
        repeat(1 + random.nextInt(MAX_BOATS)) {
            val depth = MIN_BOAT_DEPTH + random.nextFloat() * (MAX_BOAT_DEPTH - MIN_BOAT_DEPTH)
            val heading = if (random.nextBoolean()) 1 else -1
            val drift = heading * area.width * BOAT_DRIFT * area.time
            val centerX = (area.width * (0.05f + random.nextFloat() * 0.9f) + drift).toInt()
            val waterlineY = horizonY + ((shoreY - horizonY) * depth).toInt()
            val width = (area.scaled(BOAT_WIDTH) * (0.5f + depth)).toInt().coerceAtLeast(5)
            paintSailboat(g, centerX, waterlineY, width)
        }
    }

    private fun paintSailboat(
        g: Graphics2D,
        centerX: Int,
        waterlineY: Int,
        width: Int,
    ) {
        val hullHeight = (width / 5).coerceAtLeast(2)
        val hull = Path2D.Float()
        hull.moveTo((centerX - width / 2).toDouble(), (waterlineY - hullHeight).toDouble())
        hull.lineTo((centerX + width / 2).toDouble(), (waterlineY - hullHeight).toDouble())
        hull.lineTo((centerX + width / 3).toDouble(), waterlineY.toDouble())
        hull.lineTo((centerX - width / 3).toDouble(), waterlineY.toDouble())
        hull.closePath()
        g.color = hullColor
        g.fill(hull)
        val mastTop = waterlineY - hullHeight - width
        g.fillRect(centerX, mastTop, (width / 20).coerceAtLeast(1), width)
        val sail = Path2D.Float()
        sail.moveTo((centerX + 2).toDouble(), mastTop.toDouble())
        sail.lineTo((centerX + width * 0.42).toDouble(), (waterlineY - hullHeight - 2).toDouble())
        sail.lineTo((centerX + 2).toDouble(), (waterlineY - hullHeight - 2).toDouble())
        sail.closePath()
        val jib = Path2D.Float()
        jib.moveTo((centerX - 2).toDouble(), (mastTop + width / 6).toDouble())
        jib.lineTo((centerX - width * 0.3).toDouble(), (waterlineY - hullHeight - 2).toDouble())
        jib.lineTo((centerX - 2).toDouble(), (waterlineY - hullHeight - 2).toDouble())
        jib.closePath()
        g.color = sailColor
        g.fill(sail)
        g.fill(jib)
    }

    private fun paintNearShore(
        g: Graphics2D,
        area: BackdropArea,
        shoreY: Int,
    ) {
        val waveStep = area.scaled(SHORE_WAVE_STEP)
        val waveDepth = area.scaled(SHORE_WAVE_DEPTH)
        val edge = Path2D.Float()
        edge.moveTo(0.0, area.groundY.toDouble())
        var x = 0
        var up = true
        while (x <= area.width + waveStep) {
            edge.lineTo(x.toDouble(), (shoreY + if (up) 0 else waveDepth).toDouble())
            up = !up
            x += waveStep
        }
        edge.lineTo((area.width + waveStep).toDouble(), area.groundY.toDouble())
        edge.closePath()
        g.color = foamColor
        g.fill(edge)
        val previousClip = g.clip
        g.clip(edge)
        g.color = sandColor
        g.fillRect(0, shoreY + area.scaled(FOAM_THICKNESS), area.width, area.groundY - shoreY)
        g.clip = previousClip
        val grassTop = area.yAt(GRASS_HEIGHT)
        g.color = shoreGrass
        g.fillRect(0, grassTop, area.width, area.groundY - grassTop)
    }
}
