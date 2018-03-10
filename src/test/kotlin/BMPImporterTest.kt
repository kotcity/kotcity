
import kotcity.data.BMPImporter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BMPImporterTest {
    @Test
    fun importMapTest() {
        val bmpImporter = BMPImporter()
        val cityMap = bmpImporter.load("./test_data/map.bmp")
        Assertions.assertNotNull(cityMap)
    }
}