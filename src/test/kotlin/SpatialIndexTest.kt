import kotcity.data.*
import kotcity.data.buildings.PowerPlant
import org.junit.jupiter.api.Test

class SpatialIndexTest {
    @Test
    fun testSpatialIndex() {

        val map = CityMap.flatMap(512, 512)

        // now let's drop a coal power plant...
        val powerPlant1 = PowerPlant("coal")
        val powerPlant2 = PowerPlant("coal")

        map.build(powerPlant1, BlockCoordinate(0, 0))
        map.build(powerPlant2, BlockCoordinate(10, 10))

        val nearestBuildings = map.nearestBuildings(BlockCoordinate(0, 0), 100)
        println("Nearest buildings: $nearestBuildings")
        assert(nearestBuildings.count() > 1)
    }
}
