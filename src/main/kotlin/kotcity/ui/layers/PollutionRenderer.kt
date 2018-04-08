package kotcity.ui.layers

import javafx.scene.paint.Color
import kotcity.data.BlockCoordinate
import kotcity.data.CityMap
import kotcity.ui.Algorithms
import kotcity.ui.map.CityRenderer
import kotcity.util.interpolate

class PollutionRenderer(private val cityRenderer: CityRenderer, private val cityMap: CityMap) {

    companion object {
        val NEGATIVE_COLOR: java.awt.Color = java.awt.Color.YELLOW
        val POSITIVE_COLOR: java.awt.Color = java.awt.Color.RED

        private const val maxPollution = 30.0

        fun scalePollution(pollution: Double): Double {
            return Algorithms.scale(pollution, 0.00, maxPollution, 0.0, 1.0)
        }
    }

    fun render() {
        val (startBlock, endBlock) = cityRenderer.visibleBlockRange()

        BlockCoordinate.iterateAll(startBlock, endBlock) { coord ->
            val pollutionValue = cityMap.pollutionLayer[coord] ?: 0.0
            if (pollutionValue > 0.0) {
                cityRenderer.apply {
                    val dX = (coord.x - blockOffsetX) * blockSize
                    val dY = (coord.y - blockOffsetY) * blockSize
                    canvas.graphicsContext2D.apply {
                        fill = determineColor(pollutionValue)
                        fillRect(dX, dY, blockSize, blockSize)
                    }
                }
            }

        }
    }

    private fun determineColor(pollution: Double): Color {
        val scaledPollution = scalePollution(pollution)
        val newColor = NEGATIVE_COLOR.interpolate(POSITIVE_COLOR, scaledPollution.toFloat())
        return Color(newColor.red, newColor.green, newColor.blue, 0.5)
    }

}
