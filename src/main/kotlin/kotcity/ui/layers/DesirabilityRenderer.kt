package kotcity.ui.layers

import javafx.scene.paint.Color
import kotcity.data.BlockCoordinate
import kotcity.data.CityMap
import kotcity.ui.Algorithms
import kotcity.ui.map.CityRenderer
import kotcity.util.interpolate

class DesirabilityRenderer(private val cityRenderer: CityRenderer, private val cityMap: CityMap) {

    companion object {
        val NEGATIVE_COLOR: java.awt.Color = java.awt.Color.RED
        val POSITIVE_COLOR: java.awt.Color = java.awt.Color.GREEN
    }

    fun render() {
        val (startBlock, endBlock) = cityRenderer.visibleBlockRange()

        BlockCoordinate.iterateAll(startBlock, endBlock) { coord ->
            val desirabilityScores = cityMap.desirabilityLayers.mapNotNull {
                it[coord]
            }

            val maxDesirability = 200.0
            val minDesirability = -200.0

            cityRenderer.apply {
                val tx = coord.x - blockOffsetX
                val ty = coord.y - blockOffsetY
                val blockSize = blockSize
                canvas.graphicsContext2D.apply {
                    val desirability = desirabilityScores.max()
                    if (desirability != null) {
                        fill = determineColor(desirability, minDesirability, maxDesirability)
                        fillRect(tx * blockSize, ty * blockSize, blockSize, blockSize)
                    }
                }
            }

        }
    }

    private fun determineColor(desirability: Double, minDesirability: Double, maxDesirability: Double): Color {
        val fraction = Algorithms.scale(desirability, minDesirability, maxDesirability, 0.0, 1.0)
        val newColor = NEGATIVE_COLOR.interpolate(POSITIVE_COLOR, fraction.toFloat())
        return Color(newColor.red, newColor.green, newColor.blue, 0.8)
    }
}
