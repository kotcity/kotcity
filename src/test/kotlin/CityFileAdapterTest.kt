
import kotcity.automata.ContactFulfiller
import kotcity.data.*
import kotcity.data.assets.AssetManager
import org.junit.jupiter.api.Test
import java.io.File

class CityFileAdapterTest {
    @Test
    fun testCityFileAdapterSaveAndLoad() {
        val map = CityMap.flatMap(100, 100)
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
        assert(loadedCity.width == 100)
    }

    @Test
    fun testSaveCivicBuildings() {
        val map = CityMap.flatMap(100, 100)
        val assetManager = AssetManager(map)
        map.cityName = "CivicVille"

        val contractFulfiller = ContactFulfiller(map)

        val jobCenter = assetManager.buildingFor(Civic::class, "job_center")
        map.build(jobCenter, BlockCoordinate(0,0))

        // let's drop some kind of industrial building now...
        val factory = assetManager.buildingFor(Industrial::class, "small_factory")
        map.build(factory, BlockCoordinate(3, 1))

        map.buildRoad(BlockCoordinate(0, 2), BlockCoordinate(10, 2))

        contractFulfiller.signContracts()

        val tmpFile = File.createTempFile("testcity", ".kcity")
        CityFileAdapter.save(map, tmpFile)

        val loadedCity = CityFileAdapter.load(tmpFile)

        val loadedJobCenter = loadedCity.cachedBuildingsIn(BlockCoordinate(0,0)).first().building
        println("Job center: ${loadedJobCenter.summarizeContracts()}")
        println("How much labor: ${loadedJobCenter.quantityForSale(Tradeable.LABOR)}")

        assert(loadedCity.width == 100)
    }
}