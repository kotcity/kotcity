package kotcity.ui

import aballano.kotlinmemoization.memoize
import javafx.scene.image.Image
import kotcity.data.Building
import kotcity.data.BuildingType

object SpriteLoader {

    private fun uncachedSpriteForBuildingType(building: Building, width: Double, height: Double): Image? {
        var filename = when (building.type) {
            BuildingType.POWER_PLANT -> powerPlantSprite(building)
            BuildingType.COMMERCIAL -> "file:./assets/commercial/${building.sprite}"
            BuildingType.RESIDENTIAL -> "file:./assets/residential/${building.sprite}"
            BuildingType.INDUSTRIAL -> "file:./assets/industrial/${building.sprite}"
            BuildingType.POWER_LINE -> "file:./assets/utility/power_line.png"
            BuildingType.CIVIC -> "file:./assets/civic/${building.sprite}"
            else -> throw RuntimeException("Unknown sprite for ${building.type}")
        }
        if (filename != null) {
            return Image(filename, width, height, false, false)
        }
        throw RuntimeException("Could not find a sprite for: $building")
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
