package kotcity.data

import com.github.davidmoten.rtree.RTree
import com.github.davidmoten.rtree.geometry.Geometries
import com.github.davidmoten.rtree.geometry.Rectangle
import com.github.debop.javatimes.plus
import com.github.debop.javatimes.toDateTime
import kotcity.automata.*
import kotcity.data.Tunable.MAX_BUILDING_SIZE
import kotcity.memoization.CacheOptions
import kotcity.memoization.cache
import kotcity.pathfinding.Direction
import kotcity.util.reorder
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withTimeout
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * Just a utility class for hanging onto a rectangle of BlockCoordinates
 */
data class Corners(
    private val topLeft: BlockCoordinate,
    private val bottomRight: BlockCoordinate,
    private val topRight: BlockCoordinate,
    private val bottomLeft: BlockCoordinate
) {
    fun includes(block: BlockCoordinate): Boolean {
        if (block.x >= topLeft.x && block.x <= bottomRight.x && block.y >= topLeft.y && block.y <= bottomRight.y) {
            return true
        }
        if (block.x <= topRight.x && block.x >= bottomLeft.x && block.y <= topRight.y && block.y >= bottomLeft.y) {
            return true
        }
        return false
    }
}

/**
 * Represents a grid of "desirability values" that we store in the [CityMap]
 */
data class DesirabilityLayer(val zoneType: Zone, val level: Int) : QuantizedMap<Double>(1) {
    init {
        map = map.withDefault { Double.NEGATIVE_INFINITY }
    }
}

/**
 * A map representing the city. The map hangs on to [Building]s and [Location]s. The map is set up as a grid
 * of [BlockCoordinate] and various layers hanging onto various values (desirability, crime, etc).
 *
 * @property width width in blocks of the city
 * @property height height in blocks of the city
 */
data class CityMap(var width: Int = 512, var height: Int = 512) {

    companion object {
        /**
         * Returns a totally flat map of the given size.
         * @param width width in [BlockCoordinate]s
         * @param height height in [BlockCoordinate]s
         */
        fun flatMap(width: Int = 512, height: Int = 512): CityMap {
            val map = CityMap(width, height)
            // set all tiles to ground...
            val xRange = 0..map.width
            val yRange = 0..map.height
            xRange.map { x ->
                yRange.map { y ->
                    map.groundLayer[BlockCoordinate(x, y)] = MapTile(TileType.GROUND, 0.1)
                }
            }
            return map
        }
    }

    // various layers
    val groundLayer = mutableMapOf<BlockCoordinate, MapTile>()
    private val buildingLayer = mutableMapOf<BlockCoordinate, Building>()
    val zoneLayer = mutableMapOf<BlockCoordinate, Zone>()
    val powerLineLayer = mutableMapOf<BlockCoordinate, Building>()
    val resourceLayers = mutableMapOf<String, QuantizedMap<Double>>()
    val landValueLayer = mutableMapOf<BlockCoordinate, Double>().withDefault { 0.0 }
    val fireCoverageLayer = mutableMapOf<BlockCoordinate, Double>()
    val crimeLayer = mutableMapOf<BlockCoordinate, Double>()
    val policePresenceLayer = mutableMapOf<BlockCoordinate, Double>()
    val pollutionLayer = mutableMapOf<BlockCoordinate, Double>().withDefault { 0.0 }
    var trafficLayer = mutableMapOf<BlockCoordinate, Double>().withDefault { 0.0 }
    val districtsLayer = mutableMapOf<BlockCoordinate, District>()
    val desirabilityLayers = listOf(
        DesirabilityLayer(Zone.RESIDENTIAL, 1),
        DesirabilityLayer(Zone.COMMERCIAL, 1),
        DesirabilityLayer(Zone.INDUSTRIAL, 1)
    )

    val districts = mutableListOf(
        District("Central district")
    )

