package kotcity.automata

import kotcity.data.*
import kotcity.pathfinding.Pathfinder
import kotcity.util.Debuggable

const val HEAVY_TRAFFIC = 5000
const val MEDIUM_TRAFFIC = 2500

class DesirabilityUpdater(val cityMap: CityMap): Debuggable {
    override var debug: Boolean = false
    set(value) {
        field = value
        resourceFinder.debug = debug
    }

    private val maxDistance = 100
    private val shortDistance = 10
    private val mediumDistance = 30
    private val longDistance = 100
    private val pathFinder: Pathfinder = Pathfinder(cityMap)
    private val resourceFinder = ResourceFinder(cityMap)

    init {
        resourceFinder.debug = debug
    }

    fun update() {
        // let's update the desirability...

        debug("Bulldozed Counts: ${cityMap.bulldozedCounts}")

        cityMap.desirabilityLayers.forEach { desirabilityLayer ->

            // TODO: worry about other levels later...
            if (desirabilityLayer.level == 1) {
                when (desirabilityLayer.zoneType) {
                    Zone.RESIDENTIAL -> updateResidential(desirabilityLayer)
                    Zone.INDUSTRIAL -> updateIndustrial(desirabilityLayer)
                    Zone.COMMERCIAL -> updateCommercial(desirabilityLayer)
                }
            }

        }
    }

    private fun updateCommercial(desirabilityLayer: DesirabilityLayer) {

        val commercialZones = zoneCoordinates(Zone.COMMERCIAL)

        commercialZones.forEach { coordinate ->

            if (!pathFinder.nearbyRoad(listOf(coordinate))) {
                desirabilityLayer[coordinate] = 0.0
            } else {
                val availableGoodsShortDistanceScore = resourceFinder.quantityWantedNearby(Tradeable.GOODS, coordinate, shortDistance) * 0.1
                val availableGoodsMediumDistanceScore = resourceFinder.quantityWantedNearby(Tradeable.GOODS, coordinate, mediumDistance) * 0.1
                val availableGoodsLongDistanceScore = resourceFinder.quantityWantedNearby(Tradeable.GOODS, coordinate, longDistance) * 0.1
                val availableLaborScore = resourceFinder.quantityForSaleNearby(Tradeable.LABOR, coordinate, maxDistance) * 0.1

                val trafficAdjustment = -(cityMap.trafficNearby(coordinate, 2) * 0.05)
                val pollutionAdjustment = -(cityMap.pollutionNearby(coordinate, 2) * 0.05)

                desirabilityLayer[coordinate] = if (cityMap.censusTaker.tradeBalance(Tradeable.GOODS) > 5) {
                    0.0
                } else {
                    (pollutionAdjustment + availableGoodsShortDistanceScore + availableGoodsMediumDistanceScore + availableGoodsLongDistanceScore + availableLaborScore + trafficAdjustment)
                }

            }

        }

        trimDesirabilityLayer(desirabilityLayer, commercialZones)
    }

    private fun updateIndustrial(desirabilityLayer: DesirabilityLayer) {

        // ok... we just gotta find each block with an industrial zone...
        val industryZones = zoneCoordinates(Zone.INDUSTRIAL)

        industryZones.forEach { coordinate ->
                // if we aren't near a road we are not desirable...
                if (!pathFinder.nearbyRoad(listOf(coordinate))) {
                    desirabilityLayer[coordinate] = 0.0
                } else {
                    val availableBuyingWholesaleGoodsShortDistanceScore = resourceFinder.quantityWantedNearby(Tradeable.WHOLESALE_GOODS, coordinate, shortDistance) * 0.1
                    val availableBuyingWholesaleGoodsMediumDistanceScore = resourceFinder.quantityWantedNearby(Tradeable.WHOLESALE_GOODS, coordinate, mediumDistance)  * 0.1
                    val availableBuyingWholesaleGoodsLongDistanceScore = resourceFinder.quantityWantedNearby(Tradeable.WHOLESALE_GOODS, coordinate, longDistance) * 0.1
                    val availableLaborScore = resourceFinder.quantityForSaleNearby(Tradeable.LABOR, coordinate, maxDistance)  * 0.1
                    val trafficAdjustment = -(cityMap.trafficNearby(coordinate, 2) * 0.05)
                    desirabilityLayer[coordinate] = if (cityMap.censusTaker.tradeBalance(Tradeable.WHOLESALE_GOODS) > 5) {
                        0.0
                    } else {
                        (
                            availableBuyingWholesaleGoodsShortDistanceScore +
                            availableBuyingWholesaleGoodsMediumDistanceScore +
                            availableBuyingWholesaleGoodsLongDistanceScore +
                            availableLaborScore + trafficAdjustment
                        )
                    }

                }

        }

        trimDesirabilityLayer(desirabilityLayer, industryZones)
    }

