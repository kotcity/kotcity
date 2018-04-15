package kotcity.ui.sprites

import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import kotcity.data.buildings.*
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
        return when (building) {
            is PowerPlant -> powerPlantSprite(building)
            is Commercial -> "./assets/commercial/${building.sprite}"
            is Residential -> "./assets/residential/${building.sprite}"
            is Industrial -> "./assets/industrial/${building.sprite}"
            is PowerLine -> "./assets/utility/power_line.png"
            is FireStation -> "./assets/utility/fire_station.png"
            is PoliceStation -> "./assets/utility/police_station.png"
            is TrainStation -> "./assets/transportation/trains/train_station_icon.png"
            is RailDepot -> "./assets/transportation/trains/rail_depot_icon.png"
            is Civic -> "./assets/civic/${building.sprite}"
            is School -> schoolSprite(building)
            else -> throw RuntimeException("Unknown sprite for ${building::class}")
        }
    }

    private fun schoolSprite(building: School): String {
        return when (building) {
            is School.ElementarySchool -> "./assets/civic/elementary_school.svg"
            is School.HighSchool -> "./assets/civic/high_school.svg"
            is School.University -> "./assets/civic/university.svg"
        }
    }

    private fun powerPlantSprite(powerPlant: PowerPlant): String {
        return when (powerPlant.variety) {
            PowerPlant.VARIETY_COAL -> "./assets/utility/coal_power_plant.png"
            PowerPlant.VARIETY_NUCLEAR -> "./assets/utility/nuclear_power_plant.png"
            else -> throw RuntimeException("Unknown power plant variety: ${powerPlant.variety}")
        }
    }
}
