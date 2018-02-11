package kotcity.data

import kotcity.pathfinding.Pathfinder

val MAX_DISTANCE = 100

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
        desirabilityLayer.keys().forEach { coordinate ->

            val distanceToGoods = Pathfinder.pathToNearestTrade(cityMap, listOf(coordinate), 1) { building, qty ->
                building.sellingTradeable(Tradeable.GOODS, 1)
            }?.distance() ?: MAX_DISTANCE

            val distanceToLabor = Pathfinder.pathToNearestTrade(cityMap, listOf(coordinate), 1) { building, qty ->
                building.sellingTradeable(Tradeable.LABOR, 1)
            }?.distance() ?: MAX_DISTANCE

            desirabilityLayer[coordinate] = (MAX_DISTANCE - distanceToGoods - distanceToLabor).toDouble()
        }
    }

    private fun updateIndustrial(cityMap: CityMap, desirabilityLayer: DesirabilityLayer) {
        desirabilityLayer.keys().forEach { coordinate ->

            val nearestLabor = Pathfinder.pathToNearestLabor(cityMap, listOf(coordinate))?.distance() ?: MAX_DISTANCE

            // last step...
            desirabilityLayer[coordinate] = (MAX_DISTANCE - nearestLabor).toDouble()

        }
    }

    private fun updateResidential(cityMap: CityMap, desirabilityLayer: DesirabilityLayer) {
        // we like being near places that NEED labor
        // we like being near places that PROVIDE goods
        desirabilityLayer.keys().forEach { coordinate ->
            var nearestJob = Pathfinder.pathToNearestJob(cityMap, listOf(coordinate))?.distance() ?: MAX_DISTANCE
            desirabilityLayer[coordinate] = (MAX_DISTANCE - nearestJob).toDouble()
        }
    }
}