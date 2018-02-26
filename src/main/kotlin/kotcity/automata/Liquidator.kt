package kotcity.automata

import kotcity.data.*
import kotcity.util.Debuggable

class Liquidator(val cityMap: CityMap) : Debuggable {
    override var debug: Boolean = false

    fun tick() {

        // we will only kill half of what needs to go...
        val bankruptLocations = bankruptLocations()

        val howManyNeedDestruction : Int = Math.floor(bankruptLocations.count() * 0.1).toInt()
        debug("Blowing up $howManyNeedDestruction buildings...")

        val start = System.currentTimeMillis()

        var bulldozedCounts = mutableMapOf<BuildingType, Int>().withDefault { 0 }
        bulldozedCounts[BuildingType.RESIDENTIAL] = 0
        bulldozedCounts[BuildingType.INDUSTRIAL] = 0
        bulldozedCounts[BuildingType.COMMERCIAL] = 0

        bankruptLocations.shuffled().take(howManyNeedDestruction).forEach { location ->
            if (System.currentTimeMillis() - start > 5000) {
                debug("Out of time during liquidation...")
                updateBulldozedCount(bulldozedCounts)
                return
            }
            debug("Building ${location.building.description} is bankrupt or has no goods! Blowing it up!")
            cityMap.bulldoze(location.coordinate, location.coordinate)
            bulldozedCounts[location.building.type] = (bulldozedCounts[location.building.type] ?: 0) + 1
        }
        updateBulldozedCount(bulldozedCounts)
    }

    private fun updateBulldozedCount(bulldozeCounts: MutableMap<BuildingType, Int>) {
        debug("After all that blowing up, the count is: $bulldozeCounts")
        cityMap.bulldozedCounts = bulldozeCounts
    }

    private fun bankruptLocations(): List<Location> {
        return cityMap.locations().toList().filter { location ->
            val noMoney = (location.building.quantityOnHand(Tradeable.MONEY) <= 0)
            val isResidentialWithNoGoods = (location.building.type == BuildingType.RESIDENTIAL && location.building.quantityOnHand(Tradeable.GOODS) <= 0)
            noMoney || isResidentialWithNoGoods
        }
    }
}