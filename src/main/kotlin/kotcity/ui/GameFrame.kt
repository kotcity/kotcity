package kotcity.ui

import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Accordion
import javafx.scene.control.ScrollBar
import javafx.scene.control.TitledPane
import javafx.scene.input.MouseButton
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Stage
import kotcity.data.*
import tornadofx.App
import tornadofx.View
import tornadofx.find

object Algorithms {
    fun scale(valueIn: Double, baseMin: Double, baseMax: Double, limitMin: Double, limitMax: Double): Double {
        return (limitMax - limitMin) * (valueIn - baseMin) / (baseMax - baseMin) + limitMin
    }
}

const val DRAW_GRID = true

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
    var mapMin = 0.0
    var mapMax = 1.0

    var ticks = 0
    var zoom = 1.0

    // each block should = 10 meters, square...
    // 64 pixels = 10 meters
    private fun blockSize(): Double {
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
        mapMin = getMap().groundLayer.values.mapNotNull {it.elevation}.min() ?: 0.0
        mapMax = getMap().groundLayer.values.mapNotNull {it.elevation}.max() ?: 0.0

        println("Map min: $mapMin Map max: $mapMax")
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

    private fun getMap(): CityMap {
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

    private fun bleach(color: Color, amount: Float): Color {
        var red = (color.red + amount).coerceIn(0.0, 1.0)
        var green = (color.green + amount).coerceIn(0.0, 1.0)
        var blue = (color.blue + amount).coerceIn(0.0, 1.0)
        return Color.color(red, green, blue)
    }

    fun drawMap(gc: GraphicsContext) {
        // we got that map...
        val (xRange, yRange) = getVisibleBlocks()

        xRange.toList().forEachIndexed { xi, x ->
            yRange.toList().forEachIndexed { yi, y ->
                val tile = getMap().groundLayer[BlockCoordinate(x, y)]
                if (tile != null) {
                    var newColor =
                    if (tile.type == TileType.GROUND) {
                        Color.rgb(153,102, 0)
                    } else {
                        Color.DARKBLUE
                    }
                    // this next line maps the elevations from -0.5 to 0.5 so we don't get
                    // weird looking colors....
                    val bleachAmount = Algorithms.scale(tile.elevation, mapMin, mapMax, -0.5, 0.5)
                    gc.fill = bleach(newColor, bleachAmount.toFloat())
                }

                val blockSize = blockSize()

                gc.fillRect(
                        xi * blockSize,
                        yi * blockSize,
                        blockSize, blockSize
                )

                if (DRAW_GRID && zoom >= 3.0) {
                    gc.fill = Color.BLACK
                    gc.strokeRect(xi * blockSize, yi * blockSize, blockSize, blockSize)
                }
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

        canvas.setOnMouseMoved { evt ->
            val mouseX = evt.x
            val mouseY = evt.y
            val blockCoordinate = mouseToBlock(mouseX, mouseY)
            // println("The mouse is at $blockCoordinate")
        }

        canvas.setOnMouseClicked { evt ->
            if (evt.button == MouseButton.SECONDARY) {
                val clickedBlock = mouseToBlock(evt.x, evt.y)
                panMap(clickedBlock)
            }
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

    private fun panMap(clickedBlock: BlockCoordinate) {
        // OK, we want to figure out the CENTER block now...
        val centerX = blockOffsetX + (canvasBlockWidth() / 2)
        val centerY = blockOffsetY + (canvasBlockHeight() / 2)
        println("The center block is: $centerX,$centerY")
        println("We clicked at: ${clickedBlock.x},${clickedBlock.y}")
        val dx = clickedBlock.x - centerX
        val dy = clickedBlock.y - centerY
        println("Delta is: $dx,$dy")
        blockOffsetX += (dx)
        blockOffsetY += (dy)
    }

    private fun mouseToBlock(mouseX: Double, mouseY: Double): BlockCoordinate {
        // OK... this should be pretty easy...
        // println("Block offsets: ${blockOffsetX.toInt()},${blockOffsetY.toInt()}")
        val blockX =  (mouseX / blockSize()).toInt()
        val blockY =  (mouseY / blockSize()).toInt()
        // println("Mouse block coords: $blockX,$blockY")
        return BlockCoordinate(blockX + blockOffsetX.toInt(), blockY + blockOffsetY.toInt())
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