    private val constructor = Constructor(this)
    private val contractFulfiller = ContactFulfiller(this)
    private val manufacturer = Manufacturer(this)
    private val shipper = Shipper(this)
    private val desirabilityUpdater = DesirabilityUpdater(this)
    val censusTaker = CensusTaker(this)
    private val taxCollector = TaxCollector(this)
    private val liquidator = Liquidator(this)
    private val trafficCalculator = TrafficCalculator(this)
    private val goodsConsumer = GoodsConsumer(this)
    private val zotPopulator = ZotPopulator(this)
    private val happinessUpdater = HappinessUpdater(this)
    private val upgrader: Upgrader = Upgrader(this)
    private val pollutionUpdater = PollutionUpdater(this)
    private val landValueUpdater = LandValueUpdater(this)

    val nationalTradeEntity = NationalTradeEntity(this)

    private var doingHourly = false

    /**
     * Every time the [Liquidator] runs it will drop a count of bulldozed zones in here so we know
     * how many get demolished.
     */
    var bulldozedCounts = mutableMapOf<Zone, Int>().withDefault { 0 }

    /**
     * Current time in the simulation
     */
    var time: Date

    /**
     * Should we print debug output or not?
     */
    var debug = false

    /**
     * where we loaded OR saved this city to...
     * used to determine save vs. save as...
     */
    var fileName: String? = null

    /**
     * Name of the city
     */
    var cityName: String? = null

    /**
     * An "r-tree" that lets us do relatively fast lookups on buildings in our city
     */
    private var buildingIndex = RTree.create<Building, Rectangle>()!!

    // OK! we will require one key per map cell
    private val numberOfCells = this.height.toLong() * this.width.toLong() + 100
    private val locationsInCachePair = ::locationsIn.cache(
        CacheOptions(
            weakKeys = false,
            weakValues = true,
            maximumSize = numberOfCells,
            durationUnit = TimeUnit.SECONDS,
            durationValue = 15
        )
    )

    /**
     * Cache control object for locations.
     */
    private val locationsInCache = locationsInCachePair.first
    /**
     * a cached version of [locationsIn]. This can be used when you want to do a fast lookup on a building
     * but don't really care if you are wrong or not
     */
    val cachedLocationsIn = locationsInCachePair.second

    fun debug(message: String) {
        if (!debug) return
        println("CityMap: $message")
    }

    init {
        shipper.debug = false
        contractFulfiller.debug = false
        manufacturer.debug = false
        constructor.debug = true
        taxCollector.debug = false
        desirabilityUpdater.debug = false
        liquidator.debug = false
        zotPopulator.debug = false
        happinessUpdater.debug = false
        upgrader.debug = false
        landValueUpdater.debug = false

        censusTaker.tick()
        nationalTradeEntity.resetCounts()

        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        simpleDateFormat.timeZone = TimeZone.getDefault()
        time = simpleDateFormat.parse("2000-01-01 12:00:00")
    }

    /**
     * @param callback pass a function that we'll call and pass a location in
     */
    fun eachLocation(callback: (Location) -> Unit) {
        buildingLayer.toList().forEach { entry ->
            callback(Location(this, entry.first, entry.second))
        }
    }

    /**
     * Purges spatial index of buildings...
     */
    fun purgeRTree() {
        val idx = this.buildingIndex.entries().toBlocking().iterator
        idx.forEach {
            this.buildingIndex.delete(it)
        }
    }

    /**
     * Returns the minimum and maximum heights on this map
     * @return a pair of doubles: min and max elevation on the map
     */
    fun elevations(): Pair<Double, Double> {
        val mapMinElevation = groundLayer.values.map { it.elevation }.min() ?: 0.0
        val mapMaxElevation = groundLayer.values.map { it.elevation }.max() ?: 0.0
        return Pair(mapMinElevation, mapMaxElevation)
    }

