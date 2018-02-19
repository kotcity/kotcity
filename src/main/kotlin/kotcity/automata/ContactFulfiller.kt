package kotcity.automata

import kotcity.data.CityMap
import kotcity.util.getRandomElements

class ContactFulfiller(val cityMap: CityMap) {

    var debug = false
    val resourceFinder = ResourceFinder(cityMap)

    fun signContracts() {
        cityMap.buildingLayer.forEach { coordinate, building ->
            building.consumes.forEach { tradeable, _ ->
                val needsCount = building.needs(tradeable)
                if (needsCount > 0) {
                    val nearby = resourceFinder.findSource(cityMap.buildingBlocks(coordinate, building),tradeable, 1)
                    if (nearby != null) {
                        if (debug) {
                            println()
                        }
                        building.createContract(nearby, tradeable, 1)
                        if (debug) {
                            println("${building.name}: Signed contract with ${nearby.name} for 1 $tradeable")
                            println("${building.name} now requires ${building.needs(tradeable)} $tradeable")
                            println("New setup: ${building.summarizeContracts()}")
                        }
                    } else {
                        if (debug) {
                            println("Could not find $needsCount $tradeable for ${building.name} at $coordinate")
                        }
                    }
                }
            }
        }
    }

    fun terminateRandomContracts() {
        // be chaotic and kill 3 contracts...
        cityMap.buildingLayer.keys.toList().getRandomElements(3)?.forEach { block ->
            val buildings = cityMap.buildingsIn(block)
            val blockAndBuilding = buildings.toList().getRandomElements(1)?.first()
            if (blockAndBuilding != null) {
                val building = blockAndBuilding.building
                building.voidRandomContract()
            }
        }
    }
}