package kotcity.ui

import javafx.stage.FileChooser
import javafx.stage.Stage
import kotcity.data.CityFileAdapter
import tornadofx.find
import tornadofx.runLater
import java.io.File
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.Alert
import tornadofx.View


object CityLoader {
    fun loadCity(view: View) {
        val fileChooser = FileChooser()
        fileChooser.title = "Load a city"
        // fileChooser.initialDirectory = File(System.getProperty("user.home"))
        fileChooser.extensionFilters.addAll(
                FileChooser.ExtensionFilter("KotCity Data", "*.kcity")
        )
        runLater {
            val file = fileChooser.showOpenDialog(view.currentStage)
            if (file != null) {
                val map = CityFileAdapter.load(file)
                val gameFrame = tornadofx.find(GameFrame::class)
                gameFrame.setMap(map)
                gameFrame.currentStage?.isMaximized = true
                gameFrame.openWindow()
                println("Gameframe should be open at this point...")
                gameFrame.currentStage?.isMaximized = true
                view.close()
            } else {
                val alert = Alert(AlertType.ERROR)
                alert.title = "Error during load"
                alert.headerText = "Could not load your city!"
                alert.contentText = "Why not? Totally unknown?"

                alert.showAndWait()
            }
        }
    }
}