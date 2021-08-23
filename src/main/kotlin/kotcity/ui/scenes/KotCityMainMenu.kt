package kotcity.ui.scenes

import com.almasb.fxgl.app.scene.FXGLMenu
import com.almasb.fxgl.app.scene.MenuType
import com.almasb.fxgl.dsl.*
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import kotcity.ui.GameFrame
import kotcity.ui.KotCityApp

/**
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class KotCityMainMenu : FXGLMenu(MenuType.MAIN_MENU) {

    private val mapGeneratorScene by lazy { MapGeneratorSubScene().also { it.callback = this::onMapSelected } }

    init {
        val bg = Rectangle()
        bg.widthProperty().bind(getSettings().prefWidthProperty())
        bg.heightProperty().bind(getSettings().prefHeightProperty())

        val title = getUIFactoryService().newText(getSettings().title + " v." + getSettings().version, Color.WHITESMOKE, 44.0)
        title.translateX = 30.0
        title.translateY = 70.0

        val btnNew = getUIFactoryService().newButton("New City")
        btnNew.setOnAction {
            FXGL.getSceneService().pushSubScene(mapGeneratorScene)
        }

        val btnLoad = getUIFactoryService().newButton("Load City")
        btnLoad.setOnAction {
            showMessage("TODO:")

            // TODO: CityLoader.loadCity(launchScreen)
        }

        val btnQuit = getUIFactoryService().newButton("Quit")
        btnQuit.setOnAction {
            getGameController().exit()
        }

        val vbox = VBox(10.0, btnNew, btnLoad, btnQuit)
        vbox.translateX = 50.0
        vbox.translateY = 250.0

        contentRoot.children.addAll(bg, title, vbox)
    }

    private fun onMapSelected(gameFrame: GameFrame) {
        FXGL.getAppCast<KotCityApp>().gameFrame = gameFrame
        fireNewGame()
    }
}