    private fun trimDesirabilityLayer(desirabilityLayer: DesirabilityLayer, zones: List<BlockCoordinate>) {
        // now trim any that were not in our zones...
        val keysToTrim = mutableListOf<BlockCoordinate>()
        desirabilityLayer.forEach { t, _ ->
            if (!zones.contains(t)) {
                keysToTrim.add(t)
            }
        }

        keysToTrim.forEach { desirabilityLayer.remove(it) }
    }

    private fun zoneCoordinates(zoneType: Zone): List<BlockCoordinate> {
        return cityMap.zoneLayer.toList().filter { it.second == zoneType }.map { it.first }
    }

    // TODO: waaay too slow...
    private fun updateResidential(desirabilityLayer: DesirabilityLayer) {
        // we like being near places that NEED labor
        // we like being near places that PROVIDE goods

        val residentialZones = zoneCoordinates(Zone.RESIDENTIAL)

        val population = cityMap.censusTaker.population

        residentialZones.forEach { coordinate ->

            if (!pathFinder.nearbyRoad(listOf(coordinate))) {
                desirabilityLayer[coordinate] = 0.0
            } else {



                val availableJobsShortDistance = resourceFinder.quantityWantedNearby(Tradeable.LABOR, coordinate, shortDistance)
                // every 10 jobs available nearby, we get 1 point...
                val availableJobsShortDistanceScore = availableJobsShortDistance * 0.1

                val availableJobsMediumDistance = resourceFinder.quantityWantedNearby(Tradeable.LABOR, coordinate, mediumDistance)
                // every 10 jobs available nearby, we get 1 point...
                val availableJobsMediumDistanceScore = availableJobsMediumDistance * 0.1

                val availableJobsLongDistance = resourceFinder.quantityWantedNearby(Tradeable.LABOR, coordinate, longDistance)
                val availableJobsLongDistanceScore = availableJobsLongDistance * 0.1

                val availableGoodsShortDistance = resourceFinder.quantityForSaleNearby(Tradeable.GOODS, coordinate, shortDistance)
                val availableGoodsShortDistanceScore = availableGoodsShortDistance * 0.1

                val availableGoodsMediumDistance = resourceFinder.quantityForSaleNearby(Tradeable.GOODS, coordinate, mediumDistance)
                val availableGoodsMediumDistanceScore = availableGoodsMediumDistance * 0.1

                val trafficAdjustment = -(cityMap.trafficNearby(coordinate, 2) * 0.05)
                val pollutionAdjustment = -(cityMap.pollutionNearby(coordinate, 2) * 0.05)

                if (population == 0) {
                    desirabilityLayer[coordinate] = 10.0
                } else {
                    desirabilityLayer[coordinate] = (
                            trafficAdjustment + pollutionAdjustment + availableJobsShortDistanceScore +
                            availableJobsMediumDistanceScore + availableJobsLongDistanceScore +
                            availableGoodsShortDistanceScore + availableGoodsMediumDistanceScore
                    )
                }

            }

        }

        trimDesirabilityLayer(desirabilityLayer, residentialZones)

    }
}