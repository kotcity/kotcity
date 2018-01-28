
import kotcity.data.CityFileAdapter
import kotcity.data.MapGenerator
import kotcity.data.TileType
import kotcity.data.assets.AssetManager
import org.junit.jupiter.api.Test
import java.io.File

class AssetManagerTest {
    @Test
    fun testassetManagerScan() {
        val assetManager = AssetManager()
        val resources = assetManager.findResources()
        assert(resources.count() > 0)
    }
}