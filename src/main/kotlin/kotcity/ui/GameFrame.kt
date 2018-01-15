package kotcity.ui

import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Accordion
import javafx.scene.control.ScrollBar
import javafx.scene.control.TitledPane
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Stage
import kotcity.data.CityMap
import kotcity.data.GroundTile
import kotcity.data.MapCoordinate
import kotcity.data.MapGenerator
import tornadofx.App
import tornadofx.View
import tornadofx.find


class GameFrame : View(), CanvasFitter {
    override val root: VBox by fxml("/GameFrame.fxml")
    private val canvas = ResizableCanvas()
    private val canvasPane: AnchorPane by fxid("canvasPane")
    private val accordion: Accordion by fxid()
    private val basicPane: TitledPane by fxid()
    private val verticalScroll: ScrollBar by fxid()
    private val horizontalScroll: ScrollBar by fxid()

    var blockOffsetX = 0.0
    var blockOffsetY = 0.0

    var ticks = 0
    var zoom = 1.0

    // each block should = 10 meters, square...
    // 64 pixels = 10 meters
    fun blockSize(): Double {
        // return (this.zoom * 64)
        return when (zoom) {
            1.0 -> 4.0
            2.0 -> 8.0
            3.0 -> 16.0
            4.0 -> 32.0
            5.0 -> 64.0
            else -> 64.0
        }
    }

    lateinit var _map: CityMap

    fun setMap(map: CityMap) {
        this._map = map
        // gotta resize the component now...
        setScrollbarSizes()
        setCanvasSize()
        println("Map has been set to: $_map. Size is ${canvas.width}x${canvas.height}")
    }

    private fun setCanvasSize() {
        println("map size is: ${this._map.width},${this._map.height}")
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

    fun getMap(): CityMap {
        return this._map
    }

    private fun getVisibleBlocks(): Pair<IntRange, IntRange> {
        val startBlockX = blockOffsetX.toInt()
        val startBlockY = blockOffsetY.toInt()
        var endBlockX = startBlockX+canvasBlockWidth()
        var endBlockY = startBlockY+canvasBlockHeight()

        if (endBlockX > getMap().width) {
            endBlockX = getMap().width
        }

        if (endBlockY > getMap().height) {
            endBlockY = getMap().height
        }

        return Pair(startBlockX..endBlockX, startBlockY..endBlockY)
    }

    private fun canvasBlockHeight() = (canvas.height / blockSize()).toInt()

    private fun canvasBlockWidth() = (canvas.width / blockSize()).toInt()


    fun drawMap(gc: GraphicsContext) {
        // we got that map...
        val (xRange, yRange) = getVisibleBlocks()
        println("Visible blocks: ${getVisibleBlocks()}")

        for (x in xRange) {
            for (y in yRange) {
                val tile = getMap().groundLayer[MapCoordinate(x, y)]
                if (tile == GroundTile.GROUND) {
                    gc.fill = Color.LIGHTGOLDENRODYELLOW
                } else {
                    gc.fill = Color.LIGHTBLUE
                }

                val xi = xRange.indexOf(x)
                val yi = yRange.indexOf(y)

                if (x == xRange.first && y == yRange.first) {
//                    println("i want to paint block: ${x}, ${y}")
//                    println("starting at ${xi}, ${yi}")
                }

                gc.fillRect(
                        xi * blockSize(),
                        yi * blockSize(),
                        blockSize(), blockSize()
                )
            }
        }
    }

    fun zoomOut() {
        if (zoom > 1) {
            zoom -= 1
        }
    }

    fun zoomIn() {
        zoom += 1
    }

    init {

        title = "Kotcity 0.1"

        // TODO: we are handling scrolling ourself... so we have to figure out what's
        //       visible and what's not...
        canvas.prefHeight(canvasPane.height - 20)
        canvas.prefWidth(canvasPane.width - 20)
        canvasPane.add(canvas)

        canvasPane.widthProperty().addListener {_, _, newValue ->
            println("resizing canvas width to: ${newValue}")
            canvas.width = newValue.toDouble()
            setCanvasSize()
            setScrollbarSizes()
        }

        canvasPane.heightProperty().addListener { _, _, newValue ->
            println("resizing canvas height to: ${newValue}")
            canvas.height = newValue.toDouble()
            setCanvasSize()
            setScrollbarSizes()
        }

        horizontalScroll.valueProperty().addListener { _, _, newValue ->
            this.blockOffsetX = newValue.toDouble()
        }

        verticalScroll.valueProperty().addListener { _, _, newValue ->
            this.blockOffsetY = newValue.toDouble()
        }

        with(canvas) {
            this.setOnScroll {  scrollEvent ->
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

        accordion.expandedPane = basicPane

        val timer = object : AnimationTimer() {
            override fun handle(now: Long) {
                if (ticks == 20) {
                    canvas.graphicsContext2D.fill = Color.BLACK
                    canvas.graphicsContext2D.fillRect(0.0,0.0, canvas.width, canvas.height)
                    drawMap(canvas.graphicsContext2D)
                    ticks = 0
                }
                ticks++
            }
        }
        timer.start()
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