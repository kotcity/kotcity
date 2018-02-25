package kotcity.automata

import kotcity.data.BlockCoordinate
import kotcity.data.CityMap

class TrafficCalculator(val cityMap: CityMap) {
    fun tick() {
        val newTrafficLayer = mutableMapOf<BlockCoordinate, Double>().withDefault { 0.0 }
        cityMap.locations().forEach { location ->
            location.building.contracts.flatMap {
                it.path?.blockList() ?: emptyList()
            }.forEach { coordinate ->
                newTrafficLayer[coordinate] = (newTrafficLayer[coordinate] ?: 0.0) + 1
            }
        }
        cityMap.trafficLayer = newTrafficLayer
    }
}