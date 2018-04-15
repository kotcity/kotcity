package kotcity.automata

import kotcity.data.*
import kotcity.data.buildings.*
import kotcity.util.Debuggable

class ZotPopulator(val cityMap: CityMap) : Debuggable {
    override var debug: Boolean = false

    fun tick() {
        cityMap.eachLocation { location ->

            // we gotta SKIP roads...
            if (location.building !is Road && location.building !is Railroad) {
                val newZots = when (location.building::class) {
                    Residential::class -> updateResidential(location)
                    Commercial::class -> updateCommercial(location)
                    Industrial::class -> updateIndustrial(location)
                    else -> {
                        mutableListOf()
                    }
                }
                val finalNewZots = newZots.plus(genericZots(location))

                val existingZots = location.building.zots
                val combinedZots: List<Zot> = combineZots(existingZots, finalNewZots)
                if (combinedZots.isNotEmpty()) {
                    debug { "New zots for building: $combinedZots" }
                }
                location.building.zots = combinedZots
            }

        }
    }

    private fun combineZots(existingZots: List<Zot>, newZots: List<Zot>): List<Zot> {
        val summedZots = mutableListOf<Zot>()
        newZots.forEach { newZot ->
            val oldZot = existingZots.find { it.type == newZot.type }
            if (oldZot == null) {
                summedZots.add(newZot)
            } else {
                val summedZot = Zot(oldZot.type)
                summedZot.age = oldZot.age + newZot.age
                summedZots.add(summedZot)
            }
        }
        return summedZots
    }

    private fun updateIndustrial(location: Location): List<Zot> {
        val building = location.building
        val zotList = mutableListOf<Zot>()

        if (building.totalBeingBought(Tradeable.LABOR) == 0) {
            zotList.add(Zot(ZotType.NO_WORKERS))
        }

        return zotList
    }

    private fun updateCommercial(location: Location): List<Zot> {
        val building = location.building
        val zotList = mutableListOf<Zot>()

        if (building.totalBeingBought(Tradeable.LABOR) == 0) {
            zotList.add(Zot(ZotType.NO_WORKERS))
        }

        if (cityMap.trafficNearby(location.coordinate, 5) <= 50) {
            zotList.add(Zot(ZotType.NO_CUSTOMERS))
        }

        if (cityMap.pollutionNearby(location.coordinate, Tunable.POLLUTION_RADIUS) > Tunable.POLLUTION_WARNING) {
            zotList.add(Zot(ZotType.TOO_MUCH_POLLUTION))
        }

        return zotList
    }

    private fun genericZots(location: Location): MutableList<Zot> {
        val building = location.building
        val zotList = mutableListOf<Zot>()
        if (!building.powered) {
            zotList.add(Zot(ZotType.NO_POWER))
        }
        return zotList
    }

    private fun updateResidential(location: Location): List<Zot> {

        val building = location.building
        val zotList = mutableListOf<Zot>()

        if (building.quantityOnHand(Tradeable.GOODS) <= 0) {
            zotList.add(Zot(ZotType.NO_GOODS))
        }

        if (cityMap.trafficNearby(location.coordinate, Tunable.TRAFFIC_RADIUS) > Tunable.MAX_TRAFFIC) {
            zotList.add(Zot(ZotType.TOO_MUCH_TRAFFIC))
        }

        if (cityMap.pollutionNearby(location.coordinate, Tunable.POLLUTION_RADIUS) > Tunable.POLLUTION_WARNING) {
            zotList.add(Zot(ZotType.TOO_MUCH_POLLUTION))
        }

        return zotList
    }
}
