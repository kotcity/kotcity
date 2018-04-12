package kotcity.pathfinding

import kotcity.data.BlockCoordinate

/**
 * Direction of travel of node in found paths.
 */

val north = BlockCoordinate(0, -1)
val south = BlockCoordinate(0, 1)
val east = BlockCoordinate(1, 0)
val west = BlockCoordinate(-1, 0)
val stationary = BlockCoordinate(0,0)

enum class Direction {
    NORTH, SOUTH, EAST, WEST, STATIONARY;

    fun toDelta(): BlockCoordinate {
        return when (this) {
            Direction.NORTH -> north
            Direction.SOUTH -> south
            Direction.EAST -> east
            Direction.WEST -> west
            Direction.STATIONARY -> stationary
        }
    }
}
