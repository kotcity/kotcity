package kotcity.automata

import kotcity.data.*
import kotcity.pathfinding.Pathfinder
import kotcity.util.Debuggable
import kotlinx.coroutines.experimental.*

/**
 * This class is responsible for going over each zoned tile and figuring out the "Desirability score"... in other words
 * how badly people want to live there...
 * @param cityMap The map that we pass in...
 */
class DesirabilityUpdater(val cityMap: CityMap) : Debuggable {
    override var debug: Boolean = false
        set(value) {
            field = value
            resourceFinder.debug = debug
        }

    /**
     * Distance in tiles that we stop looking for stuff..
     */
    private val maxDistance = 100

    private val shortDistance = 10
    private val mediumDistance = 30
    private val longDistance = 100
    private val pathFinder: Pathfinder = Pathfinder(cityMap)
    private val resourceFinder = ResourceFinder(cityMap)

    init {
        resourceFinder.debug = debug
    }

    /**
     * Process desirability for the given map
     */
    fun tick() {
        // let's tick the desirability...
        val jobs = arrayListOf<Deferred<Unit>>()

        runBlocking {
            cityMap.desirabilityLayers.forEach { desirabilityLayer ->

                jobs += async {
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
            jobs.forEach { it.await() }
        }
    }

    private suspend fun updateCommercial(desirabilityLayer: DesirabilityLayer) {

        val commercialZones = zoneCoordinates(Zone.COMMERCIAL)

        val jobs = arrayListOf<Job>()

        commercialZones.forEach { coordinate ->

            jobs += launch {
                if (!pathFinder.nearbyRoad(listOf(coordinate))) {
                    desirabilityLayer[coordinate] = Double.MIN_VALUE
                    return@launch
                }
                val availableGoodsShortDistanceScore =
                    resourceFinder.quantityWantedNearby(Tradeable.GOODS, coordinate, shortDistance) * 0.1
                val availableGoodsMediumDistanceScore =
                    resourceFinder.quantityWantedNearby(Tradeable.GOODS, coordinate, mediumDistance) * 0.1
                val availableGoodsLongDistanceScore =
                    resourceFinder.quantityWantedNearby(Tradeable.GOODS, coordinate, longDistance) * 0.1
                val availableLaborScore =
                    resourceFinder.quantityForSaleNearby(Tradeable.LABOR, coordinate, maxDistance) * 0.1

                val trafficAdjustment = -(cityMap.trafficNearby(coordinate, Tunable.TRAFFIC_RADIUS) * 0.05)
                val pollutionAdjustment = -(cityMap.pollutionNearby(coordinate, Tunable.POLLUTION_RADIUS) * 0.01)
                val landValueAdjustment = landValueAdjustment(coordinate)

                desirabilityLayer[coordinate] = (
                        pollutionAdjustment + availableGoodsShortDistanceScore +
                                availableGoodsMediumDistanceScore + availableGoodsLongDistanceScore +
                                availableLaborScore + trafficAdjustment + landValueAdjustment
                        )
            }
        }

        jobs.forEach { it.join() }

        trimDesirabilityLayer(desirabilityLayer, commercialZones)
    }

    /**
     * For every $20,000 of land value we give one desirability point...
     * @param coordinate
     */
    private fun landValueAdjustment(coordinate: BlockCoordinate) = (cityMap.landValueLayer[coordinate] ?: 0.0) / 50_000

    private suspend fun updateIndustrial(desirabilityLayer: DesirabilityLayer) {

        val jobs = arrayListOf<Job>()

        // ok... we just gotta find each block with an industrial zone...
        val industryZones = zoneCoordinates(Zone.INDUSTRIAL)

        industryZones.forEach { coordinate ->
            jobs += launch {
                // if we aren't near a road we are not desirable...
                if (!pathFinder.nearbyRoad(listOf(coordinate))) {
                    desirabilityLayer[coordinate] = Double.MIN_VALUE
                    return@launch
                }
                val availableBuyingWholesaleGoodsShortDistanceScore =
                    resourceFinder.quantityWantedNearby(Tradeable.WHOLESALE_GOODS, coordinate, shortDistance) * 0.1
                val availableBuyingWholesaleGoodsMediumDistanceScore =
                    resourceFinder.quantityWantedNearby(Tradeable.WHOLESALE_GOODS, coordinate, mediumDistance) * 0.1
                val availableBuyingWholesaleGoodsLongDistanceScore =
                    resourceFinder.quantityWantedNearby(Tradeable.WHOLESALE_GOODS, coordinate, longDistance) * 0.1
                val availableLaborScore =
                    resourceFinder.quantityForSaleNearby(Tradeable.LABOR, coordinate, maxDistance) * 0.1
                val trafficAdjustment = -((cityMap.trafficNearby(coordinate, Tunable.TRAFFIC_RADIUS) * 0.05) / 2)
                val landValueAdjustment = landValueAdjustment(coordinate)

                desirabilityLayer[coordinate] = (
                        availableBuyingWholesaleGoodsShortDistanceScore +
                                availableBuyingWholesaleGoodsMediumDistanceScore +
                                availableBuyingWholesaleGoodsLongDistanceScore +
                                availableLaborScore + trafficAdjustment + landValueAdjustment
                        )
            }
        }

        jobs.forEach { it.join() }

        trimDesirabilityLayer(desirabilityLayer, industryZones)
    }

    private fun trimDesirabilityLayer(desirabilityLayer: DesirabilityLayer, zones: List<BlockCoordinate>) {
        // now trim any that were not in our zones...
        val keysToTrim = mutableListOf<BlockCoordinate>()
        desirabilityLayer.forEach { t, _ ->
            if (!zones.contains(t)) {
                println("We have to trim $t because it is no longer a zone...")
                keysToTrim.add(t)
            }
        }
        keysToTrim.forEach { desirabilityLayer.remove(it) }
    }

    private fun zoneCoordinates(zoneType: Zone) =
        cityMap.zoneLayer.toList().filter { it.second == zoneType }.map { it.first }

    private suspend fun updateResidential(desirabilityLayer: DesirabilityLayer) {

        val jobs = arrayListOf<Job>()

        val population = cityMap.censusTaker.population

        val residentialZones = zoneCoordinates(Zone.RESIDENTIAL)

        // we like being near places that NEED labor
        // we like being near places that PROVIDE goods
        residentialZones.forEach { coordinate ->

            jobs += launch {
                if (!pathFinder.nearbyRoad(listOf(coordinate))) {
                    desirabilityLayer[coordinate] = Double.MIN_VALUE
                    return@launch
                }

                // if we have less than 20 people we just want to live here...
                if (population < 20) {
                    desirabilityLayer[coordinate] = 10.0
                    return@launch
                }

                val availableJobsShortDistance =
                    resourceFinder.quantityWantedNearby(Tradeable.LABOR, coordinate, shortDistance)
                // every 10 jobs available nearby, we get 1 point...
                val availableJobsShortDistanceScore = availableJobsShortDistance * 0.1

                val availableJobsMediumDistance =
                    resourceFinder.quantityWantedNearby(Tradeable.LABOR, coordinate, mediumDistance)
                // every 10 jobs available nearby, we get 1 point...
                val availableJobsMediumDistanceScore = availableJobsMediumDistance * 0.1

                val availableJobsLongDistance =
                    resourceFinder.quantityWantedNearby(Tradeable.LABOR, coordinate, longDistance)
                val availableJobsLongDistanceScore = availableJobsLongDistance * 0.1

                val availableGoodsShortDistance =
                    resourceFinder.quantityForSaleNearby(Tradeable.GOODS, coordinate, shortDistance)
                val availableGoodsShortDistanceScore = availableGoodsShortDistance * 0.1

                val availableGoodsMediumDistance =
                    resourceFinder.quantityForSaleNearby(Tradeable.GOODS, coordinate, mediumDistance)
                val availableGoodsMediumDistanceScore = availableGoodsMediumDistance * 0.1

                val trafficAdjustment = -(cityMap.trafficNearby(coordinate, Tunable.TRAFFIC_RADIUS) * 0.01)
                val pollutionAdjustment = -(cityMap.pollutionNearby(coordinate, Tunable.POLLUTION_RADIUS) * 0.05)

                val landValueAdjustment = landValueAdjustment(coordinate)

                val newResidentialValue = (
                        trafficAdjustment + pollutionAdjustment + availableJobsShortDistanceScore +
                                availableJobsMediumDistanceScore + availableJobsLongDistanceScore +
                                availableGoodsShortDistanceScore + availableGoodsMediumDistanceScore + landValueAdjustment
                        )

                desirabilityLayer[coordinate] = newResidentialValue
            }
        }

        jobs.forEach { it.join() }

        trimDesirabilityLayer(desirabilityLayer, residentialZones)
    }
}
