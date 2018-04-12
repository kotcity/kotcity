package kotcity.data

import kotcity.util.Debuggable
import kotlinx.coroutines.experimental.async

object ContractChecker: Debuggable {
    override var debug: Boolean = true

    fun checkContracts(cityMap: CityMap) {
        cityMap.locations().forEach {
            val building = it.building
            synchronized(building.contracts) {
                building.contracts.toList().forEach {
                    async {
                        // make sure each building exists...
                        val from = it.from
                        val to = it.to
                        val buildings = listOf(from, to)
                        if (buildings.any { missingBuilding(cityMap, it) }) {
                            debug {"We had a bum contract! Either $from or $to does not exist!" }
                            building.contracts.remove(it)
                        }
                    }
                }
            }
        }
    }

    private fun missingBuilding(cityMap: CityMap, tradeEntity: TradeEntity): Boolean {
        // if it's from the outside (the nation) it cannot be missing...
        if (tradeEntity is OutsideTradeEntity) {
            return false
        }
        // ok... basically just look in the map and there should be a building where we say it is
        tradeEntity.building().let { building ->
            if (cityMap.cachedLocationsIn(tradeEntity.coordinate).map {it.building}.contains(building)) {
                // we found the building, so the building is not missing...
                return false
            }
        }
        return true
    }
}