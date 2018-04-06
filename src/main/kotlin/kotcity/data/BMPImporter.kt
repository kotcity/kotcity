package kotcity.data

import kotcity.util.eachPixel
import java.awt.Color
import java.io.File
import javax.imageio.ImageIO

class BMPImporter {
    /**
     * Generates a map by loading the bitmap image file at the provided path and using the color value of each pixel
     * for determining the terrain elevation.
     *
     * @param imagePath the path to the source image file
     * @return the generated map or null if the image could not be loaded
     */
    fun load(imagePath: String): CityMap? {
        val file = File(imagePath)
        val image = ImageIO.read(file) ?: return null

        val size = minOf(image.width, image.height)
        println("Loaded map with size: $size")
        val newMap = CityMap(size, size)
        image.eachPixel { coordinate: BlockCoordinate, color: Color ->
            val elevation = color.red.toDouble()
            val type = if (elevation < 83) TileType.WATER else TileType.GROUND
            newMap.groundLayer[coordinate] = MapTile(type, elevation)
        }
        return newMap
    }
}
