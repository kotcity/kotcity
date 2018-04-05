package kotcity.automata

import kotcity.data.*
import kotcity.data.Tunable.MAX_RESOURCE_DISTANCE
import kotcity.pathfinding.Path
import kotcity.util.Debuggable
import kotcity.util.randomElements
import kotlinx.coroutines.experimental.*

class ContactFulfiller(val cityMap: CityMap) : Debuggable {

    override var debug = false
        set(value) {
            field = value
            resourceFinder.debug = value
        }
    private val resourceFinder = ResourceFinder(cityMap)

    init {
        resourceFinder.debug = debug
    }

    fun signContracts(shuffled: Boolean = true, maxMillis: Long = 5000, parallel: Boolean = true) {
        if (!parallel) {
            locationsNeedingContracts().forEach { handleBuilding(it) }
            return
        }

        var contractCollection = if (shuffled) {
            locationsNeedingContracts().shuffled()
        } else {
            locationsNeedingContracts()
        }

        val howManyNeedContracts = contractCollection.size
        var howManyProcessed = 0

        var lastSize = 1
        var newSize = 0

        // OK this is a little crummier than i would like...
        // basically we boot off a bunch of coroutines to try and set up consumer / producers
        // we keep going until we reach timeout OR until no new contracts are able to be signed...
        runBlocking {
            val contractJobs = Job()
            try {
                withTimeout(maxMillis) {
                    while (contractCollection.isNotEmpty() && newSize < lastSize) {
                        // kick them off in the background...
                        val jobList = mutableListOf<Job>()
                        contractCollection.toList().shuffled().forEach { location: Location ->
                            val job = launch(CommonPool, parent = contractJobs) {
                                handleBuilding(location)
                                howManyProcessed += 1
                            }
                            jobList.add(job)
                        }
                        jobList.forEach { it.join() }
                        // we need to make sure the size is decreasing...
                        lastSize = contractCollection.size
                        contractCollection = if (shuffled) {
                            locationsNeedingContracts().shuffled()
                        } else {
                            locationsNeedingContracts()
                        }
                        newSize = contractCollection.size
                    }
                }
            } catch (ex: CancellationException) {
                debug("Reached timeout of $maxMillis milliseconds! Cancelling all outstanding jobs!")
                contractJobs.cancelAndJoin()
            }
        }

        debug("$howManyNeedContracts buildings needed contracts and we attempted to create $howManyProcessed")
    }

    private fun handleBuilding(entry: Location) {
        val coordinate = entry.coordinate
        val building = entry.building

        val buildingTradeEntity = CityTradeEntity(coordinate, building)

        val buildingBlocks = cityMap.buildingBlocks(coordinate, building)

        synchronized(building) {
            building.consumes.forEach { tradeable, _ ->
                var done = false
                val maxAttempts = 5
                var currentAttempt = 0
                while (building.currentQuantityWanted(tradeable) > 0 && !done && currentAttempt < maxAttempts) {

                    currentAttempt += 1

                    val needsCount = building.currentQuantityWanted(tradeable)
                    debug("Building $building needs $needsCount $tradeable")
                    val bestSource: Pair<TradeEntity, Path>? = findNearbySource(buildingBlocks, tradeable, needsCount)

                    if (bestSource == null) {
                        debug("Could not find a source for $tradeable")
                        done = true
                    }

                    if (bestSource != null) {
                        synchronized(bestSource) {
                            debug("Found best source for $tradeable... ${bestSource.first.description()}!")
                            val otherTradeEntity = bestSource.first
                            val pathToOther = bestSource.second
                            val quantity = building.currentQuantityWanted(tradeable)
                                .coerceAtMost(otherTradeEntity.currentQuantityForSale(tradeable))

                            if (quantity > 0) {
                                building.createContract(otherTradeEntity, tradeable, quantity, pathToOther)
                                debug("")
                                debug("${building.name}: Signed contract with ${otherTradeEntity.description()} to buy $quantity $tradeable")
                                debug("${building.name} now requires ${building.currentQuantityWanted(tradeable)} $tradeable")
                                debug(
                                    "${otherTradeEntity.description()} has ${otherTradeEntity.currentQuantityForSale(
                                        tradeable
                                    )} left."
                                )
                                // debug("New setup: ${building.summarizeContracts()}")
                            }
                        }
                    } else {
                        debug("Could not find $needsCount $tradeable for ${building.name} at $coordinate")
                    }
                }
            }

