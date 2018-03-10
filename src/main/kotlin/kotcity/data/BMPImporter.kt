package kotcity.data

import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File
import java.util.*


data class Pixel(val r: Int, val g: Int, val b: Int, val a: Int)
class BMPImporter {

    private fun extractRGB(pixel: Int): Pixel {
        val alpha = pixel shr 24 and 0xff
        val red = pixel shr 16 and 0xff
        val green = pixel shr 8 and 0xff
        val blue = pixel and 0xff
        return Pixel(red, blue, green, alpha)
    }

    private fun eachPixel(image: BufferedImage, callback: (BlockCoordinate, Pixel) -> Unit) {
        val w = image.width
        val h = image.height

        for (x in 0 until w) {
            for (y in 0 until h) {
                val pixel = image.getRGB(x, y)
                callback(BlockCoordinate(x, y), extractRGB(pixel))
            }
        }
    }


    fun load(s: String): CityMap? {
        val file = File(s)
        // val img = BMPDecoder.read(file)
        // val img = BmpReader.read(file.inputStream())
        val decoder = BMPDecoder()
        decoder.read(file.inputStream())
        val mis = decoder.makeImageSource()
        val image = Toolkit.getDefaultToolkit().createImage(mis)
        val finalimg = BufferedImage(decoder.width, decoder.height, BufferedImage.TYPE_INT_RGB)
        val piximg = Toolkit.getDefaultToolkit().createImage(mis)
        finalimg.graphics.drawImage(piximg, 0, 0, null)

        if (image != null) {
            val size = Collections.min(listOf(finalimg.width, finalimg.height)) ?: 512
            println("Loaded map with size: $size")
            val newMap = CityMap(size, size)
            eachPixel(finalimg) { coordinate: BlockCoordinate, pixel: Pixel ->
                val elevation = pixel.r.toDouble()
                if (elevation < 83) {
                    newMap.groundLayer[coordinate] = MapTile(TileType.WATER, elevation)
                } else {
                    newMap.groundLayer[coordinate] = MapTile(TileType.GROUND, elevation)
                }

            }
            return newMap
        }
        return null
    }


}