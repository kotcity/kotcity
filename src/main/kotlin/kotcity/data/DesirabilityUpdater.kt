package kotcity.data

object DesirabilityUpdater {
    fun update(cityMap: CityMap) {
        // let's update the desirability...
        cityMap.desirabilityLayers.forEach { desirabilityLayer ->

            when(desirabilityLayer.zoneType) {
                ZoneType.RESIDENTIAL -> updateResidential(desirabilityLayer)
                ZoneType.INDUSTRIAL -> updateIndustrial(desirabilityLayer)
                ZoneType.COMMERCIAL -> updateCommercial(desirabilityLayer)
            }

        }
    }

    private fun updateCommercial(desirabilityLayer: DesirabilityLayer) {
        desirabilityLayer.keys().forEach { coordinate ->
            desirabilityLayer[coordinate] = 0.5
        }
    }

    private fun updateIndustrial(desirabilityLayer: DesirabilityLayer) {
        desirabilityLayer.keys().forEach { coordinate ->
            desirabilityLayer[coordinate] = 0.5
        }
    }

    private fun updateResidential(desirabilityLayer: DesirabilityLayer) {
        desirabilityLayer.keys().forEach { coordinate ->

            // res likes being near water...

            desirabilityLayer[coordinate] = 0.5
        }
    }
}