package kotcity.ui.map

import javafx.scene.paint.Color
import kotcity.data.BlockCoordinate
import kotcity.data.CityMap

class FireCoverageRenderer(private val cityRenderer: CityRenderer, private val cityMap: CityMap) {

    companion object {
        val NEGATIVE_COLOR: java.awt.Color = java.awt.Color.RED
        val POSITIVE_COLOR: java.awt.Color = java.awt.Color.GREEN
    }

    fun render() {
        val (startBlock, endBlock) = cityRenderer.visibleBlockRange()

        BlockCoordinate.iterate(startBlock, endBlock) { coord ->
            val coverageScore = cityMap.fireCoverageLayer[coord] ?: 0.0
            cityRenderer.apply {
                val dX = (coord.x - cityRenderer.blockOffsetX) * blockSize()
                val dY = (coord.y - cityRenderer.blockOffsetY) * blockSize()
                canvas.apply {
                    graphicsContext2D.fill = determineColor(cityRenderer, coverageScore)
                    graphicsContext2D.fillRect(dX, dY, blockSize(), blockSize())
                }
            }
        }
    }

    private fun determineColor(renderer: CityRenderer, coverage: Double): Color {
        val newColor = renderer.interpolateColor(NEGATIVE_COLOR, POSITIVE_COLOR, coverage.toFloat())
        return Color(newColor.red, newColor.green, newColor.blue, 0.5)
    }
}