package kotcity.ui.layers

import javafx.scene.paint.Color
import kotcity.data.BlockCoordinate
import kotcity.data.CityMap
import kotcity.ui.Algorithms
import kotcity.ui.map.CityRenderer
import kotcity.util.interpolate

class HappinessRenderer(private val cityRenderer: CityRenderer, private val cityMap: CityMap) {

    companion object {
        val NEGATIVE_COLOR: java.awt.Color = java.awt.Color.RED
        val POSITIVE_COLOR: java.awt.Color = java.awt.Color.GREEN
    }

    fun render() {
        val blockSize = cityRenderer.blockSize
        val (startBlock, endBlock) = cityRenderer.visibleBlockRange()

        BlockCoordinate.iterateAll(startBlock, endBlock) { coordinate ->
            // gotta get buildings at this coordinate...
            val locations = cityMap.locationsAt(coordinate)
            val happiness = locations.maxBy { it.building.happiness }?.building?.happiness ?: 0
            val tx = coordinate.x - cityRenderer.blockOffsetX
            val ty = coordinate.y - cityRenderer.blockOffsetY

            cityRenderer.canvas.graphicsContext2D.apply {
                fill = determineColor(happiness.toDouble())
                fillRect(tx * blockSize, ty * blockSize, blockSize, blockSize)
            }
        }
    }

    private fun maxHappiness(cityMap: CityMap): Int {
        return cityMap.locations().maxBy { it.building.happiness }?.building?.happiness ?: 0
    }

    private fun determineColor(happiness: Double): Color {
        val max = maxHappiness(cityMap).toDouble()
        val fraction = Algorithms.scale(happiness.coerceAtMost(max), 0.00, max, 0.0, 1.0)
        val newColor = NEGATIVE_COLOR.interpolate(POSITIVE_COLOR, fraction.toFloat())
        return Color(newColor.red, newColor.green, newColor.blue, 0.5)
    }
}
