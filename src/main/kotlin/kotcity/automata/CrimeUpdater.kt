package kotcity.automata

import kotcity.data.BlockCoordinate
import kotcity.data.CityMap
import kotcity.data.buildings.PoliceStation

object CrimeUpdater {
    fun update(map: CityMap) {
        updateCrime(map)
        updatePolicePresence(map)
    }

    private fun updateCrime(map: CityMap) {
        map.crimeLayer.clear()
        // TODO
    }

    private fun updatePolicePresence(map: CityMap) {
        map.policePresenceLayer.clear()
        // OK we gotta find all the powered fire stations on the cityMap...
        map.locations().filter { location ->
            location.building is PoliceStation && location.building.powered
        }.forEach { policeStation ->
            val center = BlockCoordinate(policeStation.coordinate.x + 1, policeStation.coordinate.y + 1)
            center.circle(16).forEach {
                if (it.distanceTo(center) <= 6) {
                    map.policePresenceLayer[it] = 1.0
                } else if (it.distanceTo(center) <= 12) {
                    if (map.policePresenceLayer[it] ?: 0.0 < 0.75) {
                        map.policePresenceLayer[it] = 0.75
                    }
                } else {
                    if (map.policePresenceLayer[it] ?: 0.0 < 0.5) {
                        map.policePresenceLayer[it] = 0.5
                    }
                }
            }
        }
    }
}
