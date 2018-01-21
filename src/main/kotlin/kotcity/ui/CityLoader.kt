package kotcity.ui

import javafx.stage.FileChooser
import javafx.stage.Stage
import kotcity.data.CityFileAdapter
import tornadofx.runLater
import java.io.File

object CityLoader {
    fun loadCity(stage: Stage) {
        val fileChooser = FileChooser()
        fileChooser.title = "Load a city"
        // fileChooser.initialDirectory = File(System.getProperty("user.home"))
        fileChooser.extensionFilters.addAll(
                FileChooser.ExtensionFilter("KotCity Data", "*.kcity")
        )
        runLater {
            val file = fileChooser.showOpenDialog(stage)
            if (file != null) {
                val map = CityFileAdapter.load(file)
                val gameFrame = tornadofx.find(GameFrame::class)
                gameFrame.setMap(map)
                gameFrame.openWindow()
            }
        }
    }
}