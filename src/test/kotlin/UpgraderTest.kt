import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import kotcity.automata.Upgrader
import kotcity.data.*
import org.junit.jupiter.api.Test

class UpgraderTest {
    @Test
    fun upgraderTest() {
        val map = CityMap.flatMap(256, 256)
        val assetManager = AssetManager(map)
        // map.buildRoad(BlockCoordinate(0, 2), BlockCoordinate(10, 2))
        // drop a slum1
        map.zone(Zone.RESIDENTIAL, BlockCoordinate(0, 0), BlockCoordinate(2, 2))
        val building = assetManager.buildingFor(Residential::class, "slum1")
        map.build(building, BlockCoordinate(0, 0))
        building.goodwill = 100
        val upgrader = Upgrader(map)
        upgrader.debug = true
        map.debug = true
        assertFalse( map.locations().all { it.building.level == 2 })
        assertTrue(map.locations().count() == 1)
        upgrader.tick()
        assertTrue(map.locations().count() == 1)
        val allLocations = map.locations().all { it.building.level == 2 }
        assertTrue(allLocations)
    }
}
