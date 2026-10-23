package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.lake

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.TreePainter
import java.awt.Color
import java.awt.Graphics2D
import java.util.Random

object FlatShore : LakeShore {
    private const val LAND_HEIGHT = 0.035f
    private const val TREE_HEIGHT = 0.06f
    private const val CLUMPS = 7
    private const val CLUMP_WIDTH = 0.08f
    private const val HOUSES = 4

    private val land = Color(104, 136, 92)
    private val distantTrees = Color(62, 98, 66)
    private val houseWall = Color(232, 228, 216)
    private val houseRoof = Color(96, 84, 78)

    override val horizonShare = 0.34f
    override val reflection: Color = Color(72, 130, 162)
    override val palette: List<Color> = listOf(land, distantTrees, houseWall, houseRoof, reflection) + TreePainter.PALETTE

    override fun paintFarShore(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
        horizonY: Int,
    ) {
        val landHeight = (area.height * LAND_HEIGHT).toInt().coerceAtLeast(2)
        g.color = land
        g.fillRect(0, horizonY - landHeight, area.width, landHeight)
        val treeHeight = (area.height * TREE_HEIGHT).toInt().coerceAtLeast(3)
        val clumpWidth = (area.width * CLUMP_WIDTH).toInt()
        repeat(CLUMPS) { clump ->
            val start = area.width * clump / CLUMPS + random.nextInt((area.width / CLUMPS - clumpWidth).coerceAtLeast(1))
            var x = start
            while (x < start + clumpWidth) {
                TreePainter.deciduous(
                    g,
                    random,
                    x,
                    horizonY - landHeight + 1,
                    (treeHeight * (0.6f + random.nextFloat() * 0.5f)).toInt(),
                    distantTrees,
                )
                x += (treeHeight / 2).coerceAtLeast(2)
            }
        }
        val houseWidth = (treeHeight * 0.9f).toInt().coerceAtLeast(3)
        repeat(HOUSES) {
            val left = random.nextInt(area.width)
            val wallTop = horizonY - landHeight - houseWidth / 2
            g.color = houseWall
            g.fillRect(left, wallTop, houseWidth, houseWidth / 2)
            g.color = houseRoof
            g.fillPolygon(
                intArrayOf(left - 1, left + houseWidth / 2, left + houseWidth + 1),
                intArrayOf(wallTop, wallTop - houseWidth / 3, wallTop),
                3,
            )
        }
    }
}
