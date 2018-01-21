package kotcity.data

import com.github.davidmoten.rtree.RTree
import com.github.davidmoten.rtree.geometry.Geometries
import com.github.davidmoten.rtree.geometry.Rectangle
import tornadofx.c

data class BlockCoordinate(val x: Int, val y: Int) {
    companion object {
        fun iterate(from: BlockCoordinate, to: BlockCoordinate, callback: (BlockCoordinate) -> Unit) {
            val xRange = (from.x .. to.x).reorder()
            val yRange = (from.y ..to.y).reorder()
            println("Wanting to iterate: $xRange to $yRange")
            for (x in xRange) {
                for (y in yRange) {
                    callback(BlockCoordinate(x, y))
                }
            }
        }
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

data class CityMap(var width: Int = 512, var height: Int = 512) {
    val groundLayer = mutableMapOf<BlockCoordinate, MapTile>()
    val buildingLayer = mutableMapOf<BlockCoordinate, Building>()
    // where we loaded OR saved this city to...
    // used to determine save vs. save as...
    var fileName: String? = null
    var cityName: String? = null
    private var buildingIndex = RTree.create<Building, Rectangle>()!!

    private fun roadBlocks(startBlock: BlockCoordinate, endBlock: BlockCoordinate): MutableList<BlockCoordinate> {
        println("Getting roadblocks for $startBlock to $endBlock")
        val blockList = mutableListOf<BlockCoordinate>()
        if (Math.abs(startBlock.x - endBlock.x) > Math.abs(startBlock.y - endBlock.y)) {
            // going horizontally...
            (startBlock.x .. endBlock.x).reorder().forEach { x ->
                println("adding block for $x, ${startBlock.y}")
                blockList.add(BlockCoordinate(x, startBlock.y))
            }
        } else {
            // going vertically...
            (startBlock.y .. endBlock.y).reorder().forEach { y ->
                println("adding block for ${startBlock.x},$y")
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

    fun updateBuildingIndex() {
        var newIndex = RTree.create<Building, Rectangle>()
        buildingLayer.forEach { coordinate, building ->
            newIndex = newIndex.add(building, Geometries.rectangle(
                    coordinate.x.toFloat(),
                    coordinate.y.toFloat(),
                    coordinate.x.toFloat() + building.width.toFloat(),
                    coordinate.y.toFloat() + building.height.toFloat())
            )
            println("Size of index is now: ${newIndex.entries().count().toBlocking().first()}")
        }
        buildingIndex = newIndex
    }

    fun suggestedFilename(): String {
        return Slug.makeSlug(cityName) + ".kcity"
    }

    fun canBuildBuildingAt(newBuilding: Building, coordinate: BlockCoordinate): Boolean {
        // OK... let's get nearby buildings to really cut this down...
        val newBuildingEnd = BlockCoordinate(coordinate.x + newBuilding.width - 1, coordinate.y + newBuilding.height - 1)

        val newBuildingTopRight = BlockCoordinate(coordinate.x + newBuilding.width - 1, coordinate.y)
        val newBuildingBottomLeft = BlockCoordinate(coordinate.x, coordinate.y + newBuilding.height - 1)

        val nearby = nearestBuildings(coordinate)
        nearby.forEach { pair: Pair<BlockCoordinate, Building> ->
            val building = pair.second
            val otherBuildingStart = pair.first
            val otherBuildingEnd = BlockCoordinate(otherBuildingStart.x + building.width - 1, otherBuildingStart.y + building.height - 1)
            // now let's test...
            if (coordinate.x <= otherBuildingEnd.x && coordinate.x >= otherBuildingStart.x && coordinate.y <= otherBuildingEnd.y && coordinate.y >= otherBuildingStart.y) {
                println("Collision with $building!")
                return false
            }

            if (newBuildingEnd.x <= otherBuildingEnd.x && newBuildingEnd.x >= otherBuildingStart.x && newBuildingEnd.y <= otherBuildingEnd.y && newBuildingEnd.y >= otherBuildingStart.y) {
                println("Collision with $building!")
                return false
            }

            // TODO: we need to get the TOP RIGHT and BOTTOM LEFT of building to check as well...
            if (newBuildingTopRight.x <= otherBuildingEnd.x && newBuildingTopRight.x >= otherBuildingStart.x && newBuildingTopRight.y <= otherBuildingEnd.y && newBuildingTopRight.y >= otherBuildingStart.y) {
                println("Collision with $building!")
                return false
            }

            if (newBuildingBottomLeft.x <= otherBuildingEnd.x && newBuildingBottomLeft.x >= otherBuildingStart.x && newBuildingBottomLeft.y <= otherBuildingEnd.y && newBuildingBottomLeft.y >= otherBuildingStart.y) {
                println("Collision with $building!")
                return false
            }
        }
        return true
    }

    fun buildRoad(from: BlockCoordinate, to: BlockCoordinate) {
        roadBlocks(from, to).forEach { block ->
            // println("Dropping a road at: $block")
            val newRoad = Road()
            if (canBuildBuildingAt(newRoad, block)) {
                buildingLayer[block] = newRoad
            } else {
                println("We have an overlap... not building!")
            }
        }
        updateBuildingIndex()
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
        }
        updateBuildingIndex()
    }

}

enum class BuildingType {
    ROAD, COAL_POWER_PLANT
}

abstract class Building {
    open var width = 1
    open var height = 1
    abstract var type: BuildingType
}

class Road : Building() {
    override var type = BuildingType.ROAD
}

class CoalPowerPlant : Building() {
    override var type: BuildingType = BuildingType.COAL_POWER_PLANT
    override var width = 4
    override var height = 4
}