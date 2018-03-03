package kotcity.ui

import aballano.kotlinmemoization.memoize
import javafx.scene.image.Image
import kotcity.data.*

object SpriteLoader {

    private fun uncachedSpriteForBuildingType(building: Building, width: Double, height: Double): Image? {
        var filename = filename(building)
        return Image(filename, width, height, true, false)
    }

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

    val spriteForBuildingType = ::uncachedSpriteForBuildingType.memoize()
}
