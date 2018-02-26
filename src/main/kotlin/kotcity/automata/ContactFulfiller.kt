package kotcity.automata

import kotcity.data.*
import kotcity.util.Debuggable
import kotcity.util.getRandomElements

class ContactFulfiller(val cityMap: CityMap): Debuggable {

    override var debug = false
    private val resourceFinder = ResourceFinder(cityMap)

    fun signContracts() {

        val locationsNeedingContracts: List<Location> = locationsNeedingContracts()

        val maxMs = 60000
        val startAt = System.currentTimeMillis()

        var totalSigned = 0

        locationsNeedingContracts.shuffled().parallelStream().forEach { entry ->
            val delta = System.currentTimeMillis() - startAt
            if (delta > maxMs) {
                debug("Out of time to sign contracts! Exceeded $maxMs milliseconds!")
            } else {
                val coordinate = entry.component1()
                val building = entry.component2()

                val buildingTradeEntity = CityTradeEntity(coordinate, building)

                val buildingBlocks = cityMap.buildingBlocks(coordinate, building)
                building.consumes.forEach { tradeable, _ ->
                    val needsCount = building.needs(tradeable)
                    if (needsCount > 0) {
                        val nearby = resourceFinder.findSource(buildingBlocks,tradeable, 1)
                        if (nearby != null) {
                            val otherTradeEntity = nearby.first
                            val pathToOther = nearby.second

                            val quantity = building.quantityWanted(tradeable).coerceAtMost(otherTradeEntity.quantityForSale(tradeable))

                            if (quantity > 0) {
                                building.createContract(otherTradeEntity, tradeable, quantity, pathToOther)
                                debug("")
                                debug("${building.name}: Signed contract with ${otherTradeEntity.description()} to buy $quantity $tradeable")
                                debug("${building.name} now requires ${building.needs(tradeable)} $tradeable")
                                debug("${otherTradeEntity.description()} has ${otherTradeEntity.quantityForSale(tradeable)} left.")
                                // debug("New setup: ${building.summarizeContracts()}")
                                debug("Still ${maxMs - delta} millis left to sign contracts...")
                                totalSigned += 1
                            }

                        } else {
                            debug("Could not find $needsCount $tradeable for ${building.name} at $coordinate")
                        }
                    }
                }

                building.produces.forEach { tradeable, _ ->
                    val producesCount = building.quantityForSale(tradeable)
                    if (producesCount > 0) {
                        val nearbyBuying = resourceFinder.nearbyBuyingTradeable(tradeable, buildingBlocks)
                        val nearest = nearbyBuying.minBy { it.first.distance() }
                        if (nearest != null) {
                            val sourceBlock = nearest.first.blockList().last()
                            val buildings = cityMap.buildingsIn(sourceBlock).filter { it.building.type != BuildingType.ROAD }
                            var sourceTradeEntity = if (buildings.count() > 0) {
                                val sourceBuilding = buildings.first()
                                CityTradeEntity(sourceBlock, sourceBuilding.building)
                            } else {
                                // it must be outside the city...
                                // jobcenter and etc cannot export...
                                if (building.type != BuildingType.CIVIC && cityMap.nationalTradeEntity.quantityWanted(tradeable) > 0) {
                                    cityMap.nationalTradeEntity.outsideEntity(coordinate)
                                } else {
                                    debug("Would have signed contract outside the city but it didn't want any...")
                                    null
                                }
                            }

                            sourceTradeEntity?.let {


                                val quantity = sourceTradeEntity.quantityWanted(tradeable).coerceAtMost(buildingTradeEntity.quantityForSale(tradeable))

                                // debug("New setup: ${building.summarizeContracts()}")
                                totalSigned += 1
                                debug("Still ${maxMs - delta} millis left to sign contracts...")

                                if (quantity > 0) {
                                    val newContract = Contract(sourceTradeEntity, buildingTradeEntity, tradeable, quantity, nearest.first)
                                    debug("")
                                    debug("${building.name}: Signed contract with ${sourceTradeEntity.description()} to sell $quantity $tradeable")
                                    sourceTradeEntity.addContract(newContract)
                                    buildingTradeEntity.addContract(newContract)
                                    debug("${building.name} now has ${building.supplyCount(tradeable)} $tradeable left to provide.")
                                    debug("${sourceTradeEntity.description()} still wants to buy ${sourceTradeEntity.quantityWanted(tradeable)} $tradeable")
                                }
                            }


                        }
                    }
                }
            }

        }


    }

    private fun locationsNeedingContracts(): List<Location> {
        return cityMap.locations().filter { it.building.needsAnyContracts() }
    }

    private fun entitiesWithContracts(): List<TradeEntity> {
        return cityMap.locations().map { CityTradeEntity(it.coordinate, it.building) }.filter { it.hasAnyContracts() }
    }

    fun terminateRandomContracts() {

        val totalBuildings = cityMap.buildingLayer.keys.count()

        if (totalBuildings == 0) {
            return
        }

        val howMany = (entitiesWithContracts().count() * 0.01).toInt()
        debug("Terminating $howMany contracts...")

        cityMap.buildingLayer.keys.toList().getRandomElements(howMany)?.forEach { block ->
            val buildings = cityMap.buildingsIn(block)
            val blockAndBuilding = buildings.toList().getRandomElements(1)?.first()
            if (blockAndBuilding != null) {
                val building = blockAndBuilding.building
                building.voidRandomContract()
            }
        }
    }
}