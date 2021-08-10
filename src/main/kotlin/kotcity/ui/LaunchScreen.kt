package kotcity.ui

import com.almasb.fxgl.app.GameApplication
import com.almasb.fxgl.app.GameSettings
import com.almasb.fxgl.app.scene.FXGLMenu
import com.almasb.fxgl.app.scene.SceneFactory
import com.almasb.fxgl.dsl.addUINode
import kotcity.ui.scenes.KotCityMainMenu

/**
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class KotCityApp : GameApplication() {

    lateinit var gameFrame: GameFrame

    override fun initSettings(settings: GameSettings) {
        with(settings) {
            title = "KotCity"
            version = "0.50.0"

            width = 1280
            height = 720
            isManualResizeEnabled = true
            isScaleAffectedOnResize = false
            isCloseConfirmation = true
            isMainMenuEnabled = true
            isGameMenuEnabled = false

            sceneFactory = object : SceneFactory() {
                override fun newMainMenu(): FXGLMenu {
                    return KotCityMainMenu()
                }
            }
        }
    }

    override fun initGame() {
        addUINode(gameFrame.root)
    }
}

fun main() {
    GameApplication.launch(KotCityApp::class.java, emptyArray())
}