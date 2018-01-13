package kotcity.ui

import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Accordion
import javafx.scene.control.ScrollPane
import javafx.scene.control.TitledPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Stage
import kotcity.data.CityMap
import kotcity.data.GroundTile
import kotcity.data.MapCoordinate
import kotcity.data.MapGenerator
import tornadofx.App
import tornadofx.View
import tornadofx.find


class GameFrame : View(), CanvasFitter {
    override val root: VBox by fxml("/GameFrame.fxml")
    private val canvas = ResizableCanvas()
    private val canvasPane: ScrollPane by fxid("canvasPane")
    private val accordion: Accordion by fxid()
    private val basicPane: TitledPane by fxid()

    var ticks = 0
    var zoom = 1.0

    lateinit var _map: CityMap

    fun setMap(map: CityMap) {
        this._map = map
        // gotta resize the component now...
        resizeCanvas()
        println("Map has been set to: $_map. Size is ${canvas.width}x${canvas.height}")
    }

    fun resizeCanvas() {
        // BUG: we cannot have a large canvas!!!
        canvas.width = _map.width.toDouble() * zoom
        canvas.height = _map.height.toDouble() * zoom
    }

    fun getMap(): CityMap {
        return this._map
    }

    fun drawMap(gc: GraphicsContext) {
        // we got that map...
        val (xRange, yRange) = canvasPane.visibleArea()

        val zoomedX = IntRange(
                (xRange.first.toDouble() / zoom).toInt(),
                (xRange.last.toDouble() / zoom).toInt()
        )

        val zoomedY = IntRange(
                (yRange.first.toDouble() / zoom).toInt(),
                (yRange.last.toDouble() / zoom).toInt()
        )

        for (x in zoomedX) {
            for (y in zoomedY) {
                val tile = getMap().groundLayer[MapCoordinate(x, y)]
                if (tile == GroundTile.GROUND) {
                    gc.fill = Color.LIGHTGOLDENRODYELLOW
                } else {
                    gc.fill = Color.LIGHTBLUE
                }
                gc.fillRect(x.toDouble() * zoom, y.toDouble() * zoom, zoom, zoom)
            }
        }
    }

    fun zoomOut() {
        if (zoom > 1) {
            zoom -= 1
            resizeCanvas()
        }
    }

    fun zoomIn() {
        zoom += 1
        resizeCanvas()
    }

    init {

        title = "Kotcity 0.1"

        // fitCanvasToPane(canvas, canvasPane)
        canvasPane.content = canvas

        with(canvas) {
            this.setOnScroll {  scrollEvent ->
                // println("We are scrolling: $scrollEvent")
                if (scrollEvent.deltaY < 0) {
                    println("Zoom out!")
                    zoomOut()
                } else if (scrollEvent.deltaY > 0) {
                    println("Zoom in!")
                    zoomIn()
                }
            }
        }

        accordion.expandedPane = basicPane

        val timer = object : AnimationTimer() {
            override fun handle(now: Long) {
                if (ticks == 20) {
                    drawMap(canvas.graphicsContext2D)
                    ticks = 0
                }
                ticks++
            }
        }
        timer.start()
    }

}

class GameFrameApp : App(GameFrame::class, KotcityStyles::class) {
    override fun start(stage: Stage) {
        stage.isResizable = true
        val gameFrame = find(GameFrame::class)
        val mapGenerator = MapGenerator()
        val map = mapGenerator.generateMap(512, 512)
        gameFrame.setMap(map)
        super.start(stage)
    }
}

fun main(args: Array<String>) {
    Application.launch(GameFrameApp::class.java, *args)
}