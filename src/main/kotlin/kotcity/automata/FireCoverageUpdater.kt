package kotcity.automata

import kotcity.data.BlockCoordinate
import kotcity.data.CityMap
import kotcity.data.buildings.FireStation

object FireCoverageUpdater {
    fun update(map: CityMap) {

        map.fireCoverageLayer.clear()
        // OK we gotta find all the powered fire stations on the cityMap...
        map.locations().filter { location ->
            location.building is FireStation && location.building.powered
        }.forEach { fireStation ->
            val center = BlockCoordinate(fireStation.coordinate.x + 1, fireStation.coordinate.y + 1)
            center.circle(16).forEach {
                if (it.distanceTo(center) <= 6) {
                    map.fireCoverageLayer[it] = 1.0
                } else if (it.distanceTo(center) <= 12) {
                    if (map.fireCoverageLayer[it] ?: 0.0 < 0.75) {
                        map.fireCoverageLayer[it] = 0.75
                    }
                } else {
                    if (map.fireCoverageLayer[it] ?: 0.0 < 0.5) {
                        map.fireCoverageLayer[it] = 0.5
                    }
                }
            }
        }
    }
}