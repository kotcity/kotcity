package kotcity.ui

import javafx.animation.AnimationTimer
import javafx.scene.control.ScrollPane
import javafx.scene.control.Slider
import javafx.scene.control.TextField
import javafx.scene.layout.BorderPane
import javafx.scene.paint.Color
import kotcity.data.MapGenerator
import kotcity.datae.CityMap
import kotcity.datae.GroundTile
import kotcity.datae.MapCoordinate
import tornadofx.View


class MapGeneratorScreen : View(), CanvasFitter {
    override val root: BorderPane by fxml("/MapGeneratorScreen.fxml")
    val canvasPane: ScrollPane by fxid("canvasPane")
    private val canvas = ResizableCanvas()
    private val mapGenerator = MapGenerator()

    private val f1Field: TextField by fxid()
    private val f2Field: TextField by fxid()
    private val f3Field: TextField by fxid()
    private val expField: TextField by fxid()
    private val heightField: TextField by fxid()
    private val widthField: TextField by fxid()
    private val seaLevelSlider: Slider by fxid()

    private var newMap = mapGenerator.generateMap(widthField.text.toInt(), heightField.text.toInt())

    init {

        canvasPane.content = canvas

        title = "Generate a Map"

        newMap = generate(widthField.text.toInt(), heightField.text.toInt())
        fitMap()

        val timer = object : AnimationTimer() {
            override fun handle(now: Long) {
                if (now % 10L == 0L) {
                    // get the graphics context...
                    val gc = canvas.graphicsContext2D

                    val hValue = canvasPane.hvalue
                    val vValue = canvasPane.vvalue
                    val width = canvasPane.viewportBoundsProperty().get().width
                    val height = canvasPane.viewportBoundsProperty().get().height

                    val startX = (canvasPane.content.boundsInParent.width - width) * hValue
                    val startY = (canvasPane.content.boundsInParent.height - height) * vValue
                    val endX = startX + width
                    val endY = startY + height

                    val xRange = startX.toInt() .. endX.toInt()
                    val yRange = startY.toInt() .. endY.toInt()

                    for (x in xRange) {
                        for (y in yRange) {
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

    private fun generate(width: Int, height: Int): CityMap {
        val f1 = f1Field.text.toDouble()
        val f2 = f2Field.text.toDouble()
        val f3 = f3Field.text.toDouble()
        val exp = expField.text.toDouble()
        println("Generating with $f1, $f2, $f3, $exp")
        mapGenerator.seaLevel = seaLevelSlider.value
        return mapGenerator.generateMap(width, height, f1, f2, f3, exp)
    }

    fun generatePressed() {
        newMap = generate(widthField.text.toInt(), heightField.text.toInt())
        fitMap()
    }

    private fun fitMap() {
        canvas.prefWidth(newMap.width.toDouble())
        canvas.prefHeight(newMap.height.toDouble())
        canvas.width = newMap.width.toDouble()
        canvas.height = newMap.height.toDouble()
    }

    fun acceptPressed() {
        this.close()
        GameFrame(newMap).openWindow()
    }
}