package kotcity.automata

import kotcity.data.BuildingType
import kotcity.data.CityMap
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
        cityMap.buildingLayer.forEach { _, building ->
            if (building.type == BuildingType.RESIDENTIAL) {
                // see how many labor is provided...
                tempPop += building.totalProvided(Tradeable.LABOR)
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

    private val listeners = mutableListOf<() -> Unit>()

    fun addUpdateListener(listener: () -> Unit) {
        this.listeners.add(listener)
    }
}