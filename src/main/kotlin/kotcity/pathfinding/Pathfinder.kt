package kotcity.pathfinding

import kotcity.data.*
import kotcity.memoization.cache
import kotcity.util.Debuggable

const val MAX_DISTANCE = 50

enum class Direction {
    NORTH, SOUTH, EAST, WEST, STATIONARY
}

enum class TransitType {
    ROAD
}

data class NavigationNode(
    val cityMap: CityMap,
    val coordinate: BlockCoordinate,
    val parent: NavigationNode?,
    val score: Double,
    val transitType: TransitType = TransitType.ROAD,
    val direction: Direction
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) return false
        val that = other as NavigationNode
        return this.coordinate == that.coordinate
    }

    override fun hashCode(): Int {
        return this.coordinate.hashCode()
    }
}


data class Path(
    val nodes: List<NavigationNode> = emptyList()
) {
    // takes traffic and etc into consideration...
    fun length(): Int = nodes.sumBy { it.score.toInt() }

    fun blocks(): List<BlockCoordinate> = nodes.map { it.coordinate }.toList()
}

class Pathfinder(val cityMap: CityMap) : Debuggable {
    override var debug: Boolean = false

    private val cachedHeuristicPair = ::heuristic.cache()
    private val heuristicCache = cachedHeuristicPair.first
    private val cachedHeuristic = cachedHeuristicPair.second

    private val cachedPathToOutsidePair = ::pathToOutside.cache()
    val cachedPathToOutside = cachedPathToOutsidePair.second

    private val cachedTripToPair = ::tripTo.cache()
    val cachedTripTo = cachedTripToPair.second

    private val mapBorders: List<BlockCoordinate> by lazy {
        // I found this frustrating to write and this code
        // can probably be improved upon...
        val widthRange = -1..cityMap.width
        val heightRange = -1..cityMap.height

        val borderBlocks = mutableSetOf<BlockCoordinate>()

        widthRange.forEach { x ->
            borderBlocks.add(BlockCoordinate(x, heightRange.first))
            borderBlocks.add(BlockCoordinate(x, heightRange.last))
        }

        heightRange.forEach { y ->
            borderBlocks.add(BlockCoordinate(widthRange.first, y))
            borderBlocks.add(BlockCoordinate(widthRange.last, y))
        }

        borderBlocks.toList()
    }

    fun purgeCaches() {
        heuristicCache.invalidateAll()
    }

    // TODO: this is too slow... maybe cache?
    fun pathToOutside(start: List<BlockCoordinate>): Path? {
        // OK... let's see if we can get a trip to the outside...
        return tripTo(start, mapBorders)
    }

    private fun findNearestTrade(
        start: List<BlockCoordinate>,
        quantity: Int,
        buildingFilter: (Building, Int) -> Boolean
    ): List<BlockCoordinate> {
        return start.flatMap { coordinate ->
            val buildings = cityMap.nearestBuildings(coordinate, MAX_DISTANCE)

            buildings.filter {
                val building = it.building
                buildingFilter(building, quantity)
            }
        }.flatMap { blocksFor(it.building, it.coordinate) }
    }

    private fun blocksFor(building: Building, coordinate: BlockCoordinate): List<BlockCoordinate> {
        val xRange = coordinate.x..(coordinate.x + building.width - 1)
        val yRange = coordinate.y..(coordinate.y + building.height - 1)
        return xRange.flatMap { x ->
            yRange.map { y ->
                BlockCoordinate(x, y)
            }
        }
    }

    fun pathToNearestLabor(start: List<BlockCoordinate>, quantity: Int = 1): Path? {
        val nearest = findNearestTrade(start, quantity) { building, _ ->
            building.currentQuantityForSale(Tradeable.LABOR) >= quantity
        }
        return tripTo(start, nearest)
    }

    fun pathToNearestJob(start: List<BlockCoordinate>, quantity: Int = 1): Path? {
        val nearest = findNearestTrade(start, quantity) { building, _ ->
            building.currentQuantityWanted(Tradeable.LABOR) >= quantity
        }
        return tripTo(start, nearest)
    }

    private fun heuristic(current: BlockCoordinate, destinations: List<BlockCoordinate>): Double {
        // calculate manhattan distance to each...
        return destinations.map { coordinate ->
            var score = manhattanDistance(current, coordinate)
            // see if this is road and lower score by a tiny bit...
            val locations = cityMap.cachedLocationsIn(current)
            if (locations.count() > 0) {
                val building = locations.first().building
                if (building is Road) {
                    score -= 3
                    score += cityMap.trafficLayer[current] ?: 0.0
                } else {
                    score += 10
                }
            }
            score
        }.min() ?: 100.0
    }

