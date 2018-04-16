package kotcity.automata.util

import kotcity.data.BlockCoordinate
import kotcity.data.buildings.Building
import kotcity.data.CityMap
import kotcity.data.Zone
import kotcity.pathfinding.Pathfinder
import kotcity.util.Debuggable

class BuildingBuilder(val cityMap: CityMap) : Debuggable {
    override var debug: Boolean = false

    private val pathfinder: Pathfinder = Pathfinder(cityMap)
    private val maxTries = 50

    fun tryToBuild(coordinate: BlockCoordinate, newBuilding: Building) {
        var tries = 0

        while (tries < maxTries) {
            val fuzzedCoordinate: BlockCoordinate = coordinate.fuzz((newBuilding.width))

            val buildingZone: Zone? = newBuilding.zone()

            if (buildingZone != null) {
                val buildingBlocks = newBuilding.buildingBlocks(fuzzedCoordinate)
                val validToBuild: Boolean = checkFootprint(buildingZone, buildingBlocks)
                if (validToBuild) {
                    debug { "Trying to build $newBuilding at $fuzzedCoordinate" }
                    if (cityMap.canBuildBuildingAt(newBuilding, fuzzedCoordinate)) {
                        cityMap.build(newBuilding, fuzzedCoordinate)
                        return
                    }
                }
            }
            tries++
        }
    }

    private fun checkFootprint(buildingZone: Zone, buildingBlocks: List<BlockCoordinate>): Boolean {
        val isZoned = buildingBlocks.all { cityMap.zoneLayer[it] == buildingZone }
        // we have to check near road again because we got fuzzed...
        val nearRoad = pathfinder.nearbyRoad(buildingBlocks)
        if (!isZoned) {
            debug { "Would have built but not all blocks were on the appropriate zone!" }
        }

        if (!nearRoad) {
            debug { "Would have built but we were not close enough to a road!" }
        }
        return isZoned && nearRoad
    }
}
