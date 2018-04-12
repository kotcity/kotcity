package kotcity.automata

import kotcity.automata.util.BuildingBuilder
import kotcity.data.*
import kotcity.pathfinding.Pathfinder
import kotcity.util.Debuggable
import kotcity.util.randomElement

/**
 * Constructor is responsible for looking at the city... seeing what the demand is for each type
 * of zone and then attempting to place them in the most desirable spots...
 * @param cityMap the map to operate on
 */
class Constructor(val cityMap: CityMap) : Debuggable {

    private val assetManager = AssetManager(cityMap)
    private val buildingBuilder = BuildingBuilder(cityMap)
    private val pathfinder: Pathfinder = Pathfinder(cityMap)
    override var debug = false

    /**
     * Makes sure this spot is empty...
     * @param coordinate coordinate to check...
     */
    private fun isEmpty(coordinate: BlockCoordinate) = cityMap.locationsAt(coordinate).count() == 0

    /**
     * loop over each [Zone] and figure out how many buildings to plop and then actually plop them...
     */
    fun tick() {
        val zoneTypes = listOf(Zone.INDUSTRIAL, Zone.COMMERCIAL, Zone.RESIDENTIAL)
        zoneTypes.forEach { zoneType ->

            val howManyBuildings = howManyToBuild(zoneType).coerceAtMost(3)

            debug { "According to our calculations we should build $howManyBuildings for $zoneType" }

            repeat(howManyBuildings, {

                val layer = cityMap.desirabilityLayer(zoneType, 1) ?: return

                val totalInLayer = layer.keys().size

                // let's extract the blocks we should check... basically it is anywhere where desirability is off the floor...
                val potentialLocations = layer.entries().map { it.key }

                val nearRoad = potentialLocations.filter { pathfinder.nearbyRoad(listOf(it)) }

                val emptyBlocks = nearRoad.filter { isEmpty(it) }

                val withCorrectZoneType = emptyBlocks.filter { correctZoneType(zoneType, it) }

                // now let's join back up with desirability...
                val byDesirabilityScore = withCorrectZoneType
                    .map { Pair(it, layer[it]) }
                    .sortedByDescending { it.second }
                    .map { it.first }

                val bestLocation = byDesirabilityScore
                    .take(10)
                    .randomElement()

                // OK now have to make sure this is zoned right....
                if (bestLocation == null) {
                    if (debug) {
                        debug { "Could not find most desirable $zoneType zone!" }
                        debug { "Total entries in desirability layer: $totalInLayer" }
                        debug { "Potential locations: ${potentialLocations.size}" }
                        debug { "Empty blocks: ${emptyBlocks.size}" }
                        debug { "With correct zone type: ${withCorrectZoneType.size}" }
                    }
                } else {
                    debug { "We will be trying to build at $bestLocation" }
                    // constructor only constructs level 1 buildings...
                    val newBuilding = assetManager.findBuilding(zoneType, 1)
                    if (newBuilding != null) {
                        debug { "The building to be attempted is: $newBuilding" }
                        // let's try like X times...
                        buildingBuilder.tryToBuild(bestLocation, newBuilding)
                    } else {
                        debug { "Sorry, no building could be found for $zoneType at $bestLocation" }
                    }
                }
            })
        }
    }

    private fun correctZoneType(zoneType: Zone, coordinate: BlockCoordinate) = cityMap.zoneLayer[coordinate] == zoneType

    /**
     * So basically, we have the ideal ratio of how much to "supply" the city with.
     * We calculate the ratio of supply vs demand. We want to make the city likely to grow, so we "overdrive" growth
     * a bit instead of looking for that perfect 1/1 ratio.
     * We end up generating a ratio and then the further off we are from this ideal ratio... the more we build.
     * @param zoneType type of zone to figure this out for...
     */
    private fun howManyToBuild(zoneType: Zone): Int {

        val idealIndustrial = 0.80
        val idealCommercial = 0.75
        val idealResidential = 0.65

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

    /**
     * for each 0.05 we are off the ratio we will build one zone... this makes
     * the number of buildings we create proportional to the demand (hopefully)
     * @param ideal the ideal ratio for the zone type
     * @param ratio the actual calculated ratio....
     */
    private fun calculateHowManyOffRatio(ideal: Double, ratio: Double): Int {
        val delta = ratio - ideal
        if (delta < 0.0) {
            return 0
        }
        return Math.ceil(delta / 0.05).toInt()
    }
}
