package kotcity.automata

import kotcity.data.BlockCoordinate
import kotcity.data.CityMap
import kotcity.data.Tunable
import kotcity.ui.Algorithms
import kotcity.util.Debuggable

class LandValueUpdater(val cityMap: CityMap) : Debuggable {
    override var debug: Boolean = false

    private fun allZoneCoordinates() = cityMap.zoneLayer.keys.toList()

    fun tick() {
        // find the min and max
        var maxLandValue = 0

        // we want to loop over each zoned square and figure out how much "value" aka money is around it...
        allZoneCoordinates().forEach {
            // get buildings nearby...
            val howMuchMoney = nearbyWealth(it)
            if (howMuchMoney > maxLandValue) {
                maxLandValue = howMuchMoney
            }
        }

        // ok... now we gotta loop AGAIN except now we can compare ourselves against min / max...
        allZoneCoordinates().forEach {
            val howMuchMoney = nearbyWealth(it)
            val landValue =
                Algorithms.scale(howMuchMoney.toDouble(), 0.0, maxLandValue.toDouble(), 0.0, Tunable.MAX_LAND_VALUE)
            if (landValue == Double.NaN) {
                cityMap.landValueLayer[it] = 0.0
            } else {
                cityMap.landValueLayer[it] = landValue
            }
        }

        // try to clean it now...
        cityMap.landValueLayer.keys.toList().forEach { coordinate ->
            // OK, basically if this isn't zoned we gotta drop it...
            if (!cityMap.zoneLayer.containsKey(coordinate)) {
                cityMap.landValueLayer.remove(coordinate)
            }
        }
    }

    /**
     * See how much money is found nearby (10 tiles)
     * @param coordinate The coordinate to examine...
     */
    private fun nearbyWealth(coordinate: BlockCoordinate): Int {
        val locations = cityMap.nearestBuildings(coordinate, 10)
        return locations.sumBy {
            it.building.balance()
        }.coerceAtLeast(0)
    }
}
