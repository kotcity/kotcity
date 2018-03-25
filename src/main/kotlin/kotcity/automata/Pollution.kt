package kotcity.automata

import kotcity.data.BlockCoordinate
import kotcity.data.CityMap
import kotcity.data.Road
import kotcity.util.Debuggable

private const val EVAPORATION_FACTOR = 0.90
private const val DIFFUSION_FACTOR = 0.90

class PollutionUpdater(val cityMap: CityMap): Debuggable {
    override var debug: Boolean = false

    fun tick() {
        generate()
        diffuse()
        evaporate()
    }

    private fun generate() {
        synchronized(cityMap.pollutionLayer) {
            cityMap.locations().forEach { location ->
                val blocksForBuilding: List<BlockCoordinate> = location.blocks()
                blocksForBuilding.forEach {
                    val pollutionGenerated = if (location.building is Road) {
                        // each 100 cars generates 1 pollution...
                        (cityMap.trafficLayer[location.coordinate] ?: 0.0 * 0.01)
                    } else {
                        location.building.pollution
                    }
                    val oldPollution = cityMap.pollutionLayer[it] ?: 0.0
                    cityMap.pollutionLayer[it] = oldPollution + pollutionGenerated
                }
            }
        }
    }

    private fun diffuse() {
        synchronized(cityMap.pollutionLayer) {
            val blocksWithPollution = cityMap.pollutionLayer.filterValues { it > 0.0 }
            blocksWithPollution.toList().shuffled().forEach {
                val (coordinate, pollutionValue) = it
                val neighbors = coordinate.neighbors(1)
                neighbors.forEach {
                    // our neighbors pollution is the AVERAGE of (p * DIFFUSION_FACTOR) of us and the neighbor...
                    cityMap.pollutionLayer[it] = listOf(cityMap.pollutionLayer[coordinate] ?: 0.0, (pollutionValue * DIFFUSION_FACTOR)).average()
                }
            }
        }
    }

    private fun evaporate() {
        synchronized(cityMap.pollutionLayer) {
            val blocksWithPollution = cityMap.pollutionLayer.filterValues { it > 0.0 }
            blocksWithPollution.keys.forEach {
                cityMap.pollutionLayer[it] = cityMap.pollutionLayer[it] ?: 0.0 * EVAPORATION_FACTOR
            }
        }
    }

}