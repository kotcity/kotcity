package kotcity.automata

import kotcity.data.*
import kotcity.pathfinding.Pathfinder
import kotcity.util.Debuggable


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
                val availableGoodsShortDistance = resourceFinder.quantityWantedNearby(Tradeable.GOODS, coordinate, shortDistance)
                val availableGoodsMediumDistance = resourceFinder.quantityWantedNearby(Tradeable.GOODS, coordinate, mediumDistance)
                val availableGoodsLongDistance = resourceFinder.quantityWantedNearby(Tradeable.GOODS, coordinate, longDistance)
                val availableLabor = resourceFinder.quantityForSaleNearby(Tradeable.LABOR, coordinate, maxDistance)

                desirabilityLayer[coordinate] = if (availableGoodsLongDistance == 0 && availableLabor == 0) {
                    0.0
                } else {
                    (availableGoodsShortDistance + availableGoodsMediumDistance + availableGoodsLongDistance + availableLabor).toDouble()
                }

            }

        }

        trimDesirabilityLayer(desirabilityLayer, commercialZones)
    }

    private fun updateIndustrial(desirabilityLayer: DesirabilityLayer) {

        // ok... we just gotta find each block with an industrial zone...
        val industryZones = zoneCoordinates(Zone.INDUSTRIAL)

        industryZones.forEach { coordinate ->

                if (!pathFinder.nearbyRoad(listOf(coordinate))) {
                    desirabilityLayer[coordinate] = 0.0
                } else {
                    val availableBuyingWholesaleGoodsShortDistance = resourceFinder.quantityWantedNearby(Tradeable.WHOLESALE_GOODS, coordinate, shortDistance)
                    val availableBuyingWholesaleGoodsMediumDistance = resourceFinder.quantityWantedNearby(Tradeable.WHOLESALE_GOODS, coordinate, mediumDistance)
                    val availableBuyingWholesaleGoodsLongDistance = resourceFinder.quantityWantedNearby(Tradeable.WHOLESALE_GOODS, coordinate, longDistance)
                    val availableLabor = resourceFinder.quantityForSaleNearby(Tradeable.LABOR, coordinate, maxDistance)

                    desirabilityLayer[coordinate] = if (availableBuyingWholesaleGoodsLongDistance == 0 && availableLabor == 0) {
                        0.0
                    } else {
                        (availableBuyingWholesaleGoodsShortDistance + availableBuyingWholesaleGoodsMediumDistance + availableBuyingWholesaleGoodsLongDistance + availableLabor).toDouble()
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
                val availableJobsMediumDistance = resourceFinder.quantityWantedNearby(Tradeable.LABOR, coordinate, mediumDistance)
                val availableJobsLongDistance = resourceFinder.quantityWantedNearby(Tradeable.LABOR, coordinate, longDistance)
                val availableGoodsShortDistance = resourceFinder.quantityForSaleNearby(Tradeable.GOODS, coordinate, shortDistance)
                val availableGoodsMediumDistance = resourceFinder.quantityForSaleNearby(Tradeable.GOODS, coordinate, mediumDistance)
                if (population == 0) {
                    desirabilityLayer[coordinate] = 0.1
                } else {
                    desirabilityLayer[coordinate] = (availableJobsShortDistance + availableJobsMediumDistance + availableJobsLongDistance + availableGoodsShortDistance + availableGoodsMediumDistance).toDouble()
                }

            }

        }

        trimDesirabilityLayer(desirabilityLayer, residentialZones)

    }
}