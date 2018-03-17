package kotcity.ui

import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.scene.control.*
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.ButtonBar.ButtonData
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Stage
import kotcity.data.*
import kotcity.data.assets.AssetManager
import kotcity.ui.charts.SupplyDemandChart
import kotcity.ui.layers.TrafficRenderer
import kotcity.ui.layers.ZotRenderer
import kotcity.ui.map.CityMapCanvas
import kotcity.ui.map.CityRenderer
import kotcity.util.Debuggable
import tornadofx.App
import tornadofx.View
import tornadofx.find
import tornadofx.runLater
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


object Algorithms {
    fun scale(valueIn: Double, baseMin: Double, baseMax: Double, limitMin: Double, limitMax: Double): Double {
        return (limitMax - limitMin) * (valueIn - baseMin) / (baseMax - baseMin) + limitMin
    }
}

fun serializeDate(date: Date): String {
    val simpleDateFormat = SimpleDateFormat("EEE MM/dd/yyyy hh:mm a")
    simpleDateFormat.timeZone = TimeZone.getDefault()
    return simpleDateFormat.format(date)
}

const val DRAW_GRID = false
const val TICK_DELAY: Int = 5 // only render every X ticks... (framerate limiter)

enum class Tool {
    BULLDOZE,
    QUERY, ROAD,
    RESIDENTIAL_ZONE, INDUSTRIAL_ZONE,
    COMMERCIAL_ZONE, COAL_POWER_PLANT,
    NUCLEAR_POWER_PLANT,
    DEZONE,
    POWER_LINES,
    JOB_CENTER,
    TOWN_WAREHOUSE,
    FIRE_STATION,
    ROUTES, RECENTER
}

enum class GameSpeed { SLOW, MEDIUM, FAST }

class GameFrame : View(), Debuggable {
    override var debug: Boolean = false
    override val root: VBox by fxml("/GameFrame.fxml")
    private val cityCanvas = ResizableCanvas()
    private val trafficCanvas = ResizableCanvas()
    private val zotCanvas = ResizableCanvas()

    private val canvasPane: StackPane by fxid("canvasStackPane")
    private val accordion: Accordion by fxid()
    private val toolsPane: TitledPane by fxid()
    private val verticalScroll: ScrollBar by fxid()
    private val horizontalScroll: ScrollBar by fxid()

    private val mapPane: BorderPane by fxid()
    private val cityMapCanvas: CityMapCanvas = CityMapCanvas()

    // BUTTONS
    private val roadButton: ToggleButton by fxid()
    private val queryButton: ToggleButton by fxid()
    private val bulldozeButton: ToggleButton by fxid()
    private val residentialButton: ToggleButton by fxid()
    private val commercialButton: ToggleButton by fxid()
    private val industrialButton: ToggleButton by fxid()
    private val coalPowerButton: ToggleButton by fxid()
    private val nuclearPowerButton: ToggleButton by fxid()
    private val dezoneButton: ToggleButton by fxid()
    private val powerLinesButton: ToggleButton by fxid()
    private val jobCenterButton: ToggleButton by fxid()
    private val townWarehouseButton: ToggleButton by fxid()
    private val fireStationButton: ToggleButton by fxid()
    private val routesButton: ToggleButton by fxid()
    private val recenterButton: ToggleButton by fxid()

    // cityMap modes...
    private val normalMapMode: RadioMenuItem by fxid()
    private val coalMapMode: RadioMenuItem by fxid()
    private val oilMapMode: RadioMenuItem by fxid()
    private val goldMapMode: RadioMenuItem by fxid()
    private val soilMapMode: RadioMenuItem by fxid()
    private val desirabilityMapMode: RadioMenuItem by fxid()
    private val fireCoverageMapMode: RadioMenuItem by fxid()
    private val trafficMapMode: RadioMenuItem by fxid()
    private val happinessMapMode: RadioMenuItem by fxid()

    private val selectedToolLabel: Label by fxid()
    private val cityNameLabel: Label by fxid()
    private val clockLabel: Label by fxid()

    private val supplyDemandMenuItem: MenuItem by fxid()

    private val pauseMenuItem: CheckMenuItem by fxid()

    var gameSpeed = GameSpeed.MEDIUM
        set(value) {
            field = value
            scheduleGameTickTimer()
        }

