package kotcity.data

open class QuantizedMap<T>(private val quantize: Int = 4) {
    protected var map: MutableMap<BlockCoordinate, T> = mutableMapOf()

    operator fun set(blockCoordinate: BlockCoordinate, value: T) {
        map[quantizeBlockCoordinate(blockCoordinate)] = value
    }

    operator fun get(blockCoordinate: BlockCoordinate): T? {
        return map[quantizeBlockCoordinate(blockCoordinate)]
    }

    fun clear() = map.clear()

    fun count() = map.count()

    fun keys() = map.keys

    fun entries() = map.entries

    fun remove(blockCoordinate: BlockCoordinate) = map.remove(blockCoordinate)

    private fun quantizeBlockCoordinate(blockCoordinate: BlockCoordinate) =
        BlockCoordinate(blockCoordinate.x / this.quantize, blockCoordinate.y / this.quantize)

    fun unquantized(coordinate: BlockCoordinate): List<BlockCoordinate> {
        val xRange = (coordinate.x..coordinate.x + quantize)
        val yRange = (coordinate.y..coordinate.y + quantize)

        val blocks = mutableListOf<BlockCoordinate>()
        xRange.forEach { x -> yRange.mapTo(blocks) { BlockCoordinate(x, it) } }
        return blocks.toList()
    }

    fun map(function: (Map.Entry<BlockCoordinate, T>) -> Any): List<Any> = map.map(function)

    fun forEach(lambda: (t: BlockCoordinate, T) -> Unit) {
        map.forEach { t, u ->
            lambda(t, u)
        }
    }

    fun clone(): QuantizedMap<T> {
        val newMap = QuantizedMap<T>(quantize = this.quantize)
        map.forEach { key, value ->
            newMap[key] = value
        }
        return newMap
    }
}
