import kotcity.automata.ContactFulfiller
import kotcity.automata.DesirabilityUpdater
import kotcity.automata.Manufacturer
import kotcity.automata.Shipper
import kotcity.data.*
import kotcity.data.AssetManager
import kotcity.data.Tunable.DEFAULT_DESIRABILITY
import kotcity.data.buildings.Civic
import kotcity.data.buildings.Commercial
import kotcity.data.buildings.Industrial
import kotcity.data.buildings.Residential
import kotcity.pathfinding.Pathfinder
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class DesirabilityTest {
    @Test
    fun desirabilityTest() {
        val map = CityMap.flatMap(1024, 1024)

        val pathfinder = Pathfinder(map)
        val desirabilityUpdater = DesirabilityUpdater(map)

        val assetManager = AssetManager(map)
        // ok what we want to do here is drop a job center at 0,0
        val jobCenter = assetManager.buildingFor(Civic::class, "job_center")
        map.build(jobCenter, BlockCoordinate(3, 0))

        val warehouse = assetManager.buildingFor(Civic::class, "town_warehouse")
        map.build(warehouse, BlockCoordinate(3, 2))

        // now let's drop a road...
        map.buildRoad(BlockCoordinate(2, 0), BlockCoordinate(2, 30))
        // now we gotta make some industrial zone...
        map.zone(Zone.INDUSTRIAL, BlockCoordinate(3, 0), BlockCoordinate(5, 30))
        map.zone(Zone.RESIDENTIAL, BlockCoordinate(0, 10), BlockCoordinate(2, 12))
        map.zone(Zone.COMMERCIAL, BlockCoordinate(0, 20), BlockCoordinate(1, 25))

        // let's plop a small house down...
        val slum = assetManager.buildingFor(Residential::class, "slum1")
        map.build(slum, BlockCoordinate(0, 5))

        val slum2 = assetManager.buildingFor(Residential::class, "slum1")
        map.build(slum, BlockCoordinate(0, 7))

        val factory = assetManager.buildingFor(Industrial::class, "small_factory")
        map.build(factory, BlockCoordinate(1, 15))

        val cornerStore = assetManager.buildingFor(Commercial::class, "corner_store")
        map.build(cornerStore, BlockCoordinate(3, 5))

        val path = pathfinder.pathToNearestLabor(listOf(BlockCoordinate(3, 10)))

        if (path == null) {
            fail<Nothing>("The path was null...")
        }

        val pathToJob = pathfinder.pathToNearestJob(listOf(BlockCoordinate(3, 10)))

        if (pathToJob == null) {
            fail<Nothing>("The path was null...")
        }

        // ok now let's make sure the desirability is actually kosher...
        desirabilityUpdater.tick()

        listOf(Zone.INDUSTRIAL, Zone.RESIDENTIAL, Zone.COMMERCIAL).forEach { zt ->
            var nonDefaultFound = false
            map.desirabilityLayer(zt, 1)?.forEach { _, d ->
                if (d != DEFAULT_DESIRABILITY) {
                    nonDefaultFound = true
                }
            }
            assertTrue(nonDefaultFound, "Error setting desirability for $zt")
        }

        assertTrue(slum.currentQuantityForSale(Tradeable.LABOR) >= 2, "Has no labor...")

        val oldSlumValue = slum.currentQuantityForSale(Tradeable.LABOR)
        val oldSlum2Value = slum.currentQuantityForSale(Tradeable.LABOR)

        // OK... the factory OR corner shore should get some workers...
        val oldFactoryValue = factory.totalBeingBought(Tradeable.LABOR)
        val oldCornerStoreValue = factory.totalBeingBought(Tradeable.LABOR)

        val cf = ContactFulfiller(map)
        cf.debug = true
        cf.signContracts(shuffled = false, maxMillis = 5000)

        // ok now let's make sure the desirability is actually kosher...
        desirabilityUpdater.tick()

        assertTrue(slum.currentQuantityForSale(Tradeable.LABOR) != oldSlumValue || slum2.currentQuantityForSale(Tradeable.LABOR) != oldSlum2Value, "Expected labor available to change...")
        assertTrue(factory.totalBeingBought(Tradeable.LABOR) != oldFactoryValue || cornerStore.totalBeingBought(Tradeable.LABOR) != oldCornerStoreValue, "Expected consumed labor to change...")

        val manufacturer = Manufacturer(map)
        manufacturer.debug = true
        manufacturer.tick()

        val shipper = Shipper(map)
        shipper.debug = true
        shipper.tick()

    }
}