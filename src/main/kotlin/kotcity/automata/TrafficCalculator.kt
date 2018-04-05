package kotcity.automata

import kotcity.data.BlockCoordinate
import kotcity.data.CityMap

class TrafficCalculator(val cityMap: CityMap) {
    fun tick() {
        val newTrafficLayer = mutableMapOf<BlockCoordinate, Double>().withDefault { 0.0 }
        cityMap.locations().forEach { location ->
            location.building.contracts.flatMap {
                it.path?.blocks()?.map { blockCoordinate -> Pair(blockCoordinate, it.quantity) } ?: emptyList()
            }.forEach {
                val coordinate = it.first
                val volume = it.second
                newTrafficLayer[coordinate] = (newTrafficLayer[coordinate] ?: 0.0) + volume
            }
        }
        cityMap.trafficLayer = newTrafficLayer
    }
}
