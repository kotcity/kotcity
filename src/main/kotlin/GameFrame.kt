import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.scene.control.SplitPane
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.stage.Stage
import tornadofx.App
import tornadofx.View


class GameFrame : View() {
    override val root: SplitPane by fxml("/GameFrame.fxml")
    private val canvas = ResizableCanvas()
    private val canvasPane: Pane by fxid("canvasPane")

    var ticks = 0

    var offset = 0.0

    init {

        title = "Kotcity 0.1"

        canvas.height = canvasPane.height
        canvas.width = canvasPane.width
        canvas.minHeight(100.0)
        canvasPane.minHeight(100.0)

        canvas.isCache = true

        canvasPane.add(canvas)

        canvasPane.widthProperty().addListener { _, _, newValue ->
            canvas.width = newValue as Double
        }

        canvasPane.heightProperty().addListener { _, _, newValue ->
            canvas.height = newValue as Double
        }

        val timer = object : AnimationTimer() {
            override fun handle(now: Long) {
                if (ticks == 5) {
                    drawIt()
                    ticks = 0
                }
                ticks++
            }

            private fun drawIt() {
                val gc = canvas.graphicsContext2D

                gc.fill = Color.AQUA
                gc.fillRect(0.0, 0.0, canvas.width, canvas.height)

                gc.fill = Color.BLACK

                gc.fillText("Time: ${System.currentTimeMillis()}", 10.0, 10.0)

                gc.fill = Color.CORNSILK
                gc.fillRect(offset*2, 0.0, 10.toDouble(), 10.toDouble())
                gc.fill = Color.FORESTGREEN
                gc.fillOval(
                        10.0 + offset,
                        10.0,
                        10.0, 10.0
                )

                offset += 1

                if (offset > 100) {
                    offset = 0.0
                }
            }
        }

        timer.start()
    }

}

class GameFrameApp : App(GameFrame::class, KotcityStyles::class) {
    override fun start(stage: Stage) {
        stage.isResizable = true
        super.start(stage)
    }
}

fun main(args: Array<String>) {
    Application.launch(GameFrameApp::class.java, *args)
}