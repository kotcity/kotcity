package kotcity.automata

import kotcity.data.BuildingType
import kotcity.data.CityMap
import kotcity.data.Tradeable
import kotcity.util.Debuggable

class CensusTaker(val cityMap: CityMap): Debuggable {
    override var debug: Boolean = false
    var population = 0

    fun tick() {
        // loop over each residential building and count the labor...
        var tempPop = 0
        cityMap.buildingLayer.forEach { coordinate, building ->
            if (building.type == BuildingType.RESIDENTIAL) {
                // see how many labor is provided...
                tempPop += building.totalProvided(Tradeable.LABOR)
            }
        }
        population = tempPop
    }
}