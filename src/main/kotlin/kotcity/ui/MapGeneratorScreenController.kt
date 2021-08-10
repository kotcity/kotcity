package kotcity.ui

import com.almasb.fxgl.ui.UIController
import javafx.animation.AnimationTimer
import javafx.scene.control.ComboBox
import javafx.scene.control.Slider
import javafx.scene.control.TextField
import javafx.scene.layout.BorderPane
import javafx.stage.FileChooser
import kotcity.data.CityMap
import kotcity.data.MapGenerator
import kotcity.data.MapMode
import kotcity.ui.map.CityMapCanvas


class MapGeneratorScreenController : UIController {
    private val cityMapCanvas = CityMapCanvas()
    private val mapGenerator = MapGenerator()

    lateinit var root: BorderPane

    lateinit var f1Field: TextField
    lateinit var f2Field: TextField
    lateinit var f3Field: TextField
    lateinit var expField: TextField
    lateinit var sizeField: TextField
    lateinit var nameField: TextField
    lateinit var seaLevelSlider: Slider

    private var timer: AnimationTimer? = null

    lateinit var mapModeComboBox: ComboBox<String>

    private lateinit var newMap: CityMap

    private val mapModes = listOf("Normal", "Oil", "Coal", "Gold", "Soil")

    override fun init() {
        newMap = mapGenerator.generateMap(sizeField.text.toInt(), sizeField.text.toInt())

        cityMapCanvas.map = newMap
        root.center = cityMapCanvas

        mapModeComboBox.items.setAll(mapModes)

        mapModeComboBox.selectionModel.select(0)

        fitMap()

        var ticks = 0

        timer = object : AnimationTimer() {
            override fun handle(now: Long) {
                ticks += 1
                if (ticks > 50) {
                    cityMapCanvas.render()
                    ticks = 0
                }
            }
        }

        timer?.start()
    }

    private fun generate(width: Int, height: Int): CityMap {
        val f1 = f1Field.text.toDouble()
        val f2 = f2Field.text.toDouble()
        val f3 = f3Field.text.toDouble()
        val exp = expField.text.toDouble()
        mapGenerator.seaLevel = seaLevelSlider.value
        return mapGenerator.generateMap(width, height, f1, f2, f3, exp)
    }

    fun importPressed() {
        val fileChooser = FileChooser()
        fileChooser.title = "Import BMP"
        // fileChooser.initialDirectory = File(System.getProperty("user.home"))
        fileChooser.extensionFilters.addAll(
            FileChooser.ExtensionFilter("SC4 Map", "*.bmp")
        )

        // TODO:
//        Platform.runLater {
//            val file = fileChooser.showOpenDialog(currentStage)
//            if (file != null) {
//                // let's use the dialog... :)
//
//
//                currentWindow?.let { window ->
//                    val workDialog = WorkIndicatorDialog<File>(window, "Importing map...")
//
//                    workDialog.exec(file, func = ToIntFunction<File> {
//                        val bmpImporter = BMPImporter()
//                        val map = bmpImporter.load(it.absolutePath)
//                        map?.let {
//                            cityMapCanvas.map = map
//                            sizeField.text = it.width.toString()
//                            newMap = map
//                        }
//                        runLater {
//                            fitMap()
//                        }
//                        1
//                    })
//                }
//            } else {
//                Alert(Alert.AlertType.ERROR).apply {
//                    title = "Error during load"
//                    headerText = "Could not import your map!"
//                    contentText = "Why not? Totally unknown?"
//                    showAndWait()
//                }
//            }
//        }
    }

    fun generatePressed() {
        var size = sizeField.text.toInt()

        if (size < 32) {
            size = 32
            sizeField.text = size.toString()
        }

        newMap = generate(sizeField.text.toInt(), sizeField.text.toInt())
        cityMapCanvas.map = newMap
        fitMap()
    }

    private fun fitMap() {
        val width = newMap.width.toDouble()
        val height = newMap.height.toDouble()
        cityMapCanvas.apply {
            prefWidth(width)
            prefHeight(height)
            setWidth(width.coerceAtMost(1024.0))
            setHeight(height.coerceAtMost(1024.0))
        }
    }

    fun mapModeSelected() {
        cityMapCanvas.mode = when (mapModeComboBox.selectionModel.selectedItem) {
            "Coal" -> MapMode.COAL
            "Gold" -> MapMode.GOLD
            "Soil" -> MapMode.SOIL
            "Oil" -> MapMode.OIL
            "Normal" -> MapMode.NORMAL
            else -> throw RuntimeException("Unknown cityMap mode: ${mapModeComboBox.selectionModel.selectedItem}")
        }
    }

    fun acceptPressed() {
        newMap.cityName = nameField.text
        timer?.stop()

        // TODO:
        //replaceWith<GameFrame>()
        //tornadofx.find(GameFrame::class).setMap(newMap)
    }
}
