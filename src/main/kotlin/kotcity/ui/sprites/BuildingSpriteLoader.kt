package kotcity.ui.sprites

import javafx.scene.image.Image
import kotcity.data.*
import kotcity.memoization.CacheOptions
import kotcity.memoization.cache

object BuildingSpriteLoader {

    fun spriteForBuildingType(building: Building, width: Double, height: Double): Image? {
        var filename = filename(building)
        return imageForFile(filename, width, height)
    }

    private fun uncachedImageForFile(filename: String, width: Double, height: Double): Image {
        return Image(filename, width, height, true, true)
    }

    val imageForFileCachePair = BuildingSpriteLoader::uncachedImageForFile.cache(CacheOptions(weakKeys = false, weakValues = false, maximumSize = 4096))
    val imageForFile = imageForFileCachePair.second

    fun filename(building: Building): String {
        return when (building::class) {
            PowerPlant::class -> powerPlantSprite(building)
            Commercial::class -> "file:./assets/commercial/${building.sprite}"
            Residential::class -> "file:./assets/residential/${building.sprite}"
            Industrial::class -> "file:./assets/industrial/${building.sprite}"
            PowerLine::class -> "file:./assets/utility/power_line.png"
            Civic::class -> "file:./assets/civic/${building.sprite}"
            else -> throw RuntimeException("Unknown sprite for ${building::class}")
        }
    }

    private fun powerPlantSprite(building: Building): String {
        return when {
            building.variety == "coal" -> "file:./assets/utility/coal_power_plant.png"
            building.variety == "nuclear" -> "file:./assets/utility/nuclear_power_plant.png"
            else -> throw RuntimeException("Unknown power plant variety: ${building.variety}")
        }

    }
}