    private fun manhattanDistance(start: BlockCoordinate, destination: BlockCoordinate): Double {
        return Math.abs(start.x - destination.x) + Math.abs(start.y - destination.y).toDouble()
    }

    fun nearbyRoad(sourceBlocks: List<BlockCoordinate>, distance: Int = 4): Boolean {
        sourceBlocks.forEach {
            val nearbyRoads = cityMap.nearestBuildings(it, distance).filter { it.building is Road }
            if (nearbyRoads.count() > 0) {
                return true
            }
        }
        return false
    }

    private fun drivable(node: NavigationNode): Boolean {
        // make sure we got a road under it...
        val locations = cityMap.cachedLocationsIn(node.coordinate)
        if (locations.count() > 0) {
            val building = locations.first().building
            return building is Road && (building.dir == Direction.STATIONARY || building.dir == node.direction)
        }
        return false
    }

    private fun isGround(node: NavigationNode) = cityMap.groundLayer[node.coordinate]?.type == TileType.GROUND

    fun tripTo(
        source: List<BlockCoordinate>,
        destinations: List<BlockCoordinate>
    ): Path? {
        // switch these to list of navigation nodes...
        val openList = source.map {
            NavigationNode(
                cityMap,
                it,
                null,
                cachedHeuristic(it, destinations),
                TransitType.ROAD,
                Direction.STATIONARY
            )
        }.toMutableSet()

        val closedList = mutableSetOf<NavigationNode>()

        var done = false

        var lastNode: NavigationNode? = null

        while (!done) {
            // bail out if we have no nodes left in the open list
            val activeNode = openList.minBy { it.score } ?: return null
            // now remove it from open list...
            openList.remove(activeNode)
            closedList.add(activeNode)

            // look within 3 nodes of here... (we can jump 3 nodes...)
            // TODO: if we are within 3 blocks we can disregard drivable nodes...
            val distanceToGoal = destinations.map { activeNode.coordinate.distanceTo(it) }.min() ?: 999.0
            val distanceFromStart = source.map { activeNode.coordinate.distanceTo(it) }.min() ?: 999.0

            if (destinations.contains(activeNode.coordinate)) {
                done = true
                lastNode = activeNode
            }

            // TODO: maybe pull out into lambda so we can re-use pathfinder...
            fun maybeAppendNode(node: NavigationNode) {
                if (!closedList.contains(node) && !openList.contains(node)) {

                    // if we are within 3 we can just skip around...
                    if (distanceToGoal <= 2 || distanceFromStart <= 2) {
                        if (isGround(node) || destinations.contains(node.coordinate)) {
                            openList.add(node)
                        } else {
                            closedList.add(node)
                        }
                    } else {
                        if (drivable(node) || destinations.contains(node.coordinate)) {
                            openList.add(node)
                        } else {
                            closedList.add(node)
                        }
                    }
                }
            }

            // ok figure out the dang neighbors...
            val north = BlockCoordinate(activeNode.coordinate.x, activeNode.coordinate.y - 1)
            val northNode = NavigationNode(
                cityMap,
                north,
                activeNode,
                activeNode.score + cachedHeuristic(north, destinations),
                TransitType.ROAD,
                Direction.NORTH
            )
            maybeAppendNode(northNode)

            val south = BlockCoordinate(activeNode.coordinate.x, activeNode.coordinate.y + 1)
            val southNode = NavigationNode(
                cityMap,
                south,
                activeNode,
                activeNode.score + cachedHeuristic(south, destinations),
                TransitType.ROAD,
                Direction.SOUTH
            )
            maybeAppendNode(southNode)

            val east = BlockCoordinate(activeNode.coordinate.x + 1, activeNode.coordinate.y)
            val eastNode = NavigationNode(
                cityMap,
                east,
                activeNode,
                activeNode.score + cachedHeuristic(east, destinations),
                TransitType.ROAD,
                Direction.EAST
            )
            maybeAppendNode(eastNode)

            val west = BlockCoordinate(activeNode.coordinate.x - 1, activeNode.coordinate.y)
            val westNode = NavigationNode(
                cityMap,
                west,
                activeNode,
                activeNode.score + cachedHeuristic(west, destinations),
                TransitType.ROAD,
                Direction.WEST
            )
            maybeAppendNode(westNode)
        }

        return lastNode?.let {
            // now we can just recurse back from
            val pathNodes = mutableListOf<NavigationNode>()
            var activeNode = lastNode

            while (activeNode != null) {
                activeNode.let {
                    pathNodes.add(it)
                }
                activeNode = activeNode.parent
            }

            Path(pathNodes.reversed())
        }
    }
}
