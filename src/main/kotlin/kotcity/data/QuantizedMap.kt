package kotcity.data

open class QuantizedMap<T>(private val quantize: Int = 4) {
    protected var map: MutableMap<BlockCoordinate, T> = mutableMapOf()

    fun put(blockCoordinate: BlockCoordinate, value: T) {
        map[quantizeBlockCoordinate(blockCoordinate)] = value
    }

    operator fun set(blockCoordinate: BlockCoordinate, value: T) {
        map[quantizeBlockCoordinate(blockCoordinate)] = value
    }

    fun count(): Int {
        return map.count()
    }

    fun entries(): MutableSet<MutableMap.MutableEntry<BlockCoordinate, T>> {
        return map.entries
    }

    operator fun get(blockCoordinate: BlockCoordinate): T? {
        return map[quantizeBlockCoordinate(blockCoordinate)]
    }

    fun remove(blockCoordinate: BlockCoordinate) {
        map.remove(blockCoordinate)
    }

    private fun quantizeBlockCoordinate(blockCoordinate: BlockCoordinate) =
            BlockCoordinate(blockCoordinate.x / this.quantize, blockCoordinate.y / this.quantize)

    fun map(function: (Map.Entry<BlockCoordinate, T>) -> Any): List<Any> {
        return map.map(function)
    }

    fun clone(): QuantizedMap<T> {
        val newMap = QuantizedMap<T>(quantize = this.quantize)
        map.forEach { t, u ->
            newMap.put(t, u)
        }
        return newMap
    }

    fun unquantized(coordinate: BlockCoordinate): List<BlockCoordinate> {
        val xRange = (coordinate.x .. coordinate.x+quantize)
        val yRange = (coordinate.y .. coordinate.y+quantize)

        val blocks = mutableListOf<BlockCoordinate>()
        xRange.forEach { x -> yRange.mapTo(blocks) { BlockCoordinate(x, it) } }
        return blocks.toList()
    }

    fun keys(): MutableSet<BlockCoordinate> {
        return map.keys
    }

}