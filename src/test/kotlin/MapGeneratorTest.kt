import kotcity.data.MapGenerator
import org.junit.jupiter.api.Test

class MapGeneratorTest {
    @Test
    fun testMapGen() {
        val mapGenerator = MapGenerator()
        assert(mapGenerator.generateMap(512, 512) != null)
    }
}