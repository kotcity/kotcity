package kotcity.automata

import kotcity.automata.util.BuildingBuilder
import kotcity.data.*
import kotcity.data.AssetManager
import kotcity.util.Debuggable
import kotcity.util.randomElement

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
            // val howManyBuildings: Int = (desirableZoneCount(zoneType).toDouble() * 0.05).coerceIn(1.0..5.0).toInt()
            val howManyBuildings = howManyToBuild(zoneType)

            debug("According to our calculations we should build $howManyBuildings for $zoneType")

            repeat(howManyBuildings, {

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

            })

        }
    }

    // the HIGHER the ratio the more we should try to build...
    private fun howManyToBuild(zoneType: Zone): Int {

        val idealIndustrial = 0.85
        val idealResidential = 0.5
        val idealCommercial = 0.80

        return when (zoneType) {
            Zone.COMMERCIAL -> {
                // OK... if we supply more GOODS than demand... don't bother building any commercial zones...
                val ratio = cityMap.censusTaker.supplyRatio(Tradeable.GOODS)
                calculateHowManyOffRatio(idealCommercial, ratio)
            }
            Zone.INDUSTRIAL -> {
                val ratio = cityMap.censusTaker.supplyRatio(Tradeable.WHOLESALE_GOODS)
                calculateHowManyOffRatio(idealIndustrial, ratio)
            }
            Zone.RESIDENTIAL -> {
                val ratio = cityMap.censusTaker.supplyRatio(Tradeable.LABOR)
                calculateHowManyOffRatio(idealResidential, ratio)
            }
        }
    }

    private fun calculateHowManyOffRatio(ideal: Double, ratio: Double): Int {
        val delta = ratio - ideal
        debug("The delta is $delta")
        if (delta < 0.0) {
            return 0
        }
        return Math.ceil(delta / 0.05).toInt()
    }

}