package kotcity.automata

import kotcity.data.*
import kotcity.pathfinding.Pathfinder
import kotcity.util.Debuggable


class DesirabilityUpdater(val cityMap: CityMap): Debuggable {
    override var debug: Boolean = false

    private val maxDistance = 100
    private val pathFinder: Pathfinder = Pathfinder(cityMap)
    private val resourceFinder = ResourceFinder(cityMap)

    fun update() {
        // let's update the desirability...
        cityMap.desirabilityLayers.forEach { desirabilityLayer ->

            // TODO: worry about other levels later...
            if (desirabilityLayer.level == 1) {
                when (desirabilityLayer.zoneType) {
                    ZoneType.RESIDENTIAL -> updateResidential(desirabilityLayer)
                    ZoneType.INDUSTRIAL -> updateIndustrial(desirabilityLayer)
                    ZoneType.COMMERCIAL -> updateCommercial(desirabilityLayer)
                }
            }

        }
    }

    private fun updateCommercial(desirabilityLayer: DesirabilityLayer) {

        // ok... we just gotta find each block with an industrial zone...
        val commercialZones = zoneCoordinates(ZoneType.COMMERCIAL)

        commercialZones.forEach { coordinate ->

            if (!pathFinder.nearbyRoad(listOf(coordinate))) {
                desirabilityLayer[coordinate] = 0.0
            } else {
                val availableGoods = resourceFinder.nearbyBuyingTradeable(Tradeable.GOODS, listOf(coordinate), maxDistance)
                val availableLabor = resourceFinder.nearbyAvailableTradeable(Tradeable.LABOR, listOf(coordinate), maxDistance)

                val score = if (availableGoods.count() == 0) {
                    0.0
                } else {
                    listOf(*availableGoods.toTypedArray()).map {
                        it.second.toDouble() / (it.first.distance() * 0.1)
                    }.sum()
                }
                if (availableLabor.count() == 0) {
                    desirabilityLayer[coordinate] = 0.0
                } else {
                    desirabilityLayer[coordinate] = score
                }

            }
        }

        trimDesirabilityLayer(desirabilityLayer, commercialZones)
    }

    private fun updateIndustrial(desirabilityLayer: DesirabilityLayer) {

        // ok... we just gotta find each block with an industrial zone...
        val industryZones = zoneCoordinates(ZoneType.INDUSTRIAL)

        industryZones.forEach { coordinate ->

            if (!pathFinder.nearbyRoad(listOf(coordinate))) {
                desirabilityLayer[coordinate] = 0.0
            } else {
                val availableBuyingWholesaleGoods = resourceFinder.nearbyBuyingTradeable(Tradeable.WHOLESALE_GOODS, listOf(coordinate), maxDistance)
                val availableLabor = resourceFinder.nearbyAvailableTradeable(Tradeable.LABOR, listOf(coordinate), maxDistance)

                val score = if (availableBuyingWholesaleGoods.count() == 0) {
                    0.0
                } else {
                    availableBuyingWholesaleGoods.map {
                        it.second.toDouble() / (it.first.distance() / 0.1)
                    }.sum()
                }
                if (availableLabor.count() == 0) {
                    desirabilityLayer[coordinate] = 0.0
                } else {
                    desirabilityLayer[coordinate] = score * 10
                }

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

    private fun zoneCoordinates(zoneType: ZoneType): List<BlockCoordinate> {
        return cityMap.zoneLayer.toList().filter { it.second == Zone(zoneType) }.map { it.first }
    }

    // TODO: waaay too slow...
    private fun updateResidential(desirabilityLayer: DesirabilityLayer) {
        // we like being near places that NEED labor
        // we like being near places that PROVIDE goods

        val residentialZones = zoneCoordinates(ZoneType.RESIDENTIAL)
        residentialZones.forEach { coordinate ->
            if (!pathFinder.nearbyRoad(listOf(coordinate))) {
                desirabilityLayer[coordinate] = 0.0
            } else {
                val availableJobs = resourceFinder.nearbyBuyingTradeable(Tradeable.LABOR, listOf(coordinate), maxDistance)

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