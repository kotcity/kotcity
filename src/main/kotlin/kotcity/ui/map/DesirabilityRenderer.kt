package kotcity.ui.map

import javafx.scene.paint.Color
import kotcity.data.BlockCoordinate
import kotcity.data.CityMap
import kotcity.ui.Algorithms
import kotcity.util.interpolate

class DesirabilityRenderer(private val cityRenderer: CityRenderer, private val cityMap: CityMap) {

    companion object {
        const val DESIRABILITY_CAP: Double = 10.0

        val NEGATIVE_COLOR: java.awt.Color = java.awt.Color.RED
        val POSITIVE_COLOR: java.awt.Color = java.awt.Color.GREEN
    }

    fun render() {
        val (startBlock, endBlock) = cityRenderer.visibleBlockRange()

        BlockCoordinate.iterate(startBlock, endBlock) { coord ->
            val desirabilityScores = cityMap.desirabilityLayers.map {
                it[coord]
            }

            val maxDesirability = desirabilityScores.filterNotNull().max() ?: 0.0

            cityRenderer.apply {
                val tx = coord.x - blockOffsetX
                val ty = coord.y - blockOffsetY
                val blockSize = blockSize()
                canvas.graphicsContext2D.apply {
                    fill = determineColor(maxDesirability)
                    fillRect(tx * blockSize, ty * blockSize, blockSize, blockSize)
                }
            }
        }
    }

    private fun determineColor(desirability: Double): Color {
        val fraction = Algorithms.scale(desirability.coerceAtMost(DESIRABILITY_CAP), 0.00, DESIRABILITY_CAP, 0.0, 1.0)
        val newColor = NEGATIVE_COLOR.interpolate(POSITIVE_COLOR, fraction.toFloat())
        return Color(newColor.red, newColor.green, newColor.blue, 0.5)
    }
}
