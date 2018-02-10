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

        // TODO: this is too fucking slow...

        desirabilityLayer.keys().forEach { coordinate ->
            desirabilityLayer[coordinate] = 0.0

            // res likes being near water...
            val potentialWaters = desirabilityLayer
                    .unquantized(coordinate)
                    .flatMap { coordinate.neighbors(3) }
                    .mapNotNull { coordinate ->
                        cityMap.groundLayer[coordinate]?.let {
                            Pair(coordinate, it)
                        }
                    }
                    .distinct()
                    .filter { mapTile ->
                        mapTile.second.type == TileType.WATER
                    }

            potentialWaters.mapNotNull {
                coordinate.distanceTo(it.first)
            }.min()?.let { waterDistance ->
                when (waterDistance) {
                    1.0 -> desirabilityLayer[coordinate]?.plus(3.0)
                    2.0 -> desirabilityLayer[coordinate]?.plus(2.0)
                    3.0 -> desirabilityLayer[coordinate]?.plus(1.0)
                    else -> {
                        println("I don't know how to handling being $waterDistance away from water...")
                    }
                }
            }

        }
    }
}