package kotcity.ui

import javafx.scene.paint.Color
import kotcity.data.MapTile
import kotcity.data.TileType

class ColorAdjuster(private val mapMinElevation: Double, private val mapMaxElevation: Double) {
    fun colorForTile(tile: MapTile): Color {
        // this next line maps the elevations from -0.5 to 0.5 so we don't get
        // weird looking colors....
        val color = when (tile.type) {
            TileType.GROUND -> Color.rgb(37, 96, 37)
            TileType.WATER -> Color.DARKBLUE
        }
        return adjustColor(tile.elevation, color)
    }

    private fun adjustColor(elevation: Double, color: Color): Color {
        val bleachAmount = Algorithms.scale(elevation, mapMinElevation, mapMaxElevation, -0.3, 0.3)
        return bleach(color, bleachAmount.toFloat())
    }

    private fun bleach(color: Color, amount: Float): Color {
        val red = (color.red + amount).coerceIn(0.0, 1.0)
        val green = (color.green + amount).coerceIn(0.0, 1.0)
        val blue = (color.blue + amount).coerceIn(0.0, 1.0)
        return Color.color(red, green, blue)
    }
}
