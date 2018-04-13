package kotcity.automata

import kotcity.data.*
import kotcity.data.Tunable.MAX_RESOURCE_DISTANCE
import kotcity.pathfinding.Path
import kotcity.pathfinding.Pathfinder
import kotcity.util.Debuggable

class ResourceFinder(val cityMap: CityMap) : Debuggable {
    override var debug = false

    private val pathfinder = Pathfinder(cityMap)

    private var lastOutsidePathFailAt: Long = System.currentTimeMillis()

    fun quantityForSaleNearby(
        tradeable: Tradeable,
        sourceBlock: BlockCoordinate,
        maxDistance: Int = MAX_RESOURCE_DISTANCE
    ): Int {
        return cityMap.nearestBuildings(sourceBlock, maxDistance).sumBy { location ->
            location.building.currentQuantityForSale(tradeable)
        }
    }

    fun quantityWantedNearby(
        tradeable: Tradeable,
        sourceBlock: BlockCoordinate,
        maxDistance: Int = MAX_RESOURCE_DISTANCE
    ): Int {
        return cityMap.nearestBuildings(sourceBlock, maxDistance).sumBy { location ->
            location.building.currentQuantityWanted(tradeable)
        }
    }

    fun findSource(sourceBlocks: List<BlockCoordinate>, tradeable: Tradeable, quantity: Int): Pair<TradeEntity, Path>? {

        // just use the first block to do a nearby query...
        val nearbyBuildings = cityMap.nearestBuildings(sourceBlocks.first(), MAX_RESOURCE_DISTANCE).distinct()

        // now we gotta make sure they got the resource...
        val buildingsWithResource = nearbyBuildings.filter { it.building.currentQuantityForSale(tradeable) >= quantity }

        // we gotta order em by distance...
        val sortedBuildingsWithResource =
            buildingsWithResource.sortedBy { location -> sourceBlocks.first().distanceTo(location.coordinate) }

        if (sortedBuildingsWithResource.isNotEmpty()) {
            debug { "We have ${sortedBuildingsWithResource.size} buildings to potentially buy $tradeable from..." }
        }

        val shortestPath: Path? = firstWithValidPath(sourceBlocks, sortedBuildingsWithResource)
        var preferredTradeEntity: TradeEntity? = null
        var preferredPath: Path? = null

        // OK! now if we got a path we want to find the building in the last block...
        shortestPath?.blocks()?.last()?.let {
            val location = cityMap.locationsAt(it).firstOrNull()
            if (location != null) {
                preferredPath = shortestPath
                preferredTradeEntity = CityTradeEntity(it, location.building)
            }
        }

        // so basically we can't find a nearby building with what we want so let's look to the nation...
        if (cityMap.hasOutsideConnections() && preferredTradeEntity == null) {
            // let's try and get a path to the outside...
            // make sure the outside city has a resource before we get too excited and make a path...
            val pair = possiblePathToOutside(tradeable, quantity, sourceBlocks)
            if (pair != null) {
                preferredPath = pair.second
                preferredTradeEntity = pair.first
            }
        }

        preferredPath?.let { path ->
            preferredTradeEntity?.let { entity ->
                return Pair(entity, path)
            }
        }

        return null
    }

    private fun firstWithValidPath(sourceBlocks: List<BlockCoordinate>, buildingsWithResource: List<Location>): Path? {
        buildingsWithResource.forEach { destinationBuilding ->
            val blocks = destinationBuilding.blocks()
            val path = pathfinder.tripTo(sourceBlocks, blocks)
            if (path != null) {
                return path
            }
        }
        return null
    }

    private fun possiblePathToOutside(
        tradeable: Tradeable,
        quantity: Int,
        sourceBlocks: List<BlockCoordinate>
    ): Pair<TradeEntity, Path>? {
        // OK... don't even bother if we have no outside connections.
        if (!cityMap.hasOutsideConnections()) {
            return null
        }

        // OK what we want to do here is don't try and get a trip to the outside all the time...
        // if we fail we won't even bother for 10 more seconds....
        if (System.currentTimeMillis() - lastOutsidePathFailAt < 30000 ) {
            return null
        }

        if (cityMap.nationalTradeEntity.currentQuantityForSale(tradeable) >= quantity) {
            val path = pathfinder.pathToOutside(sourceBlocks)
            val destinationBlock = path?.blocks()?.last()
            if (destinationBlock != null) {
                return Pair(cityMap.nationalTradeEntity.outsideEntity(destinationBlock), path)
            } else {
                // we failed!
                this.lastOutsidePathFailAt = System.currentTimeMillis()
            }
        }
        return null
    }

    fun nearestBuyingTradeable(
        tradeable: Tradeable,
        sourceBlocks: List<BlockCoordinate>,
        maxDistance: Int = MAX_RESOURCE_DISTANCE
    ): Pair<TradeEntity, Path>? {
        // OK... we need to find nearby buildings...
        val buildings =  cityMap.nearestBuildings(sourceBlocks.first(), maxDistance).distinct()
        // now we gotta make sure they got the resource...
        val buildingsWantingResource = buildings.filter { it.building.currentQuantityWanted(tradeable) > 0 }

        if (buildingsWantingResource.isNotEmpty()) {
            debug { "We have ${buildingsWantingResource.size} buildings to potentially sell $tradeable to..." }
        }

        // we gotta order em by distance...
        val sortedBuildingsWantingResources = buildingsWantingResource.sortedBy { location ->
            sourceBlocks.map { it.distanceTo(location.coordinate) }.min()
        }

        synchronized(sortedBuildingsWantingResources) {
            val shortestPath: Path? = firstWithValidPath(sourceBlocks, sortedBuildingsWantingResources)

            // OK! now if we got a path we want to find the building in the last block...
            shortestPath?.let { path ->
                path.blocks().last().let {
                    val location = cityMap.locationsAt(it).firstOrNull()
                    if (location != null) {
                        if (location.building.currentQuantityWanted(tradeable) > 0) {
                            return Pair(CityTradeEntity(it, location.building), path)
                        }
                    }
                }
            }

            if (cityMap.hasOutsideConnections() && cityMap.nationalTradeEntity.currentQuantityWanted(tradeable) >= 1) {
                val outsidePair = possiblePathToOutside(tradeable, 1, sourceBlocks)
                outsidePair?.let {
                    val tradeEntity = it.first
                    val path = it.second
                    return Pair(tradeEntity, path)
                }
            }

            return null
        }
    }
}