    var ticks = 0
    private var renderTimer: AnimationTimer? = null
    private var gameTickTimer: Timer = Timer()
    private var gameTickTask: TimerTask? = null
    private lateinit var assetManager: AssetManager
    private var lastMapRender = System.currentTimeMillis()

    var activeTool: Tool = Tool.QUERY
        set(value) {
            field = value
            selectedToolLabel.text = "Selected: $value"
        }

    private lateinit var map: CityMap
    private var cityRenderer: CityRenderer? = null
    private var trafficRenderer: TrafficRenderer? = null
    private var zotRenderer: ZotRenderer? = null

    override fun onDock() {
        currentStage?.setOnCloseRequest {
            quitPressed()
            it.consume()
        }
    }

    fun setMap(cityMap: CityMap) {

        this.map = cityMap
        cityMapCanvas.map = cityMap
        this.assetManager = AssetManager(cityMap)
        // gotta resize the component now...
        setScrollbarSizes()
        setCanvasSize()
        initComponents()

        title = "$GAME_STRING - ${cityMap.cityName}"
        cityNameLabel.text = cityMap.cityName

        // clean up the old renderers here...
        this.cityRenderer?.removePanListeners()
        this.trafficRenderer?.stop()
        this.zotRenderer?.stop()

        // allocate new ones...
        val cityRenderer = CityRenderer(this, cityCanvas, cityMap)
        val trafficRenderer = TrafficRenderer(cityMap, cityRenderer, trafficCanvas)
        val zotRenderer = ZotRenderer(cityMap, cityRenderer, zotCanvas)

        // now stand em up...
        this.cityRenderer = cityRenderer
        this.trafficRenderer = trafficRenderer
        this.zotRenderer = zotRenderer

        cityRenderer.addPanListener { visibleBlockRange ->
            // println("We have moved the cityMap around. Telling the minimal to highlight: $visibleBlockRange")
            this.cityMapCanvas.visibleBlockRange = visibleBlockRange
            trafficRenderer.visibleBlockRange = visibleBlockRange
            zotRenderer.visibleBlockRange = visibleBlockRange
        }
        this.cityMapCanvas.visibleBlockRange = this.cityRenderer?.visibleBlockRange(padding = 0)
        trafficRenderer.visibleBlockRange = this.cityRenderer?.visibleBlockRange(padding = 0)
        zotRenderer.visibleBlockRange = this.cityRenderer?.visibleBlockRange(padding = 0)

    }

    private fun setCanvasSize() {
        // println("cityMap size is: ${this.map.width},${this.map.height}")
        // println("Canvas pane size is: ${canvasPane.width},${canvasPane.height}")
        cityCanvas.prefHeight(canvasPane.height - 20)
        cityCanvas.prefWidth(canvasPane.width - 20)
        AnchorPane.setTopAnchor(cityCanvas, 0.0)
        AnchorPane.setBottomAnchor(cityCanvas, 20.0)
        AnchorPane.setLeftAnchor(cityCanvas, 0.0)
        AnchorPane.setRightAnchor(cityCanvas, 20.0)
    }

    private fun setScrollbarSizes() {
        // TODO: don't let us scroll off the edge...
        horizontalScroll.min = 0.0
        verticalScroll.min = 0.0

        horizontalScroll.max = getMap().width.toDouble()
        verticalScroll.max = getMap().height.toDouble()
    }

    private fun getMap(): CityMap {
        return this.map
    }

    fun slowClicked() {
        gameSpeed = GameSpeed.SLOW
    }

    fun mediumClicked() {
        gameSpeed = GameSpeed.MEDIUM
    }

    fun fastClicked() {
        gameSpeed = GameSpeed.FAST
    }


    fun zoomOut() {
        cityRenderer?.let {
            it.zoom -= 1
            if (it.zoom < 1) {
                it.zoom = 1.0
            }
        }
    }

    fun zoomIn() {
        cityRenderer?.let {
            it.zoom += 1
            if (it.zoom > 5.0) {
                it.zoom = 5.0
            }
        }
    }

    fun saveCity() {
        // make sure we have a filename to save to... otherwise just bounce to saveCityAs()...
        if (map.fileName != null) {
            CityFileAdapter.save(map, File(map.fileName))
            saveMessageBox()
        } else {
            saveCityAs()
        }
    }