    /**
     * Takes a coordinate and a building and returns the "footprint" of the building.
     * In other words, each block the building sits in.
     *
     * @param coordinate Coordinate of the building
     * @param building The building
     * @return a list of matching blocks
     */
    fun buildingBlocks(coordinate: BlockCoordinate, building: Building): List<BlockCoordinate> {
        val xRange = coordinate.x..coordinate.x + (building.width - 1)
        val yRange = coordinate.y..coordinate.y + (building.height - 1)
        return xRange.flatMap { x -> yRange.map { BlockCoordinate(x, it) } }
    }

    /**
     * Pass it a start and and end block and it will return you a list of all the blocks in between, in a line.
     * As the name indicates, we use this to figure out where to put roads.
     * @param startBlock Start of the road
     * @param endBlock End of the road
     */
    private fun roadBlocks(startBlock: BlockCoordinate, endBlock: BlockCoordinate): MutableList<BlockCoordinate> {
        val blockList = mutableListOf<BlockCoordinate>()
        if (Math.abs(startBlock.x - endBlock.x) > Math.abs(startBlock.y - endBlock.y)) {
            // going horizontally...
            (startBlock.x..endBlock.x).reorder().forEach { x ->
                blockList.add(BlockCoordinate(x, startBlock.y))
            }
        } else {
            // going vertically...
            (startBlock.y..endBlock.y).reorder().forEach { y ->
                blockList.add(BlockCoordinate(startBlock.x, y))
            }
        }
        return blockList
    }

    /**
     * Does a spatial lookup at *coordinate* for a list of [Location]s
     * @param coordinate coordinate to check
     * @param distance max radius in blocks
     */
    fun nearestBuildings(coordinate: BlockCoordinate, distance: Int = 10): List<Location> {
        val point = Geometries.point(coordinate.x.toFloat(), coordinate.y.toFloat())
        return buildingIndex.search(point, distance.toDouble())
            .filter({ t -> t != null }).map { entry ->
                val geometry = entry.geometry()
                val building = entry.value()
                if (geometry != null && building != null) {
                    Location(this, BlockCoordinate(geometry.x1().toInt(), geometry.y1().toInt()), building)
                } else {
                    null
                }
            }.toBlocking().toIterable().filterNotNull()
    }

    /**
     * Updates the spatial index, enabling quick building lookups...
     * @TODO get smarter about index... we don't want to be rebuilding this all the time...
     */
    fun updateBuildingIndex() {
        var newIndex = RTree.star().create<Building, Rectangle>()
        buildingLayer.forEach { coordinate, building ->
            newIndex = newIndex.add(
                building, Geometries.rectangle(
                    coordinate.x.toFloat(),
                    coordinate.y.toFloat(),
                    coordinate.x.toFloat() + building.width.toFloat() - 1,
                    coordinate.y.toFloat() + building.height.toFloat() - 1
                )
            )
        }
        buildingIndex = newIndex
        locationsInCache.invalidateAll()
    }

    /**
     * Suggests a filename to save the city as... it's based off the name of the city but made safe for filenames
     */
    fun suggestedFilename() = Slug.makeSlug(cityName) + ".kcity"

    /**
     * Main game loop. The engine calls this every X milliseconds and various functions are run from here.
     */
    fun tick() {
        time += 1000 * 60
        if (time.toDateTime().minuteOfHour == 0) {
            val hour = time.toDateTime().hourOfDay
            launch {
                if (!doingHourly) {
                    hourlyTick(hour)
                } else {
                    debug("Warning... hourly still in progress!")
                }
            }
        }
    }

