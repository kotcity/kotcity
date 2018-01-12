package kotcity.data

import kotcity.datae.CityMap
import kotcity.datae.GroundTile
import kotcity.datae.MapCoordinate
import kotcity.noise.OpenSimplexNoise

class MapGenerator {
    fun generateMap(): CityMap {
        val map = CityMap()
        // ok now we want to get some noise and populate that shit...
        val seed = 123L
        val noiseGen = OpenSimplexNoise(seed)
        repeat(map.width) { x ->
            repeat(map.height) { y ->
                val randomTile = noiseGen.eval(x.toDouble(), y.toDouble())
                if (randomTile > 0) {
                    map.groundLayer[MapCoordinate(x, y)] = GroundTile.GROUND
                } else {
                    map.groundLayer[MapCoordinate(x, y)] = GroundTile.WATER
                }
            }
        }
        return map
    }
}