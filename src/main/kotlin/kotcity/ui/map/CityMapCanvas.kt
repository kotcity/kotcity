package kotcity.ui.map

import com.github.benmanes.caffeine.cache.Caffeine
import javafx.scene.paint.Color
import kotcity.data.*
import kotcity.ui.Algorithms
import kotcity.ui.ColorAdjuster
import kotcity.ui.ResizableCanvas
import java.util.concurrent.TimeUnit

class CityMapCanvas : ResizableCanvas() {
    var map: CityMap? = null
        set(value) {
            field = value
            value?.elevations()?.let {
                colorAdjuster = ColorAdjuster(it.first, it.second)
            }
        }

    var mode: MapMode = MapMode.NORMAL
    private var colorAdjuster: ColorAdjuster? = null
    var visibleBlockRange: Pair<BlockCoordinate, BlockCoordinate>? = null

    private val visibleAreaHighlight = Color(Color.HOTPINK.red, Color.HOTPINK.green, Color.HOTPINK.blue, 0.8)

    private val buildingCache = Caffeine.newBuilder()
        .expireAfterWrite(3, TimeUnit.MINUTES)
        .refreshAfterWrite(2, TimeUnit.MINUTES)
        .softValues().build({ key: BlockCoordinate -> map?.cachedLocationsIn?.let { it(key) } })

    fun render() {

        map?.let { map ->
            // get the graphics context...
            val gc = this.graphicsContext2D

            gc.clearRect(0.0, 0.0, this.width, this.height)

            val smallerDimension = Math.min(this.width, this.height)

            for (canvasX in 0..smallerDimension.toInt()) {
                for (canvasY in 0..smallerDimension.toInt()) {

                    // OK we gotta scale the cityMap coordinates to this crap...
                    val nx = Algorithms.scale(canvasX.toDouble(), 0.0, smallerDimension, 0.0, map.width.toDouble())
                    val ny = Algorithms.scale(canvasY.toDouble(), 0.0, smallerDimension, 0.0, map.height.toDouble())

                    val translatedBlock = BlockCoordinate(nx.toInt(), ny.toInt())

                    // FIXME: this is a BUG!!!! we are getting the canvas XY coordinates but we care about the building ones...
                    val buildings = buildingCache.get(translatedBlock, { buildingBlock ->
                        map.cachedLocationsIn(buildingBlock)
                    })
                    if (buildings?.count() ?: 0 > 0) {
                        gc.fill = Color.BLACK
                        gc.fillRect(canvasX.toDouble(), canvasY.toDouble(), 1.0, 1.0)
                    } else {
                        val tile = map.groundLayer[translatedBlock]
                        tile?.let {
                            val tileColor = colorAdjuster?.colorForTile(tile)
                            if (it.type == TileType.GROUND) {
                                gc.fill = tileColor
                                gc.fillRect(canvasX.toDouble(), canvasY.toDouble(), 1.0, 1.0)
                            } else {
                                gc.fill = Color.DARKBLUE
                                gc.fillRect(canvasX.toDouble(), canvasY.toDouble(), 1.0, 1.0)
                            }
                        }
                    }
                }
            }

            if (this.mode != MapMode.NORMAL) {
                renderResources()
            }

            // now let's highlight the area of the cityMap we can see...
            visibleBlockRange?.let { visibleBlockRange ->
                val sx = Algorithms.scale(
                    visibleBlockRange.first.x.toDouble(),
                    0.0,
                    map.width.toDouble(),
                    0.0,
                    smallerDimension
                )
                val sy = Algorithms.scale(
                    visibleBlockRange.first.y.toDouble(),
                    0.0,
                    map.height.toDouble(),
                    0.0,
                    smallerDimension
                )
                val ex = Algorithms.scale(
                    visibleBlockRange.second.x.toDouble(),
                    0.0,
                    map.width.toDouble(),
                    0.0,
                    smallerDimension
                )
                val ey = Algorithms.scale(
                    visibleBlockRange.second.y.toDouble(),
                    0.0,
                    map.height.toDouble(),
                    0.0,
                    smallerDimension
                )
                val width = ex - sx
                val height = ey - sy

                gc.fill = visibleAreaHighlight
                // println("Minimap rendering starting at $sx, $sy ending at $ex, $ey")
                gc.fillRect(sx, sy, width, height)
            }
        }
    }

    private fun renderResources() {
        this.map?.let { map ->
            this.mode.let { mode ->
                val layer = when (mode) {
                    MapMode.COAL -> map.resourceLayers["coal"]
                    MapMode.OIL -> map.resourceLayers["oil"]
                    MapMode.GOLD -> map.resourceLayers["gold"]
                    MapMode.SOIL -> map.resourceLayers["soil"]
                    else -> null
                }
                layer?.let {
                    drawResourceLayer(it)
                }
            }
        }
    }

    private fun drawResourceLayer(layer: QuantizedMap<Double>) {
        val smallerDimension = Math.min(this.width, this.height)

        // println("Map is rendering from 0 to $yRange")

        for (x in 0..smallerDimension.toInt()) {
            for (y in 0..smallerDimension.toInt()) {
                // OK we gotta scale the cityMap coordinates to this crap...
                val nx = Algorithms.scale(x.toDouble(), 0.0, smallerDimension, 0.0, this.map?.width?.toDouble() ?: 0.0)
                val ny = Algorithms.scale(y.toDouble(), 0.0, smallerDimension, 0.0, this.map?.height?.toDouble() ?: 0.0)

                val tile = layer[BlockCoordinate(nx.toInt(), ny.toInt())]
                if (tile ?: 0.0 > 0.5) {
                    this.graphicsContext2D.fill = Color.YELLOW
                    this.graphicsContext2D.fillRect(x.toDouble(), y.toDouble(), 1.0, 1.0)
                }
            }
        }
    }
}
