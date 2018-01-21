
import kotcity.data.CityFileAdapter
import kotcity.data.MapGenerator
import org.junit.jupiter.api.Test
import java.io.File

class CityFileAdapterTest {
    @Test
    fun testCityFileAdapterSave() {
        val mapGenerator = MapGenerator()
        val map = mapGenerator.generateMap(4, 4)
        val tmpFile = File.createTempFile("testcity", ".kcity")
        CityFileAdapter.save(map, tmpFile)
    }
}