
import kotcity.data.CityFileAdapter
import kotcity.data.MapGenerator
import kotcity.data.TileType
import org.junit.jupiter.api.Test
import java.io.File

class CityFileAdapterTest {
    @Test
    fun testCityFileAdapterSaveAndLoad() {
        val mapGenerator = MapGenerator()
        val map = mapGenerator.generateMap(100, 100)
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
}