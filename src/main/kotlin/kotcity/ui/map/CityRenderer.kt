package kotcity.ui.map

import javafx.scene.Cursor
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import kotcity.data.*
import kotcity.data.MapMode.*
import kotcity.ui.*
import kotcity.ui.sprites.BuildingSpriteLoader
import kotcity.util.reorder
import tornadofx.runLater

const val MAX_BUILDING_SIZE = 4
const val DESIRABILITY_CAP: Double = 10.0
// the coal power plant is the biggest...

class CityRenderer(
    private val gameFrame: GameFrame,
    val canvas: ResizableCanvas,
    private val cityMap: CityMap
) {

    var zoom = 1.0
        set(value) {
            val oldCenter = centerBlock()
            field = value
            panMap(oldCenter)
        }

    private val happinessRenderer = HappinessRenderer(this, cityMap)

    private val fireCoverageRenderer = FireCoverageRenderer(this, cityMap)

    private val crimeRenderer = CrimeRenderer(this, cityMap)

    private val trafficRenderer = TrafficRenderer(this, cityMap)

    var blockOffsetX: Double = 0.0
        set(value) {
            val newValue = value.coerceIn(0.0..(cityMap.width - canvasBlockWidth() - 1.0))
            field = newValue
        }
    var blockOffsetY: Double = 0.0
        set(value) {
            val newValue = value.coerceIn(0.0..(cityMap.height - canvasBlockHeight() - 1.0))
            field = newValue
        }

    private var mapMinElevation = 0.0
    private var mapMaxElevation = 1.0
    private var colorAdjuster: ColorAdjuster = ColorAdjuster(mapMinElevation, mapMaxElevation)

    private var mouseDown = false
    private var mouseBlock: BlockCoordinate? = null
    private var firstBlockPressed: BlockCoordinate? = null

    var mapMode: MapMode = NORMAL

    var showRoutesFor: BlockCoordinate? = null

    private val panListeners: MutableList<(Pair<BlockCoordinate, BlockCoordinate>) -> Unit> = mutableListOf()

    init {
        mapMinElevation = cityMap.groundLayer.values.map { it.elevation }.min() ?: 0.0
        mapMaxElevation = cityMap.groundLayer.values.map { it.elevation }.max() ?: 0.0
        colorAdjuster = ColorAdjuster(mapMinElevation, mapMaxElevation)
    }

    fun canvasBlockHeight() = (canvas.height / blockSize()).toInt()

    fun canvasBlockWidth() = (canvas.width / blockSize()).toInt()

    // awkward... we need padding to get building off the screen...
    fun visibleBlockRange(padding: Int = 0): Pair<BlockCoordinate, BlockCoordinate> {
        val startBlockX = blockOffsetX.toInt() - padding
        val startBlockY = blockOffsetY.toInt() - padding
        var endBlockX = startBlockX + canvasBlockWidth() + padding
        var endBlockY = startBlockY + canvasBlockHeight() + padding

        endBlockX = Math.min(endBlockX, cityMap.width)
        endBlockY = Math.min(endBlockY, cityMap.height)

        val startCoordinate = BlockCoordinate(startBlockX, startBlockY)
        val endCoordinate = BlockCoordinate(endBlockX, endBlockY)

        return startCoordinate to endCoordinate
    }

    fun panToMouse() {
        mouseBlock?.let { panMap(it) }
    }

    fun panMap(coordinate: BlockCoordinate) {
        // OK, we want to figure out the CENTER block now...
        val centerBlock = centerBlock()
        val dx = coordinate.x - centerBlock.x
        val dy = coordinate.y - centerBlock.y

        blockOffsetX += dx
        blockOffsetY += dy
        firePanChanged()
    }

    fun addPanListener(listener: (Pair<BlockCoordinate, BlockCoordinate>) -> Unit) {
        this.panListeners.add(listener)
    }

    fun removePanListeners() {
        this.panListeners.clear()
    }

    private fun firePanChanged() {
        this.panListeners.forEach {
            it(this.visibleBlockRange())
        }
    }

    private fun centerBlock(): BlockCoordinate {
        val centerX = blockOffsetX + (canvasBlockWidth() / 2)
        val centerY = blockOffsetY + (canvasBlockHeight() / 2)
        return BlockCoordinate(centerX.toInt(), centerY.toInt())
    }

    // returns the first and last block that we dragged from / to
    fun blockRange() = this.firstBlockPressed to this.mouseBlock

    private fun mouseToBlock(mouseX: Double, mouseY: Double): BlockCoordinate {
        // OK... this should be pretty easy...
        val blockSize = blockSize()
        val blockX = (mouseX / blockSize).toInt()
        val blockY = (mouseY / blockSize).toInt()

        return BlockCoordinate(blockX + blockOffsetX.toInt(), blockY + blockOffsetY.toInt())
    }

    fun onMousePressed(event: MouseEvent) {
        this.mouseDown = event.isPrimaryButtonDown
        this.firstBlockPressed = mouseToBlock(event.x, event.y)
        this.mouseBlock = this.firstBlockPressed

        runLater {
            if (event.button == MouseButton.SECONDARY) {
                gameFrame.currentStage?.scene?.root?.cursor = Cursor.MOVE
            } else {
                gameFrame.currentStage?.scene?.root?.cursor = Cursor.DEFAULT
            }
        }

    }

    fun onMouseReleased(event: MouseEvent) {
        this.mouseDown = event.isPrimaryButtonDown
        runLater {
            gameFrame.currentStage?.scene?.root?.cursor = Cursor.DEFAULT
        }

    }

    fun onMouseDragged(event: MouseEvent) {
        updateMouseBlock(event)
        if (event.button == MouseButton.SECONDARY) {
            runLater {
                gameFrame.currentStage?.scene?.root?.cursor = Cursor.MOVE
            }

            val startX = firstBlockPressed?.x ?: 0
            val startY = firstBlockPressed?.y ?: 0
            val currentX = mouseBlock?.x ?: 0
            val currentY = mouseBlock?.y ?: 0
            if (startX != startY || currentX != currentY) {
                blockOffsetX += startX - currentX
                blockOffsetY += startY - currentY
                firePanChanged()
            }
        }
    }

    private fun updateMouseBlock(event: MouseEvent) {
        this.mouseBlock = mouseToBlock(event.x, event.y)
    }

    fun getHoveredBlock() = this.mouseBlock

    fun onMouseMoved(event: MouseEvent) {
        // OK... if we have an active tool we might
        // have to draw a building highlight
        updateMouseBlock(event)
    }

    private fun drawMap() {
        // we got that cityMap...
        val (startBlock, endBlock) = visibleBlockRange()
        val blockSize = blockSize()

        val xRange = (startBlock.x..endBlock.x)
        val yRange = (startBlock.y..endBlock.y)

        // this handles the offset...
        xRange.toList().forEachIndexed { xi, x ->
            yRange.toList().forEachIndexed { yi, y ->
                val tile = cityMap.groundLayer[BlockCoordinate(x, y)]
                if (tile != null) {
                    val adjustedColor = colorAdjuster.colorForTile(tile)
                    canvas.graphicsContext2D.fill = adjustedColor

                    canvas.graphicsContext2D.fillRect(
                        xi * blockSize,
                        yi * blockSize,
                        blockSize, blockSize
                    )

                    if (DRAW_GRID && zoom >= 3.0) {
                        canvas.graphicsContext2D.fill = Color.BLACK
                        canvas.graphicsContext2D.strokeRect(xi * blockSize, yi * blockSize, blockSize, blockSize)
                    }
                }
            }
        }
    }

    private fun fillBlocks(blockX: Int, blockY: Int, width: Int, height: Int) {
        for (y in blockY until blockY + height) {
            for (x in blockX until blockX + width) {
                highlightBlock(x, y)
            }
        }
    }

    fun render() {
        canvas.graphicsContext2D.fill = Color.BLACK
        canvas.graphicsContext2D.fillRect(0.0, 0.0, canvas.width, canvas.height)

        drawMap()
        drawZones()
        drawBuildings()

        if (gameFrame.activeTool == Tool.ROUTES) {
            showRoutesFor?.let {
                drawRoutes(it)
            }
        }

        when (mapMode) {
            SOIL, COAL, GOLD, OIL -> drawResources()
            FIRE_COVERAGE -> fireCoverageRenderer.render()
            CRIME -> crimeRenderer.render()
            DESIRABILITY -> drawDesirability()
            TRAFFIC -> trafficRenderer.render()
            HAPPINESS -> happinessRenderer.render()
            NORMAL -> {
                // We don't have to render anything special when in normal mode
            }
        }
        drawHighlights()
    }

    private fun drawRoutes(showRoutesFor: BlockCoordinate) {
        // ok... we know the coordinate we are interested in... so let's find ALL contracts that involve it...
        // this is going to be super gross...

        // we might be working with a big building here...
        val buildingBlocks = cityMap.locationsAt(showRoutesFor)

        val contracts = (cityMap.locations()).plus(buildingBlocks).distinct()
            .map { contractsWithPathThrough(it.building, showRoutesFor) }.flatten()
        // ok now we know which ones to draw...
        val blocksWithPath = contracts.flatMap { it.path?.blocks() ?: emptyList() }.distinct().toSet()
        // now get only ones on screen...

        val (startBlock, endBlock) = visibleBlockRange()
        BlockCoordinate.iterate(startBlock, endBlock) { blockCoordinate ->
            if (blocksWithPath.contains(blockCoordinate)) {
                highlightBlock(blockCoordinate.x, blockCoordinate.y)
            }
        }

    }

    // TODO: this is probably brutally slow...
    private fun contractsWithPathThrough(building: Building, coordinate: BlockCoordinate): List<Contract> {
        return building.contracts.toList().filter { it.path?.blocks()?.contains(coordinate) ?: false }
    }

    private fun drawDesirability() {
        val (startBlock, endBlock) = visibleBlockRange()

        BlockCoordinate.iterate(startBlock, endBlock) { coord ->
            val desirabilityScores = cityMap.desirabilityLayers.map {
                it[coord]
            }

            val maxDesirability = desirabilityScores.filterNotNull().max() ?: 0.0

            val tx = coord.x - blockOffsetX
            val ty = coord.y - blockOffsetY
            val blockSize = blockSize()
            canvas.graphicsContext2D.fill = colorValue(maxDesirability, DESIRABILITY_CAP)
            canvas.graphicsContext2D.fillRect(tx * blockSize, ty * blockSize, blockSize, blockSize)
        }
    }

    internal fun interpolateColor(color1: java.awt.Color, color2: java.awt.Color, fraction: Float): Color {
        var colorFraction = fraction
        val intToFloatConst = 1f / 255f
        colorFraction = Math.min(colorFraction, 1f)
        colorFraction = Math.max(colorFraction, 0f)

        val red1 = (color1.red * intToFloatConst)
        val green1 = (color1.green * intToFloatConst)
        val blue1 = (color1.blue * intToFloatConst)
        val alpha1 = color1.alpha * intToFloatConst

        val red2 = (color2.red * intToFloatConst)
        val green2 = (color2.green * intToFloatConst)
        val blue2 = (color2.blue * intToFloatConst)
        val alpha2 = color2.alpha * intToFloatConst

        val deltaRed = red2 - red1
        val deltaGreen = green2 - green1
        val deltaBlue = blue2 - blue1
        val deltaAlpha = alpha2 - alpha1

        var red = red1 + deltaRed * colorFraction
        var green = green1 + deltaGreen * colorFraction
        var blue = blue1 + deltaBlue * colorFraction
        var alpha = alpha1 + deltaAlpha * colorFraction

        red = Math.min(red, 1f)
        red = Math.max(red, 0f)
        green = Math.min(green, 1f)
        green = Math.max(green, 0f)
        blue = Math.min(blue, 1f)
        blue = Math.max(blue, 0f)
        alpha = Math.min(alpha, 1f)
        alpha = Math.max(alpha, 0f)

        return Color(red.toDouble(), green.toDouble(), blue.toDouble(), alpha.toDouble())
    }

    fun colorValue(value: Double, max: Double): Color {
        val color1 = java.awt.Color.RED
        val color2 = java.awt.Color.GREEN
        // gotta clamp value between 0.0f and max...
        val fraction = Algorithms.scale(value.coerceAtMost(max), 0.00, max, 0.0, 1.0)
        val newColor = interpolateColor(color1, color2, fraction.toFloat())
        return Color(newColor.red, newColor.green, newColor.blue, 0.5)
    }

    private fun resourceLayer(mode: MapMode): QuantizedMap<Double>? {
        return when (mode) {
            COAL -> cityMap.resourceLayers["coal"]
            OIL -> cityMap.resourceLayers["oil"]
            GOLD -> cityMap.resourceLayers["gold"]
            SOIL -> cityMap.resourceLayers["soil"]
            else -> null
        }
    }

    private fun drawResources() {

        val (startBlock, endBlock) = visibleBlockRange()

        val layer = resourceLayer(this.mapMode) ?: return

        BlockCoordinate.iterate(startBlock, endBlock) { coord ->
            val tile = layer[coord]
            tile?.let {

                if (tile > 0.5) {
                    val tx = coord.x - blockOffsetX
                    val ty = coord.y - blockOffsetY
                    val blockSize = blockSize()
                    canvas.graphicsContext2D.fill = Color.LIGHTGOLDENRODYELLOW
                    canvas.graphicsContext2D.fillRect(tx * blockSize, ty * blockSize, blockSize, blockSize)
                }

            }
        }
    }

    private fun drawHighlights() {
        mouseBlock?.let {
            if (mouseDown) {
                when (gameFrame.activeTool) {
                    Tool.ROAD -> drawRoadBlueprint()
                    Tool.POWER_LINES -> drawRoadBlueprint()
                    Tool.RESIDENTIAL_ZONE,
                    Tool.COMMERCIAL_ZONE,
                    Tool.INDUSTRIAL_ZONE,
                    Tool.DEZONE,
                    Tool.BULLDOZE -> firstBlockPressed?.let { first ->
                        highlightBlocks(first, it)
                    }
                    else -> Unit
                }
            } else {
                when (gameFrame.activeTool) {
                    Tool.COAL_POWER_PLANT,
                    Tool.NUCLEAR_POWER_PLANT -> {
                        highlightCenteredBlocks(it, 4, 4)
                    }
                    Tool.ROAD,
                    Tool.DEZONE,
                    Tool.POWER_LINES,
                    Tool.QUERY,
                    Tool.RECENTER,
                    Tool.BULLDOZE,
                    Tool.COMMERCIAL_ZONE,
                    Tool.INDUSTRIAL_ZONE,
                    Tool.RESIDENTIAL_ZONE,
                    Tool.ROUTES -> {
                        it.let { highlightBlocks(it, it) }
                    }
                    Tool.JOB_CENTER,
                    Tool.TOWN_WAREHOUSE -> {
                        it.let { highlightCenteredBlocks(it, 2, 2) }
                    }
                    Tool.POLICE_STATION,
                    Tool.FIRE_STATION -> {
                        it.let { highlightCenteredBlocks(it, 3, 3) }
                    }
                }
            }
        }
    }

    // TODO: so ugly to have every size hardcoded...
    private fun highlightCenteredBlocks(start: BlockCoordinate, width: Int, height: Int) {
        // TODO: we want to make this shit kind of centered...
        if (width == 3 || height == 3) {
            val offsetX = (width / 2)
            val offsetY = (height / 2)
            val newBlock = BlockCoordinate(start.x - offsetX, start.y - offsetY)
            highlightBlocks(newBlock, width, height)
        } else if (width == 2 || height == 2) {
            val offsetX = (width / 2)
            val offsetY = (height / 2)
            val newBlock = BlockCoordinate(start.x - offsetX, start.y - offsetY)
            highlightBlocks(newBlock, width, height)
        } else {
            val offsetX = (width / 2) - 1
            val offsetY = (height / 2) - 1
            val newBlock = BlockCoordinate(start.x - offsetX, start.y - offsetY)
            highlightBlocks(newBlock, width, height)
        }

    }

    private fun highlightBlocks(start: BlockCoordinate, width: Int, height: Int) {
        val startX = start.x
        val startY = start.y
        for (x in startX until startX + width) {
            for (y in startY until startY + height) {
                highlightBlock(x, y)
            }
        }
    }

    private fun highlightBlocks(from: BlockCoordinate, to: BlockCoordinate) {
        for (x in (from.x..to.x).reorder()) {
            for (y in (from.y..to.y).reorder()) {
                highlightBlock(x, y)
            }
        }
    }

    // padding brings us out to the top and left so that way we can catch the big buildings...
    // not very elegant...
    private fun visibleBlocks(padding: Int = 0): MutableList<BlockCoordinate> {
        val blockList = mutableListOf<BlockCoordinate>()
        val (from, to) = visibleBlockRange(padding = padding)
        BlockCoordinate.iterate(from, to) {
            blockList.add(it)
        }
        return blockList
    }

    private fun visibleLocations(): List<Location> {
        // TODO: we can just cityMap over the two different layers... clean up later...
        val (from, to) = visibleBlockRange(padding = MAX_BUILDING_SIZE)

        val locations = cityMap.locationsInRectangle(from, to)

        val powerLines = visibleBlocks(padding = MAX_BUILDING_SIZE).mapNotNull {
            val building = cityMap.powerLineLayer[it]
            if (building != null) {
                Location(it, building)
            } else {
                null
            }
        }
        return locations + powerLines
    }

    private fun drawBuildings() {

        // can we cache this shit at all???
        visibleLocations().forEach { location ->
            val coordinate = location.coordinate
            val building = location.building
            val tx = coordinate.x - blockOffsetX
            val ty = coordinate.y - blockOffsetY
            val blockSize = blockSize()
            when (building) {
                is Road -> {
                    canvas.graphicsContext2D.fill = Color.BLACK
                    canvas.graphicsContext2D.fillRect(tx * blockSize, ty * blockSize, blockSize, blockSize)
                }
                else -> {
                    drawBuildingType(building, tx, ty)
                }
            }
        }
    }

    private fun drawBuildingType(building: Building, tx: Double, ty: Double) {
        val blockSize = blockSize()
        val width = building.width * blockSize
        val height = building.height * blockSize
        drawBuildingBorder(building, tx, ty, width, height, blockSize)

        val shrink = blockSize * 0.10

        // OK... fucking THINK here...
        // blocksize will be like 64...

        val imgWidth = (building.width * blockSize) - (shrink * 2)
        val imgHeight = (building.height * blockSize) - (shrink * 2)

        BuildingSpriteLoader.spriteForBuildingType(building, imgWidth, imgHeight).let { img ->
            val ix = (tx * blockSize) + shrink
            val iy = (ty * blockSize) + shrink
            canvas.graphicsContext2D.drawImage(img, ix, iy)
        }
    }

    private fun drawBuildingBorder(
        building: Building,
        tx: Double,
        ty: Double,
        width: Double,
        height: Double,
        blockSize: Double
    ) {
        if (building is Road) {
            return
        }

        // this looks like shit when we are zoomed way out...
        val arcSize = arcWidth()
        canvas.graphicsContext2D.lineWidth = borderWidth()
        // we want to inset the stroke...
        val splitFactor = 40.0
        val sx = (tx * blockSize) + (width / splitFactor)
        val sy = (ty * blockSize) + (height / splitFactor)
        val ex = width - ((width / splitFactor) * 2)
        val ey = height - ((height / splitFactor) * 2)

        canvas.graphicsContext2D.fill = Color.WHITE
        canvas.graphicsContext2D.fillRoundRect(sx, sy, ex, ey, arcSize, arcSize)

        canvas.graphicsContext2D.stroke = building.borderColor
        canvas.graphicsContext2D.strokeRoundRect(sx, sy, ex, ey, arcSize, arcSize)
    }

    private fun drawZones() {
        val blockSize = blockSize()
        visibleBlocks().forEach { coordinate ->
            cityMap.zoneLayer[coordinate]?.let { zone ->
                // figure out fill color...
                val tx = coordinate.x - blockOffsetX
                val ty = coordinate.y - blockOffsetY
                val shadyColor = Color(zone.color.red, zone.color.green, zone.color.blue, 0.4)
                canvas.graphicsContext2D.fill = shadyColor
                canvas.graphicsContext2D.fillRect(tx * blockSize, ty * blockSize, blockSize, blockSize)
            }
        }
    }

    private fun highlightBlock(x: Int, y: Int) {
        canvas.graphicsContext2D.fill = Color(Color.MAGENTA.red, Color.MAGENTA.green, Color.MAGENTA.blue, 0.50)
        // gotta translate here...
        val tx = x - blockOffsetX
        val ty = y - blockOffsetY
        val blockSize = blockSize()
        canvas.graphicsContext2D.fillRect(tx * blockSize, ty * blockSize, blockSize, blockSize)
    }

    private fun arcWidth(): Double {
        return when (zoom) {
            1.0 -> 1.0
            2.0 -> 5.0
            3.0 -> 10.0
            4.0 -> 15.0
            5.0 -> 25.0
            else -> 1.0
        }
    }

    private fun borderWidth(): Double {
        return when (zoom) {
            1.0 -> 0.5
            2.0 -> 1.0
            3.0 -> 2.0
            4.0 -> 3.0
            5.0 -> 4.0
            else -> 1.0
        }
    }

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

    private fun drawRoadBlueprint() {
        // figure out if we are more horizontal or vertical away from origin point
        canvas.graphicsContext2D.fill = Color.YELLOW
        val startBlock = firstBlockPressed ?: return
        val endBlock = mouseBlock ?: return
        val x = startBlock.x
        val y = startBlock.y
        val x2 = endBlock.x
        val y2 = endBlock.y

        if (Math.abs(x - x2) > Math.abs(y - y2)) {
            // building horizontally
            // now fuck around with y2 so it's at the same level as y1
            // y2 = y

            if (x < x2) {
                fillBlocks(x, y, Math.abs(x - x2) + 1, 1)
            } else {
                fillBlocks(x2, y, Math.abs(x - x2) + 1, 1)
            }
        } else {
            // building vertically
            // now fuck around with x2 so it's at the same level as x1
            // x2 = x

            if (y < y2) {
                fillBlocks(x, y, 1, Math.abs(y - y2) + 1)
            } else {
                fillBlocks(x, y2, 1, Math.abs(y - y2) + 1)
            }
        }
    }
}
