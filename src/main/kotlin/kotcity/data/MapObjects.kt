package kotcity.data

import javafx.scene.paint.Color

enum class TileType(val color: Color) {
    GROUND(Color.rgb(37, 96, 37)),
    WATER(Color.DARKBLUE)
}

enum class BuildingType {
    RAILROAD, ROAD, RESIDENTIAL, COMMERCIAL, INDUSTRIAL, POWER_LINE, POWER_PLANT, CIVIC
}

enum class Zone(val color: Color) {
    RESIDENTIAL(Color.DARKGREEN),
    COMMERCIAL(Color.DARKBLUE),
    INDUSTRIAL(Color.LIGHTGOLDENRODYELLOW)
}

data class Location(val cityMap: CityMap, val coordinate: BlockCoordinate, val building: Building) {
    fun blocks(): List<BlockCoordinate> {
        return cityMap.buildingBlocks(coordinate, building)
    }
}
