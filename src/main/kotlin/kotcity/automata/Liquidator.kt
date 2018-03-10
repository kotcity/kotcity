package kotcity.automata

import kotcity.data.*
import kotcity.util.Debuggable
import kotlin.reflect.KClass

class Liquidator(val cityMap: CityMap) : Debuggable {
    override var debug: Boolean = false

    fun tick() {

        // we will only kill some of what needs to go...
        val bankruptLocations = bankruptLocations()

        val howManyNeedDestruction : Int = Math.floor(bankruptLocations.count() * 0.01).toInt().coerceAtMost(15)
        debug("Blowing up $howManyNeedDestruction buildings...")

        val start = System.currentTimeMillis()

        var bulldozedCounts = mutableMapOf<KClass<out Building>, Int>().withDefault { 0 }
        bulldozedCounts[Residential::class] = 0
        bulldozedCounts[Commercial::class] = 0
        bulldozedCounts[Industrial::class] = 0

        bankruptLocations.shuffled().take(howManyNeedDestruction).forEach { location ->
            if (System.currentTimeMillis() - start > 5000) {
                debug("Out of time during liquidation...")
                updateBulldozedCount(bulldozedCounts)
                return
            }
            debug("Building ${location.building.description} is bankrupt or has no goods! Blowing it up!")
            cityMap.bulldoze(location.coordinate, location.coordinate)
            bulldozedCounts[location.building::class] = (bulldozedCounts[location.building::class] ?: 0) + 1
        }
        updateBulldozedCount(bulldozedCounts)
    }

    private fun updateBulldozedCount(bulldozeCounts: MutableMap<KClass<out Building>, Int>) {
        debug("After all that blowing up, the count is: $bulldozeCounts")
        cityMap.bulldozedCounts = bulldozeCounts
    }

    private fun bankruptLocations(): List<Location> {
        return cityMap.locations().toList().filter { location ->
            val noMoney = (location.building.quantityOnHand(Tradeable.MONEY) <= 0)
            val isResidentialWithNoGoods = (location.building is Residential && location.building.quantityOnHand(Tradeable.GOODS) <= 0)
            noMoney || isResidentialWithNoGoods
        }
    }
}