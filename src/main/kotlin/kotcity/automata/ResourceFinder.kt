package kotcity.automata

import kotcity.data.BlockCoordinate
import kotcity.data.Building
import kotcity.data.CityMap
import kotcity.data.Tradeable
import kotcity.pathfinding.Path
import kotcity.pathfinding.Pathfinder
import kotcity.util.getRandomElement

object ResourceFinder {
    fun nearbyAvailableTradeable(map: CityMap, tradeable: Tradeable, sourceBlocks: List<BlockCoordinate>, maxDistance: Int): List<Pair<Path, Int>> {
        // OK... we need to find nearby buildings...
        val buildings = sourceBlocks.flatMap { map.nearestBuildings(it, maxDistance.toFloat()) }.distinct()
        // now we gotta make sure they got the resource...
        val buildingsWithResource = buildings.filter { it.second.sellingQuantity(tradeable) > 0 }

        return buildingsWithResource.mapNotNull { coordAndBuilding ->
            val buildingBlocks = map.buildingBlocks(coordAndBuilding.first, coordAndBuilding.second)
            val path = Pathfinder.tripTo(map, sourceBlocks, buildingBlocks)
            if (path == null) {
                null
            } else {
                Pair(path, coordAndBuilding.second.sellingQuantity(tradeable))
            }
        }
    }

    fun findSource(map: CityMap, sourceBlocks: List<BlockCoordinate> , tradeable: Tradeable, quantity: Int): Building? {
        val buildings = sourceBlocks.flatMap { map.nearestBuildings(it, kotcity.pathfinding.MAX_DISTANCE) }.distinct()
        // now we gotta make sure they got the resource...
        val buildingsWithResource = buildings.filter { it.second.sellingQuantity(tradeable) >= quantity }
        val buildingsWithPath = buildingsWithResource.mapNotNull { coordAndBuilding ->
            val buildingBlocks = map.buildingBlocks(coordAndBuilding.first, coordAndBuilding.second)
            val path = Pathfinder.tripTo(map, sourceBlocks, buildingBlocks)
            if (path == null) {
                null
            } else {
                Pair(path, coordAndBuilding.second)
            }
        }
        // we have to find the nearest one now...
        if (buildingsWithPath.count() > 0) {
            return buildingsWithPath.minBy { it.first.distance() }?.second
        }
        return null
    }

    fun nearbyBuyingTradeable(map: CityMap, tradeable: Tradeable, sourceBlocks: List<BlockCoordinate>, maxDistance: Int): List<Pair<Path, Int>> {
        // OK... we need to find nearby buildings...
        val buildings = sourceBlocks.flatMap { map.nearestBuildings(it, maxDistance.toFloat()) }.distinct()
        // now we gotta make sure they got the resource...
        val buildingsWithResource = buildings.filter { it.second.buyingQuantity(tradeable) > 0 }

        return buildingsWithResource.mapNotNull { coordAndBuilding ->
            val buildingBlocks = map.buildingBlocks(coordAndBuilding.first, coordAndBuilding.second)
            val path = Pathfinder.tripTo(map, sourceBlocks, buildingBlocks)
            if (path == null) {
                null
            } else {
                Pair(path, coordAndBuilding.second.buyingQuantity(tradeable))
            }
        }
    }


}