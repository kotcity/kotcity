package kotcity.data

data class MapCoordinate(val x: Int, val y: Int)

enum class GroundTile { GROUND, WATER }

class CityMap(val width: Int = 512, val height: Int = 512) {
    val groundLayer = mutableMapOf<MapCoordinate, GroundTile>()
}