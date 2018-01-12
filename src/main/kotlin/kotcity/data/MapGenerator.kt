package kotcity.data

import kotcity.datae.CityMap
import kotcity.datae.GroundTile
import kotcity.datae.MapCoordinate
import kotcity.noise.OpenSimplexNoise
import java.lang.Math.pow
import java.util.*

class MapGenerator {

    val rng = Random()
    var seaLevel = 0.0

    fun generateMap(f1: Double = 1.0, f2: Double = 2.0, f3: Double = 4.0, exp: Double = 0.5): CityMap {
        val map = CityMap()
        // ok now we want to get some noise and populate that shit...
        val seed = rng.nextLong()
        val noiseGen = OpenSimplexNoise(seed)
        repeat(map.width) { x ->
            repeat(map.height) { y ->

                //   double nx = x/width - 0.5, ny = y/height - 0.5;
                val nx = (x.toDouble() / map.width.toDouble()) - 0.5
                val ny = (y.toDouble() / map.height.toDouble()) - 0.5

                // val randomTile = noiseGen.eval(nx * freq, ny * freq)

                val n1 = (1.0 * noiseGen.eval(f1 * nx, f1 * ny))
                val n2 = (0.5 * noiseGen.eval(f2 * nx, f2 * ny))
                val n3 = (0.25 * noiseGen.eval(f3 * nx, f3 * ny))

                var randomTile = n1 + n2 + n3

                randomTile = pow(randomTile, exp)

                if (x == 0 && y == 0) {
                    println("nx: $nx, ny: $ny")
                    println("n1: $n1, n2: $n2, n3: $n3")
                    println("Our value is: ${randomTile}")
                }

                // println("Sea level is: $seaLevel")

                randomTile -= seaLevel

                if (randomTile > 0) {
                    map.groundLayer[MapCoordinate(y, x)] = GroundTile.GROUND
                } else {
                    map.groundLayer[MapCoordinate(y, x)] = GroundTile.WATER
                }
            }
        }
        return map
    }

}