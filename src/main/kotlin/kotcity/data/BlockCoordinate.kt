package kotcity.data

import kotcity.ui.map.MAX_BUILDING_SIZE
import kotcity.util.intBetween
import kotcity.util.reorder
import java.util.*

data class BlockCoordinate(
    val x: Int,
    val y: Int
) {
    companion object {
        fun iterate(from: BlockCoordinate, to: BlockCoordinate, callback: (BlockCoordinate) -> Unit) {
            val xRange = (from.x..to.x).reorder()
            val yRange = (from.y..to.y).reorder()
            for (x in xRange) {
                for (y in yRange) {
                    callback(BlockCoordinate(x, y))
                }
            }
        }
    }

    private val random = Random()

    fun fuzz(): BlockCoordinate {
        val randX = random.intBetween(-MAX_BUILDING_SIZE, MAX_BUILDING_SIZE)
        val randY = random.intBetween(-MAX_BUILDING_SIZE, MAX_BUILDING_SIZE)
        return BlockCoordinate(x + randX, y + randY)
    }

    fun neighbors(radius: Int = 1): List<BlockCoordinate> {
        val xRange = this.x - radius..this.x + radius
        val yRange = this.y - radius..this.y + radius

        return xRange.flatMap { x ->
            yRange.map { y ->
                BlockCoordinate(x, y)
            }
        }
    }

    fun circle(radius: Int = 1): List<BlockCoordinate> {
        val coords = mutableListOf<BlockCoordinate>()
        for (i in y - radius..y + radius) {
            val di2 = (i - y) * (i - y)
            // iterate through all y-coordinates
            for (j in x - radius..x + radius) {
                // test if in-circle
                if ((j - x) * (j - x) + di2 <= (radius * radius)) {
                    coords.add(BlockCoordinate(j, i))
                }
            }
        }
        return coords
    }

    fun distanceTo(otherCoordinate: BlockCoordinate): Double {
        return Math.sqrt(((this.x - otherCoordinate.x) * (this.x - otherCoordinate.x) + (this.y - otherCoordinate.y) * (this.y - otherCoordinate.y)).toDouble())
    }
}
