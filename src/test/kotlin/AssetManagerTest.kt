import kotcity.data.AssetManager
import kotcity.data.CityMap
import org.junit.Assert.assertNotNull
import org.junit.jupiter.api.Test
import java.io.File


class AssetManagerTest {
    @Test
    fun testAssetManagerScan() {
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

    @Test
    fun testLoadSingleBuilding() {
        // let's make sure we can load a building from our JSON
        val flatMap = CityMap.flatMap(128, 128)
        val assetManager = AssetManager(flatMap)
        val testAssetFile = File(this.javaClass.getResource("/cheap_house.json").file)
        val building = assetManager.loadFromFile(testAssetFile.absolutePath)
        // if we live here... the building loaded fine. if we get null OR an exception is thrown... we are in trouble!
        assertNotNull(building)
    }
}
