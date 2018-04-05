package kotcity.ui.sprites

import javafx.scene.image.Image
import kotcity.data.*
import kotcity.memoization.CacheOptions
import kotcity.memoization.cache
import javafx.embed.swing.SwingFXUtils
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.io.File

object BufferedImageUtils {
    fun resize(img: BufferedImage, newW: Int, newH: Int): BufferedImage {
        val tmp = img.getScaledInstance(newW, newH, java.awt.Image.SCALE_SMOOTH)
        val dimg = BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB)

        val g2d = dimg.createGraphics()
        g2d.drawImage(tmp, 0, 0, null)
        g2d.dispose()

        return dimg
    }
}


object BuildingSpriteLoader {

    private val imageForFileCachePair = BuildingSpriteLoader::uncachedImageForFile.cache(
        CacheOptions(
            weakKeys = false,
            weakValues = false,
            maximumSize = 4096
        )
    )
    private val imageForFile = imageForFileCachePair.second

    fun spriteForBuildingType(building: Building, width: Double, height: Double) =
        imageForFile(filename(building), width, height)

    private fun uncachedImageForFile(filename: String, width: Double, height: Double): Image {
        try {
            val bufferedImage = ImageIO.read(File(filename))
            return SwingFXUtils.toFXImage(BufferedImageUtils.resize(bufferedImage, width.toInt(), height.toInt()), null) as Image
        } catch (imgException: javax.imageio.IIOException) {
            println("Could not read: $filename")
            throw imgException
        }
    }

    fun filename(building: Building): String {
        return when (building::class) {
            PowerPlant::class -> powerPlantSprite(building)
            Commercial::class -> "./assets/commercial/${building.sprite}"
            Residential::class -> "./assets/residential/${building.sprite}"
            Industrial::class -> "./assets/industrial/${building.sprite}"
            PowerLine::class -> "./assets/utility/power_line.png"
            FireStation::class -> "./assets/utility/fire_station.png"
            PoliceStation::class -> "./assets/utility/police_station.png"
            TrainStation::class -> "./assets/transportation/trains/train_station_icon.png"
            RailDepot::class -> "./assets/transportation/trains/rail_depot_icon.png"
            Civic::class -> "./assets/civic/${building.sprite}"
            else -> throw RuntimeException("Unknown sprite for ${building::class}")
        }
    }

    private fun powerPlantSprite(building: Building): String {
        return when {
            building.variety == "coal" -> "./assets/utility/coal_power_plant.png"
            building.variety == "nuclear" -> "./assets/utility/nuclear_power_plant.png"
            else -> throw RuntimeException("Unknown power plant variety: ${building.variety}")
        }

    }
}
