package kotcity.data

import kotcity.noise.OpenSimplexNoise
import java.lang.Math.pow
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*


class MapGenerator {

    private val rng = Random()
    var seaLevel = 0.0

    private val resources = listOf("oil", "coal", "gold", "soil")

    private fun round(value: Double, places: Int): Double {
        if (places < 0) throw IllegalArgumentException()

        var bd = BigDecimal(value)
        bd = bd.setScale(places, RoundingMode.HALF_UP)
        // now quantize to nearest 0.05
        return Math.round(bd.toDouble() * 20) / 20.0
    }

    fun generateMap(width: Int, height: Int, f1: Double = 2.0, f2: Double = 10.0, f3: Double = 20.0, exp: Double = 1.0): CityMap {
        val map = CityMap(width, height)
        // ok now we want to get some noise and populate that shit...
        val seed = rng.nextLong()
        val noiseGen = OpenSimplexNoise(seed)
        repeat(map.width) { x ->
            repeat(map.height) { y ->

                val nx = (x.toDouble() / map.width.toDouble()) - 0.5
                val ny = (y.toDouble() / map.height.toDouble()) - 0.5

                val n1 = (1.0 * noiseGen.eval(f1 * nx, f1 * ny))
                val n2 = (0.5 * noiseGen.eval(f2 * nx, f2 * ny))
                val n3 = (0.25 * noiseGen.eval(f3 * nx, f3 * ny))

                var tileElevation = n1 + n2 + n3

                tileElevation = pow(tileElevation, exp)

                tileElevation = round(tileElevation - seaLevel, 2)

                if (tileElevation > 0) {
                    val newTile = MapTile(TileType.GROUND, tileElevation)
                    map.groundLayer[BlockCoordinate(y, x)] = newTile
                } else {
                    val newTile = MapTile(TileType.WATER, tileElevation)
                    map.groundLayer[BlockCoordinate(y, x)] = newTile
                }
            }
        }

        addResources(seed, map)

        println("The cityMap was generated!!!!")
        return map
    }

    private fun addResources(seed: Long, map: CityMap) {
        val f1 = 2.0
        val f2 = 10.0
        val f3 = 20
        val exp = 1.0

        val baseLine = 0.0

        resources.forEachIndexed { n, resourceName ->
            val noiseGen = OpenSimplexNoise(1 + seed + n)
            repeat(map.width) { x ->
                repeat(map.height) { y ->
                    val nx = (x.toDouble() / map.width.toDouble()) - 0.5
                    val ny = (y.toDouble() / map.height.toDouble()) - 0.5

                    val n1 = (1.0 * noiseGen.eval(f1 * nx, f1 * ny))
                    val n2 = (0.5 * noiseGen.eval(f2 * nx, f2 * ny))
                    val n3 = (0.25 * noiseGen.eval(f3 * nx, f3 * ny))

                    var resourceValue = n1 + n2 + n3

                    resourceValue = pow(resourceValue, exp)

                    resourceValue = round(resourceValue - baseLine, 2)
                    map.setResourceValue(resourceName, BlockCoordinate(x, y), resourceValue)
                }
            }
        }
    }
}
