package kotcity.automata

import kotcity.data.*

object PowerCoverageUpdater {
    fun update(map: CityMap) {

        // OK we gotta find all the power plants on the cityMap...
        val powerPlants = map.locations().filter { location ->
            location.building.type == BuildingType.POWER_PLANT
        }

        val gridmap = mutableMapOf<BlockCoordinate, PowerCoverageAutomata>()

        // now for each power plant we want to start an automata...
        var autoMataIndex = 0

        val automatas = powerPlants.map {
            autoMataIndex += 1
            PowerCoverageAutomata(it.coordinate, it.building as PowerPlant, gridmap, map)
        }.toMutableSet()
        while (automatas.any { !it.done() }) {
            automatas.forEach {
                if (!it.done()) {
                    it.tick()
                }
            }
        }

        // ok now let's set all buildings to powered that were in teh grid list...
        map.locations().forEach { location ->
            location.building.powered = gridmap.containsKey(location.coordinate)
        }

    }
}