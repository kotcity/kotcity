package kotcity.ui.layers

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.event.EventHandler
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.util.Duration
import kotcity.data.*
import kotcity.ui.ResizableCanvas
import kotcity.ui.map.CityRenderer
import kotcity.ui.sprites.ZotSpriteLoader
import kotcity.util.Debuggable
import kotcity.util.getRandomElement
import java.util.concurrent.TimeUnit

class ZotRenderer(private val cityMap: CityMap, private val cityRenderer: CityRenderer, private val zotCanvas: ResizableCanvas): Debuggable {

    private var offsetTimeline: Timeline
    private var degree = 0.0

    init {

        this.offsetTimeline = Timeline(KeyFrame(Duration.millis(50.0), EventHandler {
            degree += 5
            if (degree >= 360) {
                degree = 0.0
            }
        }))

        offsetTimeline.cycleCount = Timeline.INDEFINITE
        offsetTimeline.play()
    }

    fun stop() {
        // stop animation thread here...
        offsetTimeline.stop()
    }

    private fun drawZot(image: Image, g2d: GraphicsContext, coordinate: BlockCoordinate) {
        val tx = coordinate.x - cityRenderer.blockOffsetX
        val ty = coordinate.y - cityRenderer.blockOffsetY
        val blockSize = cityRenderer.blockSize()
        // gotta fill that background too...

        val y = (ty - 1) * blockSize + (Math.sin(Math.toRadians(degree)) * 10)
        g2d.fill = Color.WHITE
        g2d.fillOval(tx * blockSize, y, blockSize, blockSize)
        g2d.stroke = Color.RED
        g2d.strokeOval(tx * blockSize, y, blockSize, blockSize)
        g2d.drawImage(image, tx * blockSize, y)
    }

    fun render() {
        val gc = zotCanvas.graphicsContext2D
        gc.clearRect(0.0, 0.0, zotCanvas.width, zotCanvas.height)
        gc.fill = Color.AQUAMARINE

        // ok let's get all buildings with zots now...
        visibleBlockRange?.let { visibleBlockRange ->
            val locationsWithZots = cityMap.locationsIn(visibleBlockRange.first, visibleBlockRange.second).filter { it.building.zots.isNotEmpty() }
            locationsWithZots.forEach { location ->
                // TODO: we gotta get different zots every once in a while...
                val randomZot: Zot? = randomZot(location)
                if (randomZot != null) {
                    val image = ZotSpriteLoader.spriteForZot(randomZot, cityRenderer.blockSize(), cityRenderer.blockSize())
                    if (image != null) {
                        drawZot(image, gc, location.coordinate)
                    }
                }
            }
        }

    }

    var zotForBuildingCache: LoadingCache<Location, Zot?> =  Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build<Location, Zot?> { key -> key.building.zots.getRandomElement() }

    private fun randomZot(location: Location): Zot? {
        return zotForBuildingCache[location]
    }

    override var debug: Boolean = true
    var visibleBlockRange: Pair<BlockCoordinate, BlockCoordinate>? = null
}