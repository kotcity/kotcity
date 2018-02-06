
import kotcity.data.assets.AssetManager
import org.junit.jupiter.api.Test

class AssetManagerTest {
    @Test
    fun testassetManagerScan() {
        val assetManager = AssetManager()
        val resources = assetManager.findResources()
        assert(resources.count() > 0)
    }

    @Test
    fun testAllAssets() {
        val assetManager = AssetManager()
        assetManager.all().forEach { building ->
            println("Loaded asset: ${building.name}")
            println("Produces: ${building.produces}")
            println("Consumes: ${building.consumes}")
        }
    }
}