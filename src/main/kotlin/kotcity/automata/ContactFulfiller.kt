package kotcity.automata

import kotcity.data.*
import kotcity.util.Debuggable
import kotcity.util.getRandomElements

class ContactFulfiller(val cityMap: CityMap): Debuggable {

    override var debug = false
    val resourceFinder = ResourceFinder(cityMap)

    fun signContracts() {
        cityMap.buildingLayer.forEach { coordinate, building ->

            val buildingTradeEntity = CityTradeEntity(coordinate, building)

            building.consumes.forEach { tradeable, _ ->
                val needsCount = building.needs(tradeable)
                if (needsCount > 0) {
                    val nearby = resourceFinder.findSource(cityMap.buildingBlocks(coordinate, building),tradeable, 1)
                    if (nearby != null) {
                        building.createContract(nearby, tradeable, 1)
                        debug("")
                        debug("${building.name}: Signed contract with ${nearby.description()} for 1 $tradeable")
                        debug("${building.name} now requires ${building.needs(tradeable)} $tradeable")
                        debug("New setup: ${building.summarizeContracts()}")
                    } else {
                        debug("Could not find $needsCount $tradeable for ${building.name} at $coordinate")
                    }
                }
            }

            building.produces.forEach { tradeable, _ ->
                val producesCount = building.quantityForSale(tradeable)
                if (producesCount > 0) {
                    val nearbyBuying = resourceFinder.nearbyBuyingTradeable(tradeable, cityMap.buildingBlocks(coordinate, building))
                    val nearest = nearbyBuying.minBy { it.first.distance() }
                    if (nearest != null) {
                        val sourceBlock = nearest.first.blockList().last()
                        val buildings = cityMap.buildingsIn(sourceBlock).filter { it.building.type != BuildingType.ROAD }
                        var sourceTradeEntity = if (buildings.count() > 0) {
                            val sourceBuilding = buildings.first()
                            CityTradeEntity(sourceBlock, sourceBuilding.building)
                        } else {
                            // it must be outside the city...
                            cityMap.nationalTradeEntity.outsideEntity(coordinate)
                        }
                        debug("${building.name}: Signed contract with ${sourceTradeEntity.description()} for 1 $tradeable")
                        debug("${building.name} now sends ${building.needs(tradeable)} $tradeable")
                        debug("New setup: ${building.summarizeContracts()}")

                        val newContract = Contract(sourceTradeEntity, buildingTradeEntity, tradeable, 1)
                        sourceTradeEntity.addContract(newContract)
                        buildingTradeEntity.addContract(newContract)
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