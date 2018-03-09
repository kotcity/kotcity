package kotcity.ui

import javafx.scene.paint.Color
import kotcity.data.MapTile
import kotcity.data.TileType

class ColorAdjuster(val mapMinElevation: Double, val mapMaxElevation: Double) {
    fun colorForTile(tile: MapTile): Color {
        val newColor =
                if (tile.type == TileType.GROUND) {
                    Color.rgb(153, 102, 0)
                } else {
                    Color.DARKBLUE
                }
        // this next line maps the elevations from -0.5 to 0.5 so we don't get
        // weird looking colors....
        val elevation = tile.elevation
        return adjustColor(elevation, newColor)
    }

    private fun adjustColor(elevation: Double, color: Color): Color {
        val bleachAmount = Algorithms.scale(elevation, mapMinElevation, mapMaxElevation, -0.5, 0.5)
        return bleach(color, bleachAmount.toFloat())
    }

    private fun bleach(color: Color, amount: Float): Color {
        val red = (color.red + amount).coerceIn(0.0, 1.0)
        val green = (color.green + amount).coerceIn(0.0, 1.0)
        val blue = (color.blue + amount).coerceIn(0.0, 1.0)
        return Color.color(red, green, blue)
    }
}

