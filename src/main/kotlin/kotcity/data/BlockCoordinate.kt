package kotcity.data

import kotcity.ui.map.MAX_BUILDING_SIZE
import kotcity.util.intBetween
import kotcity.util.reorder
import java.util.*


/**
 * The main coordinate system used in this game. Everything is on an X-Y grid and we use this all over the place.
 */
data class BlockCoordinate(
    val x: Int,
    val y: Int
) {
    companion object {
        /**
         * Used to call a function on rectangle of [BlockCoordinate]s
         * @param from top-left corner to start from
         * @param to bottom-right corner to end at
         * @param callback function that we should call, passing in the current [BlockCoordinate]
         */
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

    /**
     * Used when we want to place something at a coordinate, but not exactly. It returns a coordinate as far away as
     * [MAX_BUILDING_SIZE]
     */
    fun fuzz(): BlockCoordinate {
        val randX = random.intBetween(-Tunable.MAX_BUILDING_SIZE, MAX_BUILDING_SIZE)
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

    fun plus(otherCoordinate: BlockCoordinate): BlockCoordinate {
        return BlockCoordinate(x + otherCoordinate.x, y + otherCoordinate.y)
    }
}
