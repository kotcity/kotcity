
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
        val assetManager = AssetManager()
        map.cityName = "CivicVille"

        val jobCenter = assetManager.buildingFor(BuildingType.CIVIC, "job_center")
        map.build(jobCenter, BlockCoordinate(0,0))

        val tmpFile = File.createTempFile("testcity", ".kcity")
        CityFileAdapter.save(map, tmpFile)

        val loadedCity = CityFileAdapter.load(tmpFile)
        assert(loadedCity.width == 100)
    }
}