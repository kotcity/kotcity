package kotcity.automata

import kotcity.data.BuildingType
import kotcity.data.CityMap
import kotcity.data.Tradeable
import kotcity.util.Debuggable

class TaxCollector(val cityMap: CityMap): Debuggable {

    override var debug = false

    fun tick() {
        cityMap.buildingLayer.forEach { _, building ->
            if (listOf(BuildingType.RESIDENTIAL, BuildingType.INDUSTRIAL, BuildingType.COMMERCIAL).contains(building.type)) {
                // figure out how much money this building has...
                val money = building.quantityOnHand(Tradeable.MONEY)
                var taxAmount = money * 0.20
                if (taxAmount < 5) {
                    taxAmount = 5.0
                }
                building.setInventory(Tradeable.MONEY, Math.floor(money - taxAmount).toInt())
                debug("${building.description} has been taxed $taxAmount")
            }
        }
    }
}