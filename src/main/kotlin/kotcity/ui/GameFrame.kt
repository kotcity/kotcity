package kotcity.ui

import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.scene.control.*
import javafx.scene.control.Alert.AlertType
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.StackPane
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.stage.WindowEvent
import kotcity.data.*
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
import kotlin.concurrent.timerTask
import javafx.scene.control.ButtonType
import javafx.scene.control.Alert
import javafx.scene.layout.Region
import kotcity.util.randomElement


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
const val TICK_DELAY_AT_REST: Int = 5 // only render every 5 ticks... (framerate limiter)
const val TICK_DELAY_AT_MOVE: Int = 1 // only render every tick when moving the camera around

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
    POLICE_STATION,
    ROUTES, RECENTER
}

enum class GameSpeed(val tickPeriod: Long) {
    SLOW(250),
    MEDIUM(125),
    FAST(50)
}

class GameFrame : View(), Debuggable {
    override var debug: Boolean = false
    override val root: BorderPane by fxml("/GameFrame.fxml")
    private val cityCanvas = ResizableCanvas()
    private val trafficCanvas = ResizableCanvas()
    private val zotCanvas = ResizableCanvas()

    private val canvasPane: StackPane by fxid("canvasStackPane")
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
    private val policeStationButton: ToggleButton by fxid()
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
    private val crimeMapMode: RadioMenuItem by fxid()
    private val trafficMapMode: RadioMenuItem by fxid()
    private val happinessMapMode: RadioMenuItem by fxid()

    private val selectedToolLabel: Label by fxid()
    private val cityNameLabel: Label by fxid()
    private val clockLabel: Label by fxid()
    private val demandLabel: Label by fxid()
    private val populationLabel: Label by fxid()

    private val supplyDemandMenuItem: MenuItem by fxid()

    private val pauseMenuItem: CheckMenuItem by fxid()

    private var gameSpeed = GameSpeed.MEDIUM
        set(value) {
            field = value
            scheduleGameTickTimer()
        }

    private var tickDelay = TICK_DELAY_AT_REST
    private var ticks = 0

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

    private val quitMessages = listOf(
            "You want to quit?\n" + "Then, thou hast lost an eighth!",
            "Don't go now, there's a\n" +
            "dimensional shambler waiting\n" +
            "at the DOS prompt!",
            "Get outta here and go back to your boring programs.",
            "Are you sure you want to quit this great game?",
            "You're trying to say you like DOS\n" +
                    "better than me, right? "
    )

    override fun onDock() {
        super.onDock()
        currentWindow?.sizeToScene()
        currentWindow?.centerOnScreen()
    }

    fun setMap(cityMap: CityMap) {
        this.map = cityMap
        cityMapCanvas.map = cityMap
        this.assetManager = AssetManager(cityMap)
        // gotta resize the component now...
        setScrollbarSizes()
        setCanvasSize()
        initComponents()

        title = "$GAME_TITLE - ${cityMap.cityName}"
        cityNameLabel.text = "City: ${cityMap.cityName}"

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

            horizontalScroll.value = cityRenderer.blockOffsetX
            verticalScroll.value = cityRenderer.blockOffsetY
        }
        val visibleBlockRange = this.cityRenderer?.visibleBlockRange()
        this.cityMapCanvas.visibleBlockRange = visibleBlockRange
        trafficRenderer.visibleBlockRange = visibleBlockRange
        zotRenderer.visibleBlockRange = visibleBlockRange
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

        val canvasBlockWidth = cityRenderer?.canvasBlockWidth() ?: 0
        val canvasBlockHeight = cityRenderer?.canvasBlockHeight() ?: 0
        horizontalScroll.max = map.width - canvasBlockWidth - 1.0
        verticalScroll.max = map.height - canvasBlockHeight - 1.0

