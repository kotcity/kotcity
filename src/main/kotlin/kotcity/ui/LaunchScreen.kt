package kotcity.ui

import com.natpryce.konfig.ConfigurationProperties
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.util.Duration
import kotcity.jmx.JMXAgent
import kotcity.util.Game
import tornadofx.App
import tornadofx.View
import tornadofx.runLater

val config = ConfigurationProperties.fromResource("config/defaults.properties")

var GAME_TITLE = "${config[Game.Name]} ${config[Game.Version]}"

class LaunchScreen : View() {
    override val root: VBox by fxml("/LaunchScreen.fxml")
    private val titleLabel: Label by fxid()

    init {
        title = GAME_TITLE
        titleLabel.text = GAME_TITLE
        currentStage?.toFront()
    }

    override fun onDock() {
        super.onDock()
        currentWindow?.sizeToScene()
        currentWindow?.centerOnScreen()
        currentStage?.requestFocus()
    }

    fun newCityPressed() {
        replaceWith<MapGeneratorScreen>()
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
            Alert(Alert.AlertType.CONFIRMATION).apply {
                title = "Quitting KotCity"
                headerText = "Are you ready to leave?"
                contentText = "Please confirm..."

                val buttonTypeOne = ButtonType("Yes, please quit.")
                val buttonTypeTwo = ButtonType("No, I want to keep playing.")
                val buttonTypeCancel = ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)

                buttonTypes.setAll(buttonTypeOne, buttonTypeTwo, buttonTypeCancel)

                val result = showAndWait()
                when (result.get()) {
                    buttonTypeOne -> {
                        Platform.exit()
                        System.exit(0)
                    }
                    else -> {
                        // don't do anything ...
                    }
                }
            }
            it.consume()
        }
    }
}

fun main(args: Array<String>) {
    // start JMX agent...
    val agent = JMXAgent()
    Application.launch(LaunchScreenApp::class.java, *args)
}
