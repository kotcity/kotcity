package kotcity.automata

import kotcity.data.*
import kotcity.data.assets.AssetManager
import kotcity.ui.map.MAX_BUILDING_SIZE
import kotcity.util.getRandomElements
import java.util.*

class Constructor(val cityMap: CityMap) {

    val assetManager = AssetManager()
    val random = Random()
    val maxTries = 30

    fun filterOccupied(entry: MutableMap.MutableEntry<BlockCoordinate, Double>): Boolean {
        return cityMap.buildingsIn(entry.key).count() == 0
    }

    fun tick() {
        // find most desirable place to stick an industrial zone...
        val blockAndScore = cityMap.desirabilityLayer(ZoneType.INDUSTRIAL, 1)
                ?.entries()?.filter { filterOccupied(it) }
                ?.maxBy { it.value }
        if (blockAndScore == null) {
            println("Could not find most desirable industrial zone!")
            return
        } else {
            println("We will be trying to build at ${blockAndScore.key} with desirability ${blockAndScore.value}")
        }
        val coordinate = blockAndScore.key
        val desirability = blockAndScore.value
        val newBuilding = findBuilding(ZoneType.INDUSTRIAL, desirability)
        if (newBuilding != null) {
            println("The building to be attempted is: $newBuilding")
        } else {
            println("Sorry, no building could be found for ${ZoneType.INDUSTRIAL} and $desirability")
            return
        }

        // let's try like X times...
        tryToBuild(coordinate, newBuilding)
    }

    private fun tryToBuild(coordinate: BlockCoordinate, newBuilding: Building) {
        var tries = 0
        var done = false
        while (tries < maxTries && !done) {
            val fuzzedCoordinate: BlockCoordinate = fuzz(coordinate)
            println("Trying to build $newBuilding at $fuzzedCoordinate")
            if (cityMap.canBuildBuildingAt(newBuilding, fuzzedCoordinate)) {
                done = true
                cityMap.build(newBuilding, fuzzedCoordinate)
            }
            tries++
        }
    }

    private fun fuzz(coordinate: BlockCoordinate): BlockCoordinate {
        val randX = rand(-MAX_BUILDING_SIZE, MAX_BUILDING_SIZE)
        val randY = rand(-MAX_BUILDING_SIZE, MAX_BUILDING_SIZE)
        return BlockCoordinate(coordinate.x + randX, coordinate.y + randY)
    }

    private fun rand(from: Int, to: Int) : Int {
        return random.nextInt(to - from) + from
    }

    fun zoneTypeToBuildingType(zoneType: ZoneType): BuildingType {
        return when (zoneType) {
            ZoneType.INDUSTRIAL -> BuildingType.INDUSTRIAL
            ZoneType.RESIDENTIAL -> BuildingType.RESIDENTIAL
            ZoneType.COMMERCIAL -> BuildingType.COMMERCIAL
        }
    }

    // TODO: use desirability later...
    private fun findBuilding(zoneType: ZoneType, desirability: Double): Building? {
        return assetManager.all().filter { it.type == zoneTypeToBuildingType(zoneType) }.getRandomElements(1)?.first()
    }
}