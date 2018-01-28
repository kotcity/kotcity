package kotcity.ui

import aballano.kotlinmemoization.memoize
import javafx.scene.image.Image
import kotcity.data.Building
import kotcity.data.BuildingType

object SpriteLoader {

    private fun uncachedSpriteForBuildingType(building: Building, width: Double, height: Double): Image? {
        var filename = when (building.type) {
            BuildingType.COAL_POWER_PLANT -> "/sprites/coal_power_plant.png"
            else -> building.sprite
        }
        if (filename != null) {
            return Image(filename, width, height, false, false)
        }
        throw RuntimeException("Could not find a sprite for: $building")
    }

    val spriteForBuildingType = ::uncachedSpriteForBuildingType.memoize()
}