    /**
     * Things that we want done each game hour
     * @param hour current hour that we are processing in military (24 hour) format... (0 .. 23)
     */
    suspend fun hourlyTick(hour: Int) {
        try {
            doingHourly = true
            debug("Processing tick for: $hour:00")

            if (hour % 3 == 0) {
                timeFunction("Terminating random contracts") { contractFulfiller.terminateRandomContracts() }
                withTimeout(5000, TimeUnit.MILLISECONDS) {
                    timeFunction("Signing contracts") { contractFulfiller.signContracts(true, 5000, true) }
                }
                timeFunction("Doing manufacturing") { manufacturer.tick() }
                timeFunction("Shipping products") { shipper.tick() }
                timeFunction("Consuming goods") { goodsConsumer.tick() }
                timeFunction("Generating traffic") { trafficCalculator.tick() }

                async { timeFunction("Populating Zots") { zotPopulator.tick() } }
                async {
                    timeFunction("Updating pollution...") { pollutionUpdater.tick() }
                    timeFunction("Updating happiness...") { happinessUpdater.tick() }
                }
            }

            if (hour == 0 || hour == 6 || hour == 12 || hour == 18) {
                timeFunction("Taking census (to calculate demand)") { censusTaker.tick() }
                timeFunction("Liquidating bankrupt properties") { liquidator.tick() }
                timeFunction("Constructing buildings") { constructor.tick() }
                timeFunction("Performing upgrades") { upgrader.tick() }
                timeFunction("Taking census again (to account for new demand...)") { censusTaker.tick() }
            }

            if (hour == 0) {
                debug("Processing tick for end of day...")
                dailyTick()
            }

        } catch (e: Exception) {
            println("WARNING! Error during hourly: ${e.message}")
            e.printStackTrace()
        } finally {
            doingHourly = false
        }
    }

    /**
     * Used for things we want done once per day.
     */
    private fun dailyTick() {
        val self = this

        async {
            timeFunction("Updating power coverage...") { PowerCoverageUpdater.update(self) }
        }

        async {
            timeFunction("Calculating desirability") { desirabilityUpdater.tick() }
        }

        async {
            timeFunction("Collect Taxes") { taxCollector.tick() }
            timeFunction("Setting National Supply") { nationalTradeEntity.resetCounts() }
        }

        async {
            timeFunction("Calculating fire coverage") { FireCoverageUpdater.update(self) }
            timeFunction("Calculating crime and police presence") { CrimeUpdater.update(self) }
        }

        async {
            timeFunction("Checking validity of contracts") { ContractChecker.checkContracts(self) }
        }

        async {
            timeFunction("Update land value") { landValueUpdater.tick() }
        }
    }

    /**
     * Used to check traffic at a given coordinate with a certain radius
     * @param coordinate coordinate to check at
     * @param radius radius in blocks
     * @param quantity quantity of traffic. Each one represents the traffic from one contract.
     */
    fun hasTrafficNearby(coordinate: BlockCoordinate, radius: Int, quantity: Int): Boolean {
        val trafficCount = trafficNearby(coordinate, radius)
        return trafficCount > quantity
    }

    /**
     * Returns a count of traffic at the given coordinate and radius
     * @param coordinate
     * @param radius
     */
    fun trafficNearby(coordinate: BlockCoordinate, radius: Int): Int {
        val neighboringBlocks = coordinate.neighbors(radius)
        val nearbyRoads = neighboringBlocks.flatMap { cachedLocationsIn(it) }
            .filter { it.building is Road }

        return nearbyRoads.sumBy { trafficLayer[it.coordinate]?.toInt() ?: 0 }
    }

    /**
     * A utility function that helps us to time various automata and functions.
     * @param desc Description of the function (printed to console)
     * @param timedFunction Actual function to invoke
     */
    private fun timeFunction(desc: String, timedFunction: () -> Unit) {
        println("Beginning $desc...")
        val startMillis = System.currentTimeMillis()
        timedFunction()
        val endMillis = System.currentTimeMillis()
        val totalTime = endMillis - startMillis
        println("$desc calc took $totalTime millis")
    }

    /**
     * Look in a given rectangle and see if we have water...
     * @param from top left [BlockCoordinate]
     * @param to bottom right [BlockCoordinate]
     */
    private fun waterFound(from: BlockCoordinate, to: BlockCoordinate): Boolean {
        var waterFound = false
        BlockCoordinate.iterate(from, to) {
            if (groundLayer[it]?.type == TileType.WATER) {
                waterFound = true
                // TODO exit iteration early
            }
        }
        return waterFound
    }