        horizontalScroll.visibleAmount = horizontalScroll.max * canvasBlockWidth / map.width
        verticalScroll.visibleAmount = verticalScroll.max * canvasBlockHeight / map.height
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
            it.zoom = Math.max(it.zoom - 1, 1.0)
            setScrollbarSizes()
        }
    }

    fun zoomIn() {
        cityRenderer?.let {
            it.zoom = Math.min(it.zoom + 1, 5.0)
            setScrollbarSizes()
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
        Alert(AlertType.INFORMATION).apply {
            title = "City Saved"
            headerText = "City Saved OK!"
            height = 200.0
            width = 400.0
            dialogPane.content = Label("Everything went great. Your city is saved to ${map.fileName}.")
            showAndWait()
        }
    }

    private fun bindButtons() {
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
        policeStationButton.setOnAction { activeTool = Tool.POLICE_STATION }
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
        crimeMapMode.setOnAction {
            setMapModes(MapMode.CRIME)
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
        title = "$GAME_TITLE - ${map.cityName}"
    }

    private fun initComponents() {
        title = GAME_TITLE

        bindCanvas()
        bindButtons()
        bindMapModes()

        mapPane.center = cityMapCanvas

        renderTimer?.stop()
        renderTimer = object : AnimationTimer() {
            override fun handle(now: Long) {
                if (ticks >= tickDelay) {
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
        gameTickTask?.cancel()
        gameTickTask = timerTask {
            runLater {
                if (!pauseMenuItem.isSelected) {
                    map.tick()
                    populationLabel.text = "Population: ${map.censusTaker.population}"
                    clockLabel.text = serializeDate(map.time)
                }
            }
        }
        gameTickTimer.scheduleAtFixedRate(gameTickTask, 0L, gameSpeed.tickPeriod)
    }

    fun quitPressed() {

        val alert = Alert(AlertType.CONFIRMATION)
        alert.title = "Are you sure you want to quit?"
        alert.headerText = "Confirm your action."
        alert.contentText = quitMessages.randomElement()
        alert.dialogPane.minHeight = Region.USE_PREF_SIZE

        val result = alert.showAndWait()
        if (result.get() === ButtonType.OK) {
            System.exit(1)
        }

    }

    private fun bindCanvas() {
        // TODO: we are handling scrolling ourself... so we have to figure out what's visible and what's not...
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
            tickDelay = TICK_DELAY_AT_REST
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
        root.setOnKeyPressed { keyEvent ->
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
            tickDelay = TICK_DELAY_AT_MOVE
            cityRenderer?.onMouseDragged(evt)
        }

        cityCanvas.setOnMouseClicked { evt ->
            // now let's handle some tools...
            if (evt.button == MouseButton.PRIMARY) {
                cityRenderer?.getHoveredBlock()?.let {
                    val newX = it.x - 1
                    val newY = it.y - 1
                    when (activeTool) {
                        Tool.COAL_POWER_PLANT -> {
                            // TODO: we have to figure out some kind of offset for this shit...
                            // can't take place at hovered block...
                            map.build(PowerPlant("coal", map), BlockCoordinate(newX, newY))
                        }
                        Tool.NUCLEAR_POWER_PLANT -> {
                            map.build(PowerPlant("nuclear", map), BlockCoordinate(newX, newY))
                        }
                        Tool.JOB_CENTER -> {
                            val jobCenter = assetManager.buildingFor(Civic::class, "job_center")
                            map.build(jobCenter, BlockCoordinate(newX, newY))
                        }
                        Tool.FIRE_STATION -> {
                            map.build(FireStation(map), BlockCoordinate(newX, newY))
                        }
                        Tool.POLICE_STATION -> {
                            map.build(PoliceStation(map), BlockCoordinate(newX, newY))
                        }
                        Tool.TOWN_WAREHOUSE -> {
                            val townWarehouse = assetManager.buildingFor(Civic::class, "town_warehouse")
                            map.build(townWarehouse, BlockCoordinate(newX, newY))
                        }
                        else -> {
                            // We handle the other tools on a different place
                        }
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

        cityCanvas.setOnScroll { scrollEvent ->
            if (scrollEvent.deltaY < 0) {
                zoomOut()
            } else if (scrollEvent.deltaY > 0) {
                zoomIn()
            }
            cityRenderer?.panToMouse()
            cityMapCanvas.render()
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
