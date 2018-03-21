import kotcity.automata.ContactFulfiller
import kotcity.data.*
import kotcity.data.AssetManager
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
        assertTrue(outsidePath == null, "Path to outside should not be null")
        // let's build a road right across...
        flatMap.buildRoad(BlockCoordinate(0, 50), BlockCoordinate(100, 50))
        pathfinder.purgeCaches()
        outsidePath = pathfinder.pathToOutside(listOf(BlockCoordinate(50, 50)))?.blocks()
        assertTrue(outsidePath != null, "Path to outside should not be null")
        assertTrue(outsidePath?.count() == 51, "Path to outside should be 51 nodes!")
    }

    @Test fun tortureTest() {
        val flatMap = CityMap.flatMap(100, 100)
        val assetManager = AssetManager(flatMap)
        // ok.. we need stripes of road
        for (i in 0 .. 10 step 2) {
            flatMap.buildRoad(BlockCoordinate(0, i), BlockCoordinate(90, i))
            for (j in 0 .. 40 step 2) {
                val slum1 = assetManager.buildingFor(Residential::class, "slum1")
                flatMap.build(slum1, BlockCoordinate(j, i+1))
            }

            for (j in 60 .. 90 step 2) {
                val factory = assetManager.buildingFor(Industrial::class, "small_factory")
                flatMap.build(factory, BlockCoordinate(j, i+1))
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
        contractFulfiller.debug = false
        contractFulfiller.signContracts(false, maxMillis = 50000)

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

        val needsLaborRatio = numberWithoutLabor / numberWithLabor
        assertTrue(needsLaborRatio < 0.05, "Expected 95%+ labor fill rate but was $needsLaborRatio")
    }
}