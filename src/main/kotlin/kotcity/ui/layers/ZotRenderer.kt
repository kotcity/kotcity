package kotcity.ui.layers

import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotcity.data.BlockCoordinate
import kotcity.data.CityMap
import kotcity.ui.ResizableCanvas
import kotcity.ui.map.CityRenderer
import kotcity.util.Debuggable

class ZotRenderer(private val cityMap: CityMap, val zotCanvas: ResizableCanvas): Debuggable {

    fun render() {
        val gc = zotCanvas.graphicsContext2D
        gc.clearRect(0.0, 0.0, zotCanvas.width, zotCanvas.height)
        gc.fill = Color.AQUAMARINE
        gc.fillText("ZOTS ARE BACK IN TOWN", 20.0, 20.0)
    }

    override var debug: Boolean = true
    var visibleBlockRange: Pair<BlockCoordinate, BlockCoordinate>? = null
}