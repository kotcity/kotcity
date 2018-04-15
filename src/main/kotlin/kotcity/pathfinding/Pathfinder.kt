package kotcity.pathfinding

import kotcity.data.*
import kotcity.data.buildings.*
import kotcity.util.Debuggable
import kotlin.reflect.KClass

// let me show you de way
//        ⢀⢀⢀⢀⢀⢀⢀⢀⢀⢀⠶⣿⣭⡧⡤⣤⣻⣛⣹⣿⣿⣿⣶⣄
//        ⢀⢀⢀⢀⢀⢀⢀⢀⢀⣼⣊⣤⣶⣷⣶⣧⣤⣽⣿⣿⣿⣿⣿⣿⣷
//        ⢀⢀⢀⢀⢀⢀⢀⢀⢀⢻⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡇
//        ⢀⢀⢀⢀⢀⢀⢀⣠⣶⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣧
//        ⢀⢀⢀⢀⢀⢀⠸⠿⣿⣿⠿⣿⣿⣿⣿⣿⣿⣿⡿⣿⣻⣿⣿⣿⣿⣿⡆
//        ⢀⢀⢀⢀⢀⢀⢀⢸⣿⣿⡀⠘⣿⡿⢿⣿⣿⡟⣾⣿⣯⣽⣼⣿⣿⣿⣿⡀
//        ⢀⢀⢀⢀⢀⢀⡠⠚⢛⣛⣃⢄⡁⢀⢀⢀⠈⠁⠛⠛⠛⠛⠚⠻⣿⣿⣿⣷
//        ⢀⢀⣴⣶⣶⣶⣷⡄⠊⠉⢻⣟⠃⢀⢀⢀⢀⡠⠔⠒⢀⢀⢀⢀⢹⣿⣿⣿⣄⣀⣀⣀⣀⣀⣀
//        ⢠⣾⣿⣿⣿⣿⣿⣿⣿⣶⣄⣙⠻⠿⠶⠒⠁⢀⢀⣀⣤⣰⣶⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣷⣄
//        ⢿⠟⠛⠋⣿⣿⣿⣿⣿⣿⣿⣟⡿⠷⣶⣶⣶⢶⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡄
//        ⢀⢀⢀⢀⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣷⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠉⠙⠻⠿⣿⣿⡿
//        ⢀⢀⢀⢀⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⢀⢀⢀⢀⠈⠁
//        ⢀⢀⢀⢀⢸⣿⣿⣿⣿⢻⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿
//        ⢀⢀⢀⢀⢸⣿⣿⣿⣿⣄⠈⠛⠿⣿⣿⣿⣿⣿⣿⣿⡿⠟⣹⣿⣿⣿⣿⣿⣿⣿⣿⠇
//        ⢀⢀⢀⢀⢀⢻⣿⣿⣿⣿⣧⣀⢀⢀⠉⠛⠛⠋⠉⢀⣠⣾⣿⣿⣿⣿⣿⣿⣿⣿⠏
//        ⢀⢀⢀⢀⢀⢀⢻⣿⣿⣿⣿⣿⣷⣤⣄⣀⣀⣤⣴⣾⣿⣿⣿⣿⣿⣿⣿⣿⡿⠋
//        ⢀⢀⢀⢀⢀⢀⢀⠙⠿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠛
//        ⢀⢀⢀⢀⢀⢀⢀⢀⢀⢹⣿⡿⢿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡟⠁
//        ⢀⢀⢀⢀⢀⢀⢀⢀⢀⢸⣿⡇⢀⠈⠙⠛⠛⠛⠛⠛⠛⠻⣿⣿⣿⠇
//        ⢀⢀⢀⢀⢀⢀⢀⢀⢀⣸⣿⡇⢀⢀⢀⢀⢀⢀⢀⢀⢀⢀⢨⣿⣿
//        ⢀⢀⢀⢀⢀⢀⢀⢀⣾⣿⡿⠃⢀⢀⢀⢀⢀⢀⢀⢀⢀⢀⢸⣿⡏
//        ⢀⢀⢀⢀⢀⢀⢀⢀⠻⠿⢀⢀⢀⢀⢀⢀⢀⢀⢀⢀⢀⢠⣿⣿⡇

/**
 * Find paths within the CityMap parameter.
 *
 * @cityMap to search for paths
 */
class Pathfinder(val cityMap: CityMap) : Debuggable {

    companion object {
        const val MAX_DISTANCE = 50
    }

    override var debug: Boolean = true

