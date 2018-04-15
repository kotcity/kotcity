package kotcity.automata

import kotcity.data.CityMap
import kotcity.data.buildings.Residential
import kotcity.data.Tradeable

class GoodsConsumer(val cityMap: CityMap) {
    fun tick() {
        cityMap.locations().forEach { location ->
            val building = location.building
            if (building is Residential) {
                // eat up a good for every worker we have on board...
                val howManyToConsume = (building.quantityOnHand(Tradeable.LABOR) * 0.5).toInt().coerceAtLeast(1)
                building.subtractInventory(Tradeable.GOODS, howManyToConsume)
            }
        }
    }
}