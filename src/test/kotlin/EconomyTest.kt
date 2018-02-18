import kotcity.data.*
import kotcity.data.assets.AssetManager
import org.junit.jupiter.api.Test

class EconomyTest {
    @Test
    fun economyTest() {
        val map = CityMap.flatMap(256,256)
        val assetManager = AssetManager(map)
        val jobCenter = assetManager.buildingFor(BuildingType.CIVIC, "job_center")
        assert(jobCenter.quantityForSale(Tradeable.LABOR) >= 2)
        val industrialBuilding = assetManager.buildingFor(BuildingType.INDUSTRIAL, "small_factory")
        jobCenter.createContract(industrialBuilding, Tradeable.LABOR, 2)
        println("Jobcenter: ${jobCenter.quantityForSale(Tradeable.LABOR) }")
        assert(jobCenter.quantityForSale(Tradeable.LABOR) == 2)
        // now void the contracts...
        jobCenter.voidContractsWith(industrialBuilding)
        assert(jobCenter.quantityForSale(Tradeable.LABOR) >= 1)
    }
}