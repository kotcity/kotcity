package kotcity.automata

import kotcity.data.CityMap
import kotcity.data.buildings.Residential
import kotcity.data.Tradeable
import kotcity.util.Debuggable
import tornadofx.runLater

enum class CountType {
    SUPPLY,
    DEMAND
}

/**
 * Stores supply/demand stats, used by charts and so forth.
 */
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

    fun consumeCount(tradeable: Tradeable) = counts[Pair(CountType.DEMAND, tradeable)] ?: 0

    fun supplyCount(tradeable: Tradeable) = counts[Pair(CountType.SUPPLY, tradeable)] ?: 0
}

/**
 * Used to figure out population and economy stats by CityMap...
 * @param cityMap the map we are concerned with...
 */
class CensusTaker(val cityMap: CityMap) : Debuggable {

    override var debug: Boolean = false
    var population = 0
    var resourceCounts: ResourceCounts = ResourceCounts()
    /**
     * A list of callbacks that we will invoke when we update our stats...
     */
    private val listeners = mutableListOf<() -> Unit>()

    fun tick() {
        calculatePopulation()
        supplyAndDemand()

        // poke anyone listening to us...
        if (listeners.count() > 0) {
            runLater {
                listeners.forEach { it() }
            }
        }
    }

    /**
     * Loop over each residential building and count the labor...
     */
    private fun calculatePopulation() {
        var tempPop = 0
        cityMap.locations().forEach { location ->
            val building = location.building
            if (building is Residential) {
                // see how many labor is provided...
                tempPop += building.producesQuantity(Tradeable.LABOR)
            }
        }
        population = tempPop
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

    /**
     * Returns the ratio of SUPPLY to DEMAND... in other words... the higher the number the more we want this [Tradeable]
     * @param tradeable the [Tradeable] in question
     */
    fun supplyRatio(tradeable: Tradeable): Double {
        // if we consume less than 10 total... create synthetic demand...
        if (resourceCounts.consumeCount(tradeable) < 10.0) {
            return 2.0
        }
        val result =
            resourceCounts.consumeCount(tradeable).toDouble() / resourceCounts.supplyCount(tradeable).toDouble()
        // if it's infinite... it's just 2.0!
        if (result == Double.NEGATIVE_INFINITY || result == Double.POSITIVE_INFINITY) {
            return 2.0
        }
        return result
    }

    /**
     * Used by the UI... adds a callback to us. When pop is updated we will call this function...
     * @param listener the callback (that we will call) :)
     */
    fun addUpdateListener(listener: () -> Unit) = this.listeners.add(listener)
}
