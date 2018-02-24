package kotcity.automata

import kotcity.data.*
import kotcity.pathfinding.MAX_DISTANCE
import kotcity.pathfinding.Path
import kotcity.pathfinding.Pathfinder
import kotcity.util.Debuggable

class ResourceFinder(val cityMap: CityMap): Debuggable {
    override var debug = false

    private val pathfinder = Pathfinder(cityMap)

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
        val buildings = sourceBlocks.flatMap { cityMap.nearestBuildings(it, kotcity.pathfinding.MAX_DISTANCE) }.distinct()
        // now we gotta make sure they got the resource...
        val buildingsWithResource = buildings.filter { it.building.quantityForSale(tradeable) >= quantity }
        val buildingsWithPath = buildingsWithResource.mapNotNull { location ->
            val buildingBlocks = cityMap.buildingBlocks(location.coordinate, location.building)
            val path = pathfinder.tripTo(sourceBlocks, buildingBlocks)
            if (path == null) {
                null
            } else {
                Pair(path, location.building)
            }
        }

        // we have to find the nearest one now...

        var preferredTradeEntity: TradeEntity? = null
        var preferredPath: Path? = null

        if (buildingsWithPath.count() > 0) {
            // ok so the last link in the path is the actual location...
            val nearestBuildingAndPath = buildingsWithPath.minBy { it.first.distance() }
            if (nearestBuildingAndPath != null) {
                val buildingCoordinate = nearestBuildingAndPath.first.blockList().last()
                preferredTradeEntity = CityTradeEntity(buildingCoordinate, nearestBuildingAndPath.second)
                preferredPath = nearestBuildingAndPath.first
            }
        }

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

        return if (preferredPath != null && preferredTradeEntity != null) {
            Pair(preferredTradeEntity, preferredPath)
        } else {
            null
        }

    }

    fun nearbyBuyingTradeable(tradeable: Tradeable, sourceBlocks: List<BlockCoordinate>, maxDistance: Int = MAX_DISTANCE): List<Pair<Path, Int>> {
        // OK... we need to find nearby buildings...
        val buildings = sourceBlocks.flatMap { cityMap.nearestBuildings(it, maxDistance) }.distinct()
        // now we gotta make sure they got the resource...
        val buildingsWithResource = buildings.filter { it.building.quantityWanted(tradeable) > 0 }

        // TODO: sort by distance... pathfind to the first one...
        val pathsAndQuantity = buildingsWithResource.mapNotNull { location ->
            val buildingBlocks = cityMap.buildingBlocks(location.coordinate, location.building)
            val path = pathfinder.tripTo(sourceBlocks, buildingBlocks)
            if (path == null) {
                null
            } else {
                Pair(path, location.building.quantityWanted(tradeable))
            }
        }.toMutableList()

        val outsidePath = pathfinder.cachedPathToOutside(sourceBlocks)
        if (outsidePath != null) {
            pathsAndQuantity.add(Pair(outsidePath, 999))
        }

        return pathsAndQuantity.sortedWith(compareBy({it.second}))

    }


}