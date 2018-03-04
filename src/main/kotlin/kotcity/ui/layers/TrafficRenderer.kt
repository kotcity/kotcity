package kotcity.ui.layers

import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import kotcity.data.BlockCoordinate
import kotcity.data.CityMap
import kotcity.data.Road
import kotcity.ui.ResizableCanvas
import kotcity.ui.map.CityRenderer
import kotcity.util.Debuggable

class TrafficRenderer(private val cityMap: CityMap, private val cityRenderer: CityRenderer, private val trafficCanvas: ResizableCanvas):  Debuggable {
    override var debug: Boolean = true

    private fun highlightBlock(g2d: GraphicsContext, x: Int, y: Int) {
        g2d.fill = Color(Color.MAGENTA.red, Color.MAGENTA.green, Color.MAGENTA.blue, 0.50)
        // gotta translate here...
        val tx = x - cityRenderer.blockOffsetX
        val ty = y - cityRenderer.blockOffsetY
        val blockSize = cityRenderer.blockSize()
        g2d.fillRect(tx * blockSize, ty * blockSize, blockSize, blockSize)
    }

    fun render() {

        trafficCanvas.graphicsContext2D.clearRect(0.0, 0.0, trafficCanvas.width, trafficCanvas.height)

        trafficCanvas.graphicsContext2D.fill = Color.HOTPINK
        trafficCanvas.graphicsContext2D.fillText("Traffic!", 10.0, 10.0)

        visibleBlockRange?.let {visibleBlockRange ->
            BlockCoordinate.iterate(visibleBlockRange.first, visibleBlockRange.second) {
                // see if we got a road under here... AND we got some traffic...
                if (cityMap.cachedBuildingsIn(it).any{it.building is Road} && cityMap.trafficLayer[it] ?: 0.0 > 0.0) {
                    highlightBlock(trafficCanvas.graphicsContext2D, it.x, it.y)
                }
            }
        }
    }

    var visibleBlockRange: Pair<BlockCoordinate, BlockCoordinate>? = null

}