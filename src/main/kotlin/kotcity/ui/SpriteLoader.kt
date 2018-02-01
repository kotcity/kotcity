package kotcity.ui

import aballano.kotlinmemoization.memoize
import javafx.scene.image.Image
import kotcity.data.Building
import kotcity.data.BuildingType

object SpriteLoader {

    private fun uncachedSpriteForBuildingType(building: Building, width: Double, height: Double): Image? {
        var filename = when (building.type) {
            BuildingType.COAL_POWER_PLANT -> "file:./assets/utility/coal_power_plant.png"
            BuildingType.COMMERCIAL -> "file:./assets/commercial/${building.sprite}"
            BuildingType.RESIDENTIAL -> "file:./assets/residential/${building.sprite}"
            BuildingType.INDUSTRIAL -> "file:./assets/industrial/${building.sprite}"
            BuildingType.POWER_LINE -> "file:./assets/utility/power_line.png"
            else -> throw RuntimeException("Unknown sprite for ${building.type}")
        }
        if (filename != null) {
            return Image(filename, width, height, false, false)
        }
        throw RuntimeException("Could not find a sprite for: $building")
    }

    val spriteForBuildingType = ::uncachedSpriteForBuildingType.memoize()
}