    /**
     * See if we can plop a new building on the map at the given coordinate
     * @param newBuilding proposed building
     * @param coordinate top left coordinate of building
     * @param waterCheck if true, we will make sure we can't build this on water
     * @return true if we can build here, false if not
     */
    fun canBuildBuildingAt(newBuilding: Building, coordinate: BlockCoordinate, waterCheck: Boolean = true): Boolean {

        // OK... let's get nearby buildings to really cut this down...
        val newBuildingEnd =
            BlockCoordinate(coordinate.x + newBuilding.width - 1, coordinate.y + newBuilding.height - 1)

        val newBuildingTopRight = BlockCoordinate(coordinate.x + newBuilding.width - 1, coordinate.y)
        val newBuildingBottomLeft = BlockCoordinate(coordinate.x, coordinate.y + newBuilding.height - 1)

        if (waterCheck && waterFound(coordinate, newBuildingEnd)) {
            return false
        }

        val nearby = nearestBuildings(coordinate)
        nearby.forEach { cityLocation: Location ->
            val building = cityLocation.building
            val otherBuildingStart = cityLocation.coordinate
            val otherBuildingEnd =
                BlockCoordinate(otherBuildingStart.x + building.width - 1, otherBuildingStart.y + building.height - 1)
            // now let's test...
            // top left corner...
            if (coordinate.x <= otherBuildingEnd.x && coordinate.x >= otherBuildingStart.x && coordinate.y <= otherBuildingEnd.y && coordinate.y >= otherBuildingStart.y) {
                // collisionWarning("Collision with top left!", newBuilding, coordinate, building, cityLocation.coordinate)
                return false
            }

            // bottom right corner...
            if (newBuildingEnd.x <= otherBuildingEnd.x && newBuildingEnd.x >= otherBuildingStart.x && newBuildingEnd.y <= otherBuildingEnd.y && newBuildingEnd.y >= otherBuildingStart.y) {
                // collisionWarning("Collision with bottom right!", newBuilding, coordinate, building, cityLocation.coordinate)
                return false
            }

            // top right corner...
            if (newBuildingTopRight.x <= otherBuildingEnd.x && newBuildingTopRight.x >= otherBuildingStart.x && newBuildingTopRight.y <= otherBuildingEnd.y && newBuildingTopRight.y >= otherBuildingStart.y) {
                // collisionWarning("Collision with top right!", newBuilding, coordinate, building, cityLocation.coordinate)
                return false
            }

            // bottom left corner...
            if (newBuildingBottomLeft.x <= otherBuildingEnd.x && newBuildingBottomLeft.x >= otherBuildingStart.x && newBuildingBottomLeft.y <= otherBuildingEnd.y && newBuildingBottomLeft.y >= otherBuildingStart.y) {
                // collisionWarning("Collision with bottom left!", newBuilding, coordinate, building, cityLocation.coordinate)
                return false
            }
        }
        return true
    }

    /**
     * Builds [Railroad] between the two supplied [BlockCoordinate]. Railroad can currently be built in an "L" shape.
     * @see [buildRailroadLeg]
     * @param from start [BlockCoordinate]
     * @param to end [BlockCoordinate]
     */
    fun buildRailroad(from: BlockCoordinate, to: BlockCoordinate) {
        val dx = Math.abs(from.x - to.x)
        val dy = Math.abs(from.y - to.y)
        val mid =
            if (dx > dy) {
                BlockCoordinate(to.x, from.y)
            } else {
                BlockCoordinate(from.x, to.y)
            }
        buildRailroadLeg(from, mid)
        buildRailroadLeg(mid, to)
    }

