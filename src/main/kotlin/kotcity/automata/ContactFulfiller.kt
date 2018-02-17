package kotcity.automata

import kotcity.data.CityMap

class ContactFulfiller(val cityMap: CityMap) {
    fun tick() {
        cityMap.buildingLayer.forEach { coordinate, building ->
            building.consumes.forEach { tradeable, quantity ->
                val needsCount = building.needs(tradeable)
                if (needsCount > 0) {
                    val nearby = ResourceFinder.findSource(cityMap, cityMap.buildingBlocks(coordinate, building),tradeable, 1)
                    if (nearby != null) {
                        building.createContract(nearby, tradeable, 1)
                        println("Signed contract with $building for $needsCount $tradeable")
                        println("New setup: ${building.summarizeContracts()}")
                    } else {
                        println("Could not find $needsCount $tradeable for ${building.name} at $coordinate")
                    }
                }
            }
        }
    }
}