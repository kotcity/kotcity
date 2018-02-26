package kotcity.automata

import kotcity.data.BlockCoordinate
import kotcity.data.CityMap
import kotcity.data.Location
import kotcity.data.Tradeable
import kotcity.util.Debuggable

class Liquidator(val cityMap: CityMap) : Debuggable {
    override var debug: Boolean = false

    fun tick() {

        // we will only kill half of what needs to go...
        val bankruptLocations = bankruptLocations()

        val howManyNeedDestruction : Int = Math.floor(bankruptLocations.count() * 0.5).toInt()
        debug("Blowing up $howManyNeedDestruction buildings...")

        bankruptLocations.shuffled().take(howManyNeedDestruction).forEach { location ->
            debug("Building ${location.building.description} is bankrupt! Blowing it up!")
            cityMap.bulldoze(location.coordinate, location.coordinate)
        }
    }

    private fun bankruptLocations(): List<Location> {
        return cityMap.locations().toList().filter { location ->
            location.building.quantityOnHand(Tradeable.MONEY) <= 0
        }
    }
}