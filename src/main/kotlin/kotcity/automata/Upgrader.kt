package kotcity.automata

import kotcity.data.CityMap

class Upgrader(val cityMap: CityMap) {
    fun tick() {
        // (maybe) upgrade some buildings...
        val eligableBuildings = cityMap.locations().filter { it.building.goodwill > 99 }
        // pick 3 random
    }
}