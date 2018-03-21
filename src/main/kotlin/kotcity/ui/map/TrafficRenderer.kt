package kotcity.ui.map

import javafx.scene.paint.Color
import kotcity.data.BlockCoordinate
import kotcity.data.CityMap

class TrafficRenderer(private val cityRenderer: CityRenderer, private val cityMap: CityMap) {

    companion object {
        val NEGATIVE_COLOR: java.awt.Color = java.awt.Color.RED
        val POSITIVE_COLOR: java.awt.Color = java.awt.Color.GREEN
    }

    fun render() {
        val (startBlock, endBlock) = cityRenderer.visibleBlockRange()

        BlockCoordinate.iterate(startBlock, endBlock) { coord ->
            val traffic = cityMap.trafficLayer[coord] ?: 0.0

            cityRenderer.apply {
                val tx = coord.x - blockOffsetX
                val ty = coord.y - blockOffsetY
                val blockSize = blockSize()
                canvas.graphicsContext2D.apply {
                    fill = determineColor(cityRenderer, traffic)
                    fillRect(tx * blockSize, ty * blockSize, blockSize, blockSize)
                }
            }
        }
    }

    private fun determineColor(renderer: CityRenderer, traffic: Double): Color {
        val newColor = renderer.interpolateColor(NEGATIVE_COLOR, POSITIVE_COLOR, traffic.toFloat())
        return Color(newColor.red, newColor.green, newColor.blue, 0.5)
    }
}
