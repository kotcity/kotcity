package kotcity.data

import com.github.benmanes.caffeine.cache.stats.CacheStats
import com.github.davidmoten.rtree.RTree
import com.github.davidmoten.rtree.geometry.Geometries
import com.github.davidmoten.rtree.geometry.Rectangle
import com.github.debop.javatimes.plus
import com.github.debop.javatimes.toDateTime
import kotcity.automata.*
import kotcity.data.buildings.*
import kotcity.memoization.CacheOptions
import kotcity.memoization.cache
import kotcity.pathfinding.Direction
import kotcity.util.reorder
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withTimeout
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

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
    val districtLayer = mutableMapOf<BlockCoordinate, District>()
    val desirabilityLayers = listOf(
        DesirabilityLayer(Zone.RESIDENTIAL, 1),
        DesirabilityLayer(Zone.COMMERCIAL, 1),
        DesirabilityLayer(Zone.INDUSTRIAL, 1)
    )

    private var outsideConnections: List<BlockCoordinate> = listOf()

    val mainDistrict = District("Central district")
    val districts = mutableListOf(mainDistrict)

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
    var debug = true

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
    private var buildingIndex = RTree.star().create<Location, Rectangle>()!!
    private val locationsInCachePair = ::locationsAt.cache(
        CacheOptions(
            weakKeys = false,
            weakValues = false,
            limitSize = false,
            durationUnit = TimeUnit.MINUTES,
            durationValue = 5
        )
    )

    /**
     * Cache control object for locations.
     */
    private val locationsInCache = locationsInCachePair.first
    /**
     * a cached version of [locationsAt]. This can be used when you want to do a fast lookup on a building
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
        constructor.debug = false
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

    private fun locationCacheStats(): CacheStats {
        // cache...
        return locationsInCache.stats()
    }

    /**
     * @param callback pass a function that we'll call and pass a location in
     */
    fun eachLocation(callback: (Location) -> Unit) {
        this.buildingIndex.entries().forEach {
            callback(it.value())
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
            .map { it.value() }.toBlocking().toIterable().toList()
    }

    /**
     * Updates the spatial index, enabling quick building lookups...
     * @TODO get smarter about index... we don't want to be rebuilding this all the time...
     */
    fun updateBuildingIndex() {
        var newIndex = RTree.star().create<Location, Rectangle>()
        synchronized(buildingLayer) {
            buildingLayer.toList().forEach { pair ->
                val (coordinate, building) = pair
                newIndex = newIndex.add(
                    Location(coordinate, building), Geometries.rectangle(
                        coordinate.x.toFloat(),
                        coordinate.y.toFloat(),
                        coordinate.x.toFloat() + building.width.toFloat() - 1,
                        coordinate.y.toFloat() + building.height.toFloat() - 1
                    )
                )
            }
        }
        buildingIndex = newIndex
        locationsInCache.invalidateAll()
    }

    fun hasOutsideConnections() = outsideConnections.isNotEmpty()

    fun updateOutsideConnections() {
        val widthRange = -1..width
        val heightRange = -1..height

        val newOutsideConnections = mutableListOf<BlockCoordinate>()
        widthRange.forEach { x ->
            val topCoord = BlockCoordinate(x, heightRange.first)
            val topBuilding = buildingLayer[topCoord]
            val bottomCoord = BlockCoordinate(x, heightRange.last)
            val bottomBuilding = buildingLayer[bottomCoord]

            if (topBuilding.isDrivable()) {
                newOutsideConnections.add(topCoord)
            }
            if (bottomBuilding.isDrivable()) {
                newOutsideConnections.add(bottomCoord)
            }
        }
        heightRange.forEach { y ->
            val leftCoord = BlockCoordinate(widthRange.first, y)
            val leftBuilding = buildingLayer[leftCoord]
            val rightCoord = BlockCoordinate(widthRange.last, y)
            val rightBuilding = buildingLayer[rightCoord]

            if (leftBuilding.isDrivable()) {
                newOutsideConnections.add(leftCoord)
            }
            if (rightBuilding.isDrivable()) {
                newOutsideConnections.add(rightCoord)
            }
        }
        outsideConnections = newOutsideConnections
    }

    private fun Building?.isDrivable() = this is Road || this is Railroad || this is RailroadCrossing

    /**
     * Suggests a filename to save the city as... it's based off the name of the city but made safe for filenames
     */
    fun suggestedFilename() = Slug.makeSlug(cityName) + ".kcity"

    /**
     * Main game loop. The engine calls this every X milliseconds and various functions are run from here.
     */
    fun tick() {
        time += 60_000
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

                launch { timeFunction("Populating Zots") { zotPopulator.tick() } }
                launch {
                    timeFunction("Updating pollution...") { pollutionUpdater.tick() }
                    timeFunction("Updating happiness...") { happinessUpdater.tick() }
                }
            }

            if (hour == 0 || hour == 6 || hour == 12 || hour == 18) {
                timeFunction("Taking census (to calculate demand)") { censusTaker.tick() }
                timeFunction("Liquidating bankrupt properties") { liquidator.tick() }
                timeFunction("Checking validity of contracts") { ContractChecker.checkContracts(this) }
                timeFunction("Constructing buildings") { constructor.tick() }
                timeFunction("Performing upgrades") { upgrader.tick() }
                timeFunction("Taking census again (to account for new demand...)") { censusTaker.tick() }
            }

            debug("Cache stats: " + locationCacheStats().hitRate())

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

        launch {
            timeFunction("Updating power coverage...") { PowerCoverageUpdater.update(self) }
        }

        launch {
            timeFunction("Calculating desirability") { desirabilityUpdater.tick() }
        }

        launch {
            timeFunction("Collect Taxes") { taxCollector.tick() }
            timeFunction("Setting National Supply") { nationalTradeEntity.resetCounts() }
        }

        launch {
            timeFunction("Calculating fire coverage") { FireCoverageUpdater.update(self) }
            timeFunction("Calculating crime and police presence") { CrimeUpdater.update(self) }
        }

        launch {
            timeFunction("Update land value") { landValueUpdater.tick() }
        }
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
     * Executes the given function and prints elapsed time to console.
     * @param description Description of the function (printed to console)
     * @param function Actual function to invoke
     */
    private inline fun timeFunction(description: String, function: () -> Unit) {
        println("Beginning $description...")
        val totalTime = measureTimeMillis(function)
        println("$description calc took $totalTime ms")
    }

    /**
     * Look in a given rectangle and see if we have water...
     * @param from top left [BlockCoordinate]
     * @param to bottom right [BlockCoordinate]
     */
    private fun waterFound(from: BlockCoordinate, to: BlockCoordinate): Boolean {
        var waterFound = false
        BlockCoordinate.iterate(from, to) {
            if (isWaterAt(it)) {
                waterFound = true
                return@iterate false
            }
            return@iterate true
        }
        return waterFound
    }

    private fun isWaterAt(coordinate: BlockCoordinate) = groundLayer[coordinate]?.type == TileType.WATER

    /**
     * See if we can plop a new building on the map at the given coordinate
     * @param building proposed building
     * @param coordinate top left coordinate of building
     * @param waterCheck if true, we will make sure we can't build this on water
     * @return true if we can build here, false if not
     */
    fun canBuildBuildingAt(building: Building, coordinate: BlockCoordinate, waterCheck: Boolean = true): Boolean {
        val newLocation = Location(coordinate, building)

        val newBuildingEnd =
            BlockCoordinate(coordinate.x + building.width - 1, coordinate.y + building.height - 1)

        if (waterCheck && waterFound(coordinate, newBuildingEnd)) {
            return false
        }

        // OK... let's get nearby buildings to really cut this down...
        return nearestBuildings(coordinate).none { newLocation.overlaps(it) }
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
            val railroad = Railroad()
            val existingRoad = buildingLayer[block]
            if (existingRoad is Road || existingRoad is RailroadCrossing) {
                buildingLayer[block] = RailroadCrossing()
                // dezone under us...
                zoneLayer.remove(block)
            } else if (existingRoad is Railroad || canBuildBuildingAt(railroad, block, waterCheck = false)) {
                buildingLayer[block] = railroad
                // dezone under us...
                zoneLayer.remove(block)
            }
        }
        updateBuildingIndex()
        updateOutsideConnections()
    }

    /**
     * Builds a (possibly) one-way road or standard road
     * @param from start [BlockCoordinate]
     * @param to end [BlockCoordinate]
     * @param isOneWay if it's true, this is a one way road. The direction starts at [from] and goes to [to]
     */
    fun buildRoad(from: BlockCoordinate, to: BlockCoordinate, isOneWay: Boolean = false) {
        val dx = Math.abs(from.x - to.x)
        val dy = Math.abs(from.y - to.y)
        val mid =
            if (dx > dy) {
                BlockCoordinate(to.x, from.y)
            } else {
                BlockCoordinate(from.x, to.y)
            }
        buildRoadLeg(from, mid, isOneWay)
        buildRoadLeg(mid, to, isOneWay)
    }

    /**
     * Builds a single segment of [Road]. Ends up being assembled into an "L" shape.
     * @param from start [BlockCoordinate]
     * @param to end [BlockCoordinate]
     */
    private fun buildRoadLeg(from: BlockCoordinate, to: BlockCoordinate, isOneWay: Boolean) {
        roadBlocks(from, to).forEach { block ->
            val newRoad =
                when (isOneWay) {
                    false -> Road()
                    true -> {
                        buildOneWayRoad(from, to, block)
                    }
                }
            val existingRoad = buildingLayer[block]
            if (existingRoad is Railroad || existingRoad is RailroadCrossing) {
                buildingLayer[block] = RailroadCrossing()
                // dezone under us...
                zoneLayer.remove(block)
            } else if (existingRoad is Road || canBuildBuildingAt(newRoad, block, waterCheck = false)) {
                buildingLayer[block] = newRoad
                // dezone under us...
                zoneLayer.remove(block)
            }
        }
        updateBuildingIndex()
        updateOutsideConnections()
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

        val dir =
            if (isDxGreater && to.x > from.x) {
                val above = buildingLayer[block.top()]
                val below = buildingLayer[block.bottom()]
                if (above is Road || below is Road) {
                    Direction.STATIONARY
                } else {
                    Direction.EAST
                }
            } else if (isDxGreater && to.x < from.x) {
                val above = buildingLayer[block.top()]
                val below = buildingLayer[block.bottom()]
                if (above is Road || below is Road) {
                    Direction.STATIONARY
                } else {
                    Direction.WEST
                }
            } else if (isDyGreater && to.y > from.y) {
                val left = buildingLayer[block.left()]
                val right = buildingLayer[block.right()]
                if (left is Road || right is Road) {
                    Direction.STATIONARY
                } else {
                    Direction.SOUTH
                }
            } else if (isDyGreater && to.y < from.y) {
                val left = buildingLayer[block.left()]
                val right = buildingLayer[block.right()]
                if (left is Road || right is Road) {
                    Direction.STATIONARY
                } else {
                    Direction.NORTH
                }
            } else {
                Direction.STATIONARY
            }
        return Road(dir)
    }

    /**
     * Designates a given area as a certain [Zone]
     * @param type [Zone] to mark
     * @param from top-left corner
     * @param to bottom-right corner
     */
    fun zone(type: Zone, from: BlockCoordinate, to: BlockCoordinate) {
        BlockCoordinate.iterateAll(from, to) {
            if (!isWaterAt(it)) {
                if (locationsAt(it).count() == 0) {
                    zoneLayer[it] = type
                }
            }
        }
    }

    /**
     * Designates a given area as a certain [District]
     * @param district [District] to assign
     * @param from top-left corner
     * @param to bottom-right corner
     */
    fun assignDistrict(district: District, from: BlockCoordinate, to: BlockCoordinate) {
        BlockCoordinate.iterateAll(from, to) {
            val oldDistrict = districtAt(it)
            if (oldDistrict != mainDistrict) {
                oldDistrict.blocks.remove(it)
                oldDistrict.clearCorners()
                if (oldDistrict.blocks.isEmpty()) {
                    districts.remove(oldDistrict)
                }
            }
            if (district != mainDistrict) {
                district.blocks.add(it)
                oldDistrict.clearCorners()
            }
            districtLayer[it] = district
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
                building.buildingBlocks(block).forEach { zoneLayer.remove(it) }
            }
            if (building.isDrivable()) {
                updateOutsideConnections()
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
            BlockCoordinate.iterateAll(from, to) { coordinate ->
                synchronized(powerLineLayer) {
                    powerLineLayer.remove(coordinate)
                }
                val buildings = locationsAt(coordinate)

                buildings.forEach {
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
        updateOutsideConnections()
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
        resourceLayers[resourceName]?.set(blockCoordinate, resourceValue)
    }

    /**
     * Removes zoning designation from a rectangle of [BlockCoordinate]
     * @param firstBlock top left
     * @param lastBlock bottom right
     */
    fun dezone(firstBlock: BlockCoordinate, lastBlock: BlockCoordinate) {
        BlockCoordinate.iterateAll(firstBlock, lastBlock) {
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
            val newPowerLine = PowerLine()
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
    fun locationsInRectangle(topLeft: BlockCoordinate, bottomRight: BlockCoordinate): Iterable<Location> {
        val buildings = buildingIndex.search(
            Geometries.rectangle(
                topLeft.x.toDouble(),
                topLeft.y.toDouble(),
                bottomRight.x.toDouble(),
                bottomRight.y.toDouble()
            )
        )
        return buildings.map { it.value() }.toBlocking().toIterable().filterNotNull()
    }

    /**
     * Returns a list of [Location]s that we found at the current coordinate
     * @param coordinate coordinate to search at
     */
    fun locationsAt(coordinate: BlockCoordinate): List<Location> {
        val point = Geometries.point(coordinate.x.toDouble(), coordinate.y.toDouble())
        val buildings = buildingIndex.search(point)
        return buildings.map { it.value() }.toBlocking().toIterable().toList()
    }

    /**
     * Returns the [District] that we found at the current coordinate
     * @param coordinate the coordinate to search at
     */
    fun districtAt(coordinate: BlockCoordinate) = districtLayer[coordinate] ?: mainDistrict

    /**
     * We will probably end up having a desirability layer for each zone and each level... but I am not 100% sure yet
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
            return sequence.map { Location(it.key, it.value) }.toList()
        }
    }
}
