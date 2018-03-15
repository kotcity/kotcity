package kotcity.ui.map

import kotcity.data.BlockCoordinate
import kotcity.data.CityMap

class HappinessRenderer(private val cityRenderer: CityRenderer, val cityMap: CityMap) {
    fun render() {

        val maxHappiness = maxHappiness(cityMap)

        val (startBlock, endBlock) = cityRenderer.visibleBlockRange()
        BlockCoordinate.iterate(startBlock, endBlock) { coordinate ->
            // gotta get buildings at this coordinate...
            val locations = cityMap.locationsAt(coordinate)
            val happiness = locations.maxBy { it.building.happiness }?.building?.happiness ?: 0
            val tx = coordinate.x - cityRenderer.blockOffsetX
            val ty = coordinate.y - cityRenderer.blockOffsetY
            val blockSize = cityRenderer.blockSize()
            val canvas = cityRenderer.canvas
            canvas.graphicsContext2D.fill = cityRenderer.colorValue(happiness.toDouble(), maxHappiness.toDouble())
            canvas.graphicsContext2D.fillRect(tx * blockSize, ty * blockSize, blockSize, blockSize)
        }
    }

    private fun maxHappiness(cityMap: CityMap): Int {
        return cityMap.locations().maxBy { it.building.happiness }?.building?.happiness ?: 0
    }
}