    /**
     * Builds a single segment of [Railroad]. Ends up being assembled into an "L" shape.
     * @param from start [BlockCoordinate]
     * @param to end [BlockCoordinate]
     */
    private fun buildRailroadLeg(from: BlockCoordinate, to: BlockCoordinate) {
        roadBlocks(from, to).forEach { block ->
            val railroad = Railroad(this)
            val existingRoad = buildingLayer[block]
            if (existingRoad is Road || existingRoad is RailroadCrossing) {
                buildingLayer[block] = RailroadCrossing(this)
                // dezone under us...
                zoneLayer.remove(block)
            } else if (existingRoad is Railroad || canBuildBuildingAt(railroad, block, waterCheck = false)) {
                buildingLayer[block] = railroad
                // dezone under us...
                zoneLayer.remove(block)
            }
        }
        updateBuildingIndex()
    }

    /**
     * Builds a (possibly) one-way road or standard road
     * @param from start [BlockCoordinate]
     * @param to end [BlockCoordinate]
     * @param isOneWay if it's true, this is a one way road. The direction starts at [from] and goes to [to]
     */
    fun buildRoad(from: BlockCoordinate, to: BlockCoordinate, isOneWay: Boolean = false) {
        roadBlocks(from, to).forEach { block ->
            val newRoad =
                when (isOneWay) {
                    false -> Road(this)
                    true -> {
                        buildOneWayRoad(from, to, block)
                    }
                }
            val existingRoad = buildingLayer[block]
            if ((existingRoad is Road) || canBuildBuildingAt(newRoad, block, waterCheck = false)) {
                buildingLayer[block] = newRoad
                // dezone under us...
                zoneLayer.remove(block)
            }
        }
        updateBuildingIndex()
    }

    /**
     * Returns a count of pollution at the given [BlockCoordinate]
     * @param coordinate coordinate to check
     * @param radius radius in [BlockCoordinate]s to look at
     */
    fun pollutionNearby(coordinate: BlockCoordinate, radius: Int): Double {
        val blocksToCheck = coordinate.neighbors(radius)
        return blocksToCheck.map {
            pollutionLayer[it] ?: 0.0
        }.sum()
    }

    /**
     * Builds a [Road] that is one-way
     * @TODO why are we passing in [block] here? I need to look...
     * @param from start [BlockCoordinate]
     * @param to end [BlockCoordinate]
     * @param block I am not sure
     */
    private fun buildOneWayRoad(from: BlockCoordinate, to: BlockCoordinate, block: BlockCoordinate): Road {
        val dx = Math.abs(to.x - from.x)
        val dy = Math.abs(to.y - from.y)
        val isDxGreater = dx > dy
        val isDyGreater = dy > dx
        val left = buildingLayer[BlockCoordinate(block.x - 1, block.y)]
        val right = buildingLayer[BlockCoordinate(block.x + 1, block.y)]
        val above = buildingLayer[BlockCoordinate(block.x, block.y - 1)]
        val below = buildingLayer[BlockCoordinate(block.x, block.y + 1)]
        val dir =
            if (isDxGreater && to.x > from.x) {
                if (above is Road || below is Road) {
                    Direction.STATIONARY
                } else {
                    Direction.EAST
                }
            } else if (isDxGreater && to.x < from.x) {
                if (above is Road || below is Road) {
                    Direction.STATIONARY
                } else {
                    Direction.WEST
                }
            } else if (isDyGreater && to.y > from.y) {
                if (left is Road || right is Road) {
                    Direction.STATIONARY
                } else {
                    Direction.SOUTH
                }
            } else if (isDyGreater && to.y < from.y) {
                if (left is Road || right is Road) {
                    Direction.STATIONARY
                } else {
                    Direction.NORTH
                }
            } else {
                Direction.STATIONARY
            }
        return Road(this, dir)
    }

