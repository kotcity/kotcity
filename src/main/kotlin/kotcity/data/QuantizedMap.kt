package kotcity.data

class QuantizedMap<T>(private val quantize: Int = 4) {
    val map: MutableMap<BlockCoordinate, T> = mutableMapOf()

    fun put(blockCoordinate: BlockCoordinate, value: T) {
        map[quantizeBlockCoordinate(blockCoordinate)] = value
    }

    operator fun set(blockCoordinate: BlockCoordinate, value: T) {
        map[quantizeBlockCoordinate(blockCoordinate)] = value
    }

    fun count(): Int {
        return map.count()
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

}