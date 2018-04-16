package kotcity.ui.layers

import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.event.EventHandler
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.util.Duration
import kotcity.data.BlockCoordinate
import kotcity.data.CityMap
import kotcity.data.buildings.Road
import kotcity.memoization.CacheOptions
import kotcity.memoization.cache
import kotcity.ui.ResizableCanvas
import kotcity.ui.map.CityRenderer
import kotcity.util.Debuggable

class TrafficAnimationRenderer(
    private val cityMap: CityMap,
    private val cityRenderer: CityRenderer,
    private val trafficCanvas: ResizableCanvas
) : Debuggable {
    override var debug: Boolean = true

    private var currentImage = 0
    private var timelineX: Timeline

    // don't want to use weak cache here so we can hang on to the images...
    private var scaledTrafficCachePair = ::scaledImages.cache(CacheOptions(false, false))
    private val cachedScaledTrafficImages = scaledTrafficCachePair.second

    var visibleBlockRange: Pair<BlockCoordinate, BlockCoordinate>? = null

    private val horizontalFilenames = listOf(
        "file:./assets/animations/light_traffic_1.png",
        "file:./assets/animations/light_traffic_2.png",
        "file:./assets/animations/light_traffic_3.png",
        "file:./assets/animations/light_traffic_4.png",
        "file:./assets/animations/light_traffic_5.png",
        "file:./assets/animations/light_traffic_6.png",
        "file:./assets/animations/light_traffic_7.png",
        "file:./assets/animations/light_traffic_8.png"
    )

    private val verticalFilenames = listOf(
        "file:./assets/animations/light_traffic_1_vertical.png",
        "file:./assets/animations/light_traffic_2_vertical.png",
        "file:./assets/animations/light_traffic_3_vertical.png",
        "file:./assets/animations/light_traffic_4_vertical.png",
        "file:./assets/animations/light_traffic_5_vertical.png",
        "file:./assets/animations/light_traffic_6_vertical.png",
        "file:./assets/animations/light_traffic_7_vertical.png",
        "file:./assets/animations/light_traffic_8_vertical.png"
    )

    init {
        timelineX = Timeline(KeyFrame(Duration.millis(250.0), EventHandler {
            currentImage += 1
            if (currentImage >= horizontalFilenames.count()) {
                currentImage = 0
            }
        }))
        timelineX.cycleCount = Timeline.INDEFINITE
        timelineX.play()
    }

    private fun scaledImages(filenames: List<String>, blockSize: Double): List<Image> {
        return filenames.map {
            Image(it, blockSize, blockSize, true, true)
        }
    }

    fun render() {
        trafficCanvas.graphicsContext2D.clearRect(0.0, 0.0, trafficCanvas.width, trafficCanvas.height)

        visibleBlockRange?.let { visibleBlockRange ->
            cityMap.locationsInRectangle(visibleBlockRange.first, visibleBlockRange.second)
                .filter { it.building is Road && cityMap.trafficLayer[it.coordinate] ?: 0.0 > 30 }
                .forEach { drawTrafficImage(trafficCanvas.graphicsContext2D, it.coordinate) }
        }
    }

    fun stop() = timelineX.stop()

    private fun drawTrafficImage(g2d: GraphicsContext, coordinate: BlockCoordinate) {
        // g2d.fill = Color(Color.MAGENTA.red, Color.MAGENTA.green, Color.MAGENTA.blue, 0.50)
        // gotta translate here...
        val horizontalImages = cachedScaledTrafficImages(horizontalFilenames, cityRenderer.blockSize)
        val verticalImages = cachedScaledTrafficImages(verticalFilenames, cityRenderer.blockSize)

        if (horizontalRoad(coordinate)) {
            drawHorizontal(horizontalImages[currentImage], g2d, coordinate)
        }
        if (verticalRoad(coordinate)) {
            drawVertical(verticalImages[currentImage], g2d, coordinate)
        }
    }

    private fun verticalRoad(coordinate: BlockCoordinate) = hasRoad(coordinate.top()) || hasRoad(coordinate.bottom())

    private fun horizontalRoad(coordinate: BlockCoordinate) = hasRoad(coordinate.left()) || hasRoad(coordinate.right())

    private fun hasRoad(coordinate: BlockCoordinate) = cityMap.cachedLocationsIn(coordinate).any { it.building is Road }

    private fun drawVertical(image: Image, g2d: GraphicsContext, coordinate: BlockCoordinate) {
        val tx = coordinate.x - cityRenderer.blockOffsetX
        val ty = coordinate.y - cityRenderer.blockOffsetY
        val blockSize = cityRenderer.blockSize
        g2d.drawImage(image, tx * blockSize, ty * blockSize)
    }

    private fun drawHorizontal(image: Image, g2d: GraphicsContext, coordinate: BlockCoordinate) {
        // handle horizontal road...
        val tx = coordinate.x - cityRenderer.blockOffsetX
        val ty = coordinate.y - cityRenderer.blockOffsetY
        val blockSize = cityRenderer.blockSize
        g2d.drawImage(image, tx * blockSize, ty * blockSize)
    }
}
