import kotcity.automata.ContactFulfiller
import kotcity.data.*
import kotcity.data.buildings.Industrial
import kotcity.data.buildings.Residential
import kotcity.data.buildings.TrainStation
import kotcity.pathfinding.Pathfinder
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PathfinderTest {
    @Test
    fun testPathfinder() {
        val flatMap = CityMap.flatMap(100, 100)
        val pathfinder = Pathfinder(flatMap)
        pathfinder.debug = true
        var outsidePath = pathfinder.pathToOutside(listOf(BlockCoordinate(50, 50)))?.blocks()
        assertTrue(outsidePath == null, "Path to outside should be null")
        // let's build a road right across...
        flatMap.buildRoad(BlockCoordinate(0, 50), BlockCoordinate(100, 50))

        outsidePath = pathfinder.pathToOutside(listOf(BlockCoordinate(50, 50)))?.blocks()
        assertTrue(outsidePath != null, "Path to outside should not be null")
        assertTrue(
            outsidePath?.count() == 51,
            "Path to outside should be 51 nodes! But it is actually ${outsidePath?.count()}"
        )
    }

    @Test
    fun testPathfinderOneWayRoads() {
        val startBlock = BlockCoordinate(50, 50)
        val endBlock = BlockCoordinate(100, 50)
        val startList = listOf(startBlock)
        val endList = listOf(endBlock)

        val branchStartBlock = BlockCoordinate(75, 49)
        val branchEndBlock = BlockCoordinate(75, 75)
        val branchStartList = listOf(branchStartBlock)
        val branchEndList = listOf(branchEndBlock)

        val flatMap = CityMap.flatMap(100, 100)
        val pathfinder = Pathfinder(flatMap)
        pathfinder.debug = true
        assertTrue(pathfinder.pathToOutside(startList) == null, "Path to outside should be null.")

        // build one way road
        flatMap.buildRoad(startBlock, endBlock, true)

        assertTrue(
            pathfinder.tripTo(startList, endList)?.blocks()?.count() == 51,
            "Path to outside should be 51 nodes."
        )
        assertTrue(pathfinder.tripTo(endList, startList) == null, "Path going wrong way should be null.")

        // build one way road branching off
        flatMap.buildRoad(branchStartBlock, branchEndBlock, true)

        assertTrue(
            pathfinder.tripTo(branchStartList, branchEndList)?.blocks()?.count() == 27,
            "Branch path totalScore should be 27 nodes."
        )
        assertTrue(
            pathfinder.tripTo(branchEndList, branchStartList) == null,
            "Branch path going wrong way should be null."
        )

        // Test turning right at crossroads works
        assertTrue(
            pathfinder.tripTo(startList, branchEndList)?.blocks()?.count() == 51,
            "Main to branch path totalScore should be 51 nodes."
        )
    }

    @Test
    fun tortureTest() {
        val flatMap = CityMap.flatMap(100, 100)
        val assetManager = AssetManager(flatMap)
        // ok.. we need stripes of road
        for (i in 0..10 step 2) {
            flatMap.buildRoad(BlockCoordinate(0, i), BlockCoordinate(90, i))
            for (j in 0..40 step 2) {
                val slum1 = assetManager.buildingFor(Residential::class, "slum1")
                flatMap.build(slum1, BlockCoordinate(j, i + 1))
            }

            for (j in 60..90 step 2) {
                val factory = assetManager.buildingFor(Industrial::class, "small_factory")
                flatMap.build(factory, BlockCoordinate(j, i + 1))
            }
        }

        // gotta make a vertical road that cuts across the whole thing...
        flatMap.buildRoad(BlockCoordinate(50, 0), BlockCoordinate(50, 100))

        // now get all industrial zones... they MUST NOT have all labor populated...
        flatMap.eachLocation { location ->
            if (location.building is Industrial) {
                assert(location.building.currentQuantityWanted(Tradeable.LABOR) != 0)
            }
        }

        val contractFulfiller = ContactFulfiller(flatMap)
        contractFulfiller.debug = true
        contractFulfiller.signContracts(false, maxMillis = 50000, parallel = false)

        // now get all industrial zones... they MUST have all labor populated...

        var numberWithLabor = 0.0
        var numberWithoutLabor = 0.0

        flatMap.eachLocation { location ->
            if (location.building is Industrial) {
                if (location.building.currentQuantityWanted(Tradeable.LABOR) == 0) {
                    numberWithLabor += 1
                } else {
                    numberWithoutLabor += 1
                }
            }
        }

        assertTrue(numberWithoutLabor == 0.0, "Expected 0 to need labor but $numberWithoutLabor still need it.")
    }

    @Test
    fun testRailroadsNoStations() {
        val aBlock = BlockCoordinate(10, 10)
        val bBlock = BlockCoordinate(20, 10)

        val flatMap = CityMap.flatMap(100, 100)
        val pathfinder = Pathfinder(flatMap)
        pathfinder.debug = true

        flatMap.buildRailroad(aBlock, bBlock)

        val trip = pathfinder.tripTo(listOf(aBlock), listOf(bBlock))
        assertTrue(trip == null, "Path from a to b should not exist.")
    }

    @Test
    fun testRailroadsWithStartStation() {
        val aBlock = BlockCoordinate(10, 10)
        val bBlock = BlockCoordinate(20, 10)
        val oneBelowStart = aBlock.bottom()
        val twoBelowStart = oneBelowStart.bottom()

        val flatMap = CityMap.flatMap(100, 100)
        val pathfinder = Pathfinder(flatMap)
        pathfinder.debug = true

        flatMap.buildRoad(twoBelowStart, twoBelowStart)
        flatMap.buildRailroad(aBlock, bBlock)
        flatMap.build(TrainStation(), oneBelowStart)

        val trip = pathfinder.tripTo(listOf(twoBelowStart), listOf(bBlock.top()))
        if (trip != null) {
            println("TRIP: ")
            trip.nodes.forEach { node ->
                println("${node.coordinate} ${node.transitType}")
            }
        }
        assertTrue(trip == null, "Path from a to b should not exist.")
    }

    @Test
    fun testRailroadsWithStartAndEndStations() {
        val aBlock = BlockCoordinate(10, 10)
        val oneBelowStart = aBlock.bottom()
        val fourBelowStart = oneBelowStart.bottom().bottom().bottom()
        val bBlock = BlockCoordinate(20, 10)
        val oneBelowEnd = bBlock.bottom()

        val flatMap = CityMap.flatMap(100, 100)
        val pathfinder = Pathfinder(flatMap)
        pathfinder.debug = true

        flatMap.buildRoad(fourBelowStart, fourBelowStart)
        flatMap.buildRailroad(aBlock, bBlock)
        flatMap.build(TrainStation(), oneBelowStart)
        flatMap.build(TrainStation(), oneBelowEnd)

        val trip = pathfinder.tripTo(listOf(fourBelowStart), listOf(oneBelowEnd))
        assertTrue(trip?.blocks()?.count() == 16, "Path from a to b should be length 16.")

    }
}
