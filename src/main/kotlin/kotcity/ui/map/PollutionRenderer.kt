package kotcity.ui.map

import javafx.scene.paint.Color
import kotcity.data.BlockCoordinate
import kotcity.data.CityMap
import kotcity.ui.Algorithms
import kotcity.util.interpolate

class PollutionRenderer(private val cityRenderer: CityRenderer, private val cityMap: CityMap) {

    private var lastMaxCalculation = System.currentTimeMillis()
    private var cachedMaxPollution = 0.0

    companion object {
        val NEGATIVE_COLOR: java.awt.Color = java.awt.Color.GREEN
        val POSITIVE_COLOR: java.awt.Color = java.awt.Color.RED
    }

    fun render() {
        val (startBlock, endBlock) = cityRenderer.visibleBlockRange()

        val maxPollution: Double = cachedMaxPollution()

        BlockCoordinate.iterate(startBlock, endBlock) { coord ->
            val pollutionValue = cityMap.pollutionLayer[coord] ?: 0.0
            cityRenderer.apply {
                val dX = (coord.x - blockOffsetX) * blockSize()
                val dY = (coord.y - blockOffsetY) * blockSize()
                canvas.graphicsContext2D.apply {
                    fill = determineColor(pollutionValue, maxPollution)
                    fillRect(dX, dY, blockSize(), blockSize())
                }
            }
        }
    }

    private fun cachedMaxPollution(): Double {
        if (System.currentTimeMillis() - lastMaxCalculation > 10000) {
            // we must recalculate...
            cachedMaxPollution = calculateMaxPollution()
            lastMaxCalculation = System.currentTimeMillis()
            println("Recalculating max pollution...")
        }
        return cachedMaxPollution
    }

    private fun determineColor(pollution: Double, maxPollution: Double): Color {
        val maxPollution = 1000.0
        val fraction = Algorithms.scale(pollution.coerceAtMost(maxPollution), 0.00, maxPollution, 0.0, 1.0)
        val newColor = NEGATIVE_COLOR.interpolate(POSITIVE_COLOR, fraction.toFloat())
        return Color(newColor.red, newColor.green, newColor.blue, 0.5)
    }

    private fun calculateMaxPollution(): Double {
        synchronized(cityMap.pollutionLayer) {
            return cityMap.pollutionLayer.values.max() ?: 0.0
        }
    }

}
