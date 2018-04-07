package kotcity.ui.layers

import javafx.scene.paint.Color
import kotcity.data.BlockCoordinate
import kotcity.data.CityMap
import kotcity.data.Tunable
import kotcity.ui.Algorithms
import kotcity.ui.map.CityRenderer


class LandValueRenderer(private val cityRenderer: CityRenderer, private val cityMap: CityMap) {

    companion object {
        val NEGATIVE_COLOR: Color = Color.RED
        val POSITIVE_COLOR: Color = Color.GREEN
    }

    fun render() {
        val (startBlock, endBlock) = cityRenderer.visibleBlockRange()

        BlockCoordinate.iterateAll(startBlock, endBlock) { coordinate ->
            val landValue = cityMap.landValueLayer[coordinate] ?: 0.0
            cityRenderer.apply {
                val dX = (coordinate.x - blockOffsetX) * blockSize
                val dY = (coordinate.y - blockOffsetY) * blockSize
                canvas.graphicsContext2D.apply {

                    // OK we gotta scale between min and max...
                    val scaledLandValue = Algorithms.scale(landValue, 0.0, Tunable.MAX_LAND_VALUE, 0.0, 1.0)

                    val landValueColor = NEGATIVE_COLOR.interpolate(POSITIVE_COLOR, scaledLandValue)
                    fill = Color(landValueColor.red, landValueColor.green, landValueColor.blue, 0.8)
                    fillRect(dX, dY, blockSize, blockSize)
                }
            }
        }
    }
}
