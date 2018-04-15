/**
 * Basic data structures
 */
package kotcity.data

import kotcity.data.buildings.Building

/**
 * Represents if we have ground or water
 */
enum class TileType {
    GROUND,
    WATER
}

/**
 * Type of zone, R/C/I
 */
enum class Zone {
    RESIDENTIAL,
    COMMERCIAL,
    INDUSTRIAL
}

/**
 * Represents a pair of a building and its location in the city.
 * @param coordinate (top left) coordinate of building
 * @param building building we are referring to
 */
data class Location(val coordinate: BlockCoordinate, val building: Building) {
    fun blocks(): List<BlockCoordinate> {
        return building.buildingBlocks(coordinate)
    }

    fun overlaps(other: Location): Boolean {
        val xOverlaps =
            (coordinate.x <= other.coordinate.x + other.building.width - 1 && coordinate.x + building.width - 1 >= other.coordinate.x)

        val yOverlaps =
            (coordinate.y <= other.coordinate.y + other.building.height - 1 && coordinate.y + building.height - 1 >= other.coordinate.y)

        return xOverlaps && yOverlaps
    }
}
