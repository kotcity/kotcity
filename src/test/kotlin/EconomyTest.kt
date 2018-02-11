import kotcity.data.*
import kotcity.data.assets.AssetManager
import org.junit.jupiter.api.Test

class EconomyTest {
    @Test
    fun economyTest() {
        val assetManager = AssetManager()
        val jobCenter = assetManager.buildingFor(BuildingType.CIVIC, "job_center")
        assert(jobCenter.sellingTradeable(Tradeable.LABOR, 10))
        val industrialBuilding = assetManager.buildingFor(BuildingType.INDUSTRIAL, "small_factory")
        jobCenter.createContract(industrialBuilding, Tradeable.LABOR, 10)
        assert(!jobCenter.sellingTradeable(Tradeable.LABOR, 1))
        // now void the contracts...
        jobCenter.voidContractsWith(industrialBuilding)
        assert(jobCenter.sellingTradeable(Tradeable.LABOR, 1))
    }
}