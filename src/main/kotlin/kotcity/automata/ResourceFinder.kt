package kotcity.automata

import kotcity.data.BlockCoordinate
import kotcity.data.Building
import kotcity.data.CityMap
import kotcity.data.Tradeable
import kotcity.pathfinding.Path
import kotcity.pathfinding.Pathfinder

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