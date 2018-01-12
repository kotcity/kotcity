import org.junit.jupiter.api.Test

class MapGeneratorTest {
    @Test
    fun testMapGen() {
        assert(MapGenerator().generateMap() == 1)
    }
}