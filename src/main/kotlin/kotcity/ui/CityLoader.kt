package kotcity.ui

import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.stage.FileChooser
import kotcity.data.CityFileAdapter
import tornadofx.View
import tornadofx.find
import tornadofx.runLater


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
                val launchScreen = find(LaunchScreen::class)
                launchScreen.close()
                view.close()
                view.currentStage?.close()
                view.primaryStage.close()

                val map = CityFileAdapter.load(file)
                val gameFrame = tornadofx.find(GameFrame::class)
                gameFrame.setMap(map)
                gameFrame.currentStage?.isMaximized = true
                gameFrame.openWindow()
                println("Gameframe should be open at this point...")
                gameFrame.currentStage?.isMaximized = true
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