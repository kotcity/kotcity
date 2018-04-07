package kotcity.ui.layers

import javafx.scene.paint.Color
import kotcity.data.BlockCoordinate
import kotcity.data.CityMap
import kotcity.ui.map.CityRenderer
import kotcity.util.interpolate

class FireCoverageRenderer(private val cityRenderer: CityRenderer, private val cityMap: CityMap) {

    companion object {
        val NEGATIVE_COLOR: java.awt.Color = java.awt.Color.RED
        val POSITIVE_COLOR: java.awt.Color = java.awt.Color.GREEN
    }

    fun render() {
        val (startBlock, endBlock) = cityRenderer.visibleBlockRange()

        BlockCoordinate.iterateAll(startBlock, endBlock) { coord ->
            val coverageScore = cityMap.fireCoverageLayer[coord] ?: 0.0
            cityRenderer.apply {
                val dX = (coord.x - blockOffsetX) * blockSize
                val dY = (coord.y - blockOffsetY) * blockSize
                canvas.graphicsContext2D.apply {
                    fill = determineColor(coverageScore)
                    fillRect(dX, dY, blockSize, blockSize)
                }
            }
        }
    }

    private fun determineColor(coverage: Double): Color {
        val newColor = NEGATIVE_COLOR.interpolate(POSITIVE_COLOR, coverage.toFloat())
        return Color(newColor.red, newColor.green, newColor.blue, 0.5)
    }
}
