package kotcity.data

import com.github.davidmoten.rtree.RTree
import com.github.davidmoten.rtree.geometry.Geometries
import com.github.davidmoten.rtree.geometry.Rectangle
import com.github.debop.javatimes.plus
import com.github.debop.javatimes.toDateTime
import kotlinx.coroutines.experimental.async
import java.text.SimpleDateFormat
import java.util.*

const val DEFAULT_DESIRABILITY = 0.0

data class BlockCoordinate(val x: Int, val y: Int) {
    companion object {
        fun iterate(from: BlockCoordinate, to: BlockCoordinate, callback: (BlockCoordinate) -> Unit) {
            val xRange = (from.x .. to.x).reorder()
            val yRange = (from.y ..to.y).reorder()
            // println("Wanting to iterate: $xRange to $yRange")
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

data class DesirabilityLayer(val zoneType: ZoneType, val level: Int): QuantizedMap<Double>(1) {
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

    private fun initializeDesirabilityLayers(): List<DesirabilityLayer> {
        val layers = listOf(
                DesirabilityLayer(ZoneType.RESIDENTIAL, 1),
                DesirabilityLayer(ZoneType.RESIDENTIAL, 2),
                DesirabilityLayer(ZoneType.RESIDENTIAL, 3),
                DesirabilityLayer(ZoneType.COMMERCIAL, 1),
                DesirabilityLayer(ZoneType.COMMERCIAL, 2),
                DesirabilityLayer(ZoneType.COMMERCIAL, 3),
                DesirabilityLayer(ZoneType.INDUSTRIAL, 1),
                DesirabilityLayer(ZoneType.INDUSTRIAL, 2),
                DesirabilityLayer(ZoneType.INDUSTRIAL, 3)
        )

        return layers
    }

    var time = defaultTime()

    // where we loaded OR saved this city to...
    // used to determine save vs. save as...
    var fileName: String? = null
    var cityName: String? = null
    private var buildingIndex = RTree.create<Building, Rectangle>()!!

    fun elevations(): Pair<Double, Double> {
        val mapMinElevation = groundLayer.values.mapNotNull { it.elevation }.min() ?: 0.0
        val mapMaxElevation = groundLayer.values.mapNotNull { it.elevation }.max() ?: 0.0
        return Pair(mapMinElevation, mapMaxElevation)
    }

    private fun roadBlocks(startBlock: BlockCoordinate, endBlock: BlockCoordinate): MutableList<BlockCoordinate> {
        val blockList = mutableListOf<BlockCoordinate>()
        if (Math.abs(startBlock.x - endBlock.x) > Math.abs(startBlock.y - endBlock.y)) {
            // going horizontally...
            (startBlock.x .. endBlock.x).reorder().forEach { x ->
                // println("adding block for $x, ${startBlock.y}")
                blockList.add(BlockCoordinate(x, startBlock.y))
            }
        } else {
            // going vertically...
            (startBlock.y .. endBlock.y).reorder().forEach { y ->
                // println("adding block for ${startBlock.x},$y")
                blockList.add(BlockCoordinate(startBlock.x, y))
            }
        }
        return blockList
    }

    fun nearestBuildings(coordinate: BlockCoordinate, distance: Float = 10f): List<Pair<BlockCoordinate, Building>> {
        val point = Geometries.point(coordinate.x.toFloat(), coordinate.y.toFloat())
        return buildingIndex.search(point, distance.toDouble())
                .toBlocking().toIterable().mapNotNull { entry ->
            // println("Found entry: $entry")
            val geometry = entry.geometry() as Rectangle
            val building = entry.value() as Building
            if (geometry != null && building != null) {
                Pair(BlockCoordinate(geometry.x1().toInt(), geometry.y1().toInt()), building)
            } else {
                null
            }
        }
    }

    // TODO: get smarter about index... we don't want to be rebuilding this all the time...
    fun updateBuildingIndex() {
        var newIndex = RTree.create<Building, Rectangle>()
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
        // println("Ticked to: ${kotcity.ui.serializeDate(time)}")
        val self = this
        if (time.toDateTime().minuteOfHour == 0) {
            synchronized(self) {
                async {
                    hourlyTick()
                }
            }
        }

    }

    @Synchronized fun hourlyTick() {
        println("Top of the hour stuff...")

        val hour = time.toDateTime().hourOfDay
        println("Hour is: $hour")
        if (hour == 0) {
            println("Top of the day stuff...")
            populateZones()

            PowerCoverageUpdater.update(this)
            DesirabilityUpdater.update(this)
        }
    }

    private fun populateZones() {
        // what we want to do here is find an empty residential zone and toss a house in...
        // we will add smarts later to make sure it makes sense...
//        val houseToBuild = SmallHouse()
//        findEmptyZone(houseToBuild, ZoneType.RESIDENTIAL)?.let {
//            build(houseToBuild, it)
//        }
//
//        val cornerStoreToBuild = CornerStore()
//        findEmptyZone(cornerStoreToBuild, ZoneType.COMMERCIAL)?.let {
//            build(cornerStoreToBuild, it)
//        }
//
//        val workshopToBuild = Workshop()
//        findEmptyZone(workshopToBuild, ZoneType.INDUSTRIAL)?.let {
//            build(workshopToBuild, it)
//        }
    }

    private fun findEmptyZone(building: Building, zoneType: ZoneType): BlockCoordinate? {
        return zoneLayer.toList().shuffled().find { entry ->
            val coordinate = entry.first
            val zone = entry.second
            zone.type == zoneType && canBuildBuildingAt(building, coordinate)
        }?.first
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

    private fun canBuildBuildingAt(newBuilding: Building, coordinate: BlockCoordinate, waterCheck: Boolean = true): Boolean {

        // OK... let's get nearby buildings to really cut this down...
        val newBuildingEnd = BlockCoordinate(coordinate.x + newBuilding.width - 1, coordinate.y + newBuilding.height - 1)

        val newBuildingTopRight = BlockCoordinate(coordinate.x + newBuilding.width - 1, coordinate.y)
        val newBuildingBottomLeft = BlockCoordinate(coordinate.x, coordinate.y + newBuilding.height - 1)

        if (waterCheck && waterFound(coordinate, newBuildingEnd)) {
            return false
        }

        val nearby = nearestBuildings(coordinate)
        nearby.forEach { pair: Pair<BlockCoordinate, Building> ->
            val building = pair.second
            val otherBuildingStart = pair.first
            val otherBuildingEnd = BlockCoordinate(otherBuildingStart.x + building.width - 1, otherBuildingStart.y + building.height - 1)
            // now let's test...
            // top left corner...
            if (coordinate.x <= otherBuildingEnd.x && coordinate.x >= otherBuildingStart.x && coordinate.y <= otherBuildingEnd.y && coordinate.y >= otherBuildingStart.y) {
                collisionWarning("Collision with top left!", newBuilding, coordinate, building, pair.first)
                return false
            }

            // bottom right corner...
            if (newBuildingEnd.x <= otherBuildingEnd.x && newBuildingEnd.x >= otherBuildingStart.x && newBuildingEnd.y <= otherBuildingEnd.y && newBuildingEnd.y >= otherBuildingStart.y) {
                collisionWarning("Collision with bottom right!", newBuilding, coordinate, building, pair.first)
                return false
            }

            // top right corner...
            if (newBuildingTopRight.x <= otherBuildingEnd.x && newBuildingTopRight.x >= otherBuildingStart.x && newBuildingTopRight.y <= otherBuildingEnd.y && newBuildingTopRight.y >= otherBuildingStart.y) {
                collisionWarning("Collision with top right!", newBuilding, coordinate, building, pair.first)
                return false
            }

            // bottom left corner...
            if (newBuildingBottomLeft.x <= otherBuildingEnd.x && newBuildingBottomLeft.x >= otherBuildingStart.x && newBuildingBottomLeft.y <= otherBuildingEnd.y && newBuildingBottomLeft.y >= otherBuildingStart.y) {
                collisionWarning("Collision with bottom left!", newBuilding, coordinate, building, pair.first)
                return false
            }
        }
        return true
    }

    private fun collisionWarning(errorMessage: String, newBuilding: Building, coordinate: BlockCoordinate, building: Building, otherCoordinate: BlockCoordinate) {
        println("$errorMessage -> $newBuilding at $coordinate: collision with $building at $otherCoordinate!")
    }

    fun buildRoad(from: BlockCoordinate, to: BlockCoordinate) {
        roadBlocks(from, to).forEach { block ->
            // println("Dropping a road at: $block")
            val newRoad = Road()
            if (canBuildBuildingAt(newRoad, block, waterCheck = false)) {
                buildingLayer[block] = newRoad
            } else {
                println("We have an overlap... not building!")
            }
        }
        updateBuildingIndex()
    }

    fun zone(type: ZoneType, from: BlockCoordinate, to: BlockCoordinate) {
        BlockCoordinate.iterate(from, to) {
            if (!waterFound(it, it)) {
                zoneLayer[it] = Zone(type)
            }
        }
    }

    fun build(building: Building, block: BlockCoordinate) {
        if (canBuildBuildingAt(building, block)) {
            this.buildingLayer[block] = building
            updateBuildingIndex()
        } else {
            println("We have an overlap! not building!")
        }
    }

    fun bulldoze(from: BlockCoordinate, to: BlockCoordinate) {
        println("Want to bulldoze from $from to $to")
        BlockCoordinate.iterate(from, to) {
            buildingLayer.remove(it)
            powerLineLayer.remove(it)
        }
        updateBuildingIndex()
    }

    fun setResourceValue(resourceName: String, blockCoordinate: BlockCoordinate, resourceValue: Double) {
        // println("On the resourceZone: $resourceName we want to set $resourceValue at $blockCoordinate")

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
            val newPowerLine = PowerLine()
            if (buildingLayer[block]?.type == BuildingType.ROAD || canBuildBuildingAt(newPowerLine, block, waterCheck = false)) {
                powerLineLayer[block] = newPowerLine
                // println("Dropping a powerline at: $block")
            } else {
                println("We have an overlap... not building!")
            }
        }
    }

    // TODO: we should start throwing back 4 corners again to use this for overlaps...
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

    fun buildingsIn(block: BlockCoordinate): List<Pair<BlockCoordinate, Building>> {
        val nearestBuildings = nearestBuildings(block, 10f)
        val filteredBuildings = nearestBuildings.filter {
            val coordinate = it.first
            val building = it.second
            buildingCorners(building, coordinate).includes(block)
        }

        // now we also need the power lines that are here...
        powerLineLayer[block]?.let {
            return filteredBuildings.plus(Pair(block, it))
        }
        return filteredBuildings
    }

    fun desirabilityLayer(type: ZoneType, level: Int): DesirabilityLayer? {
        return desirabilityLayers.find { it.level == level && it.zoneType == type }
    }

}

