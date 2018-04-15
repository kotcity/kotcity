package kotcity.automata

import kotcity.data.CityMap
import kotcity.data.buildings.Commercial
import kotcity.data.buildings.Industrial
import kotcity.data.Tradeable
import kotcity.util.Debuggable

class Manufacturer(val cityMap: CityMap) : Debuggable {

    override var debug = false

    fun tick() {
        // for each industrial zone we want to see if we have at least one labor...
        cityMap.locations().forEach { location ->
            when (location.building) {
                is Industrial -> handleIndustrial(location.building)
                is Commercial -> handleCommercial(location.building)
                else -> {
                    Unit
                }
            }
        }
    }

    private fun handleCommercial(building: Commercial) {
        // every worker can flip 3 goods...
        val availableLabor: Int = building.totalBeingBought(Tradeable.LABOR) * 10
        val availableWholesaleGoods: Int = building.totalBeingBought(Tradeable.WHOLESALE_GOODS)
        // we want to convert "wholesale goods" to "goods"
        if (availableWholesaleGoods == 0 || availableLabor == 0) {
            debug { "${building.description}: We are missing either goods or workers..." }
            return
        }

        // produce up to 10x...
        val maxToProduce: Int = building.consumesQuantity(Tradeable.WHOLESALE_GOODS) * 10

        repeat(availableLabor) {
            if (building.totalBeingBought(Tradeable.WHOLESALE_GOODS) > 0 && building.quantityOnHand(Tradeable.GOODS) < maxToProduce) {
                building.subtractInventory(Tradeable.WHOLESALE_GOODS, 1)
                building.addInventory(Tradeable.GOODS, 4)
                debug { "${building.description}: Converted 1 wholesale goods to 4 goods..." }
            }
        }
        building.payWorkers()
    }

    private fun handleIndustrial(building: Industrial) {
        // TODO: we probably should look to see how much money we have...
        val availableLabor: Int = building.totalBeingBought(Tradeable.LABOR)
        // OK... for every labor we have here we get one thing that we produce...
        val products: List<Tradeable> = building.productList()
        if (availableLabor > 0) {
            products.forEach { tradeable ->
                debug { "${building.description} just manufactured $availableLabor $tradeable" }
                building.addInventory(tradeable, availableLabor * 10)
            }
        } else {
            debug { "Wanted to make products but we didn't have any labor!" }
        }
        building.payWorkers()
    }
}