    fun newCityPressed() {
        println("New city was pressed!")
        this.currentStage?.close()
        val launchScreen = tornadofx.find(LaunchScreen::class)
        renderTimer?.stop()
        gameTickTask?.cancel()
        launchScreen.openWindow()
    }

    fun saveCityAs() {
        val fileChooser = FileChooser()
        fileChooser.title = "Save your city"
        fileChooser.extensionFilters.addAll(
            FileChooser.ExtensionFilter("KotCity City", "*.kcity")
        )
        fileChooser.initialFileName = map.suggestedFilename()
        val file = fileChooser.showSaveDialog(this.primaryStage)
        CityFileAdapter.save(map, file)
        map.fileName = file.absoluteFile.toString()

        saveMessageBox()
    }

    private fun saveMessageBox() {
        val alert = Alert(AlertType.INFORMATION)
        alert.title = "City Saved"
        alert.headerText = "City Saved OK!"
        alert.height = 200.0
        alert.width = 400.0
        alert.dialogPane.content = Label("Everything went great. Your city is saved to ${map.fileName}.")
        alert.showAndWait()
    }

    fun bindButtons() {
        roadButton.setOnAction { activeTool = Tool.ROAD }
        queryButton.setOnAction { activeTool = Tool.QUERY }
        bulldozeButton.setOnAction { activeTool = Tool.BULLDOZE }
        recenterButton.setOnAction { activeTool = Tool.RECENTER }
        residentialButton.setOnAction { activeTool = Tool.RESIDENTIAL_ZONE }
        commercialButton.setOnAction { activeTool = Tool.COMMERCIAL_ZONE }
        industrialButton.setOnAction { activeTool = Tool.INDUSTRIAL_ZONE }
        coalPowerButton.setOnAction { activeTool = Tool.COAL_POWER_PLANT }
        dezoneButton.setOnAction { activeTool = Tool.DEZONE }
        powerLinesButton.setOnAction { activeTool = Tool.POWER_LINES }
        nuclearPowerButton.setOnAction { activeTool = Tool.NUCLEAR_POWER_PLANT }
        townWarehouseButton.setOnAction { activeTool = Tool.TOWN_WAREHOUSE }
        jobCenterButton.setOnAction { activeTool = Tool.JOB_CENTER }
        fireStationButton.setOnAction { activeTool = Tool.FIRE_STATION }
        routesButton.setOnAction { activeTool = Tool.ROUTES }
        supplyDemandMenuItem.setOnAction {
            val supplyDemandChart = find(SupplyDemandChart::class)
            supplyDemandChart.census = cityMapCanvas.map?.censusTaker
            supplyDemandChart.openWindow()
        }
    }

    private fun setMapModes(mapMode: MapMode) {
        cityRenderer?.mapMode = mapMode
        cityMapCanvas.mode = mapMode
    }

    private fun bindMapModes() {
        normalMapMode.setOnAction {
            setMapModes(MapMode.NORMAL)
        }
        oilMapMode.setOnAction {
            setMapModes(MapMode.OIL)
        }
        goldMapMode.setOnAction {
            setMapModes(MapMode.GOLD)
        }
        coalMapMode.setOnAction {
            setMapModes(MapMode.COAL)
        }
        soilMapMode.setOnAction {
            setMapModes(MapMode.SOIL)
        }
        desirabilityMapMode.setOnAction {
            setMapModes(MapMode.DESIRABILITY)
        }
        fireCoverageMapMode.setOnAction {
            setMapModes(MapMode.FIRE_COVERAGE)
        }
        trafficMapMode.setOnAction {
            setMapModes(MapMode.TRAFFIC)
        }
        happinessMapMode.setOnAction {
            setMapModes(MapMode.HAPPINESS)
        }
    }

    fun loadCityPressed() {
        // TODO: wrap this with a dialog...
        // we will just be loading...
        this.map.purgeRTree()
        this.currentStage?.close()
        renderTimer?.stop()
        gameTickTask?.cancel()
        CityLoader.loadCity(this)
        title = "$GAME_STRING - ${map.cityName}"
    }

    init {

    }

