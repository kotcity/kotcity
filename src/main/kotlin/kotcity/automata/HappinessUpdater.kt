package kotcity.automata

import kotcity.data.*
import kotcity.data.buildings.Building
import kotcity.data.buildings.Commercial
import kotcity.data.buildings.Industrial
import kotcity.data.buildings.Residential
import kotcity.util.Debuggable

class HappinessUpdater(val cityMap: CityMap) : Debuggable {
    override var debug: Boolean = false

    private fun developedZone(building: Building): Boolean {
        return when (building) {
            is Residential,
            is Commercial,
            is Industrial -> true
            else -> false
        }
    }

    fun tick() {
        // get all populated "zones"...
        val populatedZones = cityMap.locations().filter { developedZone(it.building) }
        // OK! we got them...
        // now an ugly case statement...
        populatedZones.parallelStream().forEach { location ->
            location?.let {
                // ok great! let's figure out what kinda zone we have...
                val zoneHappiness = when {
                    location.building is Residential -> processResidential(it)
                    location.building is Commercial -> processCommercial(it)
                    location.building is Industrial -> processIndustrial(it)
                    else -> {
                        0.0
                    }
                }

                it.building.goodwill += zoneHappiness.toInt()
                it.building.goodwill.coerceIn(-100..100)

                // TODO: we will probably have some generic ones too... like fire coverage crime... pollution... etc.
                location.building.happiness = zoneHappiness.toInt()
            }
        }
    }

    private fun processIndustrial(location: Location): Double {
        var newValue = 0.0
        newValue = hasLabor(location, newValue)

        return newValue
    }

    private fun processCommercial(location: Location): Double {
        var newValue = 0.0
        newValue = hasLabor(location, newValue)

        if (location.building.zots.any { it.type == ZotType.TOO_MUCH_POLLUTION }) {
            newValue += Tunable.POLLUTION_HAPPINESS_PENALTY
        }

        return newValue
    }

    private fun hasLabor(location: Location, newValue: Double): Double {
        // we are happy if we don't need any workers...
        var result = newValue
        val laborConsumed = location.building.consumesQuantity(Tradeable.LABOR).toDouble()
        val laborBuying = location.building.totalBeingBought(Tradeable.LABOR).toDouble()
        val laborWanted = location.building.currentQuantityWanted(Tradeable.LABOR).toDouble()

        if (laborBuying == 0.0) {
            return -3.0
        }
        if (laborWanted == 0.0) {
            return 5.0
        }

        // let's figure out a ratio...
        val ratio = laborBuying / laborConsumed
        // debug("Building is buying $laborBuying and should be consuming $laborConsumed. That makes the ratio $ratio")

        result += when (ratio) {
            in 0.0..0.2 -> 1.0
            in 0.2..0.4 -> 2.0
            in 0.4..0.6 -> 3.0
            in 0.6..0.8 -> 4.0
            in 0.8..10.0 -> 5.0
            else -> 0.0
        }
        return result
    }

    private fun processResidential(location: Location): Double {
        var newValue = 0.0

        // res is happy when everyone is employed...
        val laborProvided = location.building.producesQuantity(Tradeable.LABOR).toDouble()
        val laborForSale = location.building.currentQuantityForSale(Tradeable.LABOR).toDouble()
        val ratio = laborForSale / laborProvided
        // debug("Building is providing $laborProvided and is selling $laborForSale. The employment ratio is: $ratio.")
        newValue += when (ratio) {
            in 0.0..0.2 -> 1.0
            in 0.2..0.4 -> 2.0
            in 0.4..0.6 -> 3.0
            in 0.6..0.8 -> 4.0
            in 0.8..10.0 -> 5.0
            else -> 0.0
        }

        // we get angry if there is too much traffic...
        if (location.building.zots.any { it.type == ZotType.TOO_MUCH_TRAFFIC }) {
            newValue -= 3.0
        }

        if (location.building.zots.any { it.type == ZotType.TOO_MUCH_POLLUTION }) {
            newValue += Tunable.POLLUTION_HAPPINESS_PENALTY
        }

        // TODO: we should also be slightly influenced by the happiness of our neighbors... will return here later...
        return newValue
    }
}
