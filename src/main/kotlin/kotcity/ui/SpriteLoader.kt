package kotcity.ui

import aballano.kotlinmemoization.memoize
import javafx.scene.image.Image
import kotcity.data.BuildingType

object SpriteLoader {

    private fun uncachedSpriteForBuildingType(type: BuildingType, width: Double, height: Double): Image? {
        var filename = when (type) {
            BuildingType.COAL_POWER_PLANT -> "/sprites/coal_power_plant.png"
            BuildingType.SMALL_HOUSE -> "/sprites/small_house.png"
            BuildingType.CORNER_STORE -> "/sprites/corner_store.png"
            BuildingType.WORKSHOP -> "./sprites/workshop.png"
            else -> null
        }
        if (filename != null) {
            return Image(filename, width, height, false, false)
        }
        throw RuntimeException("Could not find a sprite for: $type")
    }

    val spriteForBuildingType = ::uncachedSpriteForBuildingType.memoize()
}
