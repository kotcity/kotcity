package kotcity.ui.layers

import javafx.geometry.VPos
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontSmoothingType
import javafx.scene.text.TextAlignment
import kotcity.data.BlockCoordinate
import kotcity.data.CityMap
import kotcity.data.District
import kotcity.ui.map.CityRenderer

class DistrictRenderer(private val renderer: CityRenderer, private val map: CityMap) {

    fun render() {
        if (renderer.zoom > 2) {
            // We only want to render the districts when zoomed out so they dont obstruct the view too much
            return
        }
        val (startBlock, endBlock) = renderer.visibleBlockRange()
        val blockSize = renderer.blockSize
        val visibleDistricts = mutableSetOf<District>()

        val gc = renderer.canvas.graphicsContext2D
        gc.fill = Color.gray(1.0, 0.1)
        gc.lineWidth = 2.0

        BlockCoordinate.iterateAll(startBlock, endBlock) { coordinate ->
            map.districtLayer[coordinate]?.let { district ->
                if (district == map.mainDistrict) {
                    // We only want to render districts the player created and not the default one
                    return@let
                }
                visibleDistricts.add(district)

                val tx = coordinate.x - renderer.blockOffsetX
                val ty = coordinate.y - renderer.blockOffsetY

                // We highlight the are of the district in a very fainted gray
                gc.fillRect(tx * blockSize, ty * blockSize, blockSize, blockSize)

                val left = tx * blockSize
                val right = tx * blockSize + blockSize - gc.lineWidth
                val top = ty * blockSize
                val bottom = ty * blockSize + blockSize - gc.lineWidth

                // We draw a border around the edges of the district in a predefined color
                gc.stroke = district.color
                if (map.districtLayer[coordinate.top()] != district) {
                    gc.strokeLine(left, top, right, top)
                }
                if (map.districtLayer[coordinate.bottom()] != district) {
                    gc.strokeLine(left, bottom, right, bottom)
                }
                if (map.districtLayer[coordinate.left()] != district) {
                    gc.strokeLine(left, top, left, bottom)
                }
                if (map.districtLayer[coordinate.right()] != district) {
                    gc.strokeLine(right, top, right, bottom)
                }
            }
        }

        gc.textAlign = TextAlignment.CENTER
        gc.textBaseline = VPos.CENTER
        gc.font = Font.font(16.0)
        gc.fontSmoothingType = FontSmoothingType.LCD

        visibleDistricts.forEach {
            var topLeft = it.topLeft ?: it.blocks.first()
            var bottomRight = it.bottomRight ?: it.blocks.last()

            if (it.topLeft == null || it.bottomRight == null) {
                it.blocks.forEach {
                    if (it.x <= topLeft.x && it.y <= topLeft.y) {
                        topLeft = it
                    } else if (it.x >= bottomRight.x && it.y >= bottomRight.y) {
                        bottomRight = it
                    }
                }
                it.topLeft = topLeft
                it.bottomRight = bottomRight
            }

            val x = ((topLeft.x + bottomRight.x) / 2) - renderer.blockOffsetX
            val y = ((topLeft.y + bottomRight.y) / 2) - renderer.blockOffsetY

            gc.fill = it.color
            gc.fillText(it.name, x * blockSize, y * blockSize)
        }
    }
}
