package kotcity.ui.sprites

import javafx.scene.image.Image
import kotcity.data.Zot
import kotcity.data.ZotType
import kotcity.memoization.CacheOptions
import kotcity.memoization.cache

object ZotSpriteLoader {

    private val imageForFileCachePair =
        ::uncachedImageForFile.cache(CacheOptions(weakKeys = false, weakValues = false, maximumSize = 4096))
    private val imageForFile = imageForFileCachePair.second

    fun spriteForZot(zot: Zot, width: Double, height: Double): Image? {
        val filename = when (zot.type) {
            ZotType.TOO_MUCH_TRAFFIC -> "file:./assets/zots/too_much_traffic.png"
            ZotType.NO_POWER -> "file:./assets/zots/no_power.png"
            ZotType.NO_CUSTOMERS -> "file:./assets/zots/no_customers.png"
            ZotType.NO_GOODS -> "file:./assets/zots/no_goods.png"
            ZotType.NO_WORKERS -> "file:./assets/zots/no_workers.png"
            ZotType.UNHAPPY_NEIGHBORS -> "file:./assets/zots/unhappy_neighbors.png"
            ZotType.TOO_MUCH_POLLUTION -> "file:./assets/zots/too_much_pollution.png"
            else -> return null
        }
        return imageForFile(filename, width, height)
    }

    private fun uncachedImageForFile(filename: String, width: Double, height: Double) =
        Image(filename, width, height, true, true)

}
