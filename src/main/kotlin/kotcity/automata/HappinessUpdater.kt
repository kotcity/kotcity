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
                when {
                    location.building is Residential -> processResidential(it)
                    location.building is Commercial -> processCommercial(it)
                    location.building is Industrial -> processIndustrial(it)
                }
            }

        }
    }

    private fun processIndustrial(location: Location) {
        // TODO
    }

    private fun processCommercial(location: Location) {
        // TODO
    }

    private fun processResidential(location: Location) {
        // TODO
    }
}