            building.produces.forEach { tradeable, _ ->
                var done = false
                val maxAttempts = 5
                var currentAttempt = 0
                while (building.currentQuantityForSale(tradeable) > 0 && !done && currentAttempt < maxAttempts) {
                    currentAttempt += 1
                    // limit us to selling at most 33% of our output to a single source...
                    val forSaleCount = building.currentQuantityForSale(tradeable)
                        .coerceAtMost((building.producesQuantity(tradeable) / 3).coerceAtLeast(1))
                    debug("${building.description} trying to sell $forSaleCount $tradeable")
                    if (resourceFinder.quantityWantedNearby(tradeable, coordinate) > 0) {
                        val entityAndPath =
                            resourceFinder.nearestBuyingTradeable(tradeable, buildingBlocks, MAX_RESOURCE_DISTANCE)

                        if (entityAndPath == null) {
                            debug("Want to sell $tradeable but can't find a path to a buyer!")
                            done = true
                        }

                        if (entityAndPath != null) {
                            val otherEntity = entityAndPath.first
                            val path = entityAndPath.second

                            val quantity = otherEntity.currentQuantityWanted(tradeable).coerceAtMost(forSaleCount)

                            debug("Found a buyer for our $tradeable. It wants $quantity and we are selling $forSaleCount")

                            if (quantity > 0) {
                                val newContract = Contract(buildingTradeEntity, otherEntity, tradeable, quantity, path)
                                debug("")
                                debug("${building.name}: Signed contract with ${otherEntity.description()} to sell $quantity $tradeable")
                                otherEntity.addContract(newContract)
                                buildingTradeEntity.addContract(newContract)
                                debug("${building.name} now has ${building.currentQuantityForSale(tradeable)} $tradeable left to provide.")
                                // debug("${otherEntity.description()} still wants to buy ${otherEntity.currentQuantityWanted(tradeable)} $tradeable")
                            }
                        }
                    } else {
                        debug("Cannot find any place to sell $tradeable nearby. Won't bother with pathfinding...")
                        done = true
                    }
                }
            }
        }
    }

    // TODO: this is most likely bugged...
    private fun findNearbySource(
        buildingBlocks: List<BlockCoordinate>,
        tradeable: Tradeable,
        needsCount: Int
    ): Pair<TradeEntity, Path>? {
        (1..needsCount).reversed().forEach { quantity ->
            val source = resourceFinder.findSource(buildingBlocks, tradeable, quantity)
            if (source != null) {
                return source
            }
        }
        return null
    }

    private fun locationsNeedingContracts() = cityMap.locations().filter { it.building.needsAnyContracts() }

    private fun entitiesWithContracts() =
        cityMap.locations().map { CityTradeEntity(it.coordinate, it.building) }.filter { it.hasAnyContracts() }

    fun terminateRandomContracts() {
        val totalBuildings = cityMap.locations().count()

        if (totalBuildings == 0) {
            return
        }

        if (entitiesWithContracts().size > 100) {
            val howMany = 1
            cityMap.locations().randomElements(howMany).forEach { location ->
                val buildings = cityMap.cachedLocationsIn(location.coordinate)
                val blockAndBuilding = buildings.toList().randomElements(1).first()
                val building = blockAndBuilding.building
                building.voidRandomContract()
            }
        }
    }
}
