package kotcity.automata

import kotcity.data.*
import kotcity.pathfinding.MAX_DISTANCE
import kotcity.pathfinding.Path
import kotcity.pathfinding.Pathfinder
import kotcity.util.Debuggable

class ResourceFinder(val cityMap: CityMap): Debuggable {
    override var debug = false

    private val pathfinder = Pathfinder(cityMap)

    fun quantityForSaleNearby(tradeable: Tradeable, sourceBlock: BlockCoordinate, maxDistance: Int = 30): Int {
        val locations = cityMap.nearestBuildings(sourceBlock, maxDistance)
        val quantityNearby = locations.sumBy {  location -> location.building.quantityForSale(tradeable) }
        return quantityNearby + cityMap.nationalTradeEntity.quantityForSale(tradeable)
    }

    fun quantityWantedNearby(tradeable: Tradeable, sourceBlock: BlockCoordinate, maxDistance: Int = 30): Int {
        val locations = cityMap.nearestBuildings(sourceBlock, maxDistance)
        val quantityNearby = locations.sumBy {  location -> location.building.quantityWanted(tradeable) }
        return quantityNearby + cityMap.nationalTradeEntity.quantityWanted(tradeable)
    }

    // TODO: refactor this... basically we need "paths to resources" as well as just like how
    // many in the neighborhood...
    fun nearbyAvailableTradeable(tradeable: Tradeable, sourceBlocks: List<BlockCoordinate>, maxDistance: Int): List<Pair<Path, Int>> {
        // OK... we need to find nearby buildings...
        val buildings = sourceBlocks.flatMap { cityMap.nearestBuildings(it, maxDistance) }.distinct()
        // now we gotta make sure they got the resource...
        val buildingsWithResource = buildings.filter { it.building.quantityForSale(tradeable) > 0 }

        val pathsAndQuantity = buildingsWithResource.mapNotNull { location ->
            val buildingBlocks = cityMap.buildingBlocks(location.coordinate, location.building)
            val path = pathfinder.tripTo(sourceBlocks, buildingBlocks)
            if (path == null) {
                null
            } else {
                Pair(path, location.building.quantityForSale(tradeable))
            }
        }.toMutableList()

        val outsidePath = pathfinder.cachedPathToOutside(sourceBlocks)
        if (outsidePath != null) {
            pathsAndQuantity.add(Pair(outsidePath, 999))
        }

        return pathsAndQuantity.toList()
    }

    fun findSource(sourceBlocks: List<BlockCoordinate>, tradeable: Tradeable, quantity: Int): Pair<TradeEntity, Path>? {
        // TODO: we can just do this once for the "center" of the building... (i think)
        val buildings = sourceBlocks.flatMap { cityMap.nearestBuildings(it, kotcity.pathfinding.MAX_DISTANCE) }.distinct().toList()
        // now we gotta make sure they got the resource...
        val buildingsWithResource = buildings.filter { it.building.quantityForSale(tradeable) >= quantity }

        val allBuildingBlocks = buildingsWithResource.flatMap { cityMap.buildingBlocks(it.coordinate, it.building) }.distinct()

        val shortestPath = pathfinder.tripTo(sourceBlocks, allBuildingBlocks)

        var preferredTradeEntity: TradeEntity? = null
        var preferredPath: Path? = null
        // OK! now if we got a path we want to find the building in the last block...
        shortestPath?.blockList()?.last()?.let {
            val location = cityMap.buildingsIn(it).firstOrNull()
            if (location != null) {
                preferredPath = shortestPath
                preferredTradeEntity = CityTradeEntity(it, location.building)
            }
        }

        // we have to find the nearest one now...
        if (preferredTradeEntity == null) {
            // let's try and get a path to the outside...
            // make sure the outside city has a resource before we get too excited and make a path...
            if (cityMap.nationalTradeEntity.quantityForSale(tradeable) >= quantity) {
                val destinationBlock = pathfinder.cachedPathToOutside(sourceBlocks)?.blockList()?.last()
                if (destinationBlock != null) {
                    preferredTradeEntity = cityMap.nationalTradeEntity.outsideEntity(destinationBlock)
                }
            }
        }

        preferredPath?.let {path ->
            preferredTradeEntity?.let { entity ->
                return Pair(entity, path)
            }
        }

        return null
    }

    fun nearestBuyingTradeable(tradeable: Tradeable, sourceBlocks: List<BlockCoordinate>, maxDistance: Int = MAX_DISTANCE): Pair<TradeEntity, Path>? {
        // OK... we need to find nearby buildings...
        val buildings = sourceBlocks.flatMap { cityMap.nearestBuildings(it, maxDistance) }.distinct()
        // now we gotta make sure they got the resource...
        val buildingsWithResource = buildings.filter { it.building.quantityWanted(tradeable) > 0 }

        val allBuildingBlocks = buildingsWithResource.flatMap { cityMap.buildingBlocks(it.coordinate, it.building) }.distinct()

        val shortestPath = pathfinder.tripTo(sourceBlocks, allBuildingBlocks)

        // OK! now if we got a path we want to find the building in the last block...
        shortestPath?.blockList()?.last()?.let {
            val location = cityMap.buildingsIn(it).firstOrNull()
            if (location != null) {
                return Pair(CityTradeEntity(it, location.building), shortestPath)
            }
        }

        if (cityMap.nationalTradeEntity.quantityWanted(tradeable) >= 1) {
            val outsidePath = pathfinder.cachedPathToOutside(sourceBlocks)
            outsidePath?.let {
                return Pair(cityMap.nationalTradeEntity.outsideEntity(it.blockList().last()), it)
            }
        }

        return null
    }


}