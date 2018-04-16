import kotcity.data.*
import kotcity.data.AssetManager
import kotcity.data.buildings.Residential
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CollisionTest {
    @Test
    fun collisionTest() {
        val map = CityMap.flatMap(512, 512)
        val assetManager = AssetManager(map)
        val slum1 = assetManager.buildingFor(Residential::class, "slum1")
        val slum2 = assetManager.buildingFor(Residential::class, "slum1")
        val slum3 = assetManager.buildingFor(Residential::class, "slum1")
        val slum4 = assetManager.buildingFor(Residential::class, "slum1")

        map.build(slum1, BlockCoordinate(0, 0))
        map.build(slum2, BlockCoordinate(0, 2))
        map.build(slum3, BlockCoordinate(0, 4))
        // try to do an overlap
        map.build(slum4, BlockCoordinate(0, 0))
        assertTrue(map.locations().count() == 3)

        // let's test locations in...
        // slum1 should be at 0,0 0,1, 1,0, 1,1
        assert(map.locationsAt(BlockCoordinate(0,0 )).first().building == slum1)
        assert(map.locationsAt(BlockCoordinate(1,0 )).first().building == slum1)
        assert(map.locationsAt(BlockCoordinate(0,1 )).first().building == slum1)
        assert(map.locationsAt(BlockCoordinate(1,1 )).first().building == slum1)
        // this should not be true...
        assert(!map.locationsAt(BlockCoordinate(1,2 )).map { it.building}.contains(slum1) )
    }
}