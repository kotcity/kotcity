package kotcity.ui.map

import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import kotcity.data.*
import kotcity.ui.*

const val MAX_BUILDING_SIZE = 4
// the coal power plant is the biggest...

class CityRenderer(private val gameFrame: GameFrame, private val canvas: ResizableCanvas, private val map: CityMap) {

    var zoom = 1.0
        set(value) {
            var oldCenter = centerBlock()
            field = value
            panMap(oldCenter)
        }

    var blockOffsetX = 0.0
    var blockOffsetY = 0.0
    private var mapMinElevation = 0.0
    private var mapMaxElevation = 1.0
    private var colorAdjuster: ColorAdjuster = ColorAdjuster(mapMinElevation, mapMaxElevation)

    private var mouseDown = false
    var mouseBlock: BlockCoordinate? = null
    private var firstBlockPressed: BlockCoordinate? = null

    var mapMode: MapMode = MapMode.NORMAL

    init {
        mapMinElevation = map.groundLayer.values.mapNotNull { it.elevation }.min() ?: 0.0
        mapMaxElevation = map.groundLayer.values.mapNotNull { it.elevation }.max() ?: 0.0
        colorAdjuster = ColorAdjuster(mapMinElevation, mapMaxElevation)

        println("Map min: $mapMinElevation Map max: $mapMaxElevation")
        println("Map has been set to: $map. Size is ${canvas.width}x${canvas.height}")
    }

    private fun canvasBlockHeight() = (canvas.height / blockSize()).toInt()

    private fun canvasBlockWidth() = (canvas.width / blockSize()).toInt()

    // awkward... we need padding to get building off the screen...
    fun visibleBlockRange(padding : Int = 0): Pair<BlockCoordinate, BlockCoordinate> {
        var startBlockX = blockOffsetX.toInt() - padding
        var startBlockY = blockOffsetY.toInt() - padding
        var endBlockX = startBlockX + canvasBlockWidth() + padding
        var endBlockY = startBlockY + canvasBlockHeight() + padding

        if (endBlockX > map.width) {
            endBlockX = map.width
        }

        if (endBlockY > map.height) {
            endBlockY = map.height
        }

        val startCoordinate = BlockCoordinate(startBlockX, startBlockY)
        val endCoordinate = BlockCoordinate(endBlockX, endBlockY)

        return Pair(startCoordinate, endCoordinate)
    }

    private fun panMap(coordinate: BlockCoordinate) {
        // OK, we want to figure out the CENTER block now...
        val centerBlock = centerBlock()
        // println("The center block is: $centerX,$centerY")
        // println("We clicked at: ${clickedBlock.x},${clickedBlock.y}")
        val dx = coordinate.x - centerBlock.x
        val dy = coordinate.y - centerBlock.y
        // println("Delta is: $dx,$dy")
        blockOffsetX += (dx)
        blockOffsetY += (dy)
        firePanChanged()
    }

    private val listeners: MutableList<(Pair<BlockCoordinate, BlockCoordinate>) -> Unit> = mutableListOf()

    fun addPanListener(listener: (Pair<BlockCoordinate, BlockCoordinate>) -> Unit) {
        this.listeners.add(listener)
    }

    private fun firePanChanged() {
        this.listeners.forEach {
            it(this.visibleBlockRange())
        }
    }

    fun centerBlock(): BlockCoordinate {
        val centerX = blockOffsetX + (canvasBlockWidth() / 2)
        val centerY = blockOffsetY + (canvasBlockHeight() / 2)
        return BlockCoordinate(centerX.toInt(), centerY.toInt())
    }

    // returns the first and last block that we dragged from / to
    fun blockRange(): Pair<BlockCoordinate?, BlockCoordinate?> {
        return Pair(this.firstBlockPressed, this.mouseBlock)
    }

    private fun mouseToBlock(mouseX: Double, mouseY: Double): BlockCoordinate {
        // OK... this should be pretty easy...
        val blockSize = blockSize()
        val blockX = (mouseX / blockSize).toInt()
        val blockY = (mouseY / blockSize).toInt()
        // println("Mouse block coords: $blockX,$blockY")
        return BlockCoordinate(blockX + blockOffsetX.toInt(), blockY + blockOffsetY.toInt())
    }

    fun onMousePressed(evt: MouseEvent) {
        this.mouseDown = true
        this.firstBlockPressed = mouseToBlock(evt.x, evt.y)
        this.mouseBlock = this.firstBlockPressed
        // println("Pressed on block: $firstBlockPressed")
    }

    fun onMouseReleased(evt: MouseEvent) {
        this.mouseDown = false
    }

    fun onMouseDragged(evt: MouseEvent) {
        updateMouseBlock(evt)
        // println("The mouse is at $blockCoordinate")
    }

