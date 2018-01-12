package kotcity.ui

import javafx.application.Application
import javafx.scene.layout.VBox
import javafx.stage.Stage
import tornadofx.App
import tornadofx.View


class LaunchScreen : View() {
    override val root: VBox by fxml("/LaunchScreen.fxml")

    init {
        title = "Kotcity 0.1"
    }

    fun newCityPressed() {
        println("We want a new city!")
        this.primaryStage.close()
        MapGeneratorScreen().openWindow()
    }

    fun loadCityPressed() {
        throw NotImplementedError()
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