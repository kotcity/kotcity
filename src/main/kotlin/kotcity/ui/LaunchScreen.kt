package kotcity.ui

import javafx.application.Application
import javafx.application.Platform
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.stage.WindowEvent
import javafx.util.Duration
import tornadofx.App
import tornadofx.View
import tornadofx.find
import tornadofx.runLater


const val GAME_STRING = "KotCity 0.42"

class LaunchScreen : View() {
    override val root: VBox by fxml("/LaunchScreen.fxml")
    val titleLabel: Label by fxid()

    init {
        title = GAME_STRING
        titleLabel.text = GAME_STRING
        currentStage?.toFront()

        primaryStage.setOnCloseRequest {
            Platform.exit()
            java.lang.System.exit(0)
        }
    }

    fun newCityPressed() {
        println("We want a new city!")
        this.currentStage?.close()
        this.primaryStage.close()
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
        stage.toFront()
        stage.isAlwaysOnTop = true
        runLater(Duration(5000.0)) {
            stage.isAlwaysOnTop = false
        }
        super.start(stage)

        stage.setOnCloseRequest {
            val gameFrame = find(GameFrame::class)
            gameFrame.quitPressed()
            it.consume()
        }
    }

}

fun main(args: Array<String>) {
    Application.launch(LaunchScreenApp::class.java, *args)
}