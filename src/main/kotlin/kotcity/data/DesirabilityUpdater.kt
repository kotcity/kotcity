package kotcity.data

import kotcity.pathfinding.Pathfinder

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
            desirabilityLayer[coordinate] = 0.0
        }
    }

    private fun updateIndustrial(cityMap: CityMap, desirabilityLayer: DesirabilityLayer) {
        desirabilityLayer.keys().forEach { coordinate ->

            val nearestLabor = Pathfinder.pathToNearestLabor(cityMap, listOf(coordinate))?.distance() ?: 0

            // last step...
            desirabilityLayer[coordinate] = (100 - nearestLabor).toDouble()

        }
    }

    private fun updateResidential(cityMap: CityMap, desirabilityLayer: DesirabilityLayer) {
        // we like being near places that NEED labor
        // we like being near places that PROVIDE goods
        desirabilityLayer.keys().forEach { coordinate ->
            var nearestJob = Pathfinder.pathToNearestJob(cityMap, listOf(coordinate))?.distance() ?: 0
        }
    }
}