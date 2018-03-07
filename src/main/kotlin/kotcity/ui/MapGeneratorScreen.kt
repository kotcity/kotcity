package kotcity.ui

import javafx.animation.AnimationTimer
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.stage.FileChooser
import kotcity.data.*
import kotcity.ui.map.CityMapCanvas
import tornadofx.View
import tornadofx.runLater
import tornadofx.selectedItem
import java.io.File
import java.util.function.ToIntFunction


class MapGeneratorScreen : View() {
    override val root: BorderPane by fxml("/MapGeneratorScreen.fxml")

    private val cityMapCanvas = CityMapCanvas()
    private val mapGenerator = MapGenerator()

    private val f1Field: TextField by fxid()
    private val f2Field: TextField by fxid()
    private val f3Field: TextField by fxid()
    private val expField: TextField by fxid()
    private val sizeField: TextField by fxid()
    private val seaLevelSlider: Slider by fxid()

    private var timer : AnimationTimer? = null

    private val mapModeComboBox: ComboBox<String> by fxid()

    private var newMap = mapGenerator.generateMap(sizeField.text.toInt(), sizeField.text.toInt())

    val mapModes = listOf("Normal", "Oil", "Coal", "Gold", "Soil")

    init {

        cityMapCanvas.map = newMap
        root.center = cityMapCanvas

        mapModeComboBox.items.clear()
        mapModeComboBox.items.addAll(mapModes)

        mapModeComboBox.selectionModel.select(0)

        title = "$GAME_STRING - Generate a Map"

        // newMap = generate(sizeField.text.toInt(), sizeField.text.toInt())
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
        println("Generating with $f1, $f2, $f3, $exp")
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
        runLater {
            val file = fileChooser.showOpenDialog(currentStage)
            if (file != null) {

                // let's use the dialog... :)
                currentWindow?.let { window ->
                    val workDialog = WorkIndicatorDialog<File>(window, "Importing map...")

                    workDialog.exec(file, func = ToIntFunction<File> {
                        runLater {
                            val bmpImporter = BMPImporter()
                            val map = bmpImporter.load(it.absolutePath)
                            cityMapCanvas.map = map
                            fitMap()
                        }
                        1
                    })
                }


            } else {
                val alert = Alert(Alert.AlertType.ERROR)
                alert.title = "Error during load"
                alert.headerText = "Could not import your map!"
                alert.contentText = "Why not? Totally unknown?"

                alert.showAndWait()
            }
        }
    }

    fun generatePressed() {
        newMap = generate(sizeField.text.toInt(), sizeField.text.toInt())
        cityMapCanvas.map = newMap
        fitMap()
    }

    private fun fitMap() {
        cityMapCanvas.prefWidth(newMap.width.toDouble())
        cityMapCanvas.prefHeight(newMap.height.toDouble())
        cityMapCanvas.width = newMap.width.toDouble()
        cityMapCanvas.height = newMap.height.toDouble()
    }

    fun mapModeSelected() {
        println("We selected this mode: ${mapModeComboBox.selectedItem}")
        cityMapCanvas.mode = when (mapModeComboBox.selectedItem) {
            "Coal" -> MapMode.COAL
            "Gold" -> MapMode.GOLD
            "Soil" -> MapMode.SOIL
            "Oil" -> MapMode.OIL
            "Normal" -> MapMode.NORMAL
            else -> throw RuntimeException("Unknown cityMap mode: ${mapModeComboBox.selectedItem}")
        }
    }

    fun acceptPressed() {

        val dialog = TextInputDialog("My City")
        dialog.title = "Name your city"
        dialog.headerText = "Choose a name for your city"
        dialog.contentText = "Please enter your city's name:"

        // Traditional way to get the response value.
        val result = dialog.showAndWait()
        if (result.isPresent) {
            newMap.cityName = result.get()
        }

        this.close()
        val gameFrame = tornadofx.find(GameFrame::class)
        timer?.stop()
        gameFrame.setMap(newMap)
        gameFrame.openWindow()
        println("Now maximizing...")
        gameFrame.currentStage?.isMaximized = true
    }
}