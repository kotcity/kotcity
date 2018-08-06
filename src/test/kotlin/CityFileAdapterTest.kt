
import kotcity.automata.ContactFulfiller
import kotcity.data.*
import kotcity.data.AssetManager
import kotcity.data.buildings.Civic
import kotcity.data.buildings.Industrial
import org.junit.jupiter.api.Test
import java.io.File

const val citySize = 50

class CityFileAdapterTest {

    @Test
    fun testCityFileAdapterSaveAndLoad() {
        println("Start save and load test...")
        val map = CityMap.flatMap(citySize, citySize)
        // need to find some land...
        val tileEntry = map.groundLayer.asIterable().find { entry ->
            entry.value.type == TileType.GROUND
        }
        if (tileEntry != null) {
            val coordinate = tileEntry.key
            map.buildRoad(coordinate, coordinate)
        }
        map.cityName = "Unit Testville"
        val tmpFile = File.createTempFile("testcity", ".kcity")
        CityFileAdapter.save(map, tmpFile)

        val loadedCity = CityFileAdapter.load(tmpFile)
        assert(loadedCity.width == citySize)
    }

    @Test
    fun testSaveCivicBuildings() {
        println("Starting save civic building test...")
        val map = CityMap.flatMap(citySize, citySize)
        val assetManager = AssetManager(map)
        map.cityName = "CivicVille"

        val contractFulfiller = ContactFulfiller(map)
        contractFulfiller.debug = true

        val jobCenter = assetManager.buildingFor(Civic::class, "job_center")
        map.build(jobCenter, BlockCoordinate(0,0))

        // let's drop some kind of industrial building now...
        val factory = assetManager.buildingFor(Industrial::class, "small_factory")
        map.build(factory, BlockCoordinate(3, 1))

        map.buildRoad(BlockCoordinate(0, 2), BlockCoordinate(10, 2))

        println("Preparing to sign contracts...")
        contractFulfiller.signContracts(maxMillis = 5000)
        println("Contract signing complete!")

        val tmpFile = File.createTempFile("testcity", ".kcity")
        println("Saving city!")
        CityFileAdapter.save(map, tmpFile)

        println("Now loading city")
        val loadedCity = CityFileAdapter.load(tmpFile)

        val loadedJobCenter = loadedCity.cachedLocationsIn(BlockCoordinate(0,0)).first().building
        println("Job center: ${loadedJobCenter.summarizeContracts()}")
        println("How much labor: ${loadedJobCenter.currentQuantityForSale(Tradeable.LABOR)}")

        assert(loadedCity.width == citySize)
    }
}