package kotcity.automata

import kotcity.data.BlockCoordinate
import kotcity.data.CityMap
import kotcity.data.Tradeable
import kotcity.util.Debuggable

class Liquidator(val cityMap: CityMap) : Debuggable {
    override var debug: Boolean = false

    fun tick() {
        cityMap.buildingLayer.forEach { coordinate, building ->
            val buildingsToRaze = mutableListOf<BlockCoordinate>()
            if (building.quantityOnHand(Tradeable.MONEY) <= 0) {
                debug("Building ${building.description} is bankrupt! Blowing it up!")
                buildingsToRaze.add(coordinate)
            }
            buildingsToRaze.forEach { coordinate ->
                cityMap.bulldoze(coordinate, coordinate)
            }
        }
    }
}