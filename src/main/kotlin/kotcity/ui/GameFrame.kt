package kotcity.ui

import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.ScrollPane
import javafx.scene.control.SplitPane
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
    override val root: SplitPane by fxml("/GameFrame.fxml")
    private val canvas = ResizableCanvas()
    private val canvasPane: ScrollPane by fxid("canvasPane")

    var ticks = 0

    lateinit var _map: CityMap

    fun setMap(map: CityMap) {
        this._map = map
        // gotta resize the component now...
        canvas.width = map.width.toDouble()
        canvas.height = map.height.toDouble()
        println("Map has been set to: $_map. Size is ${canvas.width}x${canvas.height}")
    }

    fun getMap(): CityMap {
        return this._map
    }

    fun drawMap(gc: GraphicsContext) {
        if (this._map == null) {
            return
        }

        // we got that map...
        val (xRange, yRange) = canvasPane.visibleArea()

        for (x in xRange) {
            for (y in yRange) {
                val tile = getMap().groundLayer[MapCoordinate(x, y)]
                if (tile == GroundTile.GROUND) {
                    gc.fill = Color.LIGHTGOLDENRODYELLOW
                } else {
                    gc.fill = Color.LIGHTBLUE
                }
                gc.fillRect(x.toDouble(), y.toDouble(), 1.0, 1.0)
            }
        }
    }

    init {

        title = "Kotcity 0.1"

        // fitCanvasToPane(canvas, canvasPane)
        canvasPane.content = canvas

        val timer = object : AnimationTimer() {
            override fun handle(now: Long) {
                if (ticks == 5) {
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