    private fun initComponents() {
        title = GAME_STRING

        bindCanvas()
        bindButtons()
        bindMapModes()

        mapPane.center = cityMapCanvas
        accordion.expandedPane = toolsPane

        renderTimer?.stop()
        renderTimer = object : AnimationTimer() {
            override fun handle(now: Long) {
                if (ticks == TICK_DELAY) {
                    trafficRenderer?.render()
                    cityRenderer?.render()
                    zotRenderer?.render()
                    ticks = 0
                }
                // only render map each while...
                val delta = System.currentTimeMillis() - lastMapRender
                if (delta > 10000) {
                    cityMapCanvas.render()
                    lastMapRender = System.currentTimeMillis()
                }
                ticks++
            }
        }
        renderTimer?.start()

        scheduleGameTickTimer()
    }

    private fun scheduleGameTickTimer() {
        val delay = 0L // delay for 0 sec.

        val period = when (gameSpeed) {
            GameSpeed.SLOW -> 250L
            GameSpeed.MEDIUM -> 125L
            GameSpeed.FAST -> 50L
        }
        gameTickTask?.cancel()
        gameTickTask = object : TimerTask() {
            override fun run() {
                runLater {
                    if (!pauseMenuItem.isSelected) {
                        map.tick()
                        clockLabel.text = serializeDate(map.time)
                    }
                }
            }
        }

        gameTickTimer.scheduleAtFixedRate(gameTickTask, delay, period)
    }

    fun quitPressed() {
        val alert = Alert(AlertType.CONFIRMATION)
        alert.title = "Quitting KotCity"
        alert.headerText = "Are you ready to leave?"
        alert.contentText = "Please confirm..."

        val buttonTypeOne = ButtonType("Yes, please quit.")
        val buttonTypeTwo = ButtonType("No, I want to keep playing.")
        val buttonTypeCancel = ButtonType("Cancel", ButtonData.CANCEL_CLOSE)

        alert.buttonTypes.setAll(buttonTypeOne, buttonTypeTwo, buttonTypeCancel)

        val result = alert.showAndWait()
        when {
            result.get() == buttonTypeOne -> System.exit(1)
            result.get() == buttonTypeTwo -> {
                // don't do anything...
            }
            else -> {
                // don't do anything..
            }
        }
    }

