package kotcity.automata

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import kotcity.data.*
import kotcity.data.Tunable.MAX_RESOURCE_DISTANCE
import kotcity.data.buildings.Building
import kotcity.jmx.KotCity
import kotcity.pathfinding.Path
import kotcity.util.Debuggable
import kotcity.util.randomElements
import kotlinx.coroutines.experimental.*
import java.util.concurrent.TimeUnit

class ContactFulfiller(val cityMap: CityMap) : Debuggable {

    override var debug = false
        set(value) {
            field = value
            resourceFinder.debug = value
        }

    companion object {
        private const val ticksToSkip = 6 // only do our thing every 6 ticks...

        // BUG:
        // shit... this will grow unbound...
        private val tickCounts: LoadingCache<Location, Int> = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build<Location, Int> { _ -> ticksToSkip }
    }

    private val resourceFinder = ResourceFinder(cityMap)
    private val mBean = KotCity

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

                            // see if we even have to bother...
                            val ticksSinceLastContractFulfillment = tickCounts[location] ?: ticksToSkip

                            if (ticksSinceLastContractFulfillment >= ticksToSkip) {
                                tickCounts.put(location, 0)

                                val job = launch(CommonPool, parent = contractJobs) {
                                    handleBuilding(location)
                                    howManyProcessed += 1
                                }
                                jobList.add(job)
                            } else {
                                tickCounts.put(location, ticksSinceLastContractFulfillment + 1)
                                debug { "Skipping $location since it tried to find contracts recently..." }
                            }
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
                debug { "Reached timeout of $maxMillis milliseconds! Cancelling all outstanding jobs!" }
                contractJobs.cancel()
            } finally {
                // update JMX...
                mBean.contractsSigned = howManyProcessed
                mBean.pendingContracts = locationsNeedingContracts().size
            }
        }

        debug { "$howManyNeedContracts buildings needed contracts and we attempted to create $howManyProcessed" }

    }

    private fun handleBuilding(location: Location) {
        val coordinate = location.coordinate
        val building = location.building

        val buildingBlocks = building.buildingBlocks(coordinate)

        handleConsumes(building, buildingBlocks, coordinate)
        handleProduces(building, buildingBlocks, coordinate)
    }

    private fun handleProduces(building: Building, buildingBlocks: List<BlockCoordinate>, coordinate: BlockCoordinate) {

        val buildingTradeEntity = CityTradeEntity(coordinate, building)

        building.produces.forEach { tradeable, _ ->
            var done = false
            val maxAttempts = 5
            var currentAttempt = 0
            while (building.currentQuantityForSale(tradeable) > 0 && !done && currentAttempt < maxAttempts) {
                currentAttempt += 1
                // limit us to selling at most 33% of our output to a single source...
                val forSaleCount = building.currentQuantityForSale(tradeable)
                    .coerceAtMost((building.producesQuantity(tradeable) / 3).coerceAtLeast(1))
                debug { "${building.description} trying to sell $forSaleCount $tradeable" }
                if (resourceFinder.quantityWantedNearby(tradeable, coordinate) > 0) {
                    val entityAndPath =
                        resourceFinder.nearestBuyingTradeable(tradeable, buildingBlocks, MAX_RESOURCE_DISTANCE)

                    if (entityAndPath == null) {
                        debug { "Want to sell $tradeable but can't find a path to a buyer!" }
                        done = true
                    }

                    if (entityAndPath != null) {
                        val otherEntity = entityAndPath.first
                        val path = entityAndPath.second

                        val quantity = otherEntity.currentQuantityWanted(tradeable).coerceAtMost(forSaleCount)

                        debug { "Found a buyer for our $tradeable. It wants $quantity and we are selling $forSaleCount" }

                        if (quantity > 0) {
                            val newContract = Contract(buildingTradeEntity, otherEntity, tradeable, quantity, path)
                            debug { "" }
                            debug { "${building.name}: Signed contract with ${otherEntity.description()} to sell $quantity $tradeable" }
                            otherEntity.addContract(newContract)
                            buildingTradeEntity.addContract(newContract)
                            debug { "${building.name} now has ${building.currentQuantityForSale(tradeable)} $tradeable left to provide." }
                            // debug("${otherEntity.description()} still wants to buy ${otherEntity.currentQuantityWanted(tradeable)} $tradeable")
                        }
                    }
                } else {
                    debug { "Cannot find any place to sell $tradeable nearby. Won't bother with pathfinding..." }
                    done = true
                }
            }

        }
    }

    private fun handleConsumes(building: Building, buildingBlocks: List<BlockCoordinate>, coordinate: BlockCoordinate) {
        building.consumes.forEach { tradeable, _ ->
            var done = false
            val maxAttempts = 5
            var currentAttempt = 0

            if (building.currentQuantityWanted(tradeable) > 0) {

                var maxAvailableNearby: Int = maxAvailableNearby(coordinate, tradeable)

                while (building.currentQuantityWanted(tradeable) > 0 && maxAvailableNearby > 0 && !done && currentAttempt < maxAttempts) {

                    currentAttempt += 1

                    val needsCount = building.currentQuantityWanted(tradeable).coerceAtMost(maxAvailableNearby)
                    debug { "Building $building can obtain $needsCount $tradeable" }
                    val bestSource: Pair<TradeEntity, Path>? = findNearbySource(buildingBlocks, tradeable, needsCount)

                    if (bestSource == null) {
                        debug { "Could not find a source for $tradeable" }
                        done = true
                    }

                    if (bestSource != null) {
                        synchronized(bestSource) {
                            debug { "Found best source for $tradeable... ${bestSource.first.description()}!" }
                            val otherTradeEntity = bestSource.first
                            val pathToOther = bestSource.second
                            val quantity = building.currentQuantityWanted(tradeable)
                                .coerceAtMost(otherTradeEntity.currentQuantityForSale(tradeable))


                            if (quantity > 0) {
                                building.createContract(coordinate, otherTradeEntity, tradeable, quantity, pathToOther)
                                debug { "" }
                                debug { "${building.name}: Signed contract with ${otherTradeEntity.description()} to buy $quantity $tradeable" }
                                debug { "${building.name} now requires ${building.currentQuantityWanted(tradeable)} $tradeable" }
                                debug {
                                    "${otherTradeEntity.description()} has ${otherTradeEntity.currentQuantityForSale(
                                        tradeable
                                    )} left."
                                }
                                // update the max count...
                                maxAvailableNearby = maxAvailableNearby(coordinate, tradeable)
                                // debug("New setup: ${building.summarizeContracts()}")
                            }
                        }
                    } else {
                        debug { "Could not find $needsCount $tradeable for ${building.name} at $coordinate" }
                    }
                }
            }


        }
    }

    private fun maxAvailableNearby(coordinate: BlockCoordinate, tradeable: Tradeable): Int {
        val nearbyBuildings = cityMap.nearestBuildings(coordinate, MAX_RESOURCE_DISTANCE)
        return nearbyBuildings.map { it.building.currentQuantityForSale(tradeable) }.max() ?: 0
    }

    // TODO: this is most likely bugged...
    private fun findNearbySource(
        coordinates: List<BlockCoordinate>,
        tradeable: Tradeable,
        needsCount: Int
    ): Pair<TradeEntity, Path>? {
        (1..needsCount).reversed().forEach { quantity ->
            val source = resourceFinder.findSource(coordinates, tradeable, quantity)
            if (source != null) {
                debug { "Was able to find a source for $needsCount $tradeable!" }
                return source
            } else {
                debug { "Was NOT able to find a source for $needsCount $tradeable" }
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
