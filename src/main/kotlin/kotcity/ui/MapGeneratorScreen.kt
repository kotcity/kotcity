package kotcity.ui

import javafx.animation.AnimationTimer
import javafx.scene.control.Slider
import javafx.scene.control.TextField
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import kotcity.data.MapGenerator
import kotcity.datae.CityMap
import kotcity.datae.GroundTile
import kotcity.datae.MapCoordinate
import tornadofx.View

class MapGeneratorScreen : View(), CanvasFitter {
    override val root: BorderPane by fxml("/MapGeneratorScreen.fxml")
    val canvasPane: Pane by fxid("canvasPane")
    private val canvas = ResizableCanvas()
    private val mapGenerator = MapGenerator()
    private var newMap = mapGenerator.generateMap()

    private val f1Field: TextField by fxid()
    private val f2Field: TextField by fxid()
    private val f3Field: TextField by fxid()
    private val expField: TextField by fxid()
    private val seaLevelSlider: Slider by fxid()

    init {
        fitCanvasToPane(canvas, canvasPane)

        title = "Generate a Map"

        newMap = generate()

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

    private fun generate(): CityMap {
        val f1 = f1Field.text.toDouble()
        val f2 = f2Field.text.toDouble()
        val f3 = f3Field.text.toDouble()
        val exp = expField.text.toDouble()
        println("Generating with $f1, $f2, $f3, $exp")
        mapGenerator.seaLevel = seaLevelSlider.value.toDouble()
        return mapGenerator.generateMap(f1, f2, f3, exp)
    }

    fun generatePressed() {
        newMap = generate()
    }

    fun acceptPressed() {

    }
}