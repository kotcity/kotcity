package kotcity.data

import kotcity.pathfinding.Pathfinder
import kotcity.util.getRandomElements

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

            val distanceToGoods = Pathfinder.pathToNearestTrade(cityMap, listOf(coordinate), 1) { building, qty ->
                building.sellingTradeable(Tradeable.GOODS, 1)
            }?.distance() ?: MAX_DISTANCE

            val distanceToLabor = Pathfinder.pathToNearestTrade(cityMap, listOf(coordinate), 1) { building, qty ->
                building.sellingTradeable(Tradeable.LABOR, 1)
            }?.distance() ?: MAX_DISTANCE

            desirabilityLayer[coordinate] = (MAX_DISTANCE - distanceToGoods - distanceToLabor).toDouble()
        }

        trimDesirabilityLayer(desirabilityLayer, commercialZones)

    }

    private fun updateIndustrial(cityMap: CityMap, desirabilityLayer: DesirabilityLayer) {

        // ok... we just gotta find each block with an industrial zone...
        val industryZones = zoneCoordinates(cityMap, ZoneType.INDUSTRIAL)

        industryZones.forEach { coordinate ->

            val nearestLabor = Pathfinder.pathToNearestLabor(cityMap, listOf(coordinate))?.distance() ?: MAX_DISTANCE

            // last step...
            val score = (MAX_DISTANCE - nearestLabor).toDouble()
            desirabilityLayer[coordinate] = score
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
            val nearestJob = Pathfinder.pathToNearestJob(cityMap, listOf(coordinate))?.distance() ?: MAX_DISTANCE
            val desirabilityScore = (MAX_DISTANCE - nearestJob).toDouble()
            desirabilityLayer[coordinate] = desirabilityScore
        }

        trimDesirabilityLayer(desirabilityLayer, residentialZones)

    }
}