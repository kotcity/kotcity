package kotcity.ui.map

import javafx.beans.value.ObservableValue
import javafx.scene.Cursor
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.scene.text.Font
import kotcity.data.*
import kotcity.data.MapMode.*
import kotcity.data.Tunable.MAX_BUILDING_SIZE
import kotcity.data.buildings.*
import kotcity.pathfinding.Direction
import kotcity.ui.*
import kotcity.ui.layers.*
import kotcity.ui.sprites.BuildingSpriteLoader
import kotcity.util.reorder
import tornadofx.runLater
import kotlin.math.pow


class CityRenderer(
    private val gameFrame: GameFrame,
    val canvas: CityCanvas,
    private val cityMap: CityMap
) {

    var zoom = 1.0
        set(value) {
            val oldCenter = centerBlock()
            field = value
            panMap(oldCenter)
        }

    private val arcWidth get() = zoom * zoom

    private val borderWidth get() = zoom / 2

    val blockSize get() = 2.0.pow(zoom + 1)

    val canvasBlockHeight get() = (canvas.height / blockSize).toInt()

    val canvasBlockWidth get() = (canvas.width / blockSize).toInt()

    private val happinessRenderer = HappinessRenderer(this, cityMap)
    private val fireCoverageRenderer = FireCoverageRenderer(this, cityMap)
    private val crimeRenderer = CrimeRenderer(this, cityMap)
    private val trafficRenderer = TrafficRenderer(this, cityMap)
    private val desirabilityRenderer = DesirabilityRenderer(this, cityMap)
    private val pollutionRenderer = PollutionRenderer(this, cityMap)
    private val landValueRenderer = LandValueRenderer(this, cityMap)
    private val districtRenderer = DistrictRenderer(this, cityMap)

    var blockOffsetX: Double = 0.0
        set(value) {
            val newValue = value.coerceIn(0.0..(cityMap.width - canvasBlockWidth - 1.0))
            field = newValue
        }
    var blockOffsetY: Double = 0.0
        set(value) {
            val newValue = value.coerceIn(0.0..(cityMap.height - canvasBlockHeight - 1.0))
            field = newValue
        }

    private var mapMinElevation = 0.0
    private var mapMaxElevation = 1.0
    private var colorAdjuster: ColorAdjuster = ColorAdjuster(mapMinElevation, mapMaxElevation)

    private var mouseDown = false
    var mouseBlock: BlockCoordinate? = null
    private var firstBlockPressed: BlockCoordinate? = null

    var mapMode: MapMode = NORMAL

    var showRoutesFor: BlockCoordinate? = null

    private val panListeners: MutableList<(Pair<BlockCoordinate, BlockCoordinate>) -> Unit> = mutableListOf()

    private val sizeChangedListener = {observable: ObservableValue<out Number>, oldValue: Number, newValue: Number -> firePanChanged() }

    init {
        mapMinElevation = cityMap.groundLayer.values.map { it.elevation }.min() ?: 0.0
        mapMaxElevation = cityMap.groundLayer.values.map { it.elevation }.max() ?: 0.0
        colorAdjuster = ColorAdjuster(mapMinElevation, mapMaxElevation)
        canvas.addSizeChangeListener {firePanChanged()}
    }

    // awkward... we need padding to get building off the screen...
    fun visibleBlockRange(padding: Int = 0): Pair<BlockCoordinate, BlockCoordinate> {
        val startBlockX = blockOffsetX.toInt() - padding
        val startBlockY = blockOffsetY.toInt() - padding
        var endBlockX = startBlockX + canvasBlockWidth + padding
        var endBlockY = startBlockY + canvasBlockHeight + padding

        endBlockX = Math.min(endBlockX, cityMap.width)
        endBlockY = Math.min(endBlockY, cityMap.height)

        val startCoordinate = BlockCoordinate(startBlockX, startBlockY)
        val endCoordinate = BlockCoordinate(endBlockX, endBlockY)

        return startCoordinate to endCoordinate
    }

    fun panToMouse() = mouseBlock?.let { panMap(it) }

    fun panMap(coordinate: BlockCoordinate) {
        // OK, we want to figure out the CENTER block now...
        val centerBlock = centerBlock()
        val dx = coordinate.x - centerBlock.x
        val dy = coordinate.y - centerBlock.y

        blockOffsetX += dx
        blockOffsetY += dy
        firePanChanged()
    }

    fun addPanListener(listener: (Pair<BlockCoordinate, BlockCoordinate>) -> Unit) = this.panListeners.add(listener)

    fun removePanListeners() = this.panListeners.clear()

    private fun firePanChanged() = this.panListeners.forEach { it(this.visibleBlockRange()) }

    private fun centerBlock(): BlockCoordinate {
        val centerX = blockOffsetX + (canvasBlockWidth / 2)
        val centerY = blockOffsetY + (canvasBlockHeight / 2)
        return BlockCoordinate(centerX.toInt(), centerY.toInt())
    }

    // returns the first and last block that we dragged from / to
    fun blockRange() = this.firstBlockPressed to this.mouseBlock

    private fun mouseToBlock(mouseX: Double, mouseY: Double): BlockCoordinate {
        // OK... this should be pretty easy...
        val blockSize = blockSize
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

    // OK... if we have an active tool we might
    // have to draw a building highlight
    fun onMouseMoved(event: MouseEvent) = updateMouseBlock(event)

    private fun drawMap() {
        // we got that cityMap...
        val (startBlock, endBlock) = visibleBlockRange()
        val blockSize = blockSize

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
        districtRenderer.render()

        if (gameFrame.activeTool == Tool.ROUTES) {
            showRoutesFor?.let {
                drawRoutes(it)
            }
        }

        when (mapMode) {
            SOIL, COAL, GOLD, OIL -> drawResources()
            FIRE_COVERAGE -> fireCoverageRenderer.render()
            CRIME -> crimeRenderer.render()
            DESIRABILITY -> desirabilityRenderer.render()
            TRAFFIC -> trafficRenderer.render()
            HAPPINESS -> happinessRenderer.render()
            POLLUTION -> pollutionRenderer.render()
            LAND_VALUE -> landValueRenderer.render()
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
        BlockCoordinate.iterateAll(startBlock, endBlock) { blockCoordinate ->
            if (blocksWithPath.contains(blockCoordinate)) {
                highlightBlock(blockCoordinate.x, blockCoordinate.y)
            }
        }
    }

    // TODO: this is probably brutally slow...
    private fun contractsWithPathThrough(building: Building, coordinate: BlockCoordinate) =
        building.contracts.toList().filter { it.path?.blocks()?.contains(coordinate) ?: false }

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

        BlockCoordinate.iterateAll(startBlock, endBlock) { coord ->
            layer[coord]?.let {
                if (it > 0.5) {
                    val tx = coord.x - blockOffsetX
                    val ty = coord.y - blockOffsetY
                    val blockSize = blockSize
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
                    Tool.RAILROAD -> drawRailroadBlueprint()
                    Tool.ROAD -> drawRoadBlueprint()
                    Tool.ONE_WAY_ROAD -> drawRoadBlueprint()
                    Tool.POWER_LINES -> drawRoadBlueprint()
                    Tool.RESIDENTIAL_ZONE,
                    Tool.COMMERCIAL_ZONE,
                    Tool.INDUSTRIAL_ZONE,
                    Tool.ASSIGN_DISTRICT,
                    Tool.CLEAR_DISTRICT,
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
                    Tool.RAILROAD,
                    Tool.ROAD,
                    Tool.ONE_WAY_ROAD,
                    Tool.DEZONE,
                    Tool.POWER_LINES,
                    Tool.QUERY,
                    Tool.RECENTER,
                    Tool.BULLDOZE,
                    Tool.COMMERCIAL_ZONE,
                    Tool.INDUSTRIAL_ZONE,
                    Tool.RESIDENTIAL_ZONE,
                    Tool.ASSIGN_DISTRICT,
                    Tool.CLEAR_DISTRICT,
                    Tool.ROUTES -> {
                        it.let { highlightBlock(it) }
                    }
                    Tool.JOB_CENTER,
                    Tool.TOWN_WAREHOUSE -> {
                        it.let { highlightCenteredBlocks(it, 2, 2) }
                    }
                    Tool.RAIL_DEPOT,
                    Tool.TRAIN_STATION,
                    Tool.POLICE_STATION,
                    Tool.ELEMENTARY_SCHOOL,
                    Tool.HIGH_SCHOOL,
                    Tool.UNIVERSITY,
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
            val offsetX = (width / 2)
            val offsetY = (height / 2)
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
        BlockCoordinate.iterateAll(from, to) {
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
            val blockSize = blockSize
            when (building) {
                is Road -> drawRoad(tx, ty, blockSize, building)
                is Railroad -> drawRailroad(tx, ty, blockSize, building)
                is RailroadCrossing -> drawRailroadCrossing(tx, ty, blockSize, building)
                else -> drawBuildingType(building, tx, ty)
            }
        }
    }

    private fun drawRailroad(tx: Double, ty: Double, blockSize: Double, building: Railroad) =
        drawRoad(tx, ty, blockSize, building, Color.GREY)

    private fun drawRailroadCrossing(tx: Double, ty: Double, blockSize: Double, building: RailroadCrossing) =
        drawRoad(tx, ty, blockSize, building, Color.web("#424242"))

    private fun drawRoad(
            tx: Double,
            ty: Double,
            blockSize: Double,
            building: Building,
            fillColor: Color = Color.BLACK
    ) {
        canvas.graphicsContext2D.fill = fillColor
        canvas.graphicsContext2D.fillRect(tx * blockSize, ty * blockSize, blockSize, blockSize)

        val fontSize =
            when (zoom) {
                1.0 -> 4.0
                2.0 -> 8.0
                3.0 -> 12.0
                4.0 -> 20.0
                else -> 40.0
            }

        val offsetX =
            when (zoom) {
                1.0 -> 0.0
                2.0 -> 1.0
                3.0 -> 3.0
                4.0 -> 8.0
                else -> 15.0
            }

        val offsetY =
            when (zoom) {
                1.0 -> 1.0
                2.0 -> 1.0
                3.0 -> 4.0
                4.0 -> 10.0
                else -> 19.0
            }
        canvas.graphicsContext2D.fill = Color.WHITE
        canvas.graphicsContext2D.font = Font.font(fontSize)
        val blockX = tx * blockSize + offsetX
        val blockY = (ty + 1) * blockSize - offsetY
        if (building is Road) {
            when (building.direction) {
                Direction.STATIONARY -> {
                }
                Direction.NORTH -> {
                    canvas.graphicsContext2D.fillText("↑", blockX, blockY)
                }
                Direction.SOUTH -> {
                    canvas.graphicsContext2D.fillText("↓", blockX, blockY)
                }
                Direction.WEST -> {
                    canvas.graphicsContext2D.fillText("←", blockX, blockY)
                }
                Direction.EAST -> {
                    canvas.graphicsContext2D.fillText("→", blockX, blockY)
                }
            }
        }
    }

    private fun drawBuildingType(building: Building, tx: Double, ty: Double) {
        val blockSize = blockSize
        val width = building.width * blockSize
        val height = building.height * blockSize
        drawBuildingBorder(building, tx, ty, width, height, blockSize)

        // introduce a little padding to the image...
        val shrink = blockSize * 0.10

        val imgWidth = (building.width * blockSize) - (shrink * 2)
        val imgHeight = (building.height * blockSize) - (shrink * 2)

        BuildingSpriteLoader.spriteForBuildingType(building, imgWidth.toInt(), imgHeight.toInt()).let { img ->
            val ix = (tx * blockSize) + shrink
            val iy = (ty * blockSize) + shrink
            canvas.graphicsContext2D.drawImage(img, ix, iy, imgWidth, imgHeight)
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
        val arcSize = arcWidth
        canvas.graphicsContext2D.lineWidth = borderWidth
        // we want to inset the stroke...
        val splitFactor = 40.0
        val sx = (tx * blockSize) + (width / splitFactor)
        val sy = (ty * blockSize) + (height / splitFactor)
        val ex = width - ((width / splitFactor) * 2)
        val ey = height - ((height / splitFactor) * 2)

        canvas.graphicsContext2D.fill = Color.WHITE
        canvas.graphicsContext2D.fillRoundRect(sx, sy, ex, ey, arcSize, arcSize)

        canvas.graphicsContext2D.stroke = borderColor(building)
        canvas.graphicsContext2D.strokeRoundRect(sx, sy, ex, ey, arcSize, arcSize)
    }

    private fun borderColor(building: Building): Color {
        return when (building) {
            is Road -> Color.BLACK
            is Residential -> Color.GREEN
            is Commercial -> Color.BLUE
            is Industrial -> Color.GOLD
            is Civic -> Color.DARKGREY
            is PowerPlant -> Color.BLACK
            is Railroad -> Color.GRAY
            is RailroadCrossing -> Color.GRAY
            is PowerLine -> Color.BLACK
            else -> {
                Color.PINK
            }
        }
    }

    private fun drawZones() {
        val blockSize = blockSize
        visibleBlocks().forEach { coordinate ->
            cityMap.zoneLayer[coordinate]?.let { zone ->
                // figure out fill color...
                val tx = coordinate.x - blockOffsetX
                val ty = coordinate.y - blockOffsetY
                val color = zoneColor(zone)
                val shadyColor = Color(color.red, color.green, color.blue, 0.4)
                canvas.graphicsContext2D.fill = shadyColor
                canvas.graphicsContext2D.fillRect(tx * blockSize, ty * blockSize, blockSize, blockSize)
            }
        }
    }

    private fun zoneColor(zone: Zone): Color {
        return when (zone) {
            Zone.RESIDENTIAL -> Color.DARKGREEN
            Zone.COMMERCIAL -> Color.DARKBLUE
            Zone.INDUSTRIAL -> Color.LIGHTGOLDENRODYELLOW
        }
    }

    private fun highlightBlock(coordinate: BlockCoordinate) = highlightBlock(coordinate.x, coordinate.y)

    private fun highlightBlock(x: Int, y: Int) {
        canvas.graphicsContext2D.fill = Color(Color.MAGENTA.red, Color.MAGENTA.green, Color.MAGENTA.blue, 0.50)
        // gotta translate here...
        val tx = x - blockOffsetX
        val ty = y - blockOffsetY
        val blockSize = blockSize
        canvas.graphicsContext2D.fillRect(tx * blockSize, ty * blockSize, blockSize, blockSize)
    }

    private fun drawRailroadBlueprint() {
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

            val dx = Math.abs(x - x2) + 1
            val startX = Math.min(x, x2)
            fillBlocks(startX, y, dx, 1)

            if (y != y2) {
                // val endX = Math.max(x, x2)
                val dy = Math.abs(y - y2) + 1
                val startY = Math.min(y, y2)
                fillBlocks(endBlock.x, startY, 1, dy)
            }
        } else {
            // building vertically

            val dy = Math.abs(y - y2) + 1
            val startY = Math.min(y, y2)
            fillBlocks(x, startY, 1, dy)

            if (x != x2) {
                // val endY = Math.max(y, y2)
                val dx = Math.abs(x - x2) + 1
                val startX = Math.min(x, x2)
                fillBlocks(startX, endBlock.y, dx, 1)
            }
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

            val dx = Math.abs(x - x2) + 1
            val startX = Math.min(x, x2)
            fillBlocks(startX, y, dx, 1)

            if (y != y2) {
                // val endX = Math.max(x, x2)
                val dy = Math.abs(y - y2) + 1
                val startY = Math.min(y, y2)
                fillBlocks(endBlock.x, startY, 1, dy)
            }
        } else {
            // building vertically

            val dy = Math.abs(y - y2) + 1
            val startY = Math.min(y, y2)
            fillBlocks(x, startY, 1, dy)

            if (x != x2) {
                // val endY = Math.max(y, y2)
                val dx = Math.abs(x - x2) + 1
                val startX = Math.min(x, x2)
                fillBlocks(startX, endBlock.y, dx, 1)
            }
        }
    }
}
