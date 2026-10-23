package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

import java.awt.Color
import java.awt.Graphics2D
import java.util.Random

object StreetPainter {
    private const val STREET_HEIGHT = 0.12f
    private const val SIDEWALK_SHARE = 0.25f
    private const val DASH = 14
    private const val MIN_CARS = 3
    private const val MAX_CARS = 7
    private const val MIN_SPEED = 0.25f
    private const val MAX_SPEED = 0.5f
    private const val CAR_LENGTH = 0.17f

    private val sidewalk = Color(190, 186, 178)
    private val curb = Color(150, 148, 144)
    private val asphalt = Color(70, 72, 78)
    private val laneLine = Color(232, 200, 72)
    private val carColors = listOf(Color(180, 40, 36), Color(232, 232, 228), Color(46, 70, 120), Color(54, 56, 60), Color(150, 160, 168))
    private val carGlass = Color(90, 110, 128)
    private val tire = Color(28, 28, 30)

    val PALETTE: List<Color> = listOf(sidewalk, curb, asphalt, laneLine, carGlass, tire) + carColors

    fun streetTop(area: BackdropArea): Int = area.groundY - (area.height * STREET_HEIGHT).toInt().coerceAtLeast(4)

    fun paint(
        g: Graphics2D,
        area: BackdropArea,
    ) {
        val random = Random(area.playSeed)
        val top = streetTop(area)
        val height = area.groundY - top
        val sidewalkHeight = (height * SIDEWALK_SHARE).toInt().coerceAtLeast(1)
        g.color = sidewalk
        g.fillRect(0, top, area.width, sidewalkHeight)
        g.color = curb
        g.fillRect(0, top + sidewalkHeight, area.width, 1)
        g.color = asphalt
        g.fillRect(0, top + sidewalkHeight + 1, area.width, height - sidewalkHeight - 1)
        val laneY = top + sidewalkHeight + (height - sidewalkHeight) / 2
        val dash = area.scaled(DASH)
        g.color = laneLine
        var x = 0
        while (x < area.width) {
            g.fillRect(x, laneY, dash, 1)
            x += dash * 2
        }
        val carLength = (area.height * CAR_LENGTH).toInt().coerceAtLeast(6)
        val track = area.width + carLength * 2
        repeat(MIN_CARS + random.nextInt(MAX_CARS - MIN_CARS + 1)) {
            val farLane = random.nextBoolean()
            val speed = (MIN_SPEED + random.nextFloat() * (MAX_SPEED - MIN_SPEED)) * area.width * area.time
            val travelled = random.nextInt(track) + (if (farLane) -speed else speed).toInt()
            val carX = Math.floorMod(travelled, track) - carLength
            val baseY = if (farLane) laneY - 1 else area.groundY - 1
            paintCar(g, carX, baseY, carLength, carColors[random.nextInt(carColors.size)])
        }
    }

    private fun paintCar(
        g: Graphics2D,
        left: Int,
        baseY: Int,
        length: Int,
        color: Color,
    ) {
        val bodyHeight = (length / 4).coerceAtLeast(2)
        val cabinHeight = (length / 5).coerceAtLeast(2)
        val wheel = (length / 5).coerceAtLeast(2)
        g.color = color
        g.fillRect(left, baseY - wheel / 2 - bodyHeight, length, bodyHeight)
        g.fillRect(left + length / 5, baseY - wheel / 2 - bodyHeight - cabinHeight, length * 3 / 5, cabinHeight)
        g.color = carGlass
        g.fillRect(left + length / 4, baseY - wheel / 2 - bodyHeight - cabinHeight + 1, length / 5, cabinHeight - 1)
        g.fillRect(left + length / 2, baseY - wheel / 2 - bodyHeight - cabinHeight + 1, length / 5, cabinHeight - 1)
        g.color = tire
        g.fillOval(left + length / 8, baseY - wheel, wheel, wheel)
        g.fillOval(left + length - length / 8 - wheel, baseY - wheel, wheel, wheel)
    }
}
