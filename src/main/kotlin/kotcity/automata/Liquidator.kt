package kotcity.automata

import kotcity.data.*
import kotcity.util.Debuggable

class Liquidator(val cityMap: CityMap) : Debuggable {
    override var debug: Boolean = false

    fun tick() {

        // we will only kill half of what needs to go...
        val bankruptLocations = bankruptLocations()

        val howManyNeedDestruction : Int = Math.floor(bankruptLocations.count() * 0.5).toInt()
        debug("Blowing up $howManyNeedDestruction buildings...")

        val start = System.currentTimeMillis()

        bankruptLocations.shuffled().take(howManyNeedDestruction).forEach { location ->
            if (System.currentTimeMillis() - start > 5000) {
                debug("Out of time during liquidation...")
                return
            }
            debug("Building ${location.building.description} is bankrupt or has no goods! Blowing it up!")
            cityMap.bulldoze(location.coordinate, location.coordinate)
        }
    }

    private fun bankruptLocations(): List<Location> {
        return cityMap.locations().toList().filter { location ->
            val noMoney = (location.building.quantityOnHand(Tradeable.MONEY) <= 0)
            val isResidentialWithNoGoods = (location.building.type == BuildingType.RESIDENTIAL && location.building.quantityOnHand(Tradeable.GOODS) <= 0)
            noMoney || isResidentialWithNoGoods
        }
    }
}