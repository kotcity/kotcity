package kotcity.automata

import kotcity.data.BlockCoordinate
import kotcity.data.Building
import kotcity.data.CityMap
import kotcity.data.Tradeable
import kotcity.pathfinding.Path
import kotcity.pathfinding.Pathfinder

class ResourceFinder(val map: CityMap) {

    val pathfinder = Pathfinder(map)

    fun nearbyAvailableTradeable(tradeable: Tradeable, sourceBlocks: List<BlockCoordinate>, maxDistance: Int): List<Pair<Path, Int>> {
        // OK... we need to find nearby buildings...
        val buildings = sourceBlocks.flatMap { map.nearestBuildings(it, maxDistance.toFloat()) }.distinct()
        // now we gotta make sure they got the resource...
        val buildingsWithResource = buildings.filter { it.building.quantityForSale(tradeable) > 0 }

        return buildingsWithResource.mapNotNull { location ->
            val buildingBlocks = map.buildingBlocks(location.coordinate, location.building)
            val path = pathfinder.tripTo(sourceBlocks, buildingBlocks)
            if (path == null) {
                null
            } else {
                Pair(path, location.building.quantityForSale(tradeable))
            }
        }
    }

    fun findSource(sourceBlocks: List<BlockCoordinate> , tradeable: Tradeable, quantity: Int): Building? {
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
        if (buildingsWithPath.count() > 0) {
            return buildingsWithPath.minBy { it.first.distance() }?.second
        }
        return null
    }

    fun nearbyBuyingTradeable(tradeable: Tradeable, sourceBlocks: List<BlockCoordinate>, maxDistance: Int): List<Pair<Path, Int>> {
        // OK... we need to find nearby buildings...
        val buildings = sourceBlocks.flatMap { map.nearestBuildings(it, maxDistance.toFloat()) }.distinct()
        // now we gotta make sure they got the resource...
        val buildingsWithResource = buildings.filter { it.building.quantityWanted(tradeable) > 0 }

        return buildingsWithResource.mapNotNull { location ->
            val buildingBlocks = map.buildingBlocks(location.coordinate, location.building)
            val path = pathfinder.tripTo(sourceBlocks, buildingBlocks)
            if (path == null) {
                null
            } else {
                Pair(path, location.building.quantityWanted(tradeable))
            }
        }
    }


}