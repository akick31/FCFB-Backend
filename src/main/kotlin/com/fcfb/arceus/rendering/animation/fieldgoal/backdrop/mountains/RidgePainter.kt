package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.mountains

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.TreePainter
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Path2D
import java.util.Random

object RidgePainter {
    const val SEGMENTS = 96
    private const val SNOW_JAGGEDNESS = 0.035f
    private const val SMOOTHING_RADIUS = 3
    private const val MIN_FACE_DROP = 0.06f
    private const val FACE_VALLEY_DEPTH = 0.45f
    private const val FACE_VALLEY_LEAN = 0.4f
    private const val FACE_SPINE_LEAN = 0.3f
    private const val FACE_SPINE_DEPTH = 0.7f
    private const val TREE_SPACING = 0.45f
    private const val TREE_SINK = 0.35f

    val SNOW_COLOR: Color = Color(244, 246, 250)

    fun paintRange(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
        range: MountainRange,
    ): FloatArray {
        val heights = ridgeline(random, range)
        val shape = shape(area, heights)
        g.color = range.color
        g.fill(shape)
        val previousClip = g.clip
        g.clip(shape)
        if (range.shadow != range.color) paintShadowFaces(g, area, heights, range.shadow)
        range.snowLine?.let { snowLine ->
            g.clip(snowArea(random, area, snowLine))
            g.color = SNOW_COLOR
            g.fill(shape)
            paintShadowFaces(g, area, heights, range.snowShadow)
        }
        g.clip = previousClip
        return heights
    }

    fun paintPinesAlong(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
        heights: FloatArray,
        treeHeightShare: Float,
    ) {
        val treeHeight = (area.height * treeHeightShare).toInt().coerceAtLeast(4)
        val spacing = (treeHeight * TREE_SPACING).toInt().coerceAtLeast(2)
        var x = 0
        while (x < area.width) {
            val index = (x.toLong() * SEGMENTS / area.width).toInt().coerceIn(0, SEGMENTS)
            val ridgeY = yAt(area, heights[index]).toInt()
            val height = (treeHeight * (0.75f + random.nextFloat() * 0.5f)).toInt()
            TreePainter.pine(g, random, x, ridgeY + (height * TREE_SINK).toInt(), height)
            x += spacing + random.nextInt(spacing)
        }
    }

    fun xAt(
        area: BackdropArea,
        index: Int,
    ): Double = area.width.toDouble() * index / SEGMENTS

    fun yAt(
        area: BackdropArea,
        heightFraction: Float,
    ): Double = area.groundY - area.height * heightFraction.toDouble()

    fun shape(
        area: BackdropArea,
        heights: FloatArray,
    ): Path2D.Float {
        val path = Path2D.Float()
        path.moveTo(0.0, area.groundY.toDouble())
        heights.forEachIndexed { index, height -> path.lineTo(xAt(area, index), yAt(area, height)) }
        path.lineTo(area.width.toDouble(), area.groundY.toDouble())
        path.closePath()
        return path
    }

    fun snowArea(
        random: Random,
        area: BackdropArea,
        snowLine: Float,
    ): Path2D.Float {
        val snow = Path2D.Float()
        snow.moveTo(0.0, area.topY.toDouble() - area.height)
        for (index in 0..SEGMENTS) {
            val jag = (random.nextFloat() - 0.5f) * 2f * SNOW_JAGGEDNESS
            snow.lineTo(xAt(area, index), yAt(area, snowLine + jag))
        }
        snow.lineTo(area.width.toDouble(), area.topY.toDouble() - area.height)
        snow.closePath()
        return snow
    }

    private fun ridgeline(
        random: Random,
        range: MountainRange,
    ): FloatArray {
        val heights = FloatArray(SEGMENTS + 1)
        var index = 0
        var peak = random.nextBoolean()
        while (index <= SEGMENTS) {
            heights[index] =
                if (peak) {
                    range.peakLow + random.nextFloat() * (range.peakHigh - range.peakLow)
                } else {
                    range.saddleLow + random.nextFloat() * (range.saddleHigh - range.saddleLow)
                }
            peak = !peak
            index += range.anchorSegments
        }
        var step = range.anchorSegments
        var displacement = range.displacement * (range.peakHigh - range.saddleLow)
        while (step > 1) {
            val half = step / 2
            var start = 0
            while (start + step <= SEGMENTS) {
                val mid = (heights[start] + heights[start + step]) / 2f
                heights[start + half] = mid + (random.nextFloat() - 0.5f) * 2f * displacement
                start += step
            }
            displacement *= range.roughness
            step = half
        }
        return heights
    }

    private fun paintShadowFaces(
        g: Graphics2D,
        area: BackdropArea,
        heights: FloatArray,
        shadow: Color,
    ) {
        g.color = shadow
        val smoothed = smoothed(heights)
        var index = 0
        while (index < SEGMENTS) {
            if (smoothed[index + 1] >= smoothed[index]) {
                index++
                continue
            }
            val peakIndex = index
            while (index < SEGMENTS && smoothed[index + 1] < smoothed[index]) index++
            if (heights[peakIndex] - heights[index] >= MIN_FACE_DROP) g.fill(shadowFace(area, heights, peakIndex, index))
        }
    }

    private fun shadowFace(
        area: BackdropArea,
        heights: FloatArray,
        peakIndex: Int,
        valleyIndex: Int,
    ): Path2D.Float {
        val peakX = xAt(area, peakIndex)
        val peakY = yAt(area, heights[peakIndex])
        val valleyX = xAt(area, valleyIndex)
        val valleyY = yAt(area, heights[valleyIndex])
        val face = Path2D.Float()
        face.moveTo(peakX, peakY)
        for (point in peakIndex + 1..valleyIndex) face.lineTo(xAt(area, point), yAt(area, heights[point]))
        face.lineTo(valleyX - (valleyX - peakX) * FACE_VALLEY_LEAN, valleyY + (area.groundY - valleyY) * FACE_VALLEY_DEPTH)
        face.lineTo(peakX + (valleyX - peakX) * FACE_SPINE_LEAN, peakY + (area.groundY - peakY) * FACE_SPINE_DEPTH)
        face.closePath()
        return face
    }

    private fun smoothed(heights: FloatArray): FloatArray =
        FloatArray(heights.size) { index ->
            val from = (index - SMOOTHING_RADIUS).coerceAtLeast(0)
            val to = (index + SMOOTHING_RADIUS).coerceAtMost(heights.lastIndex)
            (from..to).map { heights[it] }.average().toFloat()
        }
}
