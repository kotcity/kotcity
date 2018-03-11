package kotcity.automata

import kotcity.data.*
import kotcity.data.assets.AssetManager
import kotcity.ui.map.MAX_BUILDING_SIZE
import kotcity.util.Debuggable
import kotcity.util.getRandomElement
import java.util.*
import kotlin.reflect.KClass

fun<T: Any> T.getClass(): KClass<T> {
    return javaClass.kotlin
}

class Constructor(val cityMap: CityMap) : Debuggable {

    val assetManager = AssetManager(cityMap)
    private val random = Random()
    private val maxTries = 30
    override var debug = false

    private fun isEmpty(entry: MutableMap.MutableEntry<BlockCoordinate, Double>): Boolean {
        return cityMap.cachedBuildingsIn(entry.key).count() == 0
    }

    fun tick() {

        // how many empty zones??
        // val emptyZoneCount = cityMap.zoneLayer.keys.count { cityMap.buildingsIn(it).count() == 0 }
        // debug("We have this many empty zones: $emptyZoneCount")

        // TODO: should probably look at a % of DESIRABLE zones...

        val zoneTypes = listOf(Zone.INDUSTRIAL, Zone.COMMERCIAL, Zone.RESIDENTIAL)
        zoneTypes.forEach { zoneType ->
            val howManyTimes: Int = (desirableZoneCount(zoneType).toDouble() * 0.01).coerceIn(1.0..5.0).toInt()
            println("We will be trying to construct $howManyTimes")
            repeat(howManyTimes, {

                val layer = cityMap.desirabilityLayer(zoneType, 1) ?: return

                // get the 10 best places... pick one randomly ....
                val blockAndScore = layer.entries().filter { isEmpty(it) }.filter { it.value > 0}.sortedBy { it.value }.take(10).getRandomElement()
                if (blockAndScore == null) {
                    if (debug) {
                        debug("Could not find most desirable $zoneType zone!")
                    }
                } else {
                    debug("We will be trying to build at ${blockAndScore.key} with desirability ${blockAndScore.value}")
                    val coordinate = blockAndScore.key
                    val desirability = blockAndScore.value
                    val newBuilding = findBuilding(zoneType)
                    if (newBuilding != null) {
                        debug("The building to be attempted is: $newBuilding")
                        // let's try like X times...
                        tryToBuild(coordinate, newBuilding, layer)
                    } else {
                        debug("Sorry, no building could be found for $zoneType and $desirability")
                    }

                }

            })
        }
    }

    private fun desirableZoneCount(zone: Zone): Int {
        return cityMap.zoneLayer.keys.filter { cityMap.zoneLayer[it] == zone && cityMap.cachedBuildingsIn(it).count() == 0 }.map {coordinate ->
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



    // TODO: use desirability later...
    private fun findBuilding(zoneType: Zone): Building? {
        return assetManager.all().filterIsInstance(zoneTypeToClass(zoneType)).getRandomElement()
    }

    private fun zoneTypeToClass(zoneType: Zone): Class<out Building> {
        return when (zoneType) {
            Zone.RESIDENTIAL -> Residential::class.java
            Zone.COMMERCIAL -> Commercial::class.java
            Zone.INDUSTRIAL -> Industrial::class.java
        }
    }
}