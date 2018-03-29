package kotcity.automata

import kotcity.data.*
import kotcity.util.Debuggable

class ZotPopulator(val cityMap: CityMap): Debuggable {
    override var debug: Boolean = false

    fun tick() {
        cityMap.eachLocation { location ->

            // we gotta SKIP roads...
            if (location.building !is Road && location.building !is Railroad) {
                val newZots = when(location.building::class) {
                    Residential::class -> updateResidential(location)
                    Commercial::class -> updateCommercial(location)
                    Industrial::class -> updateIndustrial(location)
                    else -> {
                        mutableListOf()
                    }
                }
                val finalZots = newZots.plus(genericZots(location))
                debug("Final zots for ${location.building} are $finalZots")
                location.building.zots = finalZots
            }

        }
    }



    private fun updateIndustrial(location: Location): List<Zot> {
        val building = location.building
        val zotList = mutableListOf<Zot>()

        if (building.totalBeingBought(Tradeable.LABOR) == 0) {
            zotList.add(Zot.NO_WORKERS)
        }

        return zotList
    }

    private fun updateCommercial(location: Location): List<Zot> {
        val building = location.building
        val zotList = mutableListOf<Zot>()

        if (building.totalBeingBought(Tradeable.LABOR) == 0) {
            zotList.add(Zot.NO_WORKERS)
        }

        if (!cityMap.hasTrafficNearby(location.coordinate, 5, 50)) {
            zotList.add(Zot.NO_CUSTOMERS)
        }

        if (cityMap.pollutionNearby(location.coordinate, Tunable.POLLUTION_RADIUS) > Tunable.POLLUTION_WARNING) {
            zotList.add(Zot.TOO_MUCH_POLLUTION)
        }

        return zotList
    }

    private fun genericZots(location: Location): MutableList<Zot> {
        val building = location.building
        val zotList = mutableListOf<Zot>()
        if (!building.powered) {
            zotList.add(Zot.NO_POWER)
        }
        return zotList
    }

    private fun updateResidential(location: Location): List<Zot> {

        val building = location.building
        val zotList = mutableListOf<Zot>()

        if (building.quantityOnHand(Tradeable.GOODS) <= 0) {
            zotList.add(Zot.NO_GOODS)
        }

        if (cityMap.hasTrafficNearby(location.coordinate, Tunable.TRAFFIC_RADIUS, Tunable.MAX_TRAFFIC)) {
            zotList.add(Zot.TOO_MUCH_TRAFFIC)
        }

        if (cityMap.pollutionNearby(location.coordinate, Tunable.POLLUTION_RADIUS) > Tunable.POLLUTION_WARNING) {
            zotList.add(Zot.TOO_MUCH_POLLUTION)
        }

        return zotList
    }
}