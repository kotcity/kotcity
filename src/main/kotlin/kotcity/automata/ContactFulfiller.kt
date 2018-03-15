package kotcity.automata

import kotcity.data.*
import kotcity.pathfinding.Path
import kotcity.util.Debuggable
import kotcity.util.getRandomElements

class ContactFulfiller(val cityMap: CityMap) : Debuggable {

    override var debug = false
    private val resourceFinder = ResourceFinder(cityMap)

    init {
        resourceFinder.debug = true
    }

    fun signContracts(shuffled: Boolean = true) {

        val maxMs = 5000
        val startAt = System.currentTimeMillis()

        val contractCollection = if (shuffled) {
            locationsNeedingContracts().shuffled()
        } else {
            locationsNeedingContracts()
        }

        contractCollection.toList().shuffled().takeWhile { System.currentTimeMillis() - startAt < maxMs }.forEach { entry ->
            val delta = System.currentTimeMillis() - startAt
            if (delta > maxMs) {
                debug("Out of time to sign contracts! Exceeded $maxMs milliseconds!")
                return
            } else {
                handleBuilding(entry, maxMs, delta)
            }
        }

    }

    private fun handleBuilding(entry: Location, maxMs: Int, delta: Long) {
        val coordinate = entry.coordinate
        val building = entry.building

        val buildingTradeEntity = CityTradeEntity(coordinate, building)

        val buildingBlocks = cityMap.buildingBlocks(coordinate, building)
        building.consumes.forEach { tradeable, _ ->
            val needsCount = building.needs(tradeable)

            if (needsCount > 0) {

                val bestSource: Pair<TradeEntity, Path>? = findNearbySource(buildingBlocks, tradeable, needsCount)

                if (bestSource != null) {
                    debug("Found best source for $tradeable... ${bestSource?.first?.description()}!")
                    val otherTradeEntity = bestSource.first
                    val pathToOther = bestSource.second
                    val quantity = building.quantityWanted(tradeable).coerceAtMost(otherTradeEntity.quantityForSale(tradeable))

                    if (quantity > 0) {
                        building.createContract(otherTradeEntity, tradeable, quantity, pathToOther)
                        debug("")
                        debug("${building.name}: Signed contract with ${otherTradeEntity.description()} to buy $quantity $tradeable")
                        debug("${building.name} now requires ${building.needs(tradeable)} $tradeable")
                        debug("${otherTradeEntity.description()} has ${otherTradeEntity.quantityForSale(tradeable)} left.")
                        // debug("New setup: ${building.summarizeContracts()}")
                        debug("Still ${maxMs - delta} millis left to sign contracts...")
                    }

                } else {
                    debug("Could not find $needsCount $tradeable for ${building.name} at $coordinate")
                }
            }
        }

        building.produces.forEach { tradeable, _ ->
            val producesCount = building.quantityForSale(tradeable)
            if (producesCount > 0) {
                if (resourceFinder.quantityWantedNearby(tradeable, coordinate) > 1) {
                    val entityAndPath = resourceFinder.nearestBuyingTradeable(tradeable, buildingBlocks)
                    if (entityAndPath != null) {
                        val otherEntity = entityAndPath.first
                        val path = entityAndPath.second

                        val quantity = otherEntity.quantityWanted(tradeable).coerceAtMost(buildingTradeEntity.quantityForSale(tradeable))

                        // debug("New setup: ${building.summarizeContracts()}")
                        debug("Still ${maxMs - delta} millis left to sign contracts...")

                        if (quantity > 0) {
                            val newContract = Contract(otherEntity, buildingTradeEntity, tradeable, quantity, path)
                            debug("")
                            debug("${building.name}: Signed contract with ${otherEntity.description()} to sell $quantity $tradeable")
                            otherEntity.addContract(newContract)
                            buildingTradeEntity.addContract(newContract)
                            debug("${building.name} now has ${building.supplyCount(tradeable)} $tradeable left to provide.")
                            debug("${otherEntity.description()} still wants to buy ${otherEntity.quantityWanted(tradeable)} $tradeable")
                        }


                    }
                } else {
                    debug("Cannot find any $tradeable nearby. Won't bother with pathfinding...")
                }

            }
        }
    }

    // TODO: this is most likely bugged...
    private fun findNearbySource(buildingBlocks: List<BlockCoordinate>, tradeable: Tradeable, needsCount: Int): Pair<TradeEntity, Path>? {
        (1..needsCount).reversed().forEach {
            val source = resourceFinder.findSource(buildingBlocks, tradeable, it)
            if (source != null) {
                return source
            }
        }
        return null
    }

    private fun locationsNeedingContracts(): List<Location> {
        return cityMap.locations().filter { it.building.needsAnyContracts() }
    }

    private fun entitiesWithContracts(): List<TradeEntity> {
        return cityMap.locations().map { CityTradeEntity(it.coordinate, it.building) }.filter { it.hasAnyContracts() }
    }

    fun terminateRandomContracts() {

        val totalBuildings = cityMap.locations().count()

        if (totalBuildings == 0) {
            return
        }

        val howMany = (entitiesWithContracts().count() * 0.01).toInt()
        debug("Terminating $howMany contracts...")

        cityMap.locations().getRandomElements(howMany)?.forEach { location ->
            val buildings = cityMap.cachedBuildingsIn(location.coordinate)
            val blockAndBuilding = buildings.toList().getRandomElements(1)?.first()
            if (blockAndBuilding != null) {
                val building = blockAndBuilding.building
                building.voidRandomContract()
            }
        }
    }
}