    /**
     * Designates a given area as a certain [Zone]
     * @param type [Zone] to mark
     * @param from top-left corner
     * @param to bottom-right corner
     */
    fun zone(type: Zone, from: BlockCoordinate, to: BlockCoordinate) {
        BlockCoordinate.iterate(from, to) {
            if (!waterFound(it, it)) {
                if (locationsIn(it).count() == 0) {
                    zoneLayer[it] = type
                }
            }
        }
    }

    /**
     * Plops the given [Building] at the given [BlockCoordinate]. Note the coordinate is the top-left
     * of the building. We do check for collisions, hopefully preventing overlaps.
     *
     * @param building [Building] to put
     * @param block [BlockCoordinate] of that building
     * @param updateBuildingIndex if true, tick the spatial index. We MAY not want to do this in a bulk scenario like map loading
     */
    fun build(building: Building, block: BlockCoordinate, updateBuildingIndex: Boolean = true) {
        if (!canBuildBuildingAt(building, block)) {
            debug("We have an overlap! not building!")
            return
        }
        synchronized(this.buildingLayer) {
            this.buildingLayer[block] = building
            building.powered = true
            if (building !is Commercial && building !is Residential && building !is Industrial) {
                val buildingBlocks = buildingBlocks(block, building)
                buildingBlocks.forEach { zoneLayer.remove(it) }
            }
            if (updateBuildingIndex) {
                updateBuildingIndex()
            }
        }
    }

    /**
     * Bulldozes from the top left to the bottom right
     * @param from top-left of bulldozed zone
     * @param to bottom-right of bulldozed zone
     */
    fun bulldoze(from: BlockCoordinate, to: BlockCoordinate) {
        synchronized(buildingLayer) {
            BlockCoordinate.iterate(from, to) { coordinate ->
                synchronized(powerLineLayer) {
                    powerLineLayer.remove(coordinate)
                }
                val buildings = locationsIn(coordinate)

                // BUG: I strongly suspect this doesn't work...
                // TODO: fix this...
                // now kill all those contracts...
                buildings.forEach {
                    buildingLayer.values.forEach { otherBuilding ->
                        val otherCoords = coordinatesForBuilding(otherBuilding)
                        if (otherCoords != null) {
                            val otherEntity = CityTradeEntity(otherCoords, otherBuilding)
                            otherBuilding.voidContractsWith(otherEntity)
                        }
                    }
                    // gotta remove building from the list...
                    val iterator = buildingLayer.iterator()
                    iterator.forEach { mutableEntry ->
                        if (mutableEntry.value == it.building) {
                            iterator.remove()
                        }
                    }
                }
            }
        }
        updateBuildingIndex()
    }

    /**
     * Used to store where oil, gold, etc are underneath the map. Not currently used.
     * @TODO should I just rip this out?
     * @param resourceName
     * @param blockCoordinate
     * @param resourceValue
     */
    fun setResourceValue(resourceName: String, blockCoordinate: BlockCoordinate, resourceValue: Double) {
        if (resourceLayers[resourceName] == null) {
            resourceLayers[resourceName] = QuantizedMap(4)
        }
        resourceLayers[resourceName]?.put(blockCoordinate, resourceValue)
    }

    /**
     * Removes zoning designation from a rectangle of [BlockCoordinate]
     * @param firstBlock top left
     * @param lastBlock bottom right
     */
    fun dezone(firstBlock: BlockCoordinate, lastBlock: BlockCoordinate) {
        BlockCoordinate.iterate(firstBlock, lastBlock) {
            zoneLayer.remove(it)
        }
    }

    /**
     * Builds a line of powerlines from [firstBlock] to [lastBlock]
     * @param firstBlock first block of powerline
     * @param lastBlock last block of powerline
     */
    fun buildPowerline(firstBlock: BlockCoordinate, lastBlock: BlockCoordinate) {
        roadBlocks(firstBlock, lastBlock).forEach { block ->
            val newPowerLine = PowerLine(this)
            if (buildingLayer[block] is Road || canBuildBuildingAt(newPowerLine, block, waterCheck = false)) {
                powerLineLayer[block] = newPowerLine
            }
        }
    }

