import kotcity.data.BlockCoordinate
import kotcity.data.CityMap
import kotcity.pathfinding.Pathfinder
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PathfinderTest {
    @Test
    fun testPathfinder() {
        val flatMap = CityMap.flatMap(100, 100)
        val pathfinder = Pathfinder(flatMap)
        var outsidePath = pathfinder.pathToOutside(listOf(BlockCoordinate(50, 50)))
        assertTrue(outsidePath == null, "Path to outside should not be null")
        // let's build a road right across...
        flatMap.buildRoad(BlockCoordinate(0, 50), BlockCoordinate(100, 50))
        outsidePath = pathfinder.pathToOutside(listOf(BlockCoordinate(50, 50)))
        assertTrue(outsidePath != null, "Path to outside should not be null")
    }
}