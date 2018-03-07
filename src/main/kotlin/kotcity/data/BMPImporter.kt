package kotcity.data

import io.nayuki.bmpio.BmpImage
import io.nayuki.bmpio.BmpReader
import net.sf.image4j.codec.bmp.BMPDecoder
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

    private fun eachPixel(image: BmpImage, callback: (BlockCoordinate, Pixel) -> Unit) {
        val w = image.image.width
        val h = image.image.height

        for (x in 0 until w) {
            for (y in 0 until h) {
                val pixel = image.image.getRgb888Pixel(x, y)
                callback(BlockCoordinate(x, y), extractRGB(pixel))
            }
        }
    }


    fun load(s: String): CityMap? {
        val file = File(s)
        // val img = BMPDecoder.read(file)
        val img = BmpReader.read(file.inputStream())

        if (img != null) {
            val size = Collections.min(listOf(img.image.width, img.image.height)) ?: 512
            val newMap = CityMap(size)
            eachPixel(img) { coordinate: BlockCoordinate, pixel: Pixel ->
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