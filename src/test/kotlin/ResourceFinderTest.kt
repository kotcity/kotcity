import kotcity.automata.ResourceFinder
import kotcity.data.*
import kotcity.data.AssetManager
import kotcity.data.buildings.Industrial
import kotcity.data.buildings.Residential
import org.junit.jupiter.api.Test

class ResourceFinderTest {
    @Test fun findSourceTest() {
        val cityMap = CityMap.flatMap(24, 24)
        val assetManager = AssetManager(cityMap)
        val slum1 = assetManager.buildingFor(Residential::class, "slum1")
        val factory1 = assetManager.buildingFor(Industrial::class, "large_factory")
        cityMap.buildRoad(BlockCoordinate(2, 0), BlockCoordinate(2, 23))
        cityMap.build(factory1, BlockCoordinate(0, 5))
        val resourceFinder = ResourceFinder(cityMap)
        val result = resourceFinder.findSource(listOf(BlockCoordinate(0,0)), Tradeable.LABOR, 1)
        assert(result == null)
        cityMap.build(slum1, BlockCoordinate(0, 0))
        val result2 = resourceFinder.findSource(listOf(BlockCoordinate(0,0)), Tradeable.LABOR, 1)
        assert(result2 != null)
    }
}