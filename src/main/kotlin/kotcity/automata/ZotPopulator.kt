package kotcity.automata

import kotcity.data.*
import kotcity.util.Debuggable

class ZotPopulator(val cityMap: CityMap): Debuggable {
    override var debug: Boolean = false

    fun tick() {
        cityMap.eachLocation { location ->

            // we gotta SKIP roads...
            if (location.building !is Road) {
                val newZots = when(location.building::class) {
                    Residential::class -> updateResidential(location)
                    Commercial::class -> updateCommercial(location)
                    Industrial::class -> updateIndustrial(location)
                    else -> {
                        mutableListOf()
                    }
                }

                location.building.zots = newZots.plus(genericZots(location))
            }

        }
    }

    private fun updateIndustrial(location: Location): List<Zot> {
        val building = location.building
        val zotList = mutableListOf<Zot>()

        if (building.quantityOnHand(Tradeable.LABOR) <= 0) {
            zotList.add(Zot.NO_WORKERS)
        }

        if (!hasNearbyTraffic(location)) {
            zotList.add(Zot.TOO_MUCH_TRAFFIC)
        }

        return zotList
    }

    private fun updateCommercial(location: Location): List<Zot> {
        val building = location.building
        val zotList = mutableListOf<Zot>()

        if (building.quantityOnHand(Tradeable.LABOR) <= 0) {
            zotList.add(Zot.NO_WORKERS)
        }

        if (!hasNearbyTraffic(location)) {
            zotList.add(Zot.NOT_ENOUGH_CUSTOMERS)
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

        if (hasNearbyTraffic(location)) {
            zotList.add(Zot.TOO_MUCH_TRAFFIC)
        }

        return zotList
    }

    private fun hasNearbyTraffic(location: Location): Boolean {
        val neighboringBlocks = location.coordinate.neighbors(5)
        val nearbyRoads = neighboringBlocks.flatMap { cityMap.cachedBuildingsIn(it) }
                                                       .filter { it.building is Road }

        val trafficCount = nearbyRoads.sumBy { cityMap.trafficLayer[it.coordinate]?.toInt() ?: 0}
        debug("The building ${location.building} has $trafficCount nearby")
        return trafficCount > 100
    }
}