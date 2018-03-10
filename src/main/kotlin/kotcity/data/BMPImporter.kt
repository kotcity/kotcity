package kotcity.data

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class BMPImporter {

    private fun BufferedImage.eachPixel(callback: (BlockCoordinate, Color) -> Unit) {
        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = getRGB(x, y)
                callback(BlockCoordinate(x, y), Color(pixel, true))
            }
        }
    }

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
            if (elevation < 83) {
                newMap.groundLayer[coordinate] = MapTile(TileType.WATER, elevation)
            } else {
                newMap.groundLayer[coordinate] = MapTile(TileType.GROUND, elevation)
            }
        }
        return newMap
    }
}