    private fun bindCanvas() {

        // TODO: we are handling scrolling ourself... so we have to figure out what's
        //       visible and what's not...
        cityCanvas.prefHeight(canvasPane.height - 20)
        cityCanvas.prefWidth(canvasPane.width - 20)

        canvasPane.add(cityCanvas)

        trafficCanvas.let {
            it.isMouseTransparent = true
            canvasPane.add(it)
            it.prefHeight(canvasPane.height - 20)
            it.prefWidth(canvasPane.width - 20)
            debug("Traffic canvas was added!")
        }

        zotCanvas.let {
            it.isMouseTransparent = true
            canvasPane.add(it)
            it.prefHeight(canvasPane.height - 20)
            it.prefWidth(canvasPane.width - 20)
            debug("Zot canvas was added!")
        }

        canvasPane.widthProperty().addListener { _, _, newValue ->
            cityCanvas.width = newValue.toDouble()
            setCanvasSize()
            setScrollbarSizes()
            cityMapCanvas.render()
        }

        cityCanvas.setOnMouseMoved { evt ->
            cityRenderer?.onMouseMoved(evt)
        }

        cityCanvas.setOnMousePressed { evt ->
            cityRenderer?.onMousePressed(evt)
        }

        cityCanvas.setOnMouseReleased { evt ->
            cityMapCanvas.render()
            cityRenderer?.let {
                it.onMouseReleased(evt)
                val (firstBlock, lastBlock) = it.blockRange()
                if (firstBlock != null && lastBlock != null) {
                    if (evt.button == MouseButton.PRIMARY) {
                        when (activeTool) {
                            Tool.ROAD -> {
                                map.buildRoad(firstBlock, lastBlock)
                            }
                            Tool.POWER_LINES -> map.buildPowerline(firstBlock, lastBlock)
                            Tool.BULLDOZE -> map.bulldoze(firstBlock, lastBlock)
                            Tool.RESIDENTIAL_ZONE -> map.zone(Zone.RESIDENTIAL, firstBlock, lastBlock)
                            Tool.COMMERCIAL_ZONE -> map.zone(Zone.COMMERCIAL, firstBlock, lastBlock)
                            Tool.INDUSTRIAL_ZONE -> map.zone(Zone.INDUSTRIAL, firstBlock, lastBlock)
                            Tool.RECENTER -> it.panMap(firstBlock)
                            Tool.DEZONE -> map.dezone(firstBlock, lastBlock)
                            Tool.QUERY -> {
                                // let's do that query...
                                val queryWindow = find(QueryWindow::class)
                                // get building under the active block...
                                queryWindow.mapAndCoordinate = Pair(map, firstBlock)
                                queryWindow.openModal()
                            }
                            Tool.ROUTES -> {
                                it.showRoutesFor = firstBlock
                            }
                            else -> {
                                println("Warning: tool $activeTool not handled...")
                            }
                        }
                    }
                }
            }

        }

        root.requestFocus()
        root.setOnKeyPressed {keyEvent ->
            cityRenderer?.apply {
                when (keyEvent.code) {
                    KeyCode.LEFT, KeyCode.A -> blockOffsetX -= 10
                    KeyCode.RIGHT, KeyCode.D -> blockOffsetX += 10
                    KeyCode.UP, KeyCode.W -> blockOffsetY -= 10
                    KeyCode.DOWN, KeyCode.S -> blockOffsetY += 10
                    else -> {
                        // noop...
                    }
                }
            }
        }

        cityCanvas.setOnMouseDragged { evt ->
            cityRenderer?.onMouseDragged(evt)
        }

        cityCanvas.setOnMouseClicked { evt ->
            cityRenderer?.onMouseClicked(evt)
            // now let's handle some tools...
            if (evt.button == MouseButton.PRIMARY) {
                when (activeTool) {
                    Tool.COAL_POWER_PLANT -> // TODO: we have to figure out some kind of offset for this shit...
                        // can't take place at hovered block...
                        cityRenderer?.getHoveredBlock()?.let {
                            val newX = it.x - 1
                            val newY = it.y - 1
                            map.build(PowerPlant("coal", map), BlockCoordinate(newX, newY))
                        }
                    Tool.NUCLEAR_POWER_PLANT -> cityRenderer?.getHoveredBlock()?.let {
                        val newX = it.x - 1
                        val newY = it.y - 1
                        map.build(PowerPlant("nuclear", map), BlockCoordinate(newX, newY))
                    }
                    Tool.JOB_CENTER -> cityRenderer?.getHoveredBlock()?.let {
                        val newX = it.x - 1
                        val newY = it.y - 1
                        val jobCenter = assetManager.buildingFor(Civic::class, "job_center")
                        map.build(jobCenter, BlockCoordinate(newX, newY))
                    }
                    Tool.FIRE_STATION -> cityRenderer?.getHoveredBlock()?.let {
                        val newX = it.x - 1
                        val newY = it.y - 1
                        map.build(FireStation(map), BlockCoordinate(newX, newY))
                    }
                    Tool.TOWN_WAREHOUSE -> cityRenderer?.getHoveredBlock()?.let {
                        val newX = it.x - 1
                        val newY = it.y - 1
                        val townWarehouse = assetManager.buildingFor(Civic::class, "town_warehouse")
                        map.build(townWarehouse, BlockCoordinate(newX, newY))
                    }
                }
            }

        }

        canvasPane.heightProperty().addListener { _, _, newValue ->
            cityCanvas.height = newValue.toDouble()
            setCanvasSize()
            setScrollbarSizes()
        }

        horizontalScroll.valueProperty().addListener { _, _, newValue ->
            cityRenderer?.blockOffsetX = newValue.toDouble()
        }

        verticalScroll.valueProperty().addListener { _, _, newValue ->
            cityRenderer?.blockOffsetY = newValue.toDouble()
        }

        with(cityCanvas) {
            this.setOnScroll { scrollEvent ->
                if (scrollEvent.deltaY < 0) {
                    zoomOut()
                } else if (scrollEvent.deltaY > 0) {
                    zoomIn()
                }
                cityMapCanvas.render()
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
        stage.isMaximized = true
        super.start(stage)
    }
}

fun main(args: Array<String>) {
    Application.launch(GameFrameApp::class.java, *args)
}