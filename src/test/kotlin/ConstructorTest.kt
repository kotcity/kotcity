import kotcity.data.*
import kotcity.data.assets.AssetManager
import org.junit.jupiter.api.Test

class ConstructorTest {
    @Test
    fun constructorTest() {
        val assetManager = AssetManager()
        val all = assetManager.all()
        assert(all.count() > 0)
        val industrial = all.filter { it.type == BuildingType.INDUSTRIAL }
        assert(industrial.count() > 0)
        industrial.forEach { assert(it.sprite != null && it.sprite != "") }
    }
}