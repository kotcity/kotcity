/**
 * Basic data structures
 */
package kotcity.data

import javafx.scene.paint.Color

/**
 * Represents if we have ground or water
 */
enum class TileType(val color: Color) {
    GROUND(Color.rgb(37, 96, 37)),
    WATER(Color.DARKBLUE)
}

/**
 * Type of zone, R/C/I
 * @param color Color to render as...
 * @TODO why should we have color in this simulator detail? This is probably best moved to renderer.
 */
enum class Zone(val color: Color) {
    RESIDENTIAL(Color.DARKGREEN),
    COMMERCIAL(Color.DARKBLUE),
    INDUSTRIAL(Color.LIGHTGOLDENRODYELLOW)
}

/**
 * Represents a pair of a building and its location in the city.
 * @param cityMap map to use
 * @param coordinate (top left) coordinate of building
 * @param building building we are referring to
 */
data class Location(val cityMap: CityMap, val coordinate: BlockCoordinate, val building: Building) {

    /**
     * Returns the "footprint" of the building in [BlockCoordinate]s
     */
    fun blocks(): List<BlockCoordinate> {
        return cityMap.buildingBlocks(coordinate, building)
    }
}
