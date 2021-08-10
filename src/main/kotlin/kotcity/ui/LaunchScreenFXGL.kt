package kotcity.ui

import com.almasb.fxgl.app.GameApplication
import com.almasb.fxgl.app.GameSettings
import com.almasb.fxgl.dsl.*
import javafx.scene.layout.VBox

/**
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class LaunchScreenFXGL : GameApplication() {
    override fun initSettings(settings: GameSettings) {
        with(settings) {
            title = "KotCity"
            version = "0.50.0"

            width = 1280
            height = 720
            isManualResizeEnabled = true
            isScaleAffectedOnResize = false
            isCloseConfirmation = true
        }
    }

    override fun initGame() {

    }

    override fun initUI() {
        val ui = getAssetLoader().loadUI("MapGeneratorScreen.fxml", MapGeneratorScreenController())

        val btnNew = getUIFactoryService().newButton("New City")
        btnNew.setOnAction {
            addUINode(ui.root)
        }

        val btnLoad = getUIFactoryService().newButton("Load City")
        btnLoad.setOnAction {
            showMessage("TODO:")
        }

        val btnQuit = getUIFactoryService().newButton("Quit")
        btnQuit.setOnAction {
            getGameController().exit()
        }

        val vbox = VBox(10.0, btnNew, btnLoad, btnQuit)
        vbox.setPrefSize(getAppWidth().toDouble(), getAppHeight().toDouble())

        addUINode(vbox)
    }
}

fun main() {
    GameApplication.launch(LaunchScreenFXGL::class.java, emptyArray())
}