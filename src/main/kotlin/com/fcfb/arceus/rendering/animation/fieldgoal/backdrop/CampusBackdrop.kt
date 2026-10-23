package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

import com.fcfb.arceus.enums.animation.BackdropVariant
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.campus.AdministrationHall
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.campus.AthleticCenter
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.campus.CampusStyle
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.campus.Fieldhouse
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.campus.GeorgianCampus
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.campus.GothicCampus
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.campus.LandmarkBuilding
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.campus.MissionCampus
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.campus.ModernCampus
import java.awt.Color
import java.awt.Graphics2D
import java.util.Random

object CampusBackdrop {
    private const val SEED = 42L
    private const val LAWN_HEIGHT = 0.06f
    private const val HEDGE_HEIGHT = 0.07f
    private const val GAP_MIN = 0.35f
    private const val GAP_MAX = 0.6f
    private const val GAP_TREE_MIN = 0.36f
    private const val GAP_TREE_MAX = 0.58f

    private val lawn = Color(86, 138, 64)
    private val hedge = Color(44, 92, 46)

    private const val LANDMARK_WIDTH_SHARE = 0.86f

    fun paletteOf(variant: BackdropVariant?): List<Color> =
        (landmarkOf(variant)?.palette ?: styleOf(variant).palette) + listOf(lawn, hedge) + TreePainter.PALETTE

    fun paint(
        g: Graphics2D,
        area: BackdropArea,
        variant: BackdropVariant?,
    ) {
        val random = Random(SEED)
        val unit = area.height.toFloat()
        val groundY = area.yAt(LAWN_HEIGHT)
        g.color = lawn
        g.fillRect(0, groundY, area.width, area.groundY - groundY)
        val landmark = landmarkOf(variant)
        if (landmark != null) {
            paintLandmark(g, random, area, landmark, groundY, unit)
            return
        }
        val style = styleOf(variant)
        var x = -(unit * GAP_MIN).toInt() + random.nextInt((unit * GAP_MIN).toInt().coerceAtLeast(1))
        val kinds = ArrayDeque<Int>()
        var previousKind = -1
        while (x < area.width) {
            x = paintTreeGap(g, random, style, x, groundY, unit)
            if (kinds.isEmpty()) kinds.addAll(shuffledKinds(random, style.kindCount, previousKind))
            previousKind = kinds.removeFirst()
            val buildingWidth = style.paintBuilding(g, random, x, groundY, unit, previousKind)
            paintHedge(g, x, groundY, buildingWidth, unit)
            x += buildingWidth
        }
    }

    private fun paintLandmark(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
        landmark: LandmarkBuilding,
        groundY: Int,
        unit: Float,
    ) {
        val width = (area.width * LANDMARK_WIDTH_SHARE).toInt()
        val left = (area.width - width) / 2
        landmark.paint(g, left, groundY, width, unit)
        paintHedge(g, left, groundY, width, unit)
        listOf(left / 3, left * 2 / 3, area.width - left / 3, area.width - left * 2 / 3).forEach { x ->
            TreePainter.deciduous(g, random, x, groundY + 1, (unit * (0.4f + random.nextFloat() * 0.15f)).toInt())
        }
    }

    private fun landmarkOf(variant: BackdropVariant?): LandmarkBuilding? =
        when (variant) {
            BackdropVariant.CAMPUS_ATHLETIC -> AthleticCenter
            BackdropVariant.CAMPUS_ADMIN -> AdministrationHall
            BackdropVariant.CAMPUS_FIELDHOUSE -> Fieldhouse
            else -> null
        }

    private fun shuffledKinds(
        random: Random,
        count: Int,
        previousKind: Int,
    ): List<Int> {
        val kinds = (0 until count).shuffled(random)
        return if (kinds.first() == previousKind && count > 1) kinds.drop(1) + kinds.first() else kinds
    }

    private fun styleOf(variant: BackdropVariant?): CampusStyle =
        when (variant) {
            BackdropVariant.CAMPUS_GOTHIC -> GothicCampus
            BackdropVariant.CAMPUS_MISSION -> MissionCampus
            BackdropVariant.CAMPUS_MODERN -> ModernCampus
            else -> GeorgianCampus
        }

    private fun paintTreeGap(
        g: Graphics2D,
        random: Random,
        style: CampusStyle,
        left: Int,
        groundY: Int,
        unit: Float,
    ): Int {
        val gap = (unit * (GAP_MIN + random.nextFloat() * (GAP_MAX - GAP_MIN))).toInt().coerceAtLeast(4)
        val trees = 1 + random.nextInt(2)
        repeat(trees) { index ->
            val height = (unit * (GAP_TREE_MIN + random.nextFloat() * (GAP_TREE_MAX - GAP_TREE_MIN))).toInt()
            style.paintTree(g, random, left + gap * (index + 1) / (trees + 1), groundY + 1, height)
        }
        return left + gap
    }

    private fun paintHedge(
        g: Graphics2D,
        left: Int,
        groundY: Int,
        width: Int,
        unit: Float,
    ) {
        val hedgeHeight = (unit * HEDGE_HEIGHT).toInt().coerceAtLeast(2)
        val bush = hedgeHeight * 2
        g.color = hedge
        var x = left - bush / 4
        while (x < left + width) {
            g.fillOval(x, groundY - hedgeHeight, bush, hedgeHeight * 2)
            x += bush * 3 / 4
        }
    }
}
