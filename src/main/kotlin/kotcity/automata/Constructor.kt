package kotcity.automata

import kotcity.data.*
import kotcity.data.assets.AssetManager
import kotcity.ui.map.MAX_BUILDING_SIZE
import kotcity.util.getRandomElements
import java.util.*

class Constructor(val cityMap: CityMap) {

    val assetManager = AssetManager(cityMap)
    val random = Random()
    val maxTries = 30
    private val DEFAULT_MONEY = 10

    fun isEmpty(entry: MutableMap.MutableEntry<BlockCoordinate, Double>): Boolean {
        return cityMap.buildingsIn(entry.key).count() == 0
    }

    fun tick() {
        repeat(3, {
            val zoneTypes = listOf(ZoneType.INDUSTRIAL, ZoneType.COMMERCIAL, ZoneType.RESIDENTIAL)
            zoneTypes.forEach { zoneType ->
                val layer = cityMap.desirabilityLayer(zoneType, 1) ?: return

                val blockAndScore = layer.entries().filter { isEmpty(it) }.filter { it.value > 0}.maxBy { it.value }
                if (blockAndScore == null) {
                    println("Could not find most desirable $zoneType zone!")
                } else {
                    println("We will be trying to build at ${blockAndScore.key} with desirability ${blockAndScore.value}")
                    val coordinate = blockAndScore.key
                    val desirability = blockAndScore.value
                    val newBuilding = findBuilding(zoneType)
                    if (newBuilding != null) {
                        println("The building to be attempted is: $newBuilding")
                        // let's try like X times...
                        tryToBuild(coordinate, newBuilding, layer)
                    } else {
                        println("Sorry, no building could be found for $zoneType and $desirability")
                    }

                }

            }
        })
    }

    private fun tryToBuild(coordinate: BlockCoordinate, newBuilding: Building, layer: DesirabilityLayer) {
        var tries = 0
        var done = false
        while (tries < maxTries && !done) {
            val fuzzedCoordinate: BlockCoordinate = fuzz(coordinate)

            // ok... desirability STILL has to be above 0...
            val buildingBlocks = cityMap.buildingBlocks(fuzzedCoordinate, newBuilding)
            // ok, each proposed block has to have desirability > 0
            val desirabilityScores = buildingBlocks.map { layer[it] ?: 0.0 }

            val acceptableDesirability = desirabilityScores.all { it > 0.0 }

            if (acceptableDesirability) {
                println("Trying to build $newBuilding at $fuzzedCoordinate")
                if (cityMap.canBuildBuildingAt(newBuilding, fuzzedCoordinate)) {
                    done = true
                    cityMap.build(newBuilding, fuzzedCoordinate)
                }
            } else {
                println("$fuzzedCoordinate didn't have any desirable blocks...")
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
    private fun findBuilding(zoneType: ZoneType): Building? {
        return assetManager.all().filter { it.type == zoneTypeToBuildingType(zoneType) }.getRandomElements(1)?.first()
    }
}