    /**
     * Coordinates that make up the border of the map
     */
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
            cityMap.nearestBuildings(coordinate, MAX_DISTANCE).filter {
                buildingFilter(it.building, quantity)
            }
        }.flatMap { it.blocks() }
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
            var score = current.manhattanDistanceTo(coordinate)
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

    private fun drivable(sourceNode: NavigationNode, destinationNode: NavigationNode): Boolean {
        // kick it out if it's an invalid transition...
        if (sourceNode.transitType == TransitType.ROAD) {
            if (destinationNode.transitType == TransitType.RAILROAD) {
                // can't go from ROAD -> RAILROAD
                debug { "Can't go directly from ROAD -> RAILROAD" }
                return false
            } else if (destinationNode.transitType == TransitType.RAIL_STATION) {
                // OK to go from ROAD -> RAIL_STATION
                return true
            } else if (destinationNode.transitType == TransitType.ROAD) {
                // if src == ROAD and dest == ROAD do 1 way check...
                // make sure we got a road under it...
                val locations = cityMap.cachedLocationsIn(destinationNode.coordinate)
                if (locations.count() > 0) {
                    val building = locations.first().building
                    return building is Road && (building.direction == Direction.STATIONARY || building.direction != oppositeDir(
                        destinationNode.direction
                    ))
                }
            }
        } else if (sourceNode.transitType == TransitType.RAILROAD) {
            when {
                destinationNode.transitType == TransitType.ROAD -> {
                    // cannot go from railroad to road...
                    debug { "Can't go directly from RAILROAD -> ROAD" }
                    return false
                }
                destinationNode.transitType == TransitType.RAIL_STATION -> return true
                destinationNode.transitType == TransitType.RAILROAD -> return true
                else -> {
                }
            }
        } else if (sourceNode.transitType == TransitType.RAIL_STATION) {
            // we can go to any kind of destination... rail OR road...
            return true
        }
        debug { "Unknown transit type: ${sourceNode.transitType} to ${destinationNode.transitType}" }
        return false
    }

    private fun coordIsRailroad(coordinate: BlockCoordinate): Boolean {
        val locations = cityMap.locationsAt(coordinate)
        val building = if (locations.isEmpty()) null else locations.first().building
        return !locations.isEmpty() && (building is Railroad || building is RailroadCrossing)
    }

    private fun isRailBuilding(coordinate: BlockCoordinate): Boolean {
        return isBuildingAt(coordinate, RailDepot::class) || isBuildingAt(coordinate, TrainStation::class)
    }

    private fun canGoViaTrain(fromCoordinate: BlockCoordinate, toCoordinate: BlockCoordinate): Boolean {

        // break this up so we can hopefully bail out early...
        val isTargetStation = isRailBuilding(toCoordinate)
        if (isTargetStation) {
            return true
        }

        val isTargetRail = coordIsRailroad(toCoordinate)
        val isSourceRail = coordIsRailroad(fromCoordinate)

        if (isSourceRail && isTargetRail) {
            return true
        }

        // finally, return if we are exiting rail...
        return isRailBuilding(fromCoordinate) && isTargetRail
    }

    private fun isGround(node: NavigationNode) = cityMap.groundLayer[node.coordinate]?.type == TileType.GROUND

    // OK... the REAL way path finding works is we have to find ourselves a road first... if we don't step on a road
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

        val nearbyLocations = nearbyBlocks.flatMap { cityMap.locationsAt(it) }.distinct()
        val nearbyRoads = nearbyLocations.filter { it.building is Road }.distinct()

        if (nearbyRoads.isEmpty()) {
            return null
        }

        // bail out if we can't get to a road...
        val pathToNearestRoad = shortestCastToRoad(source, nearbyRoads.map { it.coordinate })
        if (pathToNearestRoad == null) {
            debug { "Could not find a path to the nearest road!" }
            return null
        }

        // OK... now our source is that first road tile...
        val startRoad = pathToNearestRoad.blocks().last()

        // now try to go the rest of the way OR bail out...
        val restOfTheWay = truePathfind(listOf(startRoad), destinations)
        if (restOfTheWay == null) {
            debug { "Our source is $startRoad and destination is: $destinations" }
            debug { "Now we can't find a path AFTER finding a road!" }
            return null
        }

        return pathToNearestRoad.plus(restOfTheWay)

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
        val delta = direction.toDelta()
        var currentBlock = start

        val pathBlocks = mutableListOf(currentBlock)

        repeat(maxLength) {
            currentBlock += delta
            pathBlocks.add(currentBlock)
        }

        // convert to path with children set appropriately...
        return Path(pathBlocks.mapIndexed { index, blockCoordinate ->
            if (index == 0) {
                makeNode(blockCoordinate, direction)
            } else {
                NavigationNode(
                    blockCoordinate,
                    makeNode(pathBlocks[index - 1], direction),
                    0.0,
                    TransitType.ROAD,
                    direction
                )
            }

        })
    }

    private fun makeNode(blockCoordinate: BlockCoordinate, direction: Direction): NavigationNode {
        return NavigationNode(
            blockCoordinate,
            null,
            0.0,
            TransitType.ROAD,
            direction
        )
    }

    // so basically what we want to do here is start from each coordinate and project each way (N,S,E,W) and see if we hit a destination...
    private fun shortestCastToRoad(
        source: List<BlockCoordinate>,
        roadBlocks: List<BlockCoordinate>,
        maxLength: Int = 3
    ): Path? {
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
            debug { "Could not find any paths including road!" }
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

    private fun truePathfind(
        source: List<BlockCoordinate>,
        destinations: List<BlockCoordinate>
    ): Path? {

        // switch these to list of navigation nodes...
        val openList = generateOpenList(source, destinations)
        val closedList = mutableSetOf<NavigationNode>()

        var done = false

        var lastNode: NavigationNode? = null

        while (!done) {
            // bail out if we have no nodes left in the open list
            val activeNode = openList.minBy { it.score } ?: return null

            // now remove it from open list...
            openList.remove(activeNode)
            closedList.add(activeNode)

            if (destinations.contains(activeNode.coordinate)) {
                done = true
                lastNode = activeNode
            }

            // look within 3 nodes of here... (we can jump 3 nodes...)
            // TODO: if we are within 3 blocks we can disregard drivable nodes...
            val distanceToGoal = destinations.map { activeNode.coordinate.distanceTo(it) }.min() ?: 999.0

            for (direction in listOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
                // ok figure out the dang neighbors...
                val nextBlock = blockInDirection(activeNode.coordinate, direction)

                // determine transitType for destination
                val newTransitType = transitTypeFor(nextBlock)

                // if we are on a road and the destination is nearby... we can hit it!
                if (distanceToGoal <= 3 && activeNode.transitType == TransitType.ROAD) {
                    val nextNode = NavigationNode(
                        nextBlock,
                        activeNode,
                        activeNode.score + heuristic(nextBlock, destinations),
                        TransitType.ROAD,
                        direction
                    )
                    maybeAppendNode(activeNode, nextNode, openList, closedList, distanceToGoal, destinations)
                } else {
                    if (newTransitType != null) {
                        // need to figure out the type of node we are now...
                        val nextNode = NavigationNode(
                            nextBlock,
                            activeNode,
                            activeNode.score + heuristic(nextBlock, destinations),
                            newTransitType,
                            direction
                        )
                        maybeAppendNode(activeNode, nextNode, openList, closedList, distanceToGoal, destinations)
                    }
                }
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

    private fun blockInDirection(coordinate: BlockCoordinate, direction: Direction): BlockCoordinate {
        return when (direction) {
            Direction.NORTH -> coordinate.top()
            Direction.SOUTH -> coordinate.bottom()
            Direction.EAST -> coordinate.right()
            Direction.WEST -> coordinate.left()
            Direction.STATIONARY -> coordinate
        }
    }

    private fun transitTypeFor(coordinate: BlockCoordinate): TransitType? {
        val building = cityMap.cachedLocationsIn(coordinate).firstOrNull() ?: return null
        return when (building.building::class) {
            Road::class -> TransitType.ROAD
            RailDepot::class -> TransitType.RAIL_STATION
            TrainStation::class -> TransitType.RAIL_STATION
            Railroad::class -> TransitType.RAILROAD
            else -> {
                null
            }
        }
    }

    private fun maybeAppendNode(
        sourceNode: NavigationNode,
        destinationNode: NavigationNode,
        openList: MutableSet<NavigationNode>,
        closedList: MutableSet<NavigationNode>,
        distanceToGoal: Double,
        destinations: List<BlockCoordinate>
    ) {
        if (!closedList.contains(destinationNode) && !openList.contains(destinationNode)) {
            // if we are within 3 we can just skip around...
            // FIXME: this means if we are super close we can just ignore roads...

            // TODO: this the problem... when we get close enough we gotta cast to the destination...
            if (distanceToGoal <= 3) {
                val newNode = destinationNode.copy(score = heuristic(destinationNode.coordinate, destinations))
                if (isGround(newNode) || destinations.contains(newNode.coordinate)) {
                    openList.add(newNode)
                } else {
                    closedList.add(newNode)
                }
            } else {
                if (sourceNode.transitType == TransitType.ROAD && destinations.contains(destinationNode.coordinate)) {
                    openList.add(destinationNode)
                } else {
                    if (destinations.contains(destinationNode.coordinate) || drivable(sourceNode, destinationNode)) {
                        openList.add(destinationNode)
                    } else {
                        closedList.add(destinationNode)
                    }
                }

            }
        }
    }

    private fun generateOpenList(
        source: List<BlockCoordinate>,
        destinations: List<BlockCoordinate>
    ): MutableSet<NavigationNode> {
        return source.map {
            NavigationNode(
                it,
                null,
                heuristic(it, destinations),
                TransitType.ROAD,
                Direction.STATIONARY
            )
        }.toMutableSet()
    }
}
