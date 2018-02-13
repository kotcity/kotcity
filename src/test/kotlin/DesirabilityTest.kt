import kotcity.data.*
import kotcity.data.assets.AssetManager
import kotcity.pathfinding.Pathfinder
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class DesirabilityTest {
    @Test
    fun desirabilityTest() {
        val assetManager = AssetManager()
        val map = CityMap.flatMap(1024, 1024)
        // ok what we want to do here is drop a job center at 0,0
        val jobCenter = assetManager.buildingFor(BuildingType.CIVIC, "job_center")
        map.build(jobCenter, BlockCoordinate(3,0))

        val warehouse = assetManager.buildingFor(BuildingType.CIVIC, "town_warehouse")
        map.build(warehouse, BlockCoordinate(3, 2))

        // now let's drop a road...
        map.buildRoad(BlockCoordinate(2,0), BlockCoordinate(2, 30))
        // now we gotta make some industrial zone...
        map.zone(ZoneType.INDUSTRIAL, BlockCoordinate(3, 0), BlockCoordinate(5, 30))
        map.zone(ZoneType.RESIDENTIAL, BlockCoordinate(0, 10), BlockCoordinate(2, 12))
        map.zone(ZoneType.COMMERCIAL, BlockCoordinate(0, 20), BlockCoordinate(1, 25))

        // let's plop a small house down...
        val slum = assetManager.buildingFor(BuildingType.RESIDENTIAL, "slum1")
        map.build(slum, BlockCoordinate(0, 5))

        val factory = assetManager.buildingFor(BuildingType.INDUSTRIAL, "small_factory")
        map.build(factory, BlockCoordinate(1, 15))

        val path = Pathfinder.pathToNearestLabor(map, listOf(BlockCoordinate(3, 10)))

        if (path == null) {
            fail("The path was null...")
        }

        val pathToJob = Pathfinder.pathToNearestJob(map, listOf(BlockCoordinate(3, 10)))

        if (pathToJob == null) {
            fail("The path was null...")
        }

        // ok now let's make sure the desirability is actually kosher...
        DesirabilityUpdater.update(map)

        listOf(ZoneType.INDUSTRIAL, ZoneType.RESIDENTIAL, ZoneType.COMMERCIAL).forEach { zt ->
            var nonDefaultFound = false
            map.desirabilityLayer(zt, 1)?.forEach { _, d ->
                if (d != DEFAULT_DESIRABILITY) {
                    nonDefaultFound = true
                }
            }
            assertTrue(nonDefaultFound, "Error setting desirability for $zt")
        }

    }
}