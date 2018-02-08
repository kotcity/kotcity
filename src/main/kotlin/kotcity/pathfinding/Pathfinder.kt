package kotcity.pathfinding

import kotcity.data.BlockCoordinate
import kotcity.data.Building
import kotcity.data.CityMap
import kotcity.data.Tradeable

const val MAX_DISTANCE = 50f

object Pathfinder {
    fun findNearestLabor(cityMap: CityMap, start: List<BlockCoordinate>, quantity: Int = 1): BlockCoordinate? {
        // OK! what we want to do here is find the nearest labor

        // TODO: find the cheapest labor by transit...
        // this just sloppily returns the first one...

        // step 1! get buildings within radius...
        return start.flatMap { coordinate ->
            val buildings = cityMap.nearestBuildings(coordinate, MAX_DISTANCE)

            // OK now we want only ones with labor...
            buildings.filter {
                val building = it.second
                building.tradeableAvailable(Tradeable.LABOR, quantity)
            }
        }.distinct().firstOrNull()?.first

    }

    fun pathToNearestLabor(cityMap: CityMap, start: List<BlockCoordinate>, quantity: Int = 1): List<BlockCoordinate> {
        val nearest = findNearestLabor(cityMap, start, quantity) ?: return emptyList()
        // ok now we want to find a path...
        return tripTo(listOf(nearest), nearest)
    }

    private fun tripTo(source: List<BlockCoordinate>, destination: BlockCoordinate): List<BlockCoordinate> {
        // TODO: A* PATHFINDING!

        // At initialization add the starting location to the open list and empty the closed list
        // While there are still more possible next steps in the open list and we haven’t found the target:
        //    Select the most likely next step (based on both the heuristic and path costs)
        //    Remove it from the open list and add it to the closed
        //    Consider each neighbor of the step. For each neighbor:
        //       Calculate the path cost of reaching the neighbor
        //       If the cost is less than the cost known for this location
        //              then remove it from the open or closed lists (since we’ve now found a better route)
        //       If the location isn’t in either the open or closed list
        //              then record the costs for the location and
        //              add it to the open list (this means it’ll be considered in the next search).
        //              Record how we got to this location

        val openList = mutableListOf(*source.toTypedArray())
        val closedList = mutableListOf<BlockCoordinate>()

        while (openList.count() > 0) {
            val activeCoordinate = openList.removeAt(0)
            val neighbors = activeCoordinate.neighbors(1)
            // let's see if its the destination!
        }

        return emptyList()
    }
}