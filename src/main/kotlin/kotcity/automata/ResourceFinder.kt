package kotcity.automata

import kotcity.data.*
import kotcity.pathfinding.Path
import kotcity.pathfinding.Pathfinder

class ResourceFinder(val map: CityMap) {

    val pathfinder = Pathfinder(map)

    // TODO: let's be able to get paths from outside the city...
    fun nearbyAvailableTradeable(tradeable: Tradeable, sourceBlocks: List<BlockCoordinate>, maxDistance: Int): List<Pair<Path, Int>> {
        // OK... we need to find nearby buildings...
        val buildings = sourceBlocks.flatMap { map.nearestBuildings(it, maxDistance.toFloat()) }.distinct()
        // now we gotta make sure they got the resource...
        val buildingsWithResource = buildings.filter { it.building.quantityForSale(tradeable) > 0 }

        val pathsAndQuantity = buildingsWithResource.mapNotNull { location ->
            val buildingBlocks = map.buildingBlocks(location.coordinate, location.building)
            val path = pathfinder.tripTo(sourceBlocks, buildingBlocks)
            if (path == null) {
                null
            } else {
                Pair(path, location.building.quantityForSale(tradeable))
            }
        }.toMutableList()

        val outsidePath = pathfinder.pathToOutside(sourceBlocks)
        if (outsidePath != null) {
            pathsAndQuantity.add(Pair(outsidePath, 999))
        }

        return pathsAndQuantity.toList()
    }

    fun findSource(sourceBlocks: List<BlockCoordinate> , tradeable: Tradeable, quantity: Int): TradeEntity? {
        val buildings = sourceBlocks.flatMap { map.nearestBuildings(it, kotcity.pathfinding.MAX_DISTANCE) }.distinct()
        // now we gotta make sure they got the resource...
        val buildingsWithResource = buildings.filter { it.building.quantityForSale(tradeable) >= quantity }
        val buildingsWithPath = buildingsWithResource.mapNotNull { location ->
            val buildingBlocks = map.buildingBlocks(location.coordinate, location.building)
            val path = pathfinder.tripTo(sourceBlocks, buildingBlocks)
            if (path == null) {
                null
            } else {
                Pair(path, location.building)
            }
        }

        // we have to find the nearest one now...

        var preferredTradeEntity: TradeEntity? = null

        if (buildingsWithPath.count() > 0) {
            // ok so the last link in the path is the actual location...
            val nearestBuildingAndPath = buildingsWithPath.minBy { it.first.distance() }
            if (nearestBuildingAndPath != null) {
                val buildingCoordinate = nearestBuildingAndPath.first.blockList().last()
                preferredTradeEntity = CityTradeEntity(buildingCoordinate, building = nearestBuildingAndPath.second)
            }
        }

        if (preferredTradeEntity == null) {
            // let's try and get a path to the outside...
            val destinationBlock = pathfinder.pathToOutside(sourceBlocks)?.blockList()?.last()
            if (destinationBlock != null) {
                preferredTradeEntity = OutsideTradeEntity(destinationBlock)
            }
        }

        return preferredTradeEntity
    }

    fun nearbyBuyingTradeable(tradeable: Tradeable, sourceBlocks: List<BlockCoordinate>, maxDistance: Int): List<Pair<Path, Int>> {
        // OK... we need to find nearby buildings...
        val buildings = sourceBlocks.flatMap { map.nearestBuildings(it, maxDistance.toFloat()) }.distinct()
        // now we gotta make sure they got the resource...
        val buildingsWithResource = buildings.filter { it.building.quantityWanted(tradeable) > 0 }

        val pathsAndQuantity = buildingsWithResource.mapNotNull { location ->
            val buildingBlocks = map.buildingBlocks(location.coordinate, location.building)
            val path = pathfinder.tripTo(sourceBlocks, buildingBlocks)
            if (path == null) {
                null
            } else {
                Pair(path, location.building.quantityWanted(tradeable))
            }
        }.toMutableList()

        val outsidePath = pathfinder.pathToOutside(sourceBlocks)
        if (outsidePath != null) {
            pathsAndQuantity.add(Pair(outsidePath, 999))
        }

        return pathsAndQuantity.toList()

    }


}