package kotcity.automata

import javafx.scene.paint.Color
import kotcity.data.BlockCoordinate
import kotcity.ui.map.CityRenderer
import kotlin.math.roundToInt

class FireCoverageLayer {

    companion object {
        val NEGATIVE_COLOR: java.awt.Color = java.awt.Color.RED
        val POSITIVE_COLOR: java.awt.Color = java.awt.Color.GREEN
    }

    val data = mutableMapOf<BlockCoordinate, Double>()

    fun readableValue(coordinate: BlockCoordinate) = "${(data[coordinate] ?: 0.0).roundToInt() * 100} %"

    fun draw(renderer: CityRenderer) {
        val (startBlock, endBlock) = renderer.visibleBlockRange()

        BlockCoordinate.iterate(startBlock, endBlock) { coord ->
            val coverageScore = data[coord] ?: 0.0
            val dX = coord.x - renderer.blockOffsetX
            val dY = coord.y - renderer.blockOffsetY
            val blockSize = renderer.blockSize()
            renderer.canvas.graphicsContext2D.fill = determineColor(renderer, coverageScore)
            renderer.canvas.graphicsContext2D.fillRect(dX * blockSize, dY * blockSize, blockSize, blockSize)
        }
    }

    private fun determineColor(renderer: CityRenderer, coverage: Double): Color {
        val newColor = renderer.interpolateColor(NEGATIVE_COLOR, POSITIVE_COLOR, coverage.toFloat())
        return Color(newColor.red, newColor.green, newColor.blue, 0.5)
    }
}