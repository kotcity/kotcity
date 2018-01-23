package kotcity.ui

import aballano.kotlinmemoization.memoize
import javafx.scene.image.Image
import kotcity.data.BuildingType

object SpriteLoader {

    private fun uncachedSpriteForBuildingType(type: BuildingType, width: Double, height: Double): Image? {
        if (type == BuildingType.COAL_POWER_PLANT) {
            return Image("/sprites/coal_power_plant.png", width, height, false, false)
        }
        return null
    }

    val spriteForBuildingType = ::uncachedSpriteForBuildingType.memoize()
}
