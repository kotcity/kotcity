package kotcity.automata

import kotcity.data.*
import kotcity.pathfinding.MAX_DISTANCE
import kotcity.pathfinding.Path
import kotcity.pathfinding.Pathfinder
import kotcity.util.Debuggable

const val MAX_RESOURCE_DISTANCE = 300

class ResourceFinder(val cityMap: CityMap): Debuggable {
    override var debug = false

    private val pathfinder = Pathfinder(cityMap)

    fun quantityForSaleNearby(tradeable: Tradeable, sourceBlock: BlockCoordinate, maxDistance: Int = MAX_RESOURCE_DISTANCE): Int {
        val locations = cityMap.nearestBuildings(sourceBlock, maxDistance)
        val quantityNearby = locations.sumBy {  location -> location.building.currentQuantityForSale(tradeable) }
        return quantityNearby
    }

    fun quantityWantedNearby(tradeable: Tradeable, sourceBlock: BlockCoordinate, maxDistance: Int = MAX_RESOURCE_DISTANCE): Int {
        val locations = cityMap.nearestBuildings(sourceBlock, maxDistance)
        val quantityNearby = locations.sumBy {  location -> location.building.currentQuantityWanted(tradeable) }
        return quantityNearby
    }

    fun findSource(sourceBlocks: List<BlockCoordinate>, tradeable: Tradeable, quantity: Int): Pair<TradeEntity, Path>? {
        // TODO: we can just do this once for the "center" of the building... (i think)
        val nearbyBuildings = sourceBlocks.flatMap { cityMap.nearestBuildings(it, MAX_RESOURCE_DISTANCE) }.distinct()
        // now we gotta make sure they got the resource...
        val buildingsWithResource = nearbyBuildings.filter { it.building.currentQuantityForSale(tradeable) >= quantity }

        debug("We have ${buildingsWithResource.size} buildings to potentially buy $tradeable from...")


        var shortestPath: Path? = null
        var preferredTradeEntity: TradeEntity? = null
        var preferredPath: Path? = null

        buildingsWithResource.firstOrNull {
            val buildingBlocks = cityMap.buildingBlocks(it.coordinate, it.building)
            shortestPath = pathfinder.tripTo(sourceBlocks, buildingBlocks)
            shortestPath != null
        }

        // OK! now if we got a path we want to find the building in the last block...
        shortestPath?.blocks()?.last()?.let {
            val location = cityMap.locationsAt(it).firstOrNull()
            if (location != null) {
                preferredPath = shortestPath
                preferredTradeEntity = CityTradeEntity(it, location.building)
            }
        }

        // so basically we can't find a nearby building with what we want so let's look to the nation...
        if (preferredTradeEntity == null) {
            // let's try and get a path to the outside...
            // make sure the outside city has a resource before we get too excited and make a path...
            val pair = possiblePathToOutside(tradeable, quantity, sourceBlocks)
            if (pair != null) {
                preferredTradeEntity = pair.first
            }
        }

        preferredPath?.let {path ->
            preferredTradeEntity?.let { entity ->
                return Pair(entity, path)
            }
        }

        return null
    }

    private var lastOutsidePathFailAt: Long = System.currentTimeMillis()


    private fun possiblePathToOutside(tradeable: Tradeable, quantity: Int, sourceBlocks: List<BlockCoordinate>): Pair<TradeEntity, Path>? {
        // OK what we want to do here is don't try and get a trip to the outside all the time...
        // if we fail we won't even bother for 10 more seconds....
        if (System.currentTimeMillis() - 10000 < lastOutsidePathFailAt) {
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

    // TODO: find each path individually to each building...
    fun nearestBuyingTradeable(tradeable: Tradeable, sourceBlocks: List<BlockCoordinate>, maxDistance: Int = MAX_RESOURCE_DISTANCE): Pair<TradeEntity, Path>? {
        // OK... we need to find nearby buildings...
        val buildings = sourceBlocks.flatMap { cityMap.nearestBuildings(it, maxDistance) }.distinct()
        // now we gotta make sure they got the resource...
        val buildingsWantingResource = buildings.filter { it.building.currentQuantityWanted(tradeable) > 0 }

        debug("We have ${buildingsWantingResource.size} buildings to potentially sell $tradeable to...")

        var shortestPath: Path? = null

        buildingsWantingResource.firstOrNull {
            val buildingBlocks = cityMap.buildingBlocks(it.coordinate, it.building)
            shortestPath = pathfinder.tripTo(sourceBlocks, buildingBlocks)
            shortestPath != null
        }

        // OK! now if we got a path we want to find the building in the last block...
        shortestPath?.let {shortestPath ->
            shortestPath.blocks()?.last()?.let {
                val location = cityMap.locationsAt(it).firstOrNull()
                if (location != null) {
                    if (location.building.currentQuantityWanted(tradeable) > 0) {
                        return Pair(CityTradeEntity(it, location.building), shortestPath)
                    }
                }
            }
        }

        if (cityMap.nationalTradeEntity.currentQuantityWanted(tradeable) >= 1) {
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