package kotcity.ui.sprites

import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import kotcity.data.Zot
import kotcity.data.ZotType
import kotcity.memoization.CacheOptions
import kotcity.memoization.cache
import kotcity.util.resize
import java.io.File
import javax.imageio.ImageIO

object ZotSpriteLoader {

    private val imageForFileCachePair =
        ::uncachedImageForFile.cache(CacheOptions(weakKeys = false, weakValues = false, maximumSize = 4096))
    private val imageForFile = imageForFileCachePair.second

    fun spriteForZot(zot: Zot, width: Double, height: Double): Image? {
        val filename = when (zot.type) {
            ZotType.TOO_MUCH_TRAFFIC -> "./assets/zots/too_much_traffic.svg"
            ZotType.NO_POWER -> "./assets/zots/no_power.svg"
            ZotType.NO_CUSTOMERS -> "./assets/zots/no_customers.svg"
            ZotType.NO_GOODS -> "./assets/zots/no_goods.svg"
            ZotType.NO_WORKERS -> "./assets/zots/no_workers.svg"
            ZotType.UNHAPPY_NEIGHBORS -> "./assets/zots/unhappy_neighbors.svg"
            ZotType.TOO_MUCH_POLLUTION -> "./assets/zots/too_much_pollution.svg"
            else -> return null
        }
        return imageForFile(filename, width, height)
    }

    private fun uncachedImageForFile(filename: String, width: Double, height: Double): Image {
        try {
            val bufferedImage = ImageIO.read(File(filename))
            return SwingFXUtils.toFXImage(bufferedImage.resize(width.toInt(), height.toInt()), null) as Image
        } catch (imgException: javax.imageio.IIOException) {
            println("Could not read: $filename")
            throw imgException
        }
    }

}
