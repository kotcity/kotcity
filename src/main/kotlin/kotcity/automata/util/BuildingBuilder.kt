package kotcity.automata.util

import kotcity.data.*
import kotcity.util.Debuggable
import java.util.*

class BuildingBuilder(val cityMap: CityMap): Debuggable {
    override var debug: Boolean = false

    private val maxTries = 50

    fun tryToBuild(coordinate: BlockCoordinate, newBuilding: Building) {
        var tries = 0
        var done = false
        val layer: DesirabilityLayer = layerForBuilding(newBuilding) ?: return
        while (tries < maxTries && !done) {
            val fuzzedCoordinate: BlockCoordinate = coordinate.fuzz()

            // ok... desirability STILL has to be above 0...
            val buildingBlocks = cityMap.buildingBlocks(fuzzedCoordinate, newBuilding)
            // ok, each proposed block has to have desirability > 0
            val desirabilityScores = buildingBlocks.map { layer[it] ?: 0.0 }

            val acceptableDesirability = desirabilityScores.all { it > 0.0 }

            if (acceptableDesirability) {
                debug("Trying to build $newBuilding at $fuzzedCoordinate")
                if (cityMap.canBuildBuildingAt(newBuilding, fuzzedCoordinate)) {
                    done = true
                    cityMap.build(newBuilding, fuzzedCoordinate)
                }
            } else {
                debug("$fuzzedCoordinate didn't have any desirable blocks...")
            }
            tries++

        }
    }

    private fun layerForBuilding(building: Building): DesirabilityLayer? {
        return when (building::class) {
            Residential::class -> cityMap.desirabilityLayer(Zone.RESIDENTIAL, building.level)
            Commercial::class -> cityMap.desirabilityLayer(Zone.COMMERCIAL, building.level)
            Industrial::class -> cityMap.desirabilityLayer(Zone.INDUSTRIAL, building.level)
            else -> {
                null
            }
        }
    }

}