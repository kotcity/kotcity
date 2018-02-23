import kotcity.data.CityMap
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CensusTest {
    @Test
    fun testCensus() {
        val flatMap = CityMap.flatMap(128, 128)
        flatMap.censusTaker.tick()
        assertTrue(flatMap.censusTaker.resourceCounts.totals().count() > 0)
    }
}
