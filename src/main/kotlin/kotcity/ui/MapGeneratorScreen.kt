package kotcity.ui

import javafx.animation.AnimationTimer
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import kotcity.data.MapGenerator
import kotcity.datae.GroundTile
import kotcity.datae.MapCoordinate
import tornadofx.View

class MapGeneratorScreen : View(), CanvasFitter {
    override val root: BorderPane by fxml("/MapGeneratorScreen.fxml")
    val canvasPane: Pane by fxid("canvasPane")
    private val canvas = ResizableCanvas()
    private val mapGenerator = MapGenerator()
    private var newMap = mapGenerator.generateMap()

    init {
        fitCanvasToPane(canvas, canvasPane)

        val timer = object : AnimationTimer() {
            override fun handle(now: Long) {
                if (now % 10L == 0L) {
                    // get the graphics context...
                    val gc = canvas.graphicsContext2D;
                    repeat(newMap.width) { x ->
                        repeat(newMap.height) { y ->
                            val tile = newMap.groundLayer[MapCoordinate(x, y)]
                            if (tile == GroundTile.GROUND) {
                                gc.fill = Color.LIGHTGOLDENRODYELLOW
                                gc.fillRect(x.toDouble(), y.toDouble(), 1.0, 1.0)
                            } else {
                                gc.fill = Color.DARKBLUE
                                gc.fillRect(x.toDouble(), y.toDouble(), 1.0, 1.0)
                            }
                        }
                    }
                }
            }
        }

        timer.start()
    }

    fun generatePressed() {
        this.newMap = mapGenerator.generateMap()
    }

    fun acceptPressed() {

    }
}