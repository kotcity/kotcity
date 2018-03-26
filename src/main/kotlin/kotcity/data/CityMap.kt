package kotcity.data

import com.github.davidmoten.rtree.RTree
import com.github.davidmoten.rtree.geometry.Geometries
import com.github.davidmoten.rtree.geometry.Rectangle
import com.github.debop.javatimes.plus
import com.github.debop.javatimes.toDateTime
import kotcity.automata.*
import kotcity.memoization.CacheOptions
import kotcity.memoization.cache
import kotcity.pathfinding.Direction
import kotcity.ui.map.MAX_BUILDING_SIZE
import kotcity.util.reorder
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withTimeout
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


const val DEFAULT_DESIRABILITY = 0.0

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

data class DesirabilityLayer(val zoneType: Zone, val level: Int) : QuantizedMap<Double>(1) {
    init {
        map = map.withDefault { 0.0 }
    }
}

data class CityMap(var width: Int = 512, var height: Int = 512) {

    companion object {
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
    val fireCoverageLayer = mutableMapOf<BlockCoordinate, Double>()
    val crimeLayer = mutableMapOf<BlockCoordinate, Double>()
    val policePresenceLayer = mutableMapOf<BlockCoordinate, Double>()
    var trafficLayer = mutableMapOf<BlockCoordinate, Double>().withDefault { 0.0 }
    val desirabilityLayers = listOf(
        DesirabilityLayer(Zone.RESIDENTIAL, 1),
        DesirabilityLayer(Zone.COMMERCIAL, 1),
        DesirabilityLayer(Zone.INDUSTRIAL, 1)
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

    val nationalTradeEntity = NationalTradeEntity(this)

    private var doingHourly = false

    var bulldozedCounts = mutableMapOf<Zone, Int>().withDefault { 0 }

    var time: Date

    var debug = true

    // where we loaded OR saved this city to...
    // used to determine save vs. save as...
    var fileName: String? = null
    var cityName: String? = null
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
    private val locationsInCache = locationsInCachePair.first
    val cachedLocationsIn = locationsInCachePair.second

    fun debug(message: String) {
        if (!debug) return
        println("Map: $message")
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
        censusTaker.tick()
        happinessUpdater.debug = true
        upgrader.debug = true
        nationalTradeEntity.resetCounts()

        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        simpleDateFormat.timeZone = TimeZone.getDefault()
        time = simpleDateFormat.parse("2000-01-01 12:00:00")
    }

    fun eachLocation(callback: (Location) -> Unit) {
        buildingLayer.toList().forEach { entry ->
            callback(Location(entry.first, entry.second))
        }
    }

    fun purgeRTree() {
        val idx = this.buildingIndex.entries().toBlocking().iterator
        idx.forEach {
            this.buildingIndex.delete(it)
        }
    }

    fun elevations(): Pair<Double, Double> {
        val mapMinElevation = groundLayer.values.map { it.elevation }.min() ?: 0.0
        val mapMaxElevation = groundLayer.values.map { it.elevation }.max() ?: 0.0
        return Pair(mapMinElevation, mapMaxElevation)
    }

    fun buildingBlocks(coordinate: BlockCoordinate, building: Building): List<BlockCoordinate> {
        val xRange = coordinate.x..coordinate.x + (building.width - 1)
        val yRange = coordinate.y..coordinate.y + (building.height - 1)
        return xRange.flatMap { x -> yRange.map { BlockCoordinate(x, it) } }
    }

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

    fun nearestBuildings(coordinate: BlockCoordinate, distance: Int = 10): List<Location> {
        val point = Geometries.point(coordinate.x.toFloat(), coordinate.y.toFloat())
        return buildingIndex.search(point, distance.toDouble())
            .filter({ t -> t != null }).map { entry ->
                val geometry = entry.geometry()
                val building = entry.value()
                if (geometry != null && building != null) {
                    Location(BlockCoordinate(geometry.x1().toInt(), geometry.y1().toInt()), building)
                } else {
                    null
                }
            }.toBlocking().toIterable().filterNotNull()
    }

    // TODO: get smarter about index... we don't want to be rebuilding this all the time...
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

    fun getBuilding(blockCoordinate: BlockCoordinate): Building? {
        synchronized(this.buildingLayer) {
            return buildingLayer[blockCoordinate]
        }
    }

    fun suggestedFilename() = Slug.makeSlug(cityName) + ".kcity"

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

    suspend fun hourlyTick(hour: Int) {
        try {
            doingHourly = true
            debug("Processing tick for: $hour:00")

            if (hour % 3 == 0) {
                timeFunction("Calculating desirability") { desirabilityUpdater.update() }
                timeFunction("Terminating random contracts") { contractFulfiller.terminateRandomContracts() }
                withTimeout(5000, TimeUnit.MILLISECONDS) {
                    timeFunction("Signing contracts") { contractFulfiller.signContracts() }
                }
                timeFunction("Doing manufacturing") { manufacturer.tick() }
                timeFunction("Shipping products") { shipper.tick() }
                timeFunction("Consuming goods") { goodsConsumer.tick() }
                timeFunction("Generating traffic") { trafficCalculator.tick() }
                async { timeFunction("Taking census") { censusTaker.tick() } }
                async { timeFunction("Populating Zots") { zotPopulator.tick() } }
                async {
                    timeFunction("Updating happiness...") { happinessUpdater.tick() }
                }
            }

            if (hour == 0 || hour == 12) {
                timeFunction("Liquidating bankrupt properties") { liquidator.tick() }
                timeFunction("Constructing buildings") { constructor.tick() }
                timeFunction("Performing upgrades") { upgrader.tick() }
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

    private fun dailyTick() {
        val self = this

        async {
            timeFunction("Updating power coverage...") { PowerCoverageUpdater.update(self) }
        }

        async {
            timeFunction("Collect Taxes") { taxCollector.tick() }
            timeFunction("Setting National Supply") { nationalTradeEntity.resetCounts() }
            timeFunction("Calculating fire coverage") { FireCoverageUpdater.update(self) }
            timeFunction("Calculating crime and police presence") { CrimeUpdater.update(self) }
        }
    }

    fun hasTrafficNearby(coordinate: BlockCoordinate, radius: Int, quantity: Int): Boolean {
        val neighboringBlocks = coordinate.neighbors(radius)
        val nearbyRoads = neighboringBlocks.flatMap { cachedLocationsIn(it) }
            .filter { it.building is Road }

        val trafficCount = nearbyRoads.sumBy { trafficLayer[it.coordinate]?.toInt() ?: 0 }
        return trafficCount > quantity
    }

    private fun timeFunction(desc: String, timedFunction: () -> Unit) {
        println("Beginning $desc...")
        val startMillis = System.currentTimeMillis()
        timedFunction()
        val endMillis = System.currentTimeMillis()
        val totalTime = endMillis - startMillis
        println("$desc calc took $totalTime millis")
    }

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

    fun buildRoad(from: BlockCoordinate, to: BlockCoordinate, isOneWay: Boolean = false) {
        roadBlocks(from, to).forEach { block ->
            val newRoad =
                when (isOneWay) {
                    false -> Road(this)
                    true -> {
                        val dx = Math.abs(to.x - from.x)
                        val dy = Math.abs(to.y - from.y)
                        val isDxGreater = dx > dy
                        val isDyGreater = dy > dx
                        val left  = buildingLayer[BlockCoordinate(block.x - 1, block.y)]
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
                        Road(this, dir)
                    }
                }
            val existingRoad = buildingLayer[block]
            if ((existingRoad is Road) || canBuildBuildingAt(newRoad, block, waterCheck = false)) {
                buildingLayer[block] = newRoad
                // dezone under us...
                zoneLayer.remove(block)
            } else {
                // debug("We have an overlap... not building!")
            }
        }
        updateBuildingIndex()
    }

    fun zone(type: Zone, from: BlockCoordinate, to: BlockCoordinate) {
        BlockCoordinate.iterate(from, to) {
            if (!waterFound(it, it)) {
                if (locationsIn(it).count() == 0) {
                    zoneLayer[it] = type
                }
            }
        }
    }

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

    fun bulldoze(from: BlockCoordinate, to: BlockCoordinate) {
        synchronized(buildingLayer) {
            BlockCoordinate.iterate(from, to) { coordinate ->
                synchronized(powerLineLayer) {
                    powerLineLayer.remove(coordinate)
                }
                val buildings = locationsIn(coordinate)
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

    fun setResourceValue(resourceName: String, blockCoordinate: BlockCoordinate, resourceValue: Double) {
        // make sure that fucker is set!
        if (resourceLayers[resourceName] == null) {
            resourceLayers[resourceName] = QuantizedMap(4)
        }
        resourceLayers[resourceName]?.put(blockCoordinate, resourceValue)
    }

    fun dezone(firstBlock: BlockCoordinate, lastBlock: BlockCoordinate) {
        BlockCoordinate.iterate(firstBlock, lastBlock) {
            zoneLayer.remove(it)
        }
    }

    fun buildPowerline(firstBlock: BlockCoordinate, lastBlock: BlockCoordinate) {
        roadBlocks(firstBlock, lastBlock).forEach { block ->
            val newPowerLine = PowerLine(this)
            if (buildingLayer[block] is Road || canBuildBuildingAt(newPowerLine, block, waterCheck = false)) {
                powerLineLayer[block] = newPowerLine
            }
        }
    }

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
                Location(BlockCoordinate(x.toInt(), y.toInt()), building)
            }
        }.toBlocking().toIterable().filterNotNull()
    }

    fun locationsAt(coordinate: BlockCoordinate): List<Location> {
        val point = Geometries.point(coordinate.x.toDouble(), coordinate.y.toDouble())
        val buildings = buildingIndex.search(point)
        return buildings.map {
            val building = it.value()
            val rectangle = it.geometry()
            if (building != null && rectangle != null) {
                Location(BlockCoordinate(rectangle.x1().toInt(), rectangle.y1().toInt()), building)
            } else {
                null
            }
        }.toBlocking().toIterable().filterNotNull()
    }

    private fun buildingCorners(building: Building, block: BlockCoordinate): Corners {
        val buildingTopLeft = BlockCoordinate(block.x, block.y)
        val buildingBottomRight = BlockCoordinate(block.x + building.width - 1, block.y + building.height - 1)

        val buildingTopRight = BlockCoordinate(block.x + building.width - 1, block.y)
        val buildingBottomLeft = BlockCoordinate(block.x, block.y + building.height - 1)

        return Corners(buildingTopLeft, buildingBottomRight, buildingTopRight, buildingBottomLeft)
    }

    // TODO: this kind of sucks...
    fun coordinatesForBuilding(building: Building): BlockCoordinate? {
        synchronized(buildingLayer) {
            return buildingLayer.toList().find {
                it.second == building
            }?.first
        }
    }

    private fun locationsIn(block: BlockCoordinate): List<Location> {
        val nearestBuildings = nearestBuildings(block, MAX_BUILDING_SIZE + 1)
        val filteredBuildings = nearestBuildings.filter {
            val coordinate = it.coordinate
            val building = it.building
            buildingCorners(building, coordinate).includes(block)
        }
        // now we also need the power lines that are here...
        powerLineLayer[block]?.let {
            return filteredBuildings.plus(Location(block, it))
        }
        return filteredBuildings
    }

    fun desirabilityLayer(type: Zone, level: Int) = desirabilityLayers.find { it.level == level && it.zoneType == type }

    fun locations(): List<Location> {
        synchronized(buildingLayer) {
            val sequence = buildingLayer.entries.iterator().asSequence()
            return sequence.map { Location(it.key, it.value) }.toList()
        }
    }
}
