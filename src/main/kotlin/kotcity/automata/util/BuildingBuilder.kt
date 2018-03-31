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
            // TODO: fuzz to 50% of building width...
            val fuzzedCoordinate: BlockCoordinate = coordinate.fuzz()

            val buildingZone: Zone? = findZoneForBuilding(newBuilding)

            if (buildingZone != null) {
                val buildingBlocks = cityMap.buildingBlocks(fuzzedCoordinate, newBuilding)
                val validToBuild: Boolean = checkFootprint(buildingZone, buildingBlocks)
                if (validToBuild) {
                    debug("Trying to build $newBuilding at $fuzzedCoordinate")
                    if (cityMap.canBuildBuildingAt(newBuilding, fuzzedCoordinate)) {
                        done = true
                        cityMap.build(newBuilding, fuzzedCoordinate)
                    }
                }
            }

            tries++

        }
    }

    private fun checkFootprint(buildingZone: Zone, buildingBlocks: List<BlockCoordinate>): Boolean {
        return buildingBlocks.all { cityMap.zoneLayer[it] == buildingZone }
    }

    private fun findZoneForBuilding(newBuilding: Building): Zone? {
        return when (newBuilding::class) {
            Residential::class ->  Zone.RESIDENTIAL
            Commercial::class -> Zone.COMMERCIAL
            Industrial::class -> Zone.INDUSTRIAL
            else -> null
        }
    }


}