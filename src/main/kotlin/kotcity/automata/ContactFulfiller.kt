package kotcity.automata

import kotcity.data.CityMap
import kotcity.util.getRandomElements

class ContactFulfiller(val cityMap: CityMap) {
    fun tick() {
        cityMap.buildingLayer.forEach { coordinate, building ->
            building.consumes.forEach { tradeable, quantity ->
                val needsCount = building.needs(tradeable)
                if (needsCount > 0) {
                    val nearby = ResourceFinder.findSource(cityMap, cityMap.buildingBlocks(coordinate, building),tradeable, 1)
                    if (nearby != null) {
                        // println()
                        building.createContract(nearby, tradeable, 1)
                        // println("${building.name}: Signed contract with ${nearby.name} for 1 $tradeable")
                        // println("${building.name} now requires ${building.needs(tradeable)} $tradeable")
                        // println("New setup: ${building.summarizeContracts()}")
                    } else {
                        println("Could not find $needsCount $tradeable for ${building.name} at $coordinate")
                    }
                }
            }
        }
        // be chatotic and kill 3 contracts...
        cityMap.buildingLayer.keys.toList().getRandomElements(3)?.forEach { block ->
            val buildings = cityMap.buildingsIn(block)
            val blockAndBuilding = buildings.toList().getRandomElements(1)?.first()
            if (blockAndBuilding != null) {
                val coord = blockAndBuilding.first
                val building = blockAndBuilding.second
                val contracts = building.voidRandomContract()
            }
        }
    }
}