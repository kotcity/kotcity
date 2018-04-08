import kotcity.data.*
import kotcity.data.AssetManager
import org.junit.jupiter.api.Test

class EconomyTest {
    @Test
    fun economyTest() {
        val map = CityMap.flatMap(256,256)
        val assetManager = AssetManager(map)
        val jobCenter = assetManager.buildingFor(Civic::class, "job_center")
        assert(jobCenter.currentQuantityForSale(Tradeable.LABOR) >= 2)
        val industrialBuilding = assetManager.buildingFor(Industrial::class, "small_factory")
        val industrialTrader = CityTradeEntity(BlockCoordinate(0, 0), industrialBuilding)
        jobCenter.createContract(map, industrialTrader, Tradeable.LABOR, 2, null)
        println("Jobcenter: ${jobCenter.currentQuantityForSale(Tradeable.LABOR) }")
        assert(jobCenter.currentQuantityForSale(Tradeable.LABOR) == 2)
        // now void the contracts...
        jobCenter.voidContractsWith(industrialTrader)
        assert(jobCenter.currentQuantityForSale(Tradeable.LABOR) >= 1)
        map.tick()
    }
}
