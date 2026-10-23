package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.mountains

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Path2D
import java.util.Random

object DesertMesas : MountainStyle {
    private const val FLOOR_HEIGHT = 0.14f
    private const val STRATA_BANDS = 4
    private const val SHADOW_SHARE = 0.22f

    private class Rock(
        val face: Color,
        val shadow: Color,
        val strata: Color?,
        val minHeight: Float,
        val maxHeight: Float,
        val minWidth: Float,
        val maxWidth: Float,
        val gapShare: Float,
    )

    private val farRock = Rock(Color(212, 170, 146), Color(196, 154, 132), null, 0.36f, 0.56f, 0.7f, 1.4f, 1.6f)
    private val nearRock = Rock(Color(180, 94, 60), Color(140, 70, 46), Color(202, 124, 82), 0.5f, 0.86f, 0.5f, 0.9f, 3.0f)
    private val sand = Color(216, 182, 132)
    private val scrub = Color(128, 124, 78)
    private val cactus = Color(84, 112, 66)

    override val palette: List<Color> =
        listOf(farRock.face, farRock.shadow, nearRock.face, nearRock.shadow, nearRock.strata!!, sand, scrub, cactus)

    override fun paint(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
    ) {
        paintRow(g, random, area, farRock)
        paintRow(g, random, area, nearRock)
        paintFloor(g, random, area)
    }

    private fun paintRow(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
        rock: Rock,
    ) {
        val unit = area.height.toFloat()
        var x = -random.nextInt((unit * rock.minWidth).toInt().coerceAtLeast(1))
        while (x < area.width) {
            val width = (unit * (rock.minWidth + random.nextFloat() * (rock.maxWidth - rock.minWidth))).toInt().coerceAtLeast(6)
            val height = (unit * (rock.minHeight + random.nextFloat() * (rock.maxHeight - rock.minHeight))).toInt()
            paintMesa(g, area, rock, x, width, height)
            x += width + (unit * rock.gapShare * (0.5f + random.nextFloat() * 0.5f)).toInt()
        }
    }

    private fun paintMesa(
        g: Graphics2D,
        area: BackdropArea,
        rock: Rock,
        left: Int,
        width: Int,
        height: Int,
    ) {
        val top = area.groundY - height
        val slope = width / 7
        val skirt = width / 6
        val mesa = Path2D.Float()
        mesa.moveTo((left - skirt).toDouble(), area.groundY.toDouble())
        mesa.lineTo(left.toDouble(), (top + height * 0.7).toDouble())
        mesa.lineTo((left + slope / 2).toDouble(), top.toDouble())
        mesa.lineTo((left + width - slope / 2).toDouble(), top.toDouble())
        mesa.lineTo((left + width).toDouble(), (top + height * 0.7).toDouble())
        mesa.lineTo((left + width + skirt).toDouble(), area.groundY.toDouble())
        mesa.closePath()
        g.color = rock.face
        g.fill(mesa)
        val previousClip = g.clip
        g.clip(mesa)
        rock.strata?.let { strata ->
            g.color = strata
            for (band in 1..STRATA_BANDS) {
                val y = top + height * band / (STRATA_BANDS + 2)
                g.fillRect(left - skirt, y, width + skirt * 2, (height / 18).coerceAtLeast(1))
            }
        }
        g.color = rock.shadow
        val shadowWidth = (width * SHADOW_SHARE).toInt()
        g.fillRect(left + width - shadowWidth, top, shadowWidth + skirt, height)
        g.clip = previousClip
    }

    private fun paintFloor(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
    ) {
        val floorTop = area.yAt(FLOOR_HEIGHT)
        g.color = sand
        g.fillRect(0, floorTop, area.width, area.groundY - floorTop)
        val bush = area.scaled(6)
        g.color = scrub
        repeat(area.width / (bush * 4).coerceAtLeast(1)) {
            val x = random.nextInt(area.width)
            val y = floorTop + random.nextInt((area.groundY - floorTop).coerceAtLeast(1))
            g.fillOval(x, y - bush / 2, bush, bush / 2 + 1)
        }
        listOf(0.12f, 0.31f, 0.78f, 0.93f).forEach { share ->
            paintSaguaro(g, (area.width * share).toInt(), area.groundY, (area.height * (0.14f + random.nextFloat() * 0.06f)).toInt())
        }
    }

    private fun paintSaguaro(
        g: Graphics2D,
        centerX: Int,
        groundY: Int,
        height: Int,
    ) {
        val trunk = (height / 6).coerceAtLeast(2)
        g.color = cactus
        g.fillRoundRect(centerX - trunk / 2, groundY - height, trunk, height, trunk, trunk)
        val arm = trunk * 3 / 4
        g.fillRoundRect(centerX - trunk * 2, groundY - height * 2 / 3, arm, height / 3, arm, arm)
        g.fillRect(centerX - trunk * 2, groundY - height / 3 - arm, trunk * 2, arm)
        g.fillRoundRect(centerX + trunk + trunk / 4, groundY - height * 3 / 4, arm, height / 3, arm, arm)
        g.fillRect(centerX, groundY - height / 2 - arm, trunk * 2, arm)
    }
}
