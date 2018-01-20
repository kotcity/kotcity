package kotcity.ui

import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Accordion
import javafx.scene.control.Button
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
import java.awt.Graphics2D



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

    var blockOffsetX = 0.0
    var blockOffsetY = 0.0
    var mapMin = 0.0
    var mapMax = 1.0

    private var mouseDown = false
    private var mouseBlock: BlockCoordinate? = null
    private var firstBlockPressed: BlockCoordinate? = null

    var ticks = 0
    var zoom = 1.0

    private var activeTool: Tool = Tool.QUERY

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

    private lateinit var activeMap: CityMap

    fun setMap(map: CityMap) {
        this.activeMap = map
        // gotta resize the component now...
        setScrollbarSizes()
        setCanvasSize()
        mapMin = getMap().groundLayer.values.mapNotNull {it.elevation}.min() ?: 0.0
        mapMax = getMap().groundLayer.values.mapNotNull {it.elevation}.max() ?: 0.0

        println("Map min: $mapMin Map max: $mapMax")
        println("Map has been set to: $activeMap. Size is ${canvas.width}x${canvas.height}")
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

    private fun getVisibleBlocks(): Pair<IntRange, IntRange> {
        var startBlockX = blockOffsetX.toInt()
        var startBlockY = blockOffsetY.toInt()
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
    }

    fun zoomOut() {
        if (zoom > 1) {
            zoom -= 1
        }
    }

    fun zoomIn() {
        zoom += 1
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
                if (ticks == 20) {
                    canvas.graphicsContext2D.fill = Color.BLACK
                    canvas.graphicsContext2D.fillRect(0.0,0.0, canvas.width, canvas.height)
                    drawMap(canvas.graphicsContext2D)
                    if (mouseDown) {
                        if (activeTool == Tool.ROAD) {
                            drawRoadBlueprint(canvas.graphicsContext2D)
                        }
                    }
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
            this.mouseDown = true
            this.firstBlockPressed = mouseToBlock(evt.x, evt.y)
            this.mouseBlock = this.firstBlockPressed
            println("Pressed on block: $firstBlockPressed")
        }

        canvas.setOnMouseReleased { evt ->
            this.mouseDown = false
        }

        canvas.setOnMouseDragged { evt ->
            val mouseX = evt.x
            val mouseY = evt.y
            val blockCoordinate = mouseToBlock(mouseX, mouseY)
            this.mouseBlock = blockCoordinate
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

    private fun drawRoadBlueprint(gc: GraphicsContext) {
        // figure out if we are more horizontal or vertical away from origin point
        gc.fill = (Color.YELLOW)
        val startBlock = firstBlockPressed ?: return
        val endBlock = mouseBlock ?: return
        val x = startBlock.x
        val y = startBlock.y
        var x2 = endBlock.x
        var y2 = endBlock.y

        if (Math.abs(x - x2) > Math.abs(y - y2)) {
            // building horizontally
            // now fuck around with y2 so it's at the same level as y1
            y2 = y

            if (x < x2) {
                fillBlocks(gc, x, y, Math.abs(x - x2) + 1, 1)
                queueRoads(x, y, x2, y2)
            } else {
                fillBlocks(gc, x2, y, Math.abs(x - x2) + 1, 1)
                queueRoads(x2, y2, x, y)
            }
        } else {
            // building vertically
            // now fuck around with x2 so it's at the same level as x1
            x2 = x

            if (y < y2) {
                fillBlocks(gc, x, y, 1, Math.abs(y - y2) + 1)
                queueRoads(x, y, x2, y2)
            } else {
                fillBlocks(gc, x, y2, 1, Math.abs(y - y2) + 1)
                queueRoads(x2, y2, x, y)
            }

        }

    }

    private fun queueRoads(x: Int, y: Int, x2: Int, y2: Int) {

    }

    private fun fillBlocks(g2d: GraphicsContext, blockX: Int, blockY: Int, width: Int, height: Int) {
        for (y in blockY until blockY + height) {
            for (x in blockX until blockX + width) {
                highlightBlock(g2d, x, y)
            }
        }
    }

    private fun highlightBlock(g2d: GraphicsContext, x: Int, y: Int) {
        g2d.fill = Color.MAGENTA
        // gotta translate here...
        val tx = x - blockOffsetX
        val ty = y - blockOffsetY
        g2d.fillRect(tx * blockSize(), ty  * blockSize(), blockSize(), blockSize())
    }

    private fun highlightBlock(g2d: GraphicsContext, hoveredBlockX: Int, hoveredBlockY: Int, radius: Int) {
        val startBlockX = (hoveredBlockX - Math.floor((radius / 2).toDouble())).toInt()
        val startBlockY = (hoveredBlockY - Math.floor((radius / 2).toDouble())).toInt()
        val endBlockX = startBlockX + radius - 1
        val endBlockY = startBlockY + radius - 1

        for (y in startBlockY..endBlockY) {
            for (x in startBlockX..endBlockX) {
                highlightBlock(g2d, x, y)
            }
        }
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