package kotcity.automata

import kotcity.data.*
import kotcity.util.Debuggable

class HappinessUpdater(val cityMap: CityMap) : Debuggable {
    override var debug: Boolean = false

    private fun developedZone(building: Building): Boolean {
        return when (building::class) {
            Residential::class -> true
            Commercial::class -> true
            Industrial::class -> true
            else -> {
                false
            }
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
                // TODO: we will probably have some generic ones too... like fire coverage
                // crime... pollution... etc.
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

        return newValue
    }

    private fun hasLabor(location: Location, newValue: Double): Double {
        // we are happy if we don't need any workers...
        var newValue = newValue
        val laborConsumed = location.building.consumesQuantity(Tradeable.LABOR).toDouble()
        val laborNeeded = location.building.needs(Tradeable.LABOR).toDouble()

        if (laborNeeded == 0.0) {
            newValue = 1.0
        } else {
            // let's figure out a ratio...
            val presentLabor = laborNeeded - laborConsumed
            val ratio = presentLabor / laborConsumed
            newValue += when (ratio) {
                in 0.0..0.2 -> 1.0
                in 0.2..0.4 -> 2.0
                in 0.4..0.6 -> 3.0
                in 0.6..0.8 -> 4.0
                in 0.8..1.0 -> 5.0
                else -> 0.0
            }
        }
        return newValue
    }

    private fun processResidential(location: Location): Double {
        var newValue = 0.0

        // res is happy when everyone is employed...
        val laborProvided = location.building.producesQuantity(Tradeable.LABOR).toDouble()
        val laborForSale = location.building.quantityForSale(Tradeable.LABOR).toDouble()
        val ratio = laborForSale / laborProvided
        newValue += when (ratio) {
            in 0.0 .. 0.2 -> 1.0
            in 0.2 .. 0.4 -> 2.0
            in 0.4 .. 0.6 -> 3.0
            in 0.6 .. 0.8 -> 4.0
            in 0.8 .. 1.0 -> 5.0
            else -> 0.0
        }

        // we get angry if there is too much traffic...
        if (location.building.zots.contains(Zot.TOO_MUCH_TRAFFIC)) {
            newValue -= 3.0
        }

        // TODO: we should also be slightly influenced by the happiness of our neighbors... will return here later...
        return newValue
    }
}