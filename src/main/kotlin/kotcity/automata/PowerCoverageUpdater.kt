package kotcity.automata

import kotcity.data.*

object PowerCoverageUpdater {
    fun update(map: CityMap) {

        // OK we gotta find all the power plants on the map...
        val powerPlants = map.buildingLayer.filter { entry: Map.Entry<BlockCoordinate, Building> ->
            entry.value.type == BuildingType.POWER_PLANT
        }

        val gridmap = mutableMapOf<BlockCoordinate, PowerCoverageAutomata>()

        // now for each power plant we want to start an automata...
        var autoMataIndex = 0

        val automatas = powerPlants.map {
            autoMataIndex += 1
            PowerCoverageAutomata(it.key, it.value as PowerPlant, gridmap, map)
        }.toMutableSet()
        while (automatas.any { !it.done() }) {
            automatas.forEach {
                if (!it.done()) {
                    it.tick()
                }
            }
        }

        // ok now let's set all buildings to powered that were in teh grid list...
        map.buildingLayer.forEach { t, u ->
            u.powered = gridmap.containsKey(t)
        }

    }
}