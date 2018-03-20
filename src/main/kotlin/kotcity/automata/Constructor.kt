package kotcity.automata

import kotcity.data.*
import kotcity.data.AssetManager
import kotcity.ui.map.MAX_BUILDING_SIZE
import kotcity.util.Debuggable
import kotcity.util.randomElement
import java.util.*
import kotlin.reflect.KClass

fun<T: Any> T.getClass(): KClass<T> {
    return javaClass.kotlin
}

class Constructor(val cityMap: CityMap) : Debuggable {

    val assetManager = AssetManager(cityMap)
    private val random = Random()
    private val maxTries = 50
    override var debug = false

    private fun isEmpty(entry: MutableMap.MutableEntry<BlockCoordinate, Double>): Boolean {
        return cityMap.cachedLocationsIn(entry.key).count() == 0
    }

    fun tick() {
        val zoneTypes = listOf(Zone.INDUSTRIAL, Zone.COMMERCIAL, Zone.RESIDENTIAL)
        zoneTypes.forEach { zoneType ->
            val howManyBuildings: Int = (desirableZoneCount(zoneType).toDouble() * 0.05).coerceIn(1.0..5.0).toInt()
            repeat(howManyBuildings, {

                val howManyBulldozed = cityMap.bulldozedCounts
                if (howManyBulldozed[zoneType] ?: 0 == 0) {
                    val layer = cityMap.desirabilityLayer(zoneType, 1) ?: return

                    // get the 10 best places... pick one randomly ....
                    val blockAndScore = layer.entries().filter { isEmpty(it) }.filter { it.value > 0}.sortedByDescending { it.value }.take(10).randomElement()
                    if (blockAndScore == null) {
                        if (debug) {
                            debug("Could not find most desirable $zoneType zone!")
                        }
                    } else {
                        debug("We will be trying to build at ${blockAndScore.key} with desirability ${blockAndScore.value}")
                        val coordinate = blockAndScore.key
                        val desirability = blockAndScore.value
                        // constructor only constructs level 1 buildings...
                        val newBuilding = assetManager.findBuilding(zoneType, 1)
                        if (newBuilding != null) {
                            debug("The building to be attempted is: $newBuilding")
                            // let's try like X times...
                            tryToBuild(coordinate, newBuilding, layer)
                        } else {
                            debug("Sorry, no building could be found for $zoneType and $desirability")
                        }

                    }
                } else {
                    debug("Some $zoneType were bulldozed, so we don't want to build any...")
                }



            })
        }
    }

    private fun desirableZoneCount(zone: Zone): Int {
        return cityMap.zoneLayer.keys.filter { cityMap.zoneLayer[it] == zone && cityMap.cachedLocationsIn(it).count() == 0 }.map { coordinate ->
            cityMap.desirabilityLayers.map { it[coordinate] ?: 0.0 }.max()
        }.filterNotNull().filter { it > 0 }.count()
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

    private fun fuzz(coordinate: BlockCoordinate): BlockCoordinate {
        val randX = rand(-MAX_BUILDING_SIZE, MAX_BUILDING_SIZE)
        val randY = rand(-MAX_BUILDING_SIZE, MAX_BUILDING_SIZE)
        return BlockCoordinate(coordinate.x + randX, coordinate.y + randY)
    }

    private fun rand(from: Int, to: Int) : Int {
        return random.nextInt(to - from) + from
    }

}