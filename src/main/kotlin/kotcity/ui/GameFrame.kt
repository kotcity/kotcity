package kotcity.ui

import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.scene.control.Accordion
import javafx.scene.control.Button
import javafx.scene.control.ScrollBar
import javafx.scene.control.TitledPane
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import kotcity.data.CityMap
import kotcity.data.MapGenerator
import tornadofx.App
import tornadofx.View
import tornadofx.find


object Algorithms {
    fun scale(valueIn: Double, baseMin: Double, baseMax: Double, limitMin: Double, limitMax: Double): Double {
        return (limitMax - limitMin) * (valueIn - baseMin) / (baseMax - baseMin) + limitMin
    }
}

const val DRAW_GRID = true

enum class Tool { BULLDOZE, QUERY, ROAD }

class GameFrame : View(), CanvasFitter {
    override val root: VBox by fxml("/GameFrame.fxml")
    private val canvas = ResizableCanvas()
    private val canvasPane: AnchorPane by fxid("canvasPane")
    private val accordion: Accordion by fxid()
    private val basicPane: TitledPane by fxid()
    private val verticalScroll: ScrollBar by fxid()
    private val horizontalScroll: ScrollBar by fxid()

    // BUTTONS
    private val roadButton: Button by fxid()
    private val queryButton: Button by fxid()

    var ticks = 0
    val tickDelay: Int = 5 // only render every X ticks... (framerate limiter)

    var activeTool: Tool = Tool.QUERY

    private lateinit var activeMap: CityMap
    private var cityRenderer: CityRenderer? = null

    fun setMap(map: CityMap) {
        this.activeMap = map
        // gotta resize the component now...
        setScrollbarSizes()
        setCanvasSize()

        this.cityRenderer = CityRenderer(this, canvas, map)
    }

    private fun setCanvasSize() {
        println("map size is: ${this.activeMap.width},${this.activeMap.height}")
        println("Canvas pane size is: ${canvasPane.width},${canvasPane.height}")
        canvas.prefHeight(canvasPane.height - 20)
        canvas.prefWidth(canvasPane.width - 20)
        AnchorPane.setTopAnchor(canvas, 0.0)
        AnchorPane.setBottomAnchor(canvas, 20.0)
        AnchorPane.setLeftAnchor(canvas, 0.0)
        AnchorPane.setRightAnchor(canvas, 20.0)
    }

    private fun setScrollbarSizes() {

        // TODO: don't let us scroll off the edge...
        horizontalScroll.min = 0.0
        horizontalScroll.max = getMap().width.toDouble()

        verticalScroll.min = 0.0
        verticalScroll.max = getMap().height.toDouble()

        println("Horizontal set to: ${horizontalScroll.max}")
        println("Vertical set to: ${verticalScroll.max}")

    }

    private fun getMap(): CityMap {
        return this.activeMap
    }


    fun zoomOut() {
        cityRenderer?.let {
            it.zoom -= 1
        }
    }

    fun zoomIn() {
        cityRenderer?.let {
            it.zoom += 1
        }
    }

    fun bindButtons() {
        roadButton.setOnAction { this.activeTool = Tool.ROAD }
        queryButton.setOnAction { this.activeTool = Tool.QUERY }
    }

    init {

        title = "Kotcity 0.1"

        bindCanvas()
        bindButtons()

        accordion.expandedPane = basicPane

        val timer = object : AnimationTimer() {
            override fun handle(now: Long) {
                if (ticks == tickDelay) {
                    cityRenderer?.render()
                    ticks = 0
                }
                ticks++
            }
        }
        timer.start()
    }

    private fun bindCanvas() {
        // TODO: we are handling scrolling ourself... so we have to figure out what's
        //       visible and what's not...
        canvas.prefHeight(canvasPane.height - 20)
        canvas.prefWidth(canvasPane.width - 20)
        canvasPane.add(canvas)

        canvasPane.widthProperty().addListener { _, _, newValue ->
            println("resizing canvas width to: $newValue")
            canvas.width = newValue.toDouble()
            setCanvasSize()
            setScrollbarSizes()
        }

        canvas.setOnMouseMoved { evt ->

        }

        canvas.setOnMousePressed { evt ->
            cityRenderer?.onMousePressed(evt)
        }

        canvas.setOnMouseReleased { evt ->
            cityRenderer?.onMouseReleased(evt)
        }

        canvas.setOnMouseDragged { evt ->
            cityRenderer?.onMouseDragged(evt)
        }

        canvas.setOnMouseClicked { evt ->
            cityRenderer?.onMouseClicked(evt)
        }

        canvasPane.heightProperty().addListener { _, _, newValue ->
            println("resizing canvas height to: ${newValue}")
            canvas.height = newValue.toDouble()
            setCanvasSize()
            setScrollbarSizes()
        }

        horizontalScroll.valueProperty().addListener { _, _, newValue ->
            cityRenderer?.blockOffsetX = newValue.toDouble()
        }

        verticalScroll.valueProperty().addListener { _, _, newValue ->
            cityRenderer?.blockOffsetY = newValue.toDouble()
        }

        with(canvas) {
            this.setOnScroll { scrollEvent ->
                // println("We are scrolling: $scrollEvent")
                if (scrollEvent.deltaY < 0) {
                    println("Zoom out!")
                    zoomOut()
                } else if (scrollEvent.deltaY > 0) {
                    println("Zoom in!")
                    zoomIn()
                }
            }
        }
    }

}

class GameFrameApp : App(GameFrame::class, KotcityStyles::class) {
    override fun start(stage: Stage) {
        stage.isResizable = true
        val gameFrame = find(GameFrame::class)
        val mapGenerator = MapGenerator()
        val map = mapGenerator.generateMap(512, 512)
        gameFrame.setMap(map)
        super.start(stage)
    }
}

fun main(args: Array<String>) {
    Application.launch(GameFrameApp::class.java, *args)
}