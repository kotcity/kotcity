package kotcity.automata

import kotcity.data.*

class ZotPopulator(val cityMap: CityMap) {
    fun tick() {
        cityMap.eachLocation { location ->

            // we gotta SKIP roads...
            if (location.building !is Road) {
                val newZots = when(location.building.javaClass) {
                    Residential::class -> updateResidential(location)
                    else -> {
                        mutableListOf()
                    }
                }

                location.building.zots = newZots.plus(genericZots(location))
            }


        }
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
        val neighboringBlocks = location.coordinate.neighbors(3)
        val nearbyRoads = neighboringBlocks.flatMap { cityMap.cachedBuildingsIn(it) }
                                                       .filter { it.building is Road }

        val trafficCount = nearbyRoads.sumBy { cityMap.trafficLayer[it.coordinate]?.toInt() ?: 0}
        return trafficCount > 50
    }
}