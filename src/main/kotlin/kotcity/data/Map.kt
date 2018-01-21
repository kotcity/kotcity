package kotcity.data

data class BlockCoordinate(val x: Int, val y: Int) {
    companion object {
        fun iterate(from: BlockCoordinate, to: BlockCoordinate, callback: (BlockCoordinate) -> Unit) {
            for (x in (from.x .. to.x).sorted()) {
                for (y in (from.y .. to.y).sorted()) {
                    callback(BlockCoordinate(x, y))
                }
            }
        }
    }
}

fun IntRange.reorder(): IntRange {
    if (first < last) {
        return this
    } else {
        return last..first
    }
}

enum class TileType { GROUND, WATER}
data class MapTile(val type: TileType, val elevation: Double)

class CityMap(val width: Int = 512, val height: Int = 512) {
    val groundLayer = mutableMapOf<BlockCoordinate, MapTile>()
    val buildingLayer = mutableMapOf<BlockCoordinate, Building>()

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

    fun buildRoad(from: BlockCoordinate, to: BlockCoordinate) {
        roadBlocks(from, to).forEach { block ->
            println("Droppin a road at: $block")
            buildingLayer[block] = Road()
        }
    }

    private fun blockRange(from: BlockCoordinate, to: BlockCoordinate): List<BlockCoordinate> {
        val blocks = mutableListOf<BlockCoordinate>()
        for (x in (from.x .. to.x).reorder()) {
            for (y in (from.y .. to.y).reorder()) {
                blocks.add(BlockCoordinate(x, y))
            }
        }
        return blocks.toList()
    }

    fun bulldoze(from: BlockCoordinate, to: BlockCoordinate) {
        blockRange(from, to).forEach { buildingLayer.remove(it) }
    }

}

interface Building {
    val height: Int
    val width: Int
}

class Road : Building {
    override val height = 1
    override val width = 1
}