package kotcity.data

import kotcity.util.intBetween
import kotcity.util.reorder
import nl.pvdberg.hashkode.hashKode
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * The main coordinate system used in this game. Everything is on an X-Y grid and we use this all over the place.
 * You will see "blocks", "tiles", etc. This is our unit of measure... it is our KotCity atom.
 * @param x X coordinate
 * @param y Y coordinate
 */
data class BlockCoordinate(
    val x: Int,
    val y: Int
) {
    companion object {
        /**
         * Used for fuzzing...
         */
        private val random = Random()

        /**
         * Used to call a function on rectangle of [BlockCoordinate]s
         * @param from top-left corner to start from
         * @param to bottom-right corner to end at
         * @param callback function that we should call, passing in the current [BlockCoordinate] and returning if the iteration should be continued
         */
        fun iterate(from: BlockCoordinate, to: BlockCoordinate, callback: (BlockCoordinate) -> Boolean) {
            val xRange = (from.x..to.x).reorder()
            val yRange = (from.y..to.y).reorder()
            for (x in xRange) {
                for (y in yRange) {
                    if (!callback(BlockCoordinate(x, y))) {
                        // Allow for exiting the iteration early
                        return
                    }
                }
            }
        }

        /**
         * Used to call a function on rectangle of [BlockCoordinate]s
         * @param from top-left corner to start from
         * @param to bottom-right corner to end at
         * @param callback function that we should call, passing in the current [BlockCoordinate]
         */
        fun iterateAll(from: BlockCoordinate, to: BlockCoordinate, callback: (BlockCoordinate) -> Unit) {
            iterate(from, to, {
                callback(it)
                true
            })
        }
    }

    /**
     * Used when we want to place something at a coordinate, but not exactly. It returns a coordinate as far away as
     * [amount]
     */
    fun fuzz(amount: Int): BlockCoordinate {
        val randX = random.intBetween(-amount, amount)
        val randY = random.intBetween(-amount, amount)
        return BlockCoordinate(x + randX, y + randY)
    }

    /**
     * Returns a list of neighboring cells, including us
     * @param radius the radius in tiles
     */
    fun neighbors(radius: Int = 1): List<BlockCoordinate> {
        val xRange = this.x - radius..this.x + radius
        val yRange = this.y - radius..this.y + radius

        return xRange.flatMap { x ->
            yRange.map { y ->
                BlockCoordinate(x, y)
            }
        }
    }

    /**
     * Returns the neighboring cell directly above this cell
     */
    fun top() = BlockCoordinate(x, y - 1)

    /**
     * Returns the neighboring cell directly below this cell
     */
    fun bottom() = BlockCoordinate(x, y + 1)

    /**
     * Returns the neighboring cell directly to the left of this cell
     */
    fun left() = BlockCoordinate(x - 1, y)

    /**
     * Returns the neighboring cell directly to the right of this cell
     */
    fun right() = BlockCoordinate(x + 1, y)

    /**
     * Returns a circle-ish area of [BlockCoordinate]s around us to the given radius
     * @param radius the radius in tiles
     */
    fun circle(radius: Int = 1): List<BlockCoordinate> {
        val coordinates = mutableListOf<BlockCoordinate>()
        for (i in y - radius..y + radius) {
            val di2 = (i - y) * (i - y)
            // iterateAll through all y-coordinates
            for (j in x - radius..x + radius) {
                // test if in-circle
                if ((j - x) * (j - x) + di2 <= (radius * radius)) {
                    coordinates.add(BlockCoordinate(j, i))
                }
            }
        }
        return coordinates
    }

    /**
     * Returns the distance to the other [BlockCoordinate]
     * @param other
     */
    fun distanceTo(other: BlockCoordinate): Double {
        return sqrt(((x - other.x) * (x - other.x) + (y - other.y) * (y - other.y)).toDouble())
    }

    /**
     * Returns the distance to the other [BlockCoordinate] only allowing strictly vertical and horizontal paths
     * @param other
     */
    fun manhattanDistanceTo(other: BlockCoordinate): Double {
        return abs(x - other.x) + abs(y - other.y).toDouble()
    }

    /**
     * Adds two [BlockCoordinate] together vector style. like -> v1.x + v2.x, v1.y + v2.y
     * @param other the other coordinate
     * @return a new coordinate based on the sum of each of the xy
     */
    operator fun plus(other: BlockCoordinate): BlockCoordinate = BlockCoordinate(x + other.x, y + other.y)

    override fun equals(other: Any?) = other is BlockCoordinate && x == other.x && y == other.y

    override fun hashCode() = hashKode(x, y)
}
