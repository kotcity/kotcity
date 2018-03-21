
import kotcity.data.CityMap
import kotcity.data.AssetManager
import org.junit.jupiter.api.Test

class AssetManagerTest {
    @Test
    fun testassetManagerScan() {
        val flatMap = CityMap.flatMap(128, 128)
        val assetManager = AssetManager(flatMap)
        val resources = assetManager.findResources()
        assert(resources.count() > 0)
    }

    @Test
    fun testAllAssets() {
        val flatMap = CityMap.flatMap(128, 128)
        val assetManager = AssetManager(flatMap)
        assetManager.all().forEach { building ->
            println("Loaded asset: ${building.name}")
            println("Produces: ${building.produces}")
            println("Consumes: ${building.consumes}")
        }
    }
}