    private fun updateMouseBlock(evt: MouseEvent) {
        val mouseX = evt.x
        val mouseY = evt.y
        val blockCoordinate = mouseToBlock(mouseX, mouseY)
        this.mouseBlock = blockCoordinate
    }

    fun getHoveredBlock(): BlockCoordinate? {
        return this.mouseBlock
    }

    fun onMouseMoved(evt: MouseEvent) {
        // OK... if we have an active tool we might
        // have to draw a building highlight
        updateMouseBlock(evt)
    }

    fun onMouseClicked(evt: MouseEvent) {
        if (evt.button == MouseButton.SECONDARY) {
            val clickedBlock = mouseToBlock(evt.x, evt.y)
            panMap(clickedBlock)
        }
    }

    private fun drawMap(gc: GraphicsContext) {
        // we got that map...
        val (startBlock, endBlock) = visibleBlockRange()
        val blockSize = blockSize()

        val xRange = (startBlock.x..endBlock.x)
        val yRange = (startBlock.y..endBlock.y)

        // this handles the offset...
        xRange.toList().forEachIndexed { xi, x ->
            yRange.toList().forEachIndexed { yi, y ->
                val tile = map.groundLayer[BlockCoordinate(x, y)]
                if (tile != null) {
                    val adjustedColor = colorAdjuster.colorForTile(tile)
                    gc.fill = adjustedColor

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

    private fun fillBlocks(g2d: GraphicsContext, blockX: Int, blockY: Int, width: Int, height: Int) {
        for (y in blockY until blockY + height) {
            for (x in blockX until blockX + width) {
                highlightBlock(g2d, x, y)
            }
        }
    }

    fun render() {
        if (canvas.graphicsContext2D == null) {
            return
        }
        canvas.graphicsContext2D.fill = Color.BLACK
        canvas.graphicsContext2D.fillRect(0.0, 0.0, canvas.width, canvas.height)
        drawMap(canvas.graphicsContext2D)
        drawZones()
        drawBuildings(canvas.graphicsContext2D)
        if (mapMode != MapMode.NORMAL) {
            drawResources()
        }
        drawHighlights()
    }

    private fun resourceLayer(mode: MapMode): QuantizedMap<Double>? {
        return when(mode) {
            MapMode.COAL -> map.resourceLayers["coal"]
            MapMode.OIL -> map.resourceLayers["oil"]
            MapMode.GOLD -> map.resourceLayers["gold"]
            MapMode.SOIL -> map.resourceLayers["soil"]
            else -> null
        }
    }

    private fun drawResources() {

        val (startBlock, endBlock) = visibleBlockRange()

        val layer = resourceLayer(this.mapMode) ?: return

        BlockCoordinate.iterate(startBlock, endBlock) {coord ->
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
                if (gameFrame.activeTool == Tool.ROAD) {
                    drawRoadBlueprint(canvas.graphicsContext2D)
                } else if (gameFrame.activeTool == Tool.POWER_LINES) {
                    drawRoadBlueprint(canvas.graphicsContext2D)
                } else if (gameFrame.activeTool == Tool.BULLDOZE) {
                    firstBlockPressed?.let { first ->
                        highlightBlocks(first, it)
                    }
                } else if (gameFrame.activeTool == Tool.RESIDENTIAL_ZONE) {
                    firstBlockPressed?.let { first ->
                        highlightBlocks(first, it)
                    }
                } else if (gameFrame.activeTool == Tool.COMMERCIAL_ZONE) {
                    firstBlockPressed?.let { first ->
                        highlightBlocks(first, it)
                    }
                } else if (gameFrame.activeTool == Tool.INDUSTRIAL_ZONE) {
                    firstBlockPressed?.let { first ->
                        highlightBlocks(first, it)
                    }
                } else if (gameFrame.activeTool == Tool.DEZONE) {
                    firstBlockPressed?.let { first ->
                        highlightBlocks(first, it)
                    }
                } else {
                    Unit
                }
            } else {
                if (gameFrame.activeTool == Tool.COAL_POWER_PLANT) {
                    highlightCenteredBlocks(it, 4, 4)
                } else if (gameFrame.activeTool == Tool.ROAD || gameFrame.activeTool == Tool.POWER_LINES) {
                    mouseBlock?.let {
                        highlightBlocks(it, it)
                    }
                } else {
                    Unit
                }
            }
        }
    }

    private fun highlightCenteredBlocks(start: BlockCoordinate, width: Int, height: Int) {
        // TODO: we want to make this shit kind of centered...
        val offsetX = (width / 2) - 1
        val offsetY = (height / 2) - 1
        val newBlock = BlockCoordinate(start.x - offsetX, start.y - offsetY)
        highlightBlocks(newBlock, width, height)
    }

    private fun highlightBlocks(start: BlockCoordinate, width: Int, height: Int) {
        val startX = start.x
        val startY = start.y
        for (x in startX until startX + width) {
            for (y in startY until startY + height) {
                highlightBlock(canvas.graphicsContext2D, x, y)
            }
        }
    }

    private fun highlightBlocks(from: BlockCoordinate, to: BlockCoordinate) {
        for (x in (from.x..to.x).reorder()) {
            for (y in (from.y..to.y).reorder()) {
                highlightBlock(canvas.graphicsContext2D, x, y)
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

    private fun visibleBuildings(): List<Pair<BlockCoordinate, Building>> {
        // TODO: we can just map over the two different layers... clean up later...
        val buildings = visibleBlocks(padding = MAX_BUILDING_SIZE).mapNotNull {
            val building = map.buildingLayer[it]
            if (building != null) {
                Pair(it, building)
            } else {
                null
            }
        }
        val powerLines = visibleBlocks(padding = MAX_BUILDING_SIZE).mapNotNull {
            val building = map.powerLineLayer[it]
            if (building != null) {
                Pair(it, building)
            } else {
                null
            }
        }
        return buildings.plus(powerLines)
    }

    private fun drawBuildings(context: GraphicsContext) {
        visibleBuildings().forEach { it ->
            val coordinate = it.first
            val building = it.second
            val tx = coordinate.x - blockOffsetX
            val ty = coordinate.y - blockOffsetY
            val blockSize = blockSize()
            when (building) {
                is Road -> {
                    context.fill = Color.BLACK
                    context.fillRect(tx * blockSize, ty * blockSize, blockSize, blockSize)
                }
                is PowerPlant -> {
                    // need that sprite...
                    val type = BuildingType.POWER_PLANT
                    drawBuildingType(building, type, tx, ty)
                }
                is Building -> {
                    drawBuildingType(building, building.type, tx, ty)
                }
            }
        }
    }

    private fun drawBuildingType(building: Building, type: BuildingType, tx: Double, ty: Double) {
        val blockSize = blockSize()
        val width = building.width * blockSize
        val height = building.height * blockSize
        drawBuildingBorder(tx, ty, width, height, blockSize)

        val shrink = blockSize * 0.10

        // OK... fucking THINK here...
        // blocksize will be like 64...

        val imgWidth = (building.width * blockSize) - (shrink * 2)
        val imgHeight =  (building.height * blockSize) - (shrink * 2)

        SpriteLoader.spriteForBuildingType(building, imgWidth, imgHeight)?.let { img ->
            val ix = (tx * blockSize) + shrink
            val iy = (ty * blockSize) + shrink
            // println("ix: $ix iy: $iy width: $imgWidth height: $imgHeight")
            canvas.graphicsContext2D.drawImage(img, ix, iy)
        }

    }

    private fun drawBuildingBorder(tx: Double, ty: Double, width: Double, height: Double, blockSize: Double) {
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

        canvas.graphicsContext2D.fill = Color.BLACK
        canvas.graphicsContext2D.strokeRoundRect(sx, sy, ex, ey, arcSize, arcSize)
    }

    private fun drawZones() {
        val blockSize = blockSize()
        val graphics = canvas.graphicsContext2D
        visibleBlocks().forEach { coordinate ->
            map.zoneLayer[coordinate]?.let { zone ->
                // figure out fill color...
                val tx = coordinate.x - blockOffsetX
                val ty = coordinate.y - blockOffsetY
                val zoneColor = when (zone.type) {
                    ZoneType.RESIDENTIAL -> Color.DARKGREEN
                    ZoneType.COMMERCIAL -> Color.DARKBLUE
                    ZoneType.INDUSTRIAL -> Color.LIGHTGOLDENRODYELLOW
                }
                val shadyColor = Color(zoneColor.red, zoneColor.green, zoneColor.blue, 0.3)
                graphics.fill = shadyColor
                graphics.fillRect(tx * blockSize, ty * blockSize, blockSize, blockSize)
            }
        }
    }

    private fun highlightBlock(g2d: GraphicsContext, x: Int, y: Int) {
        g2d.fill = Color.MAGENTA
        // gotta translate here...
        val tx = x - blockOffsetX
        val ty = y - blockOffsetY
        val blockSize = blockSize()
        g2d.fillRect(tx * blockSize, ty * blockSize, blockSize, blockSize)
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
            // y2 = y

            if (x < x2) {
                fillBlocks(gc, x, y, Math.abs(x - x2) + 1, 1)
            } else {
                fillBlocks(gc, x2, y, Math.abs(x - x2) + 1, 1)
            }
        } else {
            // building vertically
            // now fuck around with x2 so it's at the same level as x1
            // x2 = x

            if (y < y2) {
                fillBlocks(gc, x, y, 1, Math.abs(y - y2) + 1)
            } else {
                fillBlocks(gc, x, y2, 1, Math.abs(y - y2) + 1)
            }

        }

    }

}