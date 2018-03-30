package kotcity.automata

import kotcity.data.CityMap
import kotcity.data.Residential
import kotcity.data.Tradeable
import kotcity.util.Debuggable
import tornadofx.runLater

enum class CountType {
    SUPPLY,
    DEMAND
}

data class EconomyReport(
        val tradeable: Tradeable,
        val supply: Int,
        val demand: Int,
        val balance: Int
)

class ResourceCounts {

    private val counts: MutableMap<Pair<CountType, Tradeable>, Int> = mutableMapOf()

    fun increment(countType: CountType, tradeable: Tradeable, quantity: Int): Int {
        val old = counts.getOrDefault(Pair(countType, tradeable), 0)
        val new = old + quantity
        counts[Pair(countType, tradeable)] = new
        return new
    }

    fun totals(): List<EconomyReport> {
        return Tradeable.values().map { tradeable ->
            val supply = counts[Pair(CountType.SUPPLY, tradeable)] ?: 0
            val demand = counts[Pair(CountType.DEMAND, tradeable)] ?: 0
            val balance = supply - demand
            EconomyReport(tradeable, supply, demand, balance)
        }
    }

    fun tradeBalance(tradeable: Tradeable): Int {
        val supply = counts[Pair(CountType.SUPPLY, tradeable)] ?: 0
        val demand = counts[Pair(CountType.DEMAND, tradeable)] ?: 0
        return supply - demand
    }

    fun consumeCount(tradeable: Tradeable): Int {
        return counts[Pair(CountType.DEMAND, tradeable)] ?: 0
    }

    fun supplyCount(tradeable: Tradeable): Int {
        return counts[Pair(CountType.SUPPLY, tradeable)] ?: 0
    }

}

class CensusTaker(val cityMap: CityMap): Debuggable {
    override var debug: Boolean = false
    var population = 0
    var resourceCounts: ResourceCounts = ResourceCounts()

    fun tick() {
        calculatePopulation()
        supplyAndDemand()
    }

    private fun calculatePopulation() {
        // loop over each residential building and count the labor...
        var tempPop = 0
        cityMap.locations().forEach { location ->
            val building = location.building
            if (building is Residential) {
                // see how many labor is provided...
                tempPop += building.producesQuantity(Tradeable.LABOR)
            }
        }
        population = tempPop
        if (listeners.count() > 0) {
            runLater {
                listeners.forEach { it() }
            }
        }
    }

    private fun supplyAndDemand() {
        val resourceCounts = ResourceCounts()
        cityMap.eachLocation { location ->
            location.building.produces.forEach { tradeable, quantity ->
                resourceCounts.increment(CountType.SUPPLY, tradeable, quantity)
            }
            location.building.consumes.forEach { tradeable, quantity ->
                resourceCounts.increment(CountType.DEMAND, tradeable, quantity)
            }
        }
        this.resourceCounts = resourceCounts
    }

    fun tradeBalance(tradeable: Tradeable): Int {
        return resourceCounts.tradeBalance(tradeable)
    }

    fun supplyRatio(tradeable: Tradeable): Double {
        // if we consume less than 10 total... create synthetic demand...
        if (resourceCounts.consumeCount(tradeable) < 10.0) {
            return 2.0
        }
        return resourceCounts.consumeCount(tradeable).toDouble() / resourceCounts.supplyCount(tradeable).toDouble()
    }

    private val listeners = mutableListOf<() -> Unit>()

    fun addUpdateListener(listener: () -> Unit) {
        this.listeners.add(listener)
    }
}