package kotcity.ui

import aballano.kotlinmemoization.memoize
import javafx.scene.image.Image
import kotcity.data.BuildingType

object SpriteLoader {

    private fun uncachedSpriteForBuildingType(type: BuildingType, width: Double, height: Double): Image? {
        var filename = if (type == BuildingType.COAL_POWER_PLANT) {
            "/sprites/coal_power_plant.png"
        } else if (type == BuildingType.SMALL_HOUSE) {
            "/sprites/small_house.png"
        } else {
            null
        }
        if (filename != null) {
            return Image(filename, width, height, false, false)
        }
        throw RuntimeException("Could not find a sprite for: $type")
    }

    val spriteForBuildingType = ::uncachedSpriteForBuildingType.memoize()
}
