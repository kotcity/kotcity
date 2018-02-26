package kotcity.automata

import kotcity.data.Building
import kotcity.data.BuildingType
import kotcity.data.CityMap
import kotcity.data.Tradeable
import kotcity.util.Debuggable

class Manufacturer(val cityMap: CityMap): Debuggable {

    override var debug = false

    fun tick() {
        // for each industrial zone we want to see if we have at least one labor...
        cityMap.buildingLayer.values.forEach { building ->
            // let's see if it's industrial...
            if (building.type == BuildingType.INDUSTRIAL) {
                handleIndustrial(building)
            }
            if (building.type == BuildingType.COMMERCIAL) {
                handleCommercial(building)
            }
        }
    }

    private fun handleCommercial(building: Building) {
        // every worker can flip 3 goods...
        val availableLabor: Int = building.supplyCount(Tradeable.LABOR) * 10
        val availableWholesaleGoods: Int = building.supplyCount(Tradeable.WHOLESALE_GOODS)
        // we want to convert "wholesale goods" to "goods"
        if (availableWholesaleGoods == 0 || availableLabor == 0) {
            debug("${building.description}: We are missing either goods or workers...")
            return
        }

        val maxToProduce: Int = building.consumesQuantity(Tradeable.WHOLESALE_GOODS) * 2

        repeat(availableLabor) {
            if (building.supplyCount(Tradeable.WHOLESALE_GOODS) > 0 && building.quantityOnHand(Tradeable.GOODS) < maxToProduce) {
                building.subtractInventory(Tradeable.WHOLESALE_GOODS, 1)
                building.addInventory(Tradeable.GOODS, 1)
                debug("${building.description}: Converted 1 wholesale goods to goods...")
            }
        }
        building.payWorkers()
    }

    private fun handleIndustrial(building: Building) {
        // TODO: we probably should look to see how much money we have...
        val availableLabor: Int = building.supplyCount(Tradeable.LABOR)
        // OK... for every labor we have here we get one thing that we produce...
        val products: List<Tradeable> = building.productList()
        if (availableLabor > 0 && building.supplyCount(Tradeable.LABOR) > 0) {
            products.forEach { tradeable ->
                debug("${building.description} just manufactured $availableLabor $tradeable")
                building.addInventory(tradeable, availableLabor)
                building.payWorkers()
            }
        } else {
            debug("Wanted to make products but we didn't have any labor!")
        }
    }
}