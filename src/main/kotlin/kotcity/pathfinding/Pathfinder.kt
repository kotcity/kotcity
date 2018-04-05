package kotcity.pathfinding

import kotcity.data.*
import kotcity.memoization.cache
import kotlin.reflect.KClass
import kotcity.util.Debuggable

const val MAX_DISTANCE = 50

/**
 * Direction of travel of node in found paths.
 */
enum class Direction {
    NORTH, SOUTH, EAST, WEST, STATIONARY
}

/**
 * Type of node in found paths.
 */
enum class TransitType {
    RAILROAD, ROAD
}

/**
 * Node in found paths.
 */
data class NavigationNode(
    val cityMap: CityMap,
    val coordinate: BlockCoordinate,
    val parent: NavigationNode?,
    val score: Double,
    val transitType: TransitType = TransitType.ROAD,
    val direction: Direction,
    val isOnRail: Boolean
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


/**
 * Path of nodes, with conveniece methods.
 *
 * @nodes list of nodes along a path
 */
data class Path(
        internal val nodes: List<NavigationNode> = emptyList()
) {
    /**
     * Total heuristic score of the nodes in this Path.
     */
    fun totalScore(): Int = nodes.sumBy { it.score.toInt() }

    /**
     * Total length of this Path.
     */
    fun length(): Int = nodes.size

    /**
     * List of coordinates in this Path.
     */
    fun blocks(): List<BlockCoordinate> = nodes.map { it.coordinate }.toList()

    /**
     * Append another Path to the end of this Path
     */
    fun plus(otherPath: Path?): Path? {
        val otherNodes = otherPath?.nodes?.toList() ?: return this

        val firstOfOther = otherNodes.first()
        // chop the first node off the other list...
        // because we have to replace it with a parent that has the other list
        // of nodes as a parent to preserve the chain...
        val newFirst = NavigationNode(
                firstOfOther.cityMap,
                firstOfOther.coordinate,
                nodes.last(),
                firstOfOther.score,
                firstOfOther.transitType,
                firstOfOther.direction,
                firstOfOther.isOnRail
        )

        // lop off the first one and shove our new one in there...
        val newOtherNodes = listOf(newFirst).plus(otherNodes.drop(1))

        return Path(nodes.plus(newOtherNodes).distinct())
    }
}

/**
 * Find paths within the CityMap parameter.
 *
 * @cityMap to search for paths
 */
class Pathfinder(val cityMap: CityMap) : Debuggable {
    override var debug: Boolean = true

    private val cachedHeuristicPair = ::heuristic.cache()
    private val heuristicCache = cachedHeuristicPair.first
    private val cachedHeuristic = cachedHeuristicPair.second

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

    /**
     * Purges the cached results for the heuristic method.
     */
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

    /**
     * Gets path to nearest labor for sale.
     */
    fun pathToNearestLabor(start: List<BlockCoordinate>, quantity: Int = 1): Path? {
        val nearest = findNearestTrade(start, quantity) { building, _ ->
            building.currentQuantityForSale(Tradeable.LABOR) >= quantity
        }
        return tripTo(start, nearest)
    }

    /**
     * Gets path to nearest wanted labor tradeable.
     */
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
                when (building) {
                    is Road -> {
                        score -= 3
                        score += (cityMap.trafficLayer[current] ?: 0.0)
                    }
                    is Railroad -> {
                        score -= 5
                        // traffic is 10 times less bad on a train :)
                        score += (cityMap.trafficLayer[current] ?: 0.0) / 10
                    }
                    else -> {
                        score += 10_000
                    }
                }
            } else {
                // it is just blank ground
                score += 10_000
            }
            score
        }.min() ?: Double.MAX_VALUE
    }


    private fun manhattanDistance(start: BlockCoordinate, destination: BlockCoordinate): Double {
        return Math.abs(start.x - destination.x) + Math.abs(start.y - destination.y).toDouble()
    }

    /**
     * Find if a road block is within distance.
     */
    fun nearbyRoad(sourceBlocks: List<BlockCoordinate>, distance: Int = 4): Boolean {
        sourceBlocks.forEach {
            val nearbyRoads = cityMap.nearestBuildings(it, distance).filter { it.building is Road }
            if (nearbyRoads.count() > 0) {
                return true
            }
        }
        return false
    }

    private fun oppositeDir(d: Direction): Direction {
        return when (d) {
            Direction.WEST -> Direction.EAST
            Direction.EAST -> Direction.WEST
            Direction.NORTH -> Direction.SOUTH
            Direction.SOUTH -> Direction.NORTH
            else -> Direction.STATIONARY
        }
    }

    private fun drivable(node: NavigationNode): Boolean {
        // make sure we got a road under it...
        val locations = cityMap.cachedLocationsIn(node.coordinate)
        if (locations.count() > 0) {
            val building = locations.first().building
            return building is Road && (building.direction == Direction.STATIONARY || building.direction != oppositeDir(node.direction))
        }
        return false
    }

    private fun coordIsRailroad(coordinate: BlockCoordinate): Boolean {
        val locations = cityMap.cachedLocationsIn(coordinate)
        val building = if (locations.isEmpty()) null else locations.first().building
        return !locations.isEmpty() && (building is Railroad || building is RailroadCrossing)
    }

    private fun canGoViaTrain(fromCoordinate: BlockCoordinate, toCoordinate: BlockCoordinate): Boolean {

        fun isRailBuilding(coordinate: BlockCoordinate): Boolean {
            return isBuildingAt(coordinate, RailDepot::class) || isBuildingAt(coordinate, TrainStation::class)
        }

        val isSourceRail = coordIsRailroad(fromCoordinate)
        val isTargetRail = coordIsRailroad(toCoordinate)
        val isEnteringRail = isRailBuilding(fromCoordinate) && isTargetRail
        val isTargetStation = isRailBuilding(toCoordinate)

        return isTargetStation || isSourceRail && isTargetRail || isEnteringRail // || isExitingRail
    }

    private fun isGround(node: NavigationNode) = cityMap.groundLayer[node.coordinate]?.type == TileType.GROUND

    // OK... the REAL way pathfinding works is we have to find ourselves a road first... if we don't step on a road
    // our path isn't valid...
    fun tripTo(
        source: List<BlockCoordinate>,
        destinations: List<BlockCoordinate>
    ): Path? {

        if (destinations.isEmpty()) {
            throw RuntimeException("We cannot path find to an empty destination!")
        }

        // let's find a road semi-nearby
        val nearbyBlocks = source.flatMap { it.neighbors(3) }.plus(source).distinct()

        val nearbyLocations = nearbyBlocks.flatMap { cityMap.locationsAt(it) }
        val nearbyRoads = nearbyLocations.filter { it.building is Road }

        if (nearbyRoads.isEmpty()) {
            return null
        }

        // bail out if we can't get to a road...
        val pathToNearestRoad = shortestCastToRoad(source, nearbyRoads.map { it.coordinate })
        if (pathToNearestRoad == null) {
            debug("Could not find a path to the nearest road!")
            return null
        }

        // OK... now our source is that first road tile...
        val startRoad = pathToNearestRoad.blocks().last()

        // now try to go the rest of the way OR bail out...
        val restOfTheWay = truePathfind(listOf(startRoad), destinations)
        if (restOfTheWay == null) {
            debug("Our source is $startRoad and destination is: $destinations")
            debug("Now we can't find a path AFTER finding a road!")
            return null
        }

        return pathToNearestRoad.plus(restOfTheWay)
    }

    private fun directionToBlockDelta(direction: Direction): BlockCoordinate {
        return when(direction) {
            Direction.NORTH -> BlockCoordinate(0, -1)
            Direction.SOUTH -> BlockCoordinate(0, 1)
            Direction.EAST  -> BlockCoordinate(1, 0)
            Direction.WEST  -> BlockCoordinate(-1, 0)
            Direction.STATIONARY -> BlockCoordinate(0, 0)
        }
    }

    private fun isBuildingAt(coordinate: BlockCoordinate, clazz: KClass<*>): Boolean {
        cityMap.cachedLocationsIn(coordinate).forEach {
            if (clazz.isInstance(it.building)) {
                return true
            }
        }
        return false
    }

    private fun path(start: BlockCoordinate, direction: Direction, maxLength: Int): Path? {
        val delta = directionToBlockDelta(direction)
        var currentBlock = start

        val pathBlocks = mutableListOf(currentBlock)

        repeat(maxLength) {
            currentBlock = currentBlock.plus(delta)
            pathBlocks.add(currentBlock)
        }

        // convert to path with children set appropriately...
        return Path(pathBlocks.mapIndexed { index, blockCoordinate ->
            if (index == 0) {
                makeNode(blockCoordinate, direction)
            } else {
                NavigationNode(
                        cityMap,
                        blockCoordinate,
                        makeNode(pathBlocks[index - 1], direction),
                        0.0,
                        TransitType.ROAD,
                        direction,
                        false
                )
            }

        })
    }

    private fun makeNode(blockCoordinate: BlockCoordinate, direction: Direction): NavigationNode {
        return NavigationNode(
                cityMap,
                blockCoordinate,
                null,
                0.0,
                TransitType.ROAD,
                direction,
                false
        )
    }

    // so basically what we want to do here is start from each coordinate and project each way (N,S,E,W) and see if we hit a destination...
    private fun shortestCastToRoad(source: List<BlockCoordinate>, roadBlocks: List<BlockCoordinate>, maxLength: Int = 3): Path? {
        val paths = source.flatMap {
            listOf(
                    path(it, Direction.NORTH, maxLength),
                    path(it, Direction.SOUTH, maxLength),
                    path(it, Direction.EAST, maxLength),
                    path(it, Direction.WEST, maxLength)
            )
        }.filterNotNull()

        // make sure these paths have a road in em...
        val pathsWithRoad = paths.filter { it.blocks().any { roadBlocks.contains(it) } }

        if (pathsWithRoad.isEmpty()) {
            debug("Could not find any paths including road!")
            return null
        }

        // ok now let's trim those paths...
        // if we are on a road we don't care to look further...
        val trimmedPaths = pathsWithRoad.map {
            val newNodes = it.nodes.dropLastWhile { !cityMap.locationsAt(it.coordinate).any { it.building is Road } }
            Path(newNodes)
        }

        return trimmedPaths.minBy { it.length() }
    }

    private fun truePathfind(source: List<BlockCoordinate>, destinations: List<BlockCoordinate>, needsRoads: Boolean = true): Path? {
        // switch these to list of navigation nodes...
        val openList = source.map {
            NavigationNode(
                    cityMap,
                    it,
                    null,
                    cachedHeuristic(it, destinations),
                    TransitType.ROAD,
                    Direction.STATIONARY,
                    false
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

            if (destinations.contains(activeNode.coordinate) && !coordIsRailroad(activeNode.coordinate)) {
                done = true
                lastNode = activeNode
            }

            // look within 3 nodes of here... (we can jump 3 nodes...)
            // TODO: if we are within 3 blocks we can disregard drivable nodes...
            val distanceToGoal = destinations.map { activeNode.coordinate.distanceTo(it) }.min() ?: 999.0

            // TODO: maybe pull out into lambda so we can re-use pathfinder...
            fun maybeAppendNode(node: NavigationNode) {
                if (!closedList.contains(node) && !openList.contains(node)) {
                    // if we are within 3 we can just skip around...
                    // BUG: this means if we are super close we can just ignore roads...

                    // TODO: this the problem... when we get close enough we gotta cast to the destination...
                    if (distanceToGoal <= 3) {

                        val newNode = node.copy(score = cachedHeuristic(node.coordinate, destinations))

                        if (isGround(newNode) || destinations.contains(newNode.coordinate)) {
                            openList.add(newNode)
                        } else {
                            closedList.add(newNode)
                        }
                    } else {
                        if (needsRoads) {
                            if (canGoViaTrain(activeNode.coordinate, node.coordinate) || drivable(node) || destinations.contains(node.coordinate)) {
                                openList.add(node)
                            } else {
                                closedList.add(node)
                            }
                        } else {
                            if (destinations.contains(node.coordinate) || canGoViaTrain(activeNode.coordinate, node.coordinate)) {
                                openList.add(node)
                            } else {
                                closedList.add(node)
                            }
                        }
                    }
                }
            }

            for (direction in listOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
                // ok figure out the dang neighbors...
                val delta = directionToBlockDelta(direction)
                val nextBlock = activeNode.coordinate.plus(delta)
                val nextNode = NavigationNode(
                        cityMap,
                        nextBlock,
                        activeNode,
                        activeNode.score + cachedHeuristic(nextBlock, destinations),
                        TransitType.ROAD,
                        direction,
                        false
                )
                maybeAppendNode(nextNode)
            }
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
