package kotcity.automata

import kotcity.automata.util.BuildingBuilder
import kotcity.data.*
import kotcity.data.AssetManager
import kotcity.ui.map.MAX_BUILDING_SIZE
import kotcity.util.Debuggable
import kotcity.util.randomElement
import java.util.*
import kotlin.reflect.KClass

class Constructor(val cityMap: CityMap) : Debuggable {

    private val assetManager = AssetManager(cityMap)
    private val buildingBuilder = BuildingBuilder(cityMap)
    override var debug = false

    private fun isEmpty(entry: MutableMap.MutableEntry<BlockCoordinate, Double>): Boolean {
        return cityMap.cachedLocationsIn(entry.key).count() == 0
    }

    fun tick() {
        val zoneTypes = listOf(Zone.INDUSTRIAL, Zone.COMMERCIAL, Zone.RESIDENTIAL)
        zoneTypes.forEach { zoneType ->
            val howManyBuildings: Int = (desirableZoneCount(zoneType).toDouble() * 0.05).coerceIn(1.0..5.0).toInt()

            val wasOverSupplied = oversupply(zoneType)

            if (wasOverSupplied) {
                debug("$zoneType is oversupplied so not bothering to build...")
            }

            // check for oversupply?
            if (!wasOverSupplied) {
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
                                buildingBuilder.tryToBuild(coordinate, newBuilding)
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
    }

    private fun oversupply(zoneType: Zone): Boolean {
        when (zoneType) {
            Zone.COMMERCIAL ->
                // OK... if we supply more GOODS than demand... don't bother building any commercial zones...
                if (cityMap.censusTaker.tradeBalance(Tradeable.GOODS) > 5) {
                    return true
                }
            Zone.INDUSTRIAL -> if (cityMap.censusTaker.tradeBalance(Tradeable.WHOLESALE_GOODS) > 5) {
                return true
            }
            Zone.RESIDENTIAL -> if (cityMap.censusTaker.tradeBalance(Tradeable.LABOR) > 5) {
                return true
            }
        }
        return false
    }

    private fun desirableZoneCount(zone: Zone): Int {
        return cityMap.zoneLayer.keys.filter { cityMap.zoneLayer[it] == zone && cityMap.cachedLocationsIn(it).count() == 0 }.mapNotNull { coordinate ->
            cityMap.desirabilityLayers.map { it[coordinate] ?: 0.0 }.max()
        }.filter { it > 0 }.count()
    }



}