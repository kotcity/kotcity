package kotcity.ui.sprites

import javafx.scene.image.Image
import kotcity.data.Zot
import kotcity.memoization.CacheOptions
import kotcity.memoization.cache

object ZotSpriteLoader {

    fun spriteForZot(zot: Zot, width: Double, height: Double): Image? {
        var filename = when(zot) {
            Zot.TOO_MUCH_TRAFFIC -> "file:./assets/zots/too_much_traffic.png"
            Zot.NO_POWER -> "file:./assets/zots/no_power.png"
            Zot.NO_CUSTOMERS -> "file:./assets/zots/no_customers.png"
            Zot.NO_GOODS -> "file:./assets/zots/no_goods.png"
            Zot.NO_WORKERS -> "file:./assets/zots/no_workers.png"
            Zot.UNHAPPY_NEIGHBORS -> "file:./assets/zots/unhappy_neighbors.png"
            else -> return null
        }
        return imageForFile(filename, width, height)
    }

    private fun uncachedImageForFile(filename: String, width: Double, height: Double): Image {
        return Image(filename, width, height, true, true)
    }

    private val imageForFileCachePair = ::uncachedImageForFile.cache(CacheOptions(weakKeys = false, weakValues = false, maximumSize = 4096))
    private val imageForFile = imageForFileCachePair.second
}