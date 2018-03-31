package kotcity.automata.util

import kotcity.data.*
import kotcity.util.Debuggable

class BuildingBuilder(val cityMap: CityMap): Debuggable {
    override var debug: Boolean = false

    private val maxTries = 50

    fun tryToBuild(coordinate: BlockCoordinate, newBuilding: Building) {
        var tries = 0
        var done = false

        while (tries < maxTries && !done) {
            val fuzzedCoordinate: BlockCoordinate = coordinate.fuzz()

            val buildingBlocks = cityMap.buildingBlocks(fuzzedCoordinate, newBuilding)

            debug("Trying to build $newBuilding at $fuzzedCoordinate")
            if (cityMap.canBuildBuildingAt(newBuilding, fuzzedCoordinate)) {
                done = true
                cityMap.build(newBuilding, fuzzedCoordinate)
            }

            tries++

        }
    }


}