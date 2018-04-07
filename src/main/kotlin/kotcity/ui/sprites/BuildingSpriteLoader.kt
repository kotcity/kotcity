package kotcity.ui.sprites

import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import kotcity.data.*
import kotcity.memoization.CacheOptions
import kotcity.memoization.cache
import kotcity.util.resize
import java.io.File
import javax.imageio.ImageIO

object BuildingSpriteLoader {

    private val imageForFileCachePair = BuildingSpriteLoader::uncachedImageForFile.cache(
        CacheOptions(
            weakKeys = false,
            weakValues = false,
            maximumSize = 4096
        )
    )
    private val imageForFile = imageForFileCachePair.second

    fun spriteForBuildingType(building: Building, width: Int, height: Int) =
        imageForFile(filename(building), width, height)

    private fun uncachedImageForFile(filename: String, width: Int, height: Int): Image {
        try {
            val bufferedImage = ImageIO.read(File(filename))
            return SwingFXUtils.toFXImage(bufferedImage.resize(width, height), null) as Image
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
