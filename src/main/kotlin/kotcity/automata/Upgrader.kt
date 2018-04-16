package kotcity.automata

import kotcity.data.*
import kotcity.data.buildings.Building
import kotcity.util.Debuggable
import kotcity.util.randomElements

class Upgrader(val cityMap: CityMap) : Debuggable {
    override var debug: Boolean = false

    val assetManager = AssetManager(cityMap)
    private val maxTries = 50

    fun tick() {
        // (maybe) upgrade some buildings...
        val eligibleBuildings = cityMap.locations().filter { it.building.goodwill > 99 }

        val lowestLevel = eligibleBuildings.minBy { it.building.level }?.building?.level ?: 1

        val lowestLevelEligable = eligibleBuildings.filter { it.building.level == lowestLevel }

        // pick 3 random
        if (lowestLevelEligable.isNotEmpty()) {
            debug { "We have some buildings to upgrade!" }
        }
        lowestLevelEligable.randomElements(1).forEach { location ->
            val oldLevel = location.building.level
            if (oldLevel < 5) {
                val newLevel = oldLevel + 1
                val zone = location.building.zone()
                if (zone != null) {
                    val newBuilding = assetManager.findBuilding(zone, newLevel)
                    if (newBuilding != null) {
                        tryToUpgrade(location, newBuilding, location.building)
                    }
                }
            }
        }
    }

    // TODO: check for that new building and old building overlap
    private fun tryToUpgrade(location: Location, newBuilding: Building, oldBuilding: Building) {
        debug { "Trying to upgrade ${location.coordinate} to ${newBuilding.description}" }
        // try to build it where it sits...
        val coordinate = location.coordinate
        if (tryToBuildAt(coordinate, newBuilding)) {
            return
        } else {
            debug { "Cannot just replace original building!" }
        }

        var tries = 0
        while (tries < maxTries) {
            debug { "Trying a fuzzed location!" }

            val fuzzedCoordinate = coordinate.fuzz((newBuilding.width / 2).coerceAtLeast(1))

            val oldCoordinate = location.coordinate

            if (Location(oldCoordinate, oldBuilding).overlaps(Location(fuzzedCoordinate, newBuilding))) {
                debug { "It does overlap!" }
                if (tryToBuildAt(fuzzedCoordinate, newBuilding)) {
                    return
                }
            } else {
                debug { "Sorry, the proposed building does NOT overlap!" }
            }
            tries++
        }
    }

    private fun tryToBuildAt(coordinate: BlockCoordinate, newBuilding: Building): Boolean {
        val proposedFootprint: List<BlockCoordinate> = newBuilding.buildingBlocks(coordinate)
        debug { "The building will consume blocks: $proposedFootprint" }
        // OK... now look at each block... the level has to be BENEATH us
        val emptyOrLower = proposedFootprint.all { emptyOrLowerLevelThan(it, newBuilding.level) }
        val sameZone = proposedFootprint.all { sameZoneType(it, newBuilding.zone()) }
        if (sameZone || emptyOrLower) {
            debug { "We found a great location!" }
            // we must bulldoze all the buildings in the proposed footprint...
            proposedFootprint.forEach { cityMap.bulldoze(it, it) }
            debug { "Building the building: $newBuilding" }
            cityMap.build(newBuilding, coordinate)
            return true
        } else {
            debug { "Same zone: $sameZone, Empty or lower: $emptyOrLower" }
        }
        return false
    }

    private fun sameZoneType(coordinate: BlockCoordinate, zone: Zone?): Boolean {
        zone ?: return false
        return cityMap.zoneLayer[coordinate] == zone
    }

    private fun emptyOrLowerLevelThan(coordinate: BlockCoordinate, level: Int): Boolean {
        val locations = cityMap.locationsAt(coordinate)
        if (locations.toList().isEmpty()) {
            return true
        }
        if (locations.any { it.building.zone() != Zone.RESIDENTIAL || it.building.zone() != Zone.COMMERCIAL || it.building.zone() != Zone.INDUSTRIAL }) {
            return false
        }
        return locations.all { it.building.level < level }
    }
}
