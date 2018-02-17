package kotcity.ui

import javafx.application.Application
import javafx.scene.layout.VBox
import javafx.stage.Stage
import tornadofx.App
import tornadofx.View
import javafx.stage.FileChooser
import kotcity.data.CityFileAdapter
import kotcity.data.MapGenerator
import tornadofx.runLater
import java.util.Collections.addAll
import java.io.File


const val GAME_STRING = "KotCity 0.2"

class LaunchScreen : View() {
    override val root: VBox by fxml("/LaunchScreen.fxml")

    init {
        title = GAME_STRING
    }

    fun newCityPressed() {
        println("We want a new city!")
        this.currentStage?.close()
        MapGeneratorScreen().openWindow()
    }

    fun loadCityPressed() {
        val launchScreen = this
        runLater {
            CityLoader.loadCity(launchScreen)
        }
    }

    fun quitPressed() {
        System.exit(0)
    }

}

class LaunchScreenApp : App(LaunchScreen::class, KotcityStyles::class) {

    override fun start(stage: Stage) {
        stage.isResizable = true
        super.start(stage)
    }
}

fun main(args: Array<String>) {
    Application.launch(LaunchScreenApp::class.java, *args)
}