package kotcity.data

data class BlockCoordinate(val x: Int, val y: Int)

enum class TileType { GROUND, WATER}
data class MapTile(val type: TileType, val elevation: Double)

class CityMap(val width: Int = 512, val height: Int = 512) {
    val groundLayer = mutableMapOf<BlockCoordinate, MapTile>()
}