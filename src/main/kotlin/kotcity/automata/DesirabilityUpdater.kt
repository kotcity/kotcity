package kotcity.automata

import kotcity.data.*
import kotcity.pathfinding.Path
import kotcity.pathfinding.Pathfinder

const val MAX_DISTANCE = 100

private const val ZONES_TO_UPDATE = 512

object DesirabilityUpdater {

    fun update(cityMap: CityMap) {
        // let's update the desirability...
        cityMap.desirabilityLayers.forEach { desirabilityLayer ->

            when (desirabilityLayer.zoneType) {
                ZoneType.RESIDENTIAL -> updateResidential(cityMap, desirabilityLayer)
                ZoneType.INDUSTRIAL -> updateIndustrial(cityMap, desirabilityLayer)
                ZoneType.COMMERCIAL -> updateCommercial(cityMap, desirabilityLayer)
            }

        }
    }

    private fun updateCommercial(cityMap: CityMap, desirabilityLayer: DesirabilityLayer) {

        // ok... we just gotta find each block with an industrial zone...
        val commercialZones = zoneCoordinates(cityMap, ZoneType.COMMERCIAL)

        commercialZones.forEach { coordinate ->

            if (!Pathfinder.nearbyRoad(cityMap, listOf(coordinate))) {
                desirabilityLayer[coordinate] = 0.0
            } else {
                val availableGoods = ResourceFinder.nearbyAvailableTradeable(cityMap, Tradeable.GOODS, listOf(coordinate), MAX_DISTANCE)
                val availableLabor = ResourceFinder.nearbyAvailableTradeable(cityMap, Tradeable.LABOR, listOf(coordinate), MAX_DISTANCE)

                val score = if (availableGoods.count() == 0 || availableLabor.count() == 0) {
                    0.0
                } else {
                    listOf(*availableGoods.toTypedArray(), *availableLabor.toTypedArray()).map {
                        it.second.toDouble() / (it.first.distance() * 0.1)
                    }.sum()
                }
                desirabilityLayer[coordinate] = score
            }
        }

        trimDesirabilityLayer(desirabilityLayer, commercialZones)
    }

    private fun updateIndustrial(cityMap: CityMap, desirabilityLayer: DesirabilityLayer) {

        // ok... we just gotta find each block with an industrial zone...
        val industryZones = zoneCoordinates(cityMap, ZoneType.INDUSTRIAL)

        industryZones.forEach { coordinate ->

            if (!Pathfinder.nearbyRoad(cityMap, listOf(coordinate))) {
                desirabilityLayer[coordinate] = 0.0
            } else {
                val availableLabor = ResourceFinder.nearbyAvailableTradeable(cityMap, Tradeable.LABOR, listOf(coordinate), MAX_DISTANCE)

                val score = if (availableLabor.count() == 0) {
                    0.0
                } else {
                    availableLabor.map {
                        it.second.toDouble() / (it.first.distance() / 0.1)
                    }.sum()
                }
                desirabilityLayer[coordinate] = score * 10
            }
        }

        trimDesirabilityLayer(desirabilityLayer, industryZones)
    }

    private fun trimDesirabilityLayer(desirabilityLayer: DesirabilityLayer, zones: List<BlockCoordinate>) {
        // now trim any that were not in our zones...
        val keysToTrim = mutableListOf<BlockCoordinate>()
        desirabilityLayer.forEach { t, _ ->
            if (!zones.contains(t)) {
                keysToTrim.add(t)
            }
        }

        keysToTrim.forEach { desirabilityLayer.remove(it) }
    }

    private fun zoneCoordinates(cityMap: CityMap, zoneType: ZoneType): List<BlockCoordinate> {
        return cityMap.zoneLayer.toList().filter { it.second == Zone(zoneType) }.map { it.first }
    }

    private fun updateResidential(cityMap: CityMap, desirabilityLayer: DesirabilityLayer) {
        // we like being near places that NEED labor
        // we like being near places that PROVIDE goods

        val residentialZones = zoneCoordinates(cityMap, ZoneType.RESIDENTIAL)
        residentialZones.forEach { coordinate ->
            if (!Pathfinder.nearbyRoad(cityMap, listOf(coordinate))) {
                desirabilityLayer[coordinate] = 0.0
            } else {
                val availableJobs = ResourceFinder.nearbyBuyingTradeable(cityMap, Tradeable.LABOR, listOf(coordinate), MAX_DISTANCE)

                val score = if (availableJobs.count() == 0) {
                    0.0
                } else {
                    availableJobs.map {
                        it.second.toDouble() / ( it.first.distance() / 0.1 )
                    }.sum()
                }
                desirabilityLayer[coordinate] = score * 10
            }
        }

        trimDesirabilityLayer(desirabilityLayer, residentialZones)

    }
}