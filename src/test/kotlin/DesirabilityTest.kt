import kotcity.data.*
import kotcity.data.assets.AssetManager
import kotcity.pathfinding.Pathfinder
import org.junit.Test

class DesirabilityTest {
    @Test
    fun desirabilityTest() {
        val assetManager = AssetManager()
        val map = CityMap.flatMap(100, 100)
        // ok what we want to do here is drop a job center at 0,0
        val jobCenter = assetManager.buildingFor(BuildingType.CIVIC, "job_center")
        map.build(jobCenter, BlockCoordinate(0,0))

        val warehouse = assetManager.buildingFor(BuildingType.CIVIC, "town_warehouse")
        map.build(warehouse, BlockCoordinate(0, 2))

        // now let's drop a road...
        map.buildRoad(BlockCoordinate(2,0), BlockCoordinate(2, 30))
        // now we gotta make some industrial zone...
        map.zone(ZoneType.INDUSTRIAL, BlockCoordinate(3, 0), BlockCoordinate(5, 30))

        val path = Pathfinder.pathToNearestLabor(map, BlockCoordinate(3, 10))
        assert(path.count() > 0)
    }
}