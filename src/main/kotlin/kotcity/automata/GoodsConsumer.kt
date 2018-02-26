package kotcity.automata

import kotcity.data.BuildingType
import kotcity.data.CityMap
import kotcity.data.Tradeable

class GoodsConsumer(val cityMap: CityMap) {
    fun tick() {
        cityMap.locations().forEach { location ->
            val building = location.building
            if (building.type == BuildingType.RESIDENTIAL) {
                // eat up a good for every worker we have on board...
                building.subtractInventory(Tradeable.GOODS, building.quantityOnHand(Tradeable.LABOR))
            }
        }
    }
}