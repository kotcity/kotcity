package kotcity.data

import com.github.davidmoten.rtree.RTree
import com.github.davidmoten.rtree.geometry.Geometries
import com.github.davidmoten.rtree.geometry.Rectangle
import com.github.debop.javatimes.plus
import com.github.debop.javatimes.toDateTime
import kotcity.automata.*
import kotcity.ui.map.MAX_BUILDING_SIZE
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.text.SimpleDateFormat
import java.util.*

const val DEFAULT_DESIRABILITY = 0.0

data class BlockCoordinate(val x: Int, val y: Int) {
    companion object {
        fun iterate(from: BlockCoordinate, to: BlockCoordinate, callback: (BlockCoordinate) -> Unit) {
            val xRange = (from.x .. to.x).reorder()
            val yRange = (from.y ..to.y).reorder()
            for (x in xRange) {
                for (y in yRange) {
                    callback(BlockCoordinate(x, y))
                }
            }
        }
    }

    fun neighbors(radius: Int = 1): List<BlockCoordinate> {
        val xRange = this.x - radius .. this.x + radius
        val yRange = this.y - radius .. this.y + radius

        return xRange.flatMap { x ->
            yRange.map { y ->
                BlockCoordinate(x,y)
            }
        }

    }

    fun distanceTo(otherCoordinate: BlockCoordinate): Double {
        return Math.sqrt(((this.x-otherCoordinate.x)*(this.x-otherCoordinate.x) + (this.y-otherCoordinate.y)*(this.y-otherCoordinate.y)).toDouble())
    }


}

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

fun IntRange.reorder(): IntRange {
    return if (first < last) {
        this
    } else {
        last..first
    }
}

enum class TileType { GROUND, WATER}
data class MapTile(val type: TileType, val elevation: Double)

fun defaultTime(): Date {
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    simpleDateFormat.timeZone = TimeZone.getDefault()
    return simpleDateFormat.parse("2000-01-01 12:00:00")
}

data class DesirabilityLayer(val zoneType: Zone, val level: Int): QuantizedMap<Double>(1) {
    init {
        map = map.withDefault { 0.0 }
    }
}

data class CityMap(var width: Int = 512, var height: Int = 512) {

    companion object {
        fun flatMap(width: Int = 512, height: Int = 512): CityMap {
            val map = CityMap(width, height)
            // set all tiles to ground...
            val xRange = 0 .. map.width
            val yRange = 0 .. map.height
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
    val buildingLayer = mutableMapOf<BlockCoordinate, Building>()
    val zoneLayer = mutableMapOf<BlockCoordinate, Zone>()
    val powerLineLayer = mutableMapOf<BlockCoordinate, Building>()
    val resourceLayers = mutableMapOf<String, QuantizedMap<Double>>()
    val desirabilityLayers = initializeDesirabilityLayers()
    var trafficLayer = mutableMapOf<BlockCoordinate, Double>().withDefault { 0.0 }

    private val constructor = Constructor(this)
    private val contractFulfiller = ContactFulfiller(this)
    private val manufacturer = Manufacturer(this)
    private val shipper = Shipper(this)
    private val desirabilityUpdater = DesirabilityUpdater(this)
    val censusTaker = CensusTaker(this)
    private val taxCollector = TaxCollector(this)
    private val liquidator = Liquidator(this)
    private val trafficCalculator = TrafficCalculator(this)

    val nationalTradeEntity = NationalTradeEntity(this)

    private var doingHourly: Boolean = false

    private fun initializeDesirabilityLayers(): List<DesirabilityLayer> {

        return listOf(
                DesirabilityLayer(Zone.RESIDENTIAL, 1),
                DesirabilityLayer(Zone.COMMERCIAL, 1),
                DesirabilityLayer(Zone.INDUSTRIAL, 1)
        )
    }

    var time = defaultTime()

    var debug = true

    // where we loaded OR saved this city to...
    // used to determine save vs. save as...
    var fileName: String? = null
    var cityName: String? = null
    private var buildingIndex = RTree.create<Building, Rectangle>()!!

    fun debug(message: String) {
        if (debug) {
            println("Map: $message")
        }
    }

    init {
        shipper.debug = false
        contractFulfiller.debug = true
        manufacturer.debug = false
        constructor.debug = true
        taxCollector.debug = false
        liquidator.debug = false
        censusTaker.tick()
        nationalTradeEntity.resetCounts()
    }

    fun eachLocation(callback: (Location) -> Unit) {
        buildingLayer.forEach { t, u -> callback(Location(t, u)) }
    }

    fun elevations(): Pair<Double, Double> {
        val mapMinElevation = groundLayer.values.mapNotNull { it.elevation }.min() ?: 0.0
        val mapMaxElevation = groundLayer.values.mapNotNull { it.elevation }.max() ?: 0.0
        return Pair(mapMinElevation, mapMaxElevation)
    }


    fun buildingBlocks(coordinate: BlockCoordinate, building: Building): List<BlockCoordinate> {
        val xRange = coordinate.x .. coordinate.x + (building.width - 1)
        val yRange = coordinate.y .. coordinate.y + (building.height - 1)
        return xRange.flatMap { x -> yRange.map { BlockCoordinate(x, it) } }
    }

    private fun roadBlocks(startBlock: BlockCoordinate, endBlock: BlockCoordinate): MutableList<BlockCoordinate> {
        val blockList = mutableListOf<BlockCoordinate>()
        if (Math.abs(startBlock.x - endBlock.x) > Math.abs(startBlock.y - endBlock.y)) {
            // going horizontally...
            (startBlock.x .. endBlock.x).reorder().forEach { x ->
                blockList.add(BlockCoordinate(x, startBlock.y))
            }
        } else {
            // going vertically...
            (startBlock.y .. endBlock.y).reorder().forEach { y ->
                blockList.add(BlockCoordinate(startBlock.x, y))
            }
        }
        return blockList
    }

    fun nearestBuildings(coordinate: BlockCoordinate, distance: Int = 10): List<Location> {
        val point = Geometries.rectangle(coordinate.x.toFloat(), coordinate.y.toFloat(),coordinate.x.toFloat()+1, coordinate.y.toFloat()+1)
        return buildingIndex.search(point, distance.toDouble())
                .filter( {t -> t != null } ).map { entry ->
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
            newIndex = newIndex.add(building, Geometries.rectangle(
                    coordinate.x.toFloat(),
                    coordinate.y.toFloat(),
                    coordinate.x.toFloat() + building.width.toFloat(),
                    coordinate.y.toFloat() + building.height.toFloat())
            )
        }
        buildingIndex = newIndex
    }

    fun suggestedFilename(): String {
        return Slug.makeSlug(cityName) + ".kcity"
    }

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

    fun hourlyTick(hour: Int) {
        try {
            doingHourly = true
            debug("Processing tick for: $hour:00")

            if (hour % 3 == 0) {
                timeFunction("Calculating desirability") { desirabilityUpdater.update() }
                timeFunction("Constructing buildings") { constructor.tick() }
                timeFunction("Terminating random contracts") { contractFulfiller.terminateRandomContracts() }
                timeFunction("Signing contracts") { contractFulfiller.signContracts() }
                timeFunction("Doing manufacturing") { manufacturer.tick() }
                timeFunction("Shipping products") { shipper.tick() }
                timeFunction("Generating traffic") { trafficCalculator.tick() }
                timeFunction("Taking census") { censusTaker.tick() }
            }

            if (hour == 0) {
                debug("Processing tick for end of day...")
                dailyTick()
            }
        } catch(e: Exception ) {
            println("WARNING! Error during hourly: ${e.message}")
            e.printStackTrace()
        } finally {
            doingHourly = false
        }
    }

    private fun dailyTick() {
        timeFunction("Updating power coverage...") { PowerCoverageUpdater.update(this) }
        timeFunction("Collect Taxes") { taxCollector.tick() }
        timeFunction("Liquidating bankrupt properties") { liquidator.tick() }
        timeFunction("Setting National Supply") { nationalTradeEntity.resetCounts() }
    }

    private fun timeFunction(desc: String, timedFunction: () -> Unit) {
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
            }
        }
        return waterFound
    }