    /**
     * Returns a list of [Location]s in the provided rectangle of coordinates
     * @return List of [Location]s that we found
     * @param topLeft top left of rectangle
     * @param bottomRight bottom right of rectangle
     */
    fun locationsInRectangle(topLeft: BlockCoordinate, bottomRight: BlockCoordinate): List<Location> {
        val buildings = buildingIndex.search(
            Geometries.rectangle(
                topLeft.x.toDouble(),
                topLeft.y.toDouble(),
                bottomRight.x.toDouble(),
                bottomRight.y.toDouble()
            )
        )
        return buildings.map { it ->
            it.value()?.let { building ->
                val x = it.geometry().x1()
                val y = it.geometry().y1()
                Location(this, BlockCoordinate(x.toInt(), y.toInt()), building)
            }
        }.toBlocking().toIterable().filterNotNull()
    }

    /**
     * Returns a list of [Location]s that we found at the current coordinate
     * @param coordinate coordinate to search at
     */
    fun locationsAt(coordinate: BlockCoordinate): List<Location> {
        val point = Geometries.point(coordinate.x.toDouble(), coordinate.y.toDouble())
        val buildings = buildingIndex.search(point)
        return buildings.map {
            val building = it.value()
            val rectangle = it.geometry()
            if (building != null && rectangle != null) {
                Location(this, BlockCoordinate(rectangle.x1().toInt(), rectangle.y1().toInt()), building)
            } else {
                null
            }
        }.toBlocking().toIterable().filterNotNull()
    }

    /**
     * Returns 4 [BlockCoordinate] that represent each corner of a building. We use this for bounds checking
     * @param building building to check
     * @param block coordinate of building
     */
    private fun buildingCorners(building: Building, block: BlockCoordinate): Corners {
        val buildingTopLeft = BlockCoordinate(block.x, block.y)
        val buildingBottomRight = BlockCoordinate(block.x + building.width - 1, block.y + building.height - 1)

        val buildingTopRight = BlockCoordinate(block.x + building.width - 1, block.y)
        val buildingBottomLeft = BlockCoordinate(block.x, block.y + building.height - 1)

        return Corners(buildingTopLeft, buildingBottomRight, buildingTopRight, buildingBottomLeft)
    }

    /**
     * For a given [Building], runs through it and finds the coordinate
     * @bug This is slow as hell...
     * @param building [Building] to locate
     */
    fun coordinatesForBuilding(building: Building): BlockCoordinate? {
        synchronized(buildingLayer) {
            return buildingLayer.toList().find {
                it.second == building
            }?.first
        }
    }

    /**
     * Checks map at a given [BlockCoordinate] to return a list of [Location]s
     * @param block coordinate to check at
     */
    fun locationsIn(block: BlockCoordinate): List<Location> {
        val nearestBuildings = nearestBuildings(block, MAX_BUILDING_SIZE + 1)
        val filteredBuildings = nearestBuildings.filter {
            val coordinate = it.coordinate
            val building = it.building
            buildingCorners(building, coordinate).includes(block)
        }
        // now we also need the power lines that are here...
        powerLineLayer[block]?.let {
            return filteredBuildings.plus(Location(this, block, it))
        }
        return filteredBuildings
    }

    /**
     * We will probably end up having a desirabilty layer for each zone and each level... but I am not 100% sure yet
     * @param type [Zone] type to get
     * @param level level of matching desirability layer (1-5)
     */
    fun desirabilityLayer(type: Zone, level: Int) = desirabilityLayers.find { it.level == level && it.zoneType == type }

    /**
     * Returns a massive list of all locations
     */
    fun locations(): List<Location> {
        synchronized(buildingLayer) {
            val sequence = buildingLayer.entries.iterator().asSequence()
            return sequence.map { Location(this, it.key, it.value) }.toList()
        }
    }
}
