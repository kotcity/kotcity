package kotcity.automata

import kotcity.data.*
import kotcity.data.buildings.Commercial
import kotcity.data.buildings.Industrial
import kotcity.data.buildings.Residential
import kotcity.util.Debuggable

class TaxCollector(val cityMap: CityMap) : Debuggable {

    override var debug = false

    fun tick() {
        cityMap.locations().forEach { location ->
            val building = location.building
            if (listOf(Residential::class, Commercial::class, Industrial::class).contains(building::class)) {
                // figure out how much money this building has...
                val money = building.quantityOnHand(Tradeable.MONEY)
                var taxAmount = money * 0.20
                if (taxAmount < 5) {
                    taxAmount = 5.0
                }
                building.setInventory(Tradeable.MONEY, Math.floor(money - taxAmount).toInt())
                debug { "${building.description} has been taxed $taxAmount" }
            }
        }
    }
}
