package kotcity.automata

import kotcity.data.*
import kotcity.util.Debuggable
import kotlin.reflect.KClass

class Liquidator(val cityMap: CityMap) : Debuggable {
    override var debug: Boolean = false

    fun tick() {

        if (cityMap.censusTaker.population < 10) {
            return
        }

        // we will only kill some of what needs to go...
        val bankruptLocations = locationsToDestroy()

        val bulldozedCounts = mutableMapOf<Zone, Int>().withDefault { 0 }
        bulldozedCounts[Zone.RESIDENTIAL] = 0
        bulldozedCounts[Zone.COMMERCIAL] = 0
        bulldozedCounts[Zone.INDUSTRIAL] = 0

        if (bankruptLocations.isNotEmpty()) {
            val howManyNeedDestruction : Int = Math.floor(bankruptLocations.count() * 0.10).toInt().coerceIn(1 .. 15)
            debug("Blowing up $howManyNeedDestruction buildings...")

            val start = System.currentTimeMillis()

            bankruptLocations.shuffled().take(howManyNeedDestruction).forEach { location ->
                if (System.currentTimeMillis() - start > 5000) {
                    debug("Out of time during liquidation...")
                    updateBulldozedCount(bulldozedCounts)
                    return
                }
                debug("Building ${location.building.description} is bankrupt or has no goods! Blowing it up!")
                cityMap.bulldoze(location.coordinate, location.coordinate)
                bulldozedCounts[zoneForBuilding(location.building::class)] = (bulldozedCounts[zoneForBuilding(location.building::class)] ?: 0) + 1
            }

        }

        updateBulldozedCount(bulldozedCounts)
    }

    private fun updateBulldozedCount(bulldozeCounts: MutableMap<Zone, Int>) {
        debug("After all that blowing up, the count is: $bulldozeCounts")
        cityMap.bulldozedCounts = bulldozeCounts
    }

    private fun zoneForBuilding(klass: KClass<out Building>): Zone {
        return when (klass) {
            Residential::class -> Zone.RESIDENTIAL
            Commercial::class -> Zone.COMMERCIAL
            Industrial::class -> Zone.INDUSTRIAL
            else -> {
                throw RuntimeException("Liquidator doesn't know to work with $klass!")
            }
        }
    }

    private fun locationsToDestroy(): List<Location> {
        return cityMap.locations().toList().filter { location ->
            val building = location.building
            building is Residential || building is Commercial || building is Industrial
        }.filter { location ->
            val noMoney = (location.building.quantityOnHand(Tradeable.MONEY) <= -50)
            val isResidentialWithNoGoods = (location.building is Residential && location.building.quantityOnHand(Tradeable.GOODS) <= 0)

            val outOfGoodwill = location.building.goodwill < -99

            noMoney || isResidentialWithNoGoods || outOfGoodwill
        }
    }
}