    fun canBuildBuildingAt(newBuilding: Building, coordinate: BlockCoordinate, waterCheck: Boolean = true): Boolean {

        // OK... let's get nearby buildings to really cut this down...
        val newBuildingEnd = BlockCoordinate(coordinate.x + newBuilding.width - 1, coordinate.y + newBuilding.height - 1)

        val newBuildingTopRight = BlockCoordinate(coordinate.x + newBuilding.width - 1, coordinate.y)
        val newBuildingBottomLeft = BlockCoordinate(coordinate.x, coordinate.y + newBuilding.height - 1)

        if (waterCheck && waterFound(coordinate, newBuildingEnd)) {
            return false
        }

        val nearby = nearestBuildings(coordinate)
        nearby.forEach { cityLocation: Location ->
            val building = cityLocation.building
            val otherBuildingStart = cityLocation.coordinate
            val otherBuildingEnd = BlockCoordinate(otherBuildingStart.x + building.width - 1, otherBuildingStart.y + building.height - 1)
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

    private fun collisionWarning(errorMessage: String, newBuilding: Building, coordinate: BlockCoordinate, building: Building, otherCoordinate: BlockCoordinate) {
        debug("$errorMessage -> ${newBuilding.name} at $coordinate: collision with ${building.name} at $otherCoordinate!")
    }

    fun buildRoad(from: BlockCoordinate, to: BlockCoordinate) {
        roadBlocks(from, to).forEach { block ->
            val newRoad = Road(this)
            if (canBuildBuildingAt(newRoad, block, waterCheck = false)) {
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
                if (buildingsIn(it).count() == 0) {
                    zoneLayer[it] = type
                }
            }
        }
    }

    fun build(building: Building, block: BlockCoordinate) {
        if (canBuildBuildingAt(building, block)) {
            this.buildingLayer[block] = building
            if (building.type != BuildingType.COMMERCIAL && building.type != BuildingType.RESIDENTIAL && building.type != BuildingType.INDUSTRIAL) {
                val buildingBlocks = buildingBlocks(block, building)
                buildingBlocks.forEach { zoneLayer.remove(it) }
            }
            updateBuildingIndex()
        } else {
            // debug("We have an overlap! not building!")
        }
    }

    fun bulldoze(from: BlockCoordinate, to: BlockCoordinate) {
        BlockCoordinate.iterate(from, to) { coordinate ->
            powerLineLayer.remove(coordinate)
            val buildings = buildingsIn(coordinate)
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
            if (buildingLayer[block]?.type == BuildingType.ROAD || canBuildBuildingAt(newPowerLine, block, waterCheck = false)) {
                powerLineLayer[block] = newPowerLine
                // println("Dropping a powerline at: $block")
            } else {
                // debug("We have an overlap... not building!")
            }
        }
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
        return buildingLayer.toList().find {
            it.second === building
        }?.first
    }

    fun buildingsIn(block: BlockCoordinate): List<Location> {
        val nearestBuildings = nearestBuildings(block, MAX_BUILDING_SIZE+1)
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

    fun desirabilityLayer(type: Zone, level: Int): DesirabilityLayer? {
        return desirabilityLayers.find { it.level == level && it.zoneType == type }
    }

    fun locations(): List<Location> {
        return buildingLayer.entries.toList().map { Location(it.key, it.value) }.